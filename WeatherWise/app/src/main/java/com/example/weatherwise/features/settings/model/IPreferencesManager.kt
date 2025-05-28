package com.example.weatherwise.features.settings.model


interface IPreferencesManager {
    fun getLocationMethod(): String
    fun setLocationMethod(method: String)
    fun getApiUnits(): String
    fun getLanguageCode(): String
    fun getManualLocation(): Pair<Double, Double>?
    fun getManualLocationWithAddress(): Triple<Double, Double, String>?
    fun setManualLocation(lat: Double, lon: Double, address: String)
    fun hasTemperatureUnitChanged(newUnit: String): Boolean
}