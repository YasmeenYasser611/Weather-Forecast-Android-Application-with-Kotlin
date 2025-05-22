package com.example.weatherwise.mainscreen.viewmodel

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.*
import com.example.weatherwise.data.model.CurrentWeatherResponse
import com.example.weatherwise.data.model.DailyForecast
import com.example.weatherwise.data.model.HourlyForecast
import com.example.weatherwise.data.model.LocationWithWeather
import com.example.weatherwise.data.model.WeatherResponse
import com.example.weatherwise.data.repository.IWeatherRepository
import com.example.weatherwise.location.LocationHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel(private val repository: IWeatherRepository, private val locationHelper: LocationHelper, private val connectivityManager: ConnectivityManager) : ViewModel() {

    private val _locationData = MutableLiveData<LocationData>()
    val locationData: LiveData<LocationData> = _locationData

    private val _weatherData = MutableLiveData<WeatherData>()
    val weatherData: LiveData<WeatherData> = _weatherData

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun checkLocationPermissions(): Boolean = locationHelper.checkPermissions()

    fun isLocationEnabled(): Boolean = locationHelper.isLocationEnabled()

    fun enableLocationServices() = locationHelper.enableLocationServices()

    fun getFreshLocation() {
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

    fun refreshWeatherData() {
        _locationData.value?.let {
            _loading.value = true
            refreshCurrentWeatherWithLocation(it.latitude, it.longitude)
        } ?: run {
            _error.value = "No location available to refresh"
        }
    }

    private fun refreshCurrentWeatherWithLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repository.setCurrentLocation(lat, lon, "metric")
                val freshData = repository.getCurrentLocationWithWeather(forceRefresh = true, isNetworkAvailable = true)

                if (freshData != null) {
                    postWeatherAndLocation(lat, lon, freshData)
                } else {
                    _error.value = "No data after refresh"
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Refresh error", e)
                _error.value = "Refresh error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    private fun fetchWeatherData(lat: Double, lon: Double, forceRefresh: Boolean) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repository.setCurrentLocation(lat, lon, "metric")
                val data = repository.getCurrentLocationWithWeather(
                    forceRefresh = forceRefresh,
                    isNetworkAvailable = isNetworkAvailable()
                )

                if (data != null) {
                    postWeatherAndLocation(lat, lon, data)
                } else {
                    _error.value = "No weather data available"
                }
            } catch (e: Exception) {
                _error.value = "Error fetching weather: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    private fun postWeatherAndLocation(lat: Double, lon: Double, data: LocationWithWeather) {
        val currentTime = System.currentTimeMillis() / 1000L
        val nextDayTime = currentTime + 86400
        val hourFormat = SimpleDateFormat("h a", Locale.getDefault())

        _weatherData.value = WeatherData(
            currentWeather = data.currentWeather,
            forecast = data.forecast,
            hourlyForecast = data.forecast?.list
                ?.filter { it.dt in currentTime..nextDayTime }
                ?.map {
                    HourlyForecast(
                        timestamp = it.dt,
                        temperature = it.main.temp,
                        icon = it.weather.firstOrNull()?.icon,
                        hour = hourFormat.format(Date(it.dt * 1000L)))
                }.orEmpty(),
            dailyForecast = processDailyForecast(data.forecast)
        )

        // Preserve the existing address if we have one, otherwise fall back to API data
        val currentAddress = _locationData.value?.address
        val newAddress = currentAddress ?: data.location.name ?: "${data.location.latitude}, ${data.location.longitude}"

        _locationData.value = LocationData(lat, lon, newAddress)
    }

    fun refreshCurrentWeather() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val locationId = repository.getCurrentLocationId()
                if (locationId == null) {
                    _error.value = "No current location set"
                    return@launch
                }

                Log.d("HomeViewModel", "Refreshing location $locationId")
                val success = repository.refreshLocation(locationId, "metric")
                if (!success) {
                    _error.value = "Refresh failed"
                    return@launch
                }

                val data = repository.getCurrentLocationWithWeather(forceRefresh = false, isNetworkAvailable = true)
                if (data != null) {
                    postWeatherAndLocation(data.location.latitude, data.location.longitude, data)
                } else {
                    _error.value = "No data after refresh"
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Refresh error", e)
                _error.value = "Refresh error: ${e.message}"
            } finally {
                _loading.value = false
                Log.d("HomeViewModel", "Refresh completed")
            }
        }
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
                description = noonEntry?.weather?.firstOrNull()?.description?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            )
        }.sortedBy {
            val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            val now = dateFormat.format(System.currentTimeMillis())
            val todayIndex = days.indexOf(now)
            val index = days.indexOf(it.day)
            (index - todayIndex + 7) % 7
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onCleared() {
        super.onCleared()
        locationHelper.stopLocationUpdates()
    }

    // Data Classes
    data class LocationData(val latitude: Double, val longitude: Double, val address: String)

    data class WeatherData(
        val currentWeather: CurrentWeatherResponse?,
        val forecast: WeatherResponse?,
        val hourlyForecast: List<HourlyForecast>?,
        val dailyForecast: List<DailyForecast>?
    )








}
