package com.example.weatherwise.features.settings.viewmodel

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherwise.R
import com.example.weatherwise.data.repository.IWeatherRepository
import com.example.weatherwise.features.settings.model.PreferencesManager
import com.example.weatherwise.utils.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class SettingsViewModel(
    private val locationHelper: LocationHelper,
    private val weatherRepository: IWeatherRepository,
    private val preferencesManager: PreferencesManager,
    private val context: Context
) : ViewModel() {

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

    private val _requestNotificationPermission = MutableStateFlow(false)
    val requestNotificationPermission: StateFlow<Boolean> = _requestNotificationPermission

    private val _notificationPermissionRequest = MutableLiveData<Boolean>()
    val notificationPermissionRequest: LiveData<Boolean> = _notificationPermissionRequest

    private val _requestOverlayPermission = MutableLiveData<Boolean>()
    val requestOverlayPermission: LiveData<Boolean> = _requestOverlayPermission



    init {

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

    fun setCurrentLocation(lat: Double, lon: Double, address: String = "") {
        viewModelScope.launch {
            try {

                _locationMethod.value = PreferencesManager.LOCATION_MANUAL
                preferencesManager.setLocationMethod(PreferencesManager.LOCATION_MANUAL)

                preferencesManager.setManualLocation(lat, lon, address)

                weatherRepository.setCurrentLocation(lat, lon )

                refreshWeatherData()

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


    private val _saveComplete = MutableLiveData<Boolean>()
    val saveComplete: LiveData<Boolean> = _saveComplete

    fun setManualLocationCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {

                val address = withContext(Dispatchers.IO) {
                    locationHelper.getLocationAddress(lat, lon).first
                }

                Log.d("LocationDebug", "Address fetched: $address")
                setCurrentLocation(lat , lon , address)

                preferencesManager.setManualLocation(lat, lon, address)
                weatherRepository.setManualLocation(lat, lon, address)

                _locationMethod.value = PreferencesManager.LOCATION_MANUAL

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

    fun setLanguage(languageCode: String) {
        if (_language.value == languageCode) return
        _language.value = languageCode
        preferencesManager.setLanguage(languageCode)
    }


    fun saveSettings() {
        viewModelScope.launch {

            _locationMethod.value?.let { preferencesManager.setLocationMethod(it) }
            _temperatureUnit.value?.let { preferencesManager.setTemperatureUnit(it) }
            _windSpeedUnit.value?.let { preferencesManager.setWindSpeedUnit(it) }
            _language.value?.let { preferencesManager.setLanguage(it) }
            _notificationsEnabled.value?.let { preferencesManager.setNotificationsEnabled(it) }

            SettingsEventBus.notifySettingsChanged()


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




    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            _notificationsEnabled.value = true
            preferencesManager.setNotificationsEnabled(true)
            createNotificationChannel()
        } else {
            _notificationsEnabled.value = false
            preferencesManager.setNotificationsEnabled(false)
            _error.postValue("Notification permission denied")
        }
        _requestNotificationPermission.value = false
    }


    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {

            true
        }
    }


    fun onNotificationPermissionResult(granted: Boolean) {
        if (granted) {
            _notificationsEnabled.value = true
            preferencesManager.setNotificationsEnabled(true)
            createNotificationChannel()
        } else {
            _notificationsEnabled.value = false
            preferencesManager.setNotificationsEnabled(false)
            _error.value = "Notifications disabled - permission denied"
        }
        _notificationPermissionRequest.value = false
    }



    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun setNotificationsEnabled(enabled: Boolean, fromUser: Boolean = false) {
        if (enabled) {
            if (hasNotificationPermission()) {
                if (hasOverlayPermission()) {

                    _notificationsEnabled.value = true
                    preferencesManager.setNotificationsEnabled(true)
                    createNotificationChannel()
                } else if (fromUser) {

                    _requestOverlayPermission.value = true
                    _notificationsEnabled.value = false
                    preferencesManager.setNotificationsEnabled(false)
                }
            } else if (fromUser) {
                _notificationPermissionRequest.value = true
                _notificationsEnabled.value = false
                preferencesManager.setNotificationsEnabled(false)
            }
        } else {
            _notificationsEnabled.value = false
            preferencesManager.setNotificationsEnabled(false)
        }
    }



    fun onOverlayPermissionResult(granted: Boolean) {
        if (granted) {

            if (hasNotificationPermission()) {
                _notificationsEnabled.value = true
                preferencesManager.setNotificationsEnabled(true)
                createNotificationChannel()
            } else {
                _notificationPermissionRequest.value = true
            }
        } else {
            _error.postValue("Alarms may not work properly without display over other apps permission")
        }
        _requestOverlayPermission.value = false
    }


    companion object {
        const val CHANNEL_ID = "weather_notifications_channel"
        const val OVERLAY_PERMISSION_REQUEST_CODE = 1002
    }

}