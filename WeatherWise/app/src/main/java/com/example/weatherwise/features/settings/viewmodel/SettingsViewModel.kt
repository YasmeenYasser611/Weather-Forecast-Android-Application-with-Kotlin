package com.example.weatherwise.features.settings.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherwise.data.repository.IWeatherRepository
import com.example.weatherwise.features.settings.model.PreferencesManager
import com.example.weatherwise.location.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val locationHelper: LocationHelper,
    private val weatherRepository: IWeatherRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    // LiveData for UI state
    private val _locationMethod = MutableLiveData<String>()
    val locationMethod: LiveData<String> = _locationMethod

    private val _temperatureUnit = MutableLiveData<String>()
    val temperatureUnit: LiveData<String> = _temperatureUnit

    private val _windSpeedUnit = MutableLiveData<String>()
    val windSpeedUnit: LiveData<String> = _windSpeedUnit

    private val _language = MutableLiveData<String>()
    val language: LiveData<String> = _language

    private val _notificationsEnabled = MutableLiveData<Boolean>()
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _navigateToMap = MutableLiveData<Boolean>()
    val navigateToMap: LiveData<Boolean> = _navigateToMap

    private val _locationSaveComplete = MutableLiveData<Boolean>(false)
    val locationSaveComplete: LiveData<Boolean> = _locationSaveComplete

    init {
        // Load initial settings from SharedPreferences
        _locationMethod.value = preferencesManager.getLocationMethod()
        _temperatureUnit.value = preferencesManager.getTemperatureUnit()
        _windSpeedUnit.value = preferencesManager.getWindSpeedUnit()
        _language.value = preferencesManager.getLanguage()
        _notificationsEnabled.value = preferencesManager.areNotificationsEnabled()
    }

    fun setLocationMethod(method: String) {
        _locationMethod.value = method
        if (method == PreferencesManager.LOCATION_GPS) {
            fetchGpsLocation()
        }
    }

    fun setTemperatureUnit(unit: String) {
        _temperatureUnit.value = unit
    }


    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun selectManualLocation() {
        _navigateToMap.value = true
    }

    fun resetNavigateToMap() {
        _navigateToMap.value = false
    }

    // In SettingsViewModel.kt
    fun setCurrentLocation(lat: Double, lon: Double, address: String = "") {
        viewModelScope.launch {
            try {
                // Set location method to manual first
                _locationMethod.value = PreferencesManager.LOCATION_MANUAL
                preferencesManager.setLocationMethod(PreferencesManager.LOCATION_MANUAL)

                // Save the location coordinates and address
                preferencesManager.setManualLocation(lat, lon, address)

                // Save the location in repository
                weatherRepository.setCurrentLocation(lat, lon )

                // Force refresh weather data
                refreshWeatherData()

                // Persist all settings
                saveSettings()
            } catch (e: Exception) {
                _error.postValue("Failed to save location: ${e.message}")
            }
        }
    }





    private suspend fun refreshWeatherData() {
        weatherRepository.getCurrentLocationWithWeather(true, true)?.let { data ->
            weatherRepository.setCurrentLocation(data.location.latitude, data.location.longitude)
        }
    }

    fun resetLocationSaveComplete() {
        _locationSaveComplete.value = false
    }


    // Add this to your ViewModel
    private val _saveComplete = MutableLiveData<Boolean>()
    val saveComplete: LiveData<Boolean> = _saveComplete

    fun setManualLocationCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                // 1. Get address
                val address = withContext(Dispatchers.IO) {
                    locationHelper.getLocationAddress(lat, lon).first
                }

                Log.d("LocationDebug", "Address fetched: $address")
                setCurrentLocation(lat , lon , address)

                // 2. Save to all layers
                preferencesManager.setManualLocation(lat, lon, address)
                weatherRepository.setManualLocation(lat, lon, address)

                // 3. Update LiveData
                _locationMethod.value = PreferencesManager.LOCATION_MANUAL

                // 4. Notify that save is complete
                _saveComplete.postValue(true)

            } catch (e: Exception) {
                _error.postValue("Save failed: ${e.message}")
                _saveComplete.postValue(false)
            }
        }
    }
    private fun fetchGpsLocation() {
        if (!locationHelper.checkPermissions()) {
            _error.value = "Please grant location permissions"
            return
        }
        if (!locationHelper.isLocationEnabled()) {
            _error.value = "Please enable location services"
            locationHelper.enableLocationServices()
            return
        }

        locationHelper.getFreshLocation(
            onLocationResult = { lat, lon, address ->
                viewModelScope.launch {
                    weatherRepository.setCurrentLocation(lat, lon)
                }
            },
            onError = { message ->
                _error.postValue(message)
            }
        )
    }


    object SettingsEventBus {
        private val _settingsChanged = MutableSharedFlow<Unit>()
        val settingsChanged = _settingsChanged.asSharedFlow()

        suspend fun notifySettingsChanged() {
            _settingsChanged.emit(Unit)
        }
    }




    fun setWindSpeedUnit(unit: String) {
        _windSpeedUnit.value = unit
    }

    fun setLanguage(language: String) {
        _language.value = language
    }

    fun saveSettings() {
        viewModelScope.launch {
            // Save settings to PreferencesManager
            _locationMethod.value?.let { preferencesManager.setLocationMethod(it) }
            _temperatureUnit.value?.let { preferencesManager.setTemperatureUnit(it) }
            _windSpeedUnit.value?.let { preferencesManager.setWindSpeedUnit(it) }
            _language.value?.let { preferencesManager.setLanguage(it) }
            _notificationsEnabled.value?.let { preferencesManager.setNotificationsEnabled(it) }

            // Notify that settings changed
            SettingsEventBus.notifySettingsChanged()

            // Refresh weather if needed
            if (shouldRefreshWeather()) {
                refreshWeatherData()
            }
        }
    }

    private suspend fun shouldRefreshWeather(): Boolean {
        return preferencesManager.hasTemperatureUnitChanged(_temperatureUnit.value ?: PreferencesManager.TEMP_CELSIUS) ||
                preferencesManager.hasWindSpeedUnitChanged(_windSpeedUnit.value ?: PreferencesManager.WIND_METERS_PER_SEC) ||
                preferencesManager.hasLanguageChanged(_language.value ?: PreferencesManager.LANGUAGE_ENGLISH)
    }
}