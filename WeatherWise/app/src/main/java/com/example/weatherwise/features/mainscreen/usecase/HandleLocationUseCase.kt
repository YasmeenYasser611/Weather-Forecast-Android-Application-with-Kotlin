package com.example.weatherwise.features.mainscreen.usecase



import com.example.weatherwise.data.model.domain.LocationData
import com.example.weatherwise.data.repository.IWeatherRepository
import com.example.weatherwise.features.settings.model.PreferencesManager
import com.example.weatherwise.utils.LocationHelper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HandleLocationUseCase(
    private val repository: IWeatherRepository,
    private val locationHelper: LocationHelper
) {
    suspend fun handleLocation(
        locationMethod: String,
        onLocationResult: (LocationData) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        when (locationMethod) {
            PreferencesManager.LOCATION_MANUAL -> handleManualLocation(onLocationResult, onError)
            PreferencesManager.LOCATION_GPS -> handleGpsLocation(onLocationResult, onError)
            else -> onError("Invalid location method")
        }
    }

    private suspend fun handleManualLocation(
        onLocationResult: (LocationData) -> Unit,
        onError: (String) -> Unit
    ) {
        repository.getManualLocationWithAddress()?.let { (lat, lon, address) ->
            val displayAddress = if (address.isEmpty()) "Selected Location" else address
            onLocationResult(LocationData(lat, lon, displayAddress))
        } ?: onError("No manual location set")
    }

    private suspend fun handleGpsLocation(
        onLocationResult: (LocationData) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!locationHelper.checkPermissions()) {
            onError("Please grant location permissions")
            return
        }
        if (!locationHelper.isLocationEnabled()) {
            onError("Please enable location services")
            locationHelper.enableLocationServices()
            return
        }

        try {
            val (latitude, longitude, address) = locationHelper.getFreshLocation()
            repository.setCurrentLocation(latitude, longitude)
            onLocationResult(LocationData(latitude, longitude, address))
        } catch (e: Exception) {
            onError(e.message ?: "Unable to get location")
        }
    }

    suspend fun handleManualLocationSelection(lat: Double, lon: Double): LocationData {
        val address = locationHelper.getAddressFromLocation(lat, lon)
        repository.setManualLocation(lat, lon, address)
        return LocationData(lat, lon, address)
    }

    fun checkLocationPermissions(): Boolean = locationHelper.checkPermissions()
    fun isLocationEnabled(): Boolean = locationHelper.isLocationEnabled()
    fun enableLocationServices() = locationHelper.enableLocationServices()
}