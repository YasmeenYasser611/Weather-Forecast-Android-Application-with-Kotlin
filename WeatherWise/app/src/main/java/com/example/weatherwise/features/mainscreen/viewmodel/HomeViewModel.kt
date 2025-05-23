package com.example.weatherwise.features.mainscreen.viewmodel

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.*
import com.example.weatherwise.data.model.domain.*
import com.example.weatherwise.data.model.response.WeatherResponse
import com.example.weatherwise.data.repository.IWeatherRepository
import com.example.weatherwise.features.settings.model.PreferencesManager
import com.example.weatherwise.location.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel(private val repository: IWeatherRepository, private val locationHelper: LocationHelper, private val connectivityManager: ConnectivityManager) : ViewModel() {

    // LiveData declarations
    private val _locationData = MutableLiveData<LocationData>()
    val locationData: LiveData<LocationData> = _locationData

    private val _weatherData = MutableLiveData<WeatherData>()
    val weatherData: LiveData<WeatherData> = _weatherData

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // Location-related functions
    fun checkLocationPermissions(): Boolean = locationHelper.checkPermissions()
    fun isLocationEnabled(): Boolean = locationHelper.isLocationEnabled()
    fun enableLocationServices() = locationHelper.enableLocationServices()

    fun getFreshLocation() {
        when (repository.getLocationMethod()) {
            PreferencesManager.LOCATION_MANUAL -> handleManualLocation()
            PreferencesManager.LOCATION_GPS -> handleGpsLocation()
        }
    }

    private fun handleManualLocation() {
        repository.getManualLocationWithAddress()?.let { (lat, lon, address) ->
            val displayAddress = address.ifEmpty { "Selected Location" }
            _locationData.postValue(LocationData(lat, lon, displayAddress))
            fetchWeatherData(lat, lon, forceRefresh = true)
        } ?: run {
            _error.postValue("No manual location set")
        }
    }

    private fun handleGpsLocation() {
        if (!checkLocationPermissions()) {
            _error.value = "Please grant location permissions"
            return
        }
        if (!isLocationEnabled()) {
            _error.value = "Please enable location services"
            enableLocationServices()
            return
        }

        _loading.value = true
        locationHelper.getFreshLocation(
            onLocationResult = { lat, lon, address ->
                _locationData.postValue(LocationData(lat, lon, address))
                fetchWeatherData(lat, lon, forceRefresh = false)
            },
            onError = { message ->
                _loading.postValue(false)
                _error.postValue(message)
            }
        )
    }

    // Weather data functions
    fun refreshWeatherData() = fetchWeatherData(forceRefresh = true)
    fun refreshCurrentWeather() = refreshCurrentWeatherWithLocation()

    private fun refreshCurrentWeatherWithLocation() {
        viewModelScope.launch {
            safeWeatherCall {
                val locationId = repository.getCurrentLocationId()
                    ?: throw IllegalStateException("No current location set")

                Log.d("HomeViewModel", "Refreshing location $locationId")
                if (!repository.refreshLocation(locationId)) {
                    throw IllegalStateException("Refresh failed")
                }

                repository.getCurrentLocationWithWeather(forceRefresh = false, isNetworkAvailable = true)
                    ?.let { data ->
                        postWeatherAndLocation(data.location.latitude, data.location.longitude, data)
                    } ?: throw IllegalStateException("No data after refresh")
            }
        }
    }

    private fun fetchWeatherData(lat: Double? = null, lon: Double? = null, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            safeWeatherCall {
                val currentLat = lat ?: _locationData.value?.latitude
                val currentLon = lon ?: _locationData.value?.longitude

                if (currentLat == null || currentLon == null) {
                    throw IllegalStateException("No location available")
                }

                repository.setCurrentLocation(currentLat, currentLon)
                repository.getCurrentLocationWithWeather(
                    forceRefresh = forceRefresh,
                    isNetworkAvailable = isNetworkAvailable()
                )?.let { data ->
                    postWeatherAndLocation(currentLat, currentLon, data)
                } ?: throw IllegalStateException("No weather data available")
            }
        }
    }

    // Data processing functions
    private fun postWeatherAndLocation(lat: Double, lon: Double, data: LocationWithWeather) {
        val currentTime = System.currentTimeMillis() / 1000L
        val hourlyForecast = processHourlyForecast(data.forecast, currentTime)
        val dailyForecast = processDailyForecast(data.forecast)

        _weatherData.value = WeatherData(
            currentWeather = data.currentWeather,
            forecast = data.forecast,
            hourlyForecast = hourlyForecast,
            dailyForecast = dailyForecast
        )

        _locationData.value = LocationData(
            lat,
            lon,
            determineBestAddress(data.location.name)
        )
    }

    private fun processHourlyForecast(forecast: WeatherResponse?, currentTime: Long): List<HourlyForecast> {
        val hourFormat = SimpleDateFormat("h a", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val forecastEndTime = currentTime + 172800 // 48 hours in seconds

        return forecast?.list
            ?.sortedBy { it.dt }
            ?.filter { it.dt >= currentTime - 3600 && it.dt <= forecastEndTime }
            ?.take(24)
            ?.map {
                HourlyForecast(
                    timestamp = it.dt,
                    temperature = it.main.temp,
                    icon = it.weather.firstOrNull()?.icon,
                    hour = hourFormat.format(Date(it.dt * 1000L)))
            }
            .orEmpty()
    }

    private fun processDailyForecast(forecast: WeatherResponse?): List<DailyForecast> {
        if (forecast?.list.isNullOrEmpty()) return emptyList()

        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val grouped = forecast!!.list.groupBy {
            calendar.timeInMillis = it.dt * 1000L
            dateFormat.format(calendar.time)
        }

        return grouped.entries.take(5).map { (day, entries) ->
            val high = entries.maxOfOrNull { it.main.temp_max } ?: 0.0
            val low = entries.minOfOrNull { it.main.temp_min } ?: 0.0
            val noonEntry = entries.minByOrNull {
                calendar.timeInMillis = it.dt * 1000L
                kotlin.math.abs(calendar.get(Calendar.HOUR_OF_DAY) - 12)
            }
            DailyForecast(
                day = day,
                highTemperature = high,
                lowTemperature = low,
                icon = noonEntry?.weather?.firstOrNull()?.icon,
                description = noonEntry?.weather?.firstOrNull()?.description?.capitalizeFirst()
            )
        }.sortedByDay(dateFormat)
    }

    // Helper functions
    private fun String.capitalizeFirst() = replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

    private fun List<DailyForecast>.sortedByDay(dateFormat: SimpleDateFormat): List<DailyForecast> {
        val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val now = dateFormat.format(System.currentTimeMillis())
        val todayIndex = days.indexOf(now)

        return sortedBy { forecast ->
            val index = days.indexOf(forecast.day.take(3)) // Take first 3 chars to match "Sun", "Mon", etc.
            (index - todayIndex + 7) % 7
        }
    }

    private fun determineBestAddress(locationName: String?): String {
        return when {
            !_locationData.value?.address.isNullOrEmpty() -> _locationData.value!!.address
            !locationName.isNullOrEmpty() -> locationName
            _locationData.value != null -> "${"%.4f".format(_locationData.value!!.latitude)}, ${"%.4f".format(_locationData.value!!.longitude)}"
            else -> "Unknown Location"
        }
    }

    private suspend fun safeWeatherCall(block: suspend () -> Unit) {
        _loading.value = true
        _error.value = null
        try {
            block()
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Weather error", e)
            _error.value = "Error: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Manual location handling
    fun handleManualLocationSelection(lat: Double, lon: Double) {
        viewModelScope.launch {
            safeWeatherCall {
                val address = withContext(Dispatchers.IO) {
                    try {
                        locationHelper.getAddressFromLocation(lat, lon) { address: String ->
                            address
                        } ?: "Selected Location"
                    } catch (e: Exception) {
                        "Selected Location"
                    }
                }

                repository.setManualLocation(lat, lon, address)
                _locationData.postValue(LocationData(lat, lon, address))
                fetchWeatherData(lat, lon, true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationHelper.stopLocationUpdates()
    }
}