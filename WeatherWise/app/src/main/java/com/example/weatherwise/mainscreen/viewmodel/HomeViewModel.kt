package com.example.weatherwise.mainscreen.viewmodel

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherwise.data.model.CurrentWeatherResponse
import com.example.weatherwise.data.model.WeatherResponse
import com.example.weatherwise.data.repository.IWeatherRepository
import com.example.weatherwise.location.LocationHelper
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: IWeatherRepository,
    private val locationHelper: LocationHelper,
    private val connectivityManager: ConnectivityManager
) : ViewModel() {

    private val _locationData = MutableLiveData<LocationData>()
    val locationData: LiveData<LocationData> = _locationData

    private val _weatherData = MutableLiveData<WeatherData>()
    val weatherData: LiveData<WeatherData> = _weatherData

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun checkLocationPermissions(): Boolean {
        return locationHelper.checkPermissions()
    }

    fun isLocationEnabled(): Boolean {
        return locationHelper.isLocationEnabled()
    }

    fun enableLocationServices() {
        locationHelper.enableLocationServices()
    }

    fun getFreshLocation() {
        if (!checkLocationPermissions()) {
            _error.postValue("Please grant location permissions")
            return
        }
        if (!isLocationEnabled()) {
            _error.postValue("Please enable location services")
            enableLocationServices()
            return
        }

        _loading.postValue(true)
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
        val currentLocation = _locationData.value
        if (currentLocation == null) {
            _error.postValue("No location available to refresh")
            _loading.postValue(false)
            return
        }
        _loading.postValue(true)
        refreshCurrentWeatherWithLocation(currentLocation.latitude, currentLocation.longitude)
    }

    private fun refreshCurrentWeatherWithLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                _loading.postValue(true)
                _error.postValue(null)

                // Force refresh by deleting and recreating the location
                repository.setCurrentLocation(lat, lon, "metric") // This will clear current flag first

                // Get fresh data
                val freshData = repository.getCurrentLocationWithWeather(
                    forceRefresh = true,
                    isNetworkAvailable = true
                )

                freshData?.let { data ->
                    _weatherData.postValue(
                        WeatherData(
                            currentWeather = data.currentWeather,
                            forecast = data.forecast,
                            hourlyForecast = data.forecast?.list?.map {
                                HourlyForecast(
                                    timestamp = it.dt,
                                    temperature = it.main.temp,
                                    icon = it.weather.firstOrNull()?.icon
                                )
                            } ?: emptyList()
                        )
                    )
                    // Also update location data if needed
                    _locationData.postValue(
                        LocationData(
                            lat,
                            lon,
                            data.location.name ?: "${data.location.latitude}, ${data.location.longitude}"
                        )
                    )
                } ?: run {
                    _error.postValue("No data after refresh")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Refresh error", e)
                _error.postValue("Refresh error: ${e.message}")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    private fun fetchWeatherData(lat: Double, lon: Double, forceRefresh: Boolean) {
        viewModelScope.launch {
            try {
                _error.postValue(null) // Clear previous errors
                repository.setCurrentLocation(lat, lon, "metric")
                val isNetworkAvailable = isNetworkAvailable()
                val locationWithWeather = repository.getCurrentLocationWithWeather(
                    forceRefresh = forceRefresh,
                    isNetworkAvailable = isNetworkAvailable
                )

                if (locationWithWeather != null) {
                    _weatherData.postValue(
                        WeatherData(
                            currentWeather = locationWithWeather.currentWeather,
                            forecast = locationWithWeather.forecast,
                            hourlyForecast = locationWithWeather.forecast?.list?.map {
                                HourlyForecast(
                                    timestamp = it.dt,
                                    temperature = it.main.temp,
                                    icon = it.weather.firstOrNull()?.icon
                                )
                            } ?: emptyList()
                        )
                    )
                } else {
                    _error.postValue("No weather data available")
                }
            } catch (e: Exception) {
                _error.postValue("Error fetching weather: ${e.message}")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun refreshCurrentWeather() {
        viewModelScope.launch {
            try {
                _loading.postValue(true)
                _error.postValue(null)

                val currentLocationId = repository.getCurrentLocationId()
                if (currentLocationId == null) {
                    _error.postValue("No current location set")
                    return@launch
                }

                Log.d("TAG", "Starting refresh for location: $currentLocationId")

                // Force refresh
                val refreshSuccess = repository.refreshLocation(currentLocationId, "metric")

                if (!refreshSuccess) {
                    _error.postValue("Refresh failed")
                    return@launch
                }

                // Get the freshly updated data
                val freshData = repository.getCurrentLocationWithWeather(
                    forceRefresh = false, // Already refreshed
                    isNetworkAvailable = true
                )

                freshData?.let { data ->
                    Log.d("TAG", "Refresh successful, posting new data")
                    _weatherData.postValue(
                        WeatherData(
                            currentWeather = data.currentWeather,
                            forecast = data.forecast,
                            hourlyForecast = data.forecast?.list?.map {
                                HourlyForecast(
                                    timestamp = it.dt,
                                    temperature = it.main.temp,
                                    icon = it.weather.firstOrNull()?.icon
                                )
                            } ?: emptyList()
                        )
                    )
                } ?: run {
                    _error.postValue("No data after refresh")
                }
            } catch (e: Exception) {
                Log.e("TAG", "Refresh error", e)
                _error.postValue("Refresh error: ${e.message}")
            } finally {
                _loading.postValue(false)
                Log.d("TAG", "Refresh completed")
            }
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

    // --- Data classes to hold LiveData content ---
    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val address: String
    )

    data class WeatherData(
        val currentWeather: CurrentWeatherResponse?,
        val forecast: WeatherResponse?,
        val hourlyForecast: List<HourlyForecast>?
    )

    data class HourlyForecast(
        val timestamp: Long,
        val temperature: Double,
        val icon: String?
    )
}