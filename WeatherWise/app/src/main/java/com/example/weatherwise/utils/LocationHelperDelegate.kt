package com.example.weatherwise.utils

import com.example.weatherwise.data.model.domain.LocationData
import com.example.weatherwise.data.repository.IWeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class LocationHelperDelegate(
    private val locationHelper: LocationHelper,
    private val repository: IWeatherRepository,
    private val viewModelScope: CoroutineScope,
    private val onLocationData: (LocationData) -> Unit,
    private val onError: (String) -> Unit,
    private val onLoading: (Boolean) -> Unit
) {
    fun checkLocationPermissions(): Boolean = locationHelper.checkPermissions()
    fun isLocationEnabled(): Boolean = locationHelper.isLocationEnabled()
    fun enableLocationServices() = locationHelper.enableLocationServices()

    fun handleManualLocation() {
        repository.getManualLocationWithAddress()?.let { (lat, lon, address) ->
            viewModelScope.launch {
                val displayAddress = address.ifEmpty { "Selected Location" }
                onLocationData(LocationData(lat, lon, displayAddress))
            }
        } ?: run {
            onError("No manual location set")
        }
    }

    fun handleGpsLocation() {
        if (!checkLocationPermissions()) {
            onError("Please grant location permissions")
            return
        }
        if (!isLocationEnabled()) {
            onError("Please enable location services")
            enableLocationServices()
            return
        }

        onLoading(true)
        locationHelper.getFreshLocation(
            onLocationResult = { lat, lon, address ->
                viewModelScope.launch {
                    repository.setCurrentLocation(lat, lon)
                    onLocationData(LocationData(lat, lon, address))
                }
            },
            onError = { message ->
                onLoading(false)
                onError(message)
            }
        )
    }
}