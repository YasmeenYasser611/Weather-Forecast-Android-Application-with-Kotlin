package com.example.weatherwise.data.repository

import com.example.weatherwise.features.settings.model.IPreferencesManager
import com.example.weatherwise.features.settings.model.PreferencesManager

class FakePreferencesManager : IPreferencesManager {
    private var locationMethod: String = PreferencesManager.LOCATION_GPS
    private var manualLocation: Triple<Double, Double, String>? = null
    private var units: String = "metric"
    private var language: String = "en"
    private var temperatureUnitChanged: Boolean = false

    override fun getLocationMethod(): String = locationMethod
    override fun setLocationMethod(method: String) {
        locationMethod = method
    }

    override fun getManualLocation(): Pair<Double, Double>? {
        return manualLocation?.let { it.first to it.second }
    }

    override fun getManualLocationWithAddress(): Triple<Double, Double, String>? {
        return manualLocation
    }

    override fun setManualLocation(lat: Double, lon: Double, address: String) {
        manualLocation = Triple(lat, lon, address)
    }

    override fun getApiUnits(): String = units
     fun setApiUnits(units: String) {
        this.units = units
    }

    override fun getLanguageCode(): String = language
     fun setLanguageCode(language: String) {
        this.language = language
    }

    override fun hasTemperatureUnitChanged(currentUnit: String): Boolean {
        return temperatureUnitChanged
    }

    fun setTemperatureUnitChanged(changed: Boolean) {
        temperatureUnitChanged = changed
    }
}