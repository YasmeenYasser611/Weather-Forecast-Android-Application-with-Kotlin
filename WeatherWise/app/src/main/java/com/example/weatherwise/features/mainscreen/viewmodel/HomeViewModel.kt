package com.example.weatherwise.features.mainscreen.viewmodel

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.*

import kotlinx.coroutines.launch

import com.example.weatherwise.data.model.domain.LocationData
import com.example.weatherwise.data.model.domain.WeatherData
import com.example.weatherwise.data.repository.IWeatherRepository
import com.example.weatherwise.features.mainscreen.usecase.GetWeatherDataUseCase
import com.example.weatherwise.features.mainscreen.usecase.HandleLocationUseCase



class HomeViewModel(
    private val repository: IWeatherRepository,
    private val handleLocationUseCase: HandleLocationUseCase,
    private val getWeatherDataUseCase: GetWeatherDataUseCase,
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

    private val _isTemporaryLocation = MutableLiveData<Boolean>(false)
    val isTemporaryLocation: LiveData<Boolean> = _isTemporaryLocation

    fun checkLocationPermissions(): Boolean = handleLocationUseCase.checkLocationPermissions()
    fun isLocationEnabled(): Boolean = handleLocationUseCase.isLocationEnabled()
    fun enableLocationServices() = handleLocationUseCase.enableLocationServices()

    fun getFreshLocation() {
        viewModelScope.launch {
            safeCall {
                handleLocationUseCase.handleLocation(
                    locationMethod = repository.getLocationMethod(),
                    onLocationResult = { locationData ->
                        _locationData.postValue(locationData)
                        fetchWeatherData(locationData.latitude, locationData.longitude, false)
                    },
                    onError = { error -> _error.postValue(error) }
                )
            }
        }
    }

    fun loadTemporaryLocation(locationId: String) {
        _isTemporaryLocation.value = true
        viewModelScope.launch {
            safeCall {
                repository.getLocationWithWeather(locationId)?.let { data ->
                    val locationData = LocationData(
                        data.location.latitude,
                        data.location.longitude,
                        data.location.name ?: "Unknown Location"
                    )
                    _locationData.postValue(locationData)
                    val weatherData = getWeatherDataUseCase(
                        data.location.latitude,
                        data.location.longitude,
                        forceRefresh = false,
                        isNetworkAvailable = isNetworkAvailable()
                    ).getOrThrow()
                    _weatherData.postValue(weatherData)
                } ?: throw IllegalStateException("Location not found")
            }
        }
    }

    fun refreshCurrentWeather() {
        viewModelScope.launch {
            safeCall {
                val locationId = repository.getCurrentLocationId()
                    ?: throw IllegalStateException("No current location set")
                if (isNetworkAvailable() && !repository.refreshLocation(locationId)) {
                    throw IllegalStateException("Refresh failed")
                }
                val weatherData = getWeatherDataUseCase(
                    _locationData.value?.latitude,
                    _locationData.value?.longitude,
                    forceRefresh = false,
                    isNetworkAvailable = isNetworkAvailable()
                ).getOrThrow()
                _weatherData.postValue(weatherData)
            }
        }
    }

    fun refreshWeatherData() {
        fetchWeatherData(forceRefresh = true)
    }

    private fun fetchWeatherData(lat: Double? = null, lon: Double? = null, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            safeCall {
                val weatherData = getWeatherDataUseCase(
                    lat ?: _locationData.value?.latitude,
                    lon ?: _locationData.value?.longitude,
                    forceRefresh,
                    isNetworkAvailable()
                ).getOrThrow()
                _weatherData.postValue(weatherData)
            }
        }
    }

    fun handleManualLocationSelection(lat: Double, lon: Double) {
        viewModelScope.launch {
            safeCall {
                val locationData = handleLocationUseCase.handleManualLocationSelection(lat, lon)
                _locationData.postValue(locationData)
                fetchWeatherData(lat, lon, true)
            }
        }
    }

    private suspend fun safeCall(block: suspend () -> Unit) {
        _loading.value = true
        _error.value = null
        try {
            block()
        } catch (e: IllegalStateException) {
            Log.e("HomeViewModel", "Weather error: ${e.message}", e)
            _error.value = e.message
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Unexpected error", e)
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

    override fun onCleared() {
        super.onCleared()
        handleLocationUseCase.enableLocationServices()
    }
}