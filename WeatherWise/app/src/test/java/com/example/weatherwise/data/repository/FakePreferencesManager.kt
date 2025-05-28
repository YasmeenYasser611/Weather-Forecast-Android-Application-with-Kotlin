package com.example.weatherwise.data.repository

import com.example.weatherwise.features.settings.model.IPreferencesManager

class FakePreferencesManager : IPreferencesManager {
    private var locationMethod = "gps"
    private var manualLocation: Triple<Double, Double, String>? = null
    private var temperatureUnit = "celsius"
    private var languageCode = "en"

    override fun setLocationMethod(method: String) {
        locationMethod = method
    }

    override fun getLocationMethod(): String {
        return locationMethod
    }

    override fun getApiUnits(): String {
        return when (temperatureUnit) {
            "celsius" -> "metric"
            "fahrenheit" -> "imperial"
            "kelvin" -> "standard"
            else -> "metric"
        }
    }

    override fun getLanguageCode(): String {
        return languageCode
    }

    override fun getManualLocation(): Pair<Double, Double>? {
        return manualLocation?.let { Pair(it.first, it.second) }
    }

    override fun getManualLocationWithAddress(): Triple<Double, Double, String>? {
        return manualLocation
    }

    override fun setManualLocation(lat: Double, lon: Double, address: String) {
        manualLocation = Triple(lat, lon, address)
    }

    override fun hasTemperatureUnitChanged(newUnit: String): Boolean {
        return temperatureUnit != newUnit
    }

    // Helper methods for testing
    fun setTemperatureUnit(unit: String) {
        temperatureUnit = unit
    }

    fun setLanguageCode(code: String) {
        languageCode = code
    }
}