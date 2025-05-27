package com.example.weatherwise.features.mainscreen.viewmodel

import android.net.ConnectivityManager
import android.util.Log
import androidx.lifecycle.*
import com.example.weatherwise.data.model.domain.*
import com.example.weatherwise.data.repository.IWeatherRepository
import com.example.weatherwise.features.settings.model.PreferencesManager
import com.example.weatherwise.utils.LocationHelper
import com.example.weatherwise.utils.LocationHelperDelegate
import com.example.weatherwise.utils.NetworkStatusChecker
import com.example.weatherwise.utils.WeatherDataProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
    private val repository: IWeatherRepository,
    private val locationHelper: LocationHelper,
    connectivityManager: ConnectivityManager
) : ViewModel() {

    // LiveData declarations
    private val _locationData = MutableLiveData<LocationData>()
    val locationData: LiveData<LocationData> = _locationData

    private val _weatherData = MutableLiveData<WeatherData>()
    val weatherData: LiveData<WeatherData> = _weatherData

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _isTemporaryLocation = MutableLiveData<Boolean>(false)
    val isTemporaryLocation: LiveData<Boolean> = _isTemporaryLocation

    // Helper classes
    private val networkStatusChecker = NetworkStatusChecker(connectivityManager)
    private val weatherDataProcessor = WeatherDataProcessor()
    private val locationHelperDelegate = LocationHelperDelegate(
        locationHelper = locationHelper,
        repository = repository,
        viewModelScope = viewModelScope,
        onLocationData = { locationData ->
            _locationData.postValue(locationData)
            fetchWeatherData(locationData.latitude, locationData.longitude, forceRefresh = false)
        },
        onError = { message -> _error.postValue(message) },
        onLoading = { isLoading -> _loading.postValue(isLoading) }
    )

    // Location-related functions
    fun checkLocationPermissions(): Boolean = locationHelperDelegate.checkLocationPermissions()
    fun isLocationEnabled(): Boolean = locationHelperDelegate.isLocationEnabled()
    fun enableLocationServices() = locationHelperDelegate.enableLocationServices()

    fun getFreshLocation() {
        when (repository.getLocationMethod()) {
            PreferencesManager.LOCATION_MANUAL -> locationHelperDelegate.handleManualLocation()
            PreferencesManager.LOCATION_GPS -> locationHelperDelegate.handleGpsLocation()
        }
    }

    fun loadTemporaryLocation(locationId: String) {
        _isTemporaryLocation.value = true
        viewModelScope.launch {
            safeWeatherCall {
                repository.getLocationWithWeather(locationId)?.let { data ->
                    postWeatherAndLocation(data.location.latitude, data.location.longitude, data)
                } ?: throw IllegalStateException("Location not found")
            }
        }
    }

    fun refreshCurrentWeather() {
        refreshCurrentWeatherWithLocation()
    }

    fun refreshWeatherData() = fetchWeatherData(forceRefresh = true)

    private fun refreshCurrentWeatherWithLocation() {
        viewModelScope.launch {
            safeWeatherCall {
                val locationId = repository.getCurrentLocationId()
                    ?: throw IllegalStateException("No current location set")

                Log.d("HomeViewModel", "Refreshing location $locationId")
                if (networkStatusChecker.isNetworkAvailable() && !repository.refreshLocation(locationId)) {
                    throw IllegalStateException("Refresh failed")
                }

                repository.getCurrentLocationWithWeather(
                    forceRefresh = false,
                    isNetworkAvailable = networkStatusChecker.isNetworkAvailable()
                )?.let { data ->
                    postWeatherAndLocation(data.location.latitude, data.location.longitude, data)
                } ?: throw IllegalStateException("No weather data available")
            }
        }
    }

    private fun fetchWeatherData(lat: Double? = null, lon: Double? = null, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val currentLat = lat ?: _locationData.value?.latitude
                val currentLon = lon ?: _locationData.value?.longitude

                if (currentLat == null || currentLon == null) {
                    throw IllegalStateException("No location available")
                }

                repository.setCurrentLocation(currentLat, currentLon)
                val isNetworkAvailable = networkStatusChecker.isNetworkAvailable()

                val weatherData = repository.getCurrentLocationWithWeather(
                    forceRefresh = forceRefresh,
                    isNetworkAvailable = isNetworkAvailable
                )

                if (weatherData != null) {
                    postWeatherAndLocation(currentLat, currentLon, weatherData)
                    if (!isNetworkAvailable) {
                        _error.value = "Showing cached data (offline)"
                    }
                } else {
                    throw IllegalStateException(
                        if (isNetworkAvailable) "Failed to load weather data"
                        else "No cached data available"
                    )
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Weather data error", e)
                _error.value = when {
                    e.message?.contains("No cached data") == true -> "No data available (offline)"
                    else -> e.message ?: "Error loading weather data"
                }
            } finally {
                _loading.value = false
            }
        }
    }

    private fun postWeatherAndLocation(lat: Double, lon: Double, data: LocationWithWeather) {
        val currentTime = System.currentTimeMillis() / 1000L
        val hourlyForecast = weatherDataProcessor.processHourlyForecast(data.forecast, currentTime)
        val dailyForecast = weatherDataProcessor.processDailyForecast(data.forecast)

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
        } catch (e: IllegalStateException) {
            Log.e("HomeViewModel", "Weather error: ${e.message}", e)
            _error.value = when {
                e.message?.contains("offline") == true -> "You're offline - showing cached data"
                e.message?.contains("No location") == true -> "Location not available"
                else -> e.message ?: "An error occurred"
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Unexpected error", e)
            _error.value = if (networkStatusChecker.isNetworkAvailable()) {
                "Error: ${e.message ?: "Please try again"}"
            } else {
                "You're offline - showing cached data"
            }
        } finally {
            _loading.value = false
        }
    }

    fun loadFavoriteDetails(locationId: String) {
        viewModelScope.launch {
            safeWeatherCall {
                repository.getLocationWithWeather(locationId)?.let { data ->
                    _isTemporaryLocation.value = true
                    postWeatherAndLocation(data.location.latitude, data.location.longitude, data)
                } ?: throw IllegalStateException("Location not found")
            }
        }
    }

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