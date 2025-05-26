package com.example.weatherwise.features.settings.model

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("WeatherWisePrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LOCATION_METHOD = "location_method"
        private const val KEY_TEMPERATURE_UNIT = "temperature_unit"
        private const val KEY_WIND_SPEED_UNIT = "wind_speed_unit"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

        const val LOCATION_GPS = "gps"
        const val LOCATION_MANUAL = "manual"
        const val TEMP_CELSIUS = "celsius"
        const val TEMP_FAHRENHEIT = "fahrenheit"
        const val TEMP_KELVIN = "kelvin"
        const val WIND_METERS_PER_SEC = "meters_per_sec"
        const val WIND_MILES_PER_HOUR = "miles_per_hour"
        const val LANGUAGE_ENGLISH = "english"
        const val LANGUAGE_ARABIC = "arabic"
    }

    // Location Method
    fun setLocationMethod(method: String) {
        sharedPreferences.edit { putString(KEY_LOCATION_METHOD, method) }
    }


    // Temperature Unit
    fun setTemperatureUnit(unit: String) {
        sharedPreferences.edit { putString(KEY_TEMPERATURE_UNIT, unit) }
    }

    fun getTemperatureUnit(): String {
        return sharedPreferences.getString(KEY_TEMPERATURE_UNIT, TEMP_CELSIUS) ?: TEMP_CELSIUS
    }

    // Wind Speed Unit
    fun setWindSpeedUnit(unit: String) {
        sharedPreferences.edit { putString(KEY_WIND_SPEED_UNIT, unit) }
    }

    fun getWindSpeedUnit(): String {
        return sharedPreferences.getString(KEY_WIND_SPEED_UNIT, WIND_METERS_PER_SEC) ?: WIND_METERS_PER_SEC
    }

    // Language
    fun setLanguage(language: String) {
        sharedPreferences.edit { putString(KEY_LANGUAGE, language) }
    }

    fun getLanguage(): String {
        return sharedPreferences.getString(KEY_LANGUAGE, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
    }

    // Notifications



    fun getTemperatureUnitSymbol(): String {
        return when (getTemperatureUnit()) {
            TEMP_CELSIUS -> "°C"
            TEMP_FAHRENHEIT -> "°F"
            TEMP_KELVIN -> "K"
            else -> "°C"
        }
    }

    fun getWindSpeedUnitSymbol(): String {
        return when (getWindSpeedUnit()) {
            WIND_METERS_PER_SEC -> "m/s"
            WIND_MILES_PER_HOUR -> "mph"
            else -> "m/s"
        }
    }



    // Method to check if units changed (for refresh optimization)
    fun hasTemperatureUnitChanged(newUnit: String): Boolean {
        return getTemperatureUnit() != newUnit
    }

    // Method to get all settings as a bundle
    fun getAllSettings(): Map<String, Any> {
        return mapOf(
            "location_method" to getLocationMethod(),
            "temperature_unit" to getTemperatureUnit(),
            "wind_speed_unit" to getWindSpeedUnit(),
            "language" to getLanguage(),
            "notifications_enabled" to areNotificationsEnabled()
        )
    }



    fun getManualLocation(): Pair<Double, Double>? {
        val lat = sharedPreferences.getString("manual_lat", null)?.toDoubleOrNull()
        val lon = sharedPreferences.getString("manual_lon", null)?.toDoubleOrNull()
        return if (lat != null && lon != null) Pair(lat, lon) else null
    }

    fun getLocationMethod(): String {
        return sharedPreferences.getString("location_method", LOCATION_GPS) ?: LOCATION_GPS
    }



    // In PreferencesManager.kt
    fun setManualLocation(lat: Double, lon: Double, address: String) {
        sharedPreferences.edit().apply {
            putString("manual_lat", lat.toString())
            putString("manual_lon", lon.toString())
            putString("manual_address", address)
            apply() // Make sure to call apply() or commit()
        }

        Log.d("LocationDebug", "Address fetched: $address")
    }

    fun getManualAddress(): String {
        return sharedPreferences.getString("manual_address", "") ?: ""
    }

    fun getManualLocationWithAddress(): Triple<Double, Double, String>? {
        val lat = sharedPreferences.getString("manual_lat", null)?.toDoubleOrNull()
        val lon = sharedPreferences.getString("manual_lon", null)?.toDoubleOrNull()
        val address = sharedPreferences.getString("manual_address", "") ?: ""

        return if (lat != null && lon != null) Triple(lat, lon, address) else null
    }

    // In PreferencesManager.kt
    fun hasWindSpeedUnitChanged(newUnit: String): Boolean {
        return getWindSpeedUnit() != newUnit
    }


    fun hasLanguageChanged(newLanguage: String): Boolean {
        return getLanguage() != newLanguage
    }

    // Ensure getApiUnits considers both temperature and wind speed units
    fun getApiUnits(): String {
        // For OpenWeatherMap, both temperature and wind speed use the same units parameter
        return when (getTemperatureUnit()) {
            TEMP_CELSIUS -> "metric" // m/s for wind, °C for temp
            TEMP_FAHRENHEIT -> "imperial" // mph for wind, °F for temp
            TEMP_KELVIN -> "standard" // m/s for wind, K for temp
            else -> "metric"
        }
    }

    fun getApiWindSpeedUnit(): String {
        // This method might be redundant since getApiUnits handles both
        return when (getWindSpeedUnit()) {
            WIND_METERS_PER_SEC -> "metric"
            WIND_MILES_PER_HOUR -> "imperial"
            else -> "metric"
        }
    }

    fun getLanguageCode(): String {
        return when (getLanguage()) {
            LANGUAGE_ENGLISH -> "en"
            LANGUAGE_ARABIC -> "ar"
            else -> "en"
        }
    }
    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled) }
    }

    fun areNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
    }

}