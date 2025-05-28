package com.example.weatherwise.features.settings.model

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

class PreferencesManager(context: Context) : IPreferencesManager {
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

    private var lastKnownSettings: Map<String, Any> = getAllSettings()

    fun haveSettingsChanged(): Boolean {
        val currentSettings = getAllSettings()
        val changed = currentSettings != lastKnownSettings
        lastKnownSettings = currentSettings
        return changed
    }

    override fun setLocationMethod(method: String) {
        sharedPreferences.edit { putString(KEY_LOCATION_METHOD, method) }
    }

    override fun getLocationMethod(): String {
        return sharedPreferences.getString("location_method", LOCATION_GPS) ?: LOCATION_GPS
    }

    override fun getApiUnits(): String {
        return when (getTemperatureUnit()) {
            TEMP_CELSIUS -> "metric"
            TEMP_FAHRENHEIT -> "imperial"
            TEMP_KELVIN -> "standard"
            else -> "metric"
        }
    }

    override fun getLanguageCode(): String {
        return sharedPreferences.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    override fun getManualLocation(): Pair<Double, Double>? {
        val lat = sharedPreferences.getString("manual_lat", null)?.toDoubleOrNull()
        val lon = sharedPreferences.getString("manual_lon", null)?.toDoubleOrNull()
        return if (lat != null && lon != null) Pair(lat, lon) else null
    }

    override fun getManualLocationWithAddress(): Triple<Double, Double, String>? {
        val lat = sharedPreferences.getString("manual_lat", null)?.toDoubleOrNull()
        val lon = sharedPreferences.getString("manual_lon", null)?.toDoubleOrNull()
        val address = sharedPreferences.getString("manual_address", "") ?: ""
        return if (lat != null && lon != null) Triple(lat, lon, address) else null
    }

    override fun setManualLocation(lat: Double, lon: Double, address: String) {
        sharedPreferences.edit {
            putString("manual_lat", lat.toString())
            putString("manual_lon", lon.toString())
            putString("manual_address", address)
            apply()
        }
        Log.d("LocationDebug", "Address fetched: $address")
    }

    override fun hasTemperatureUnitChanged(newUnit: String): Boolean {
        return getTemperatureUnit() != newUnit
    }

    // Other methods unchanged
    fun setTemperatureUnit(unit: String) {
        sharedPreferences.edit { putString(KEY_TEMPERATURE_UNIT, unit) }
    }

    fun getTemperatureUnit(): String {
        return sharedPreferences.getString(KEY_TEMPERATURE_UNIT, TEMP_CELSIUS) ?: TEMP_CELSIUS
    }

    fun setWindSpeedUnit(unit: String) {
        sharedPreferences.edit { putString(KEY_WIND_SPEED_UNIT, unit) }
    }

    fun getWindSpeedUnit(): String {
        return sharedPreferences.getString(KEY_WIND_SPEED_UNIT, WIND_METERS_PER_SEC) ?: WIND_METERS_PER_SEC
    }

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

    fun getLanguage(): String {
        val lang = sharedPreferences.getString(KEY_LANGUAGE, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
        Log.d("Localization", "Language retrieved: $lang")
        return lang
    }

    fun setLanguage(languageCode: String) {
        sharedPreferences.edit {
            putString(KEY_LANGUAGE, languageCode)
            apply()
        }
        Log.d("Localization", "Language saved: $languageCode")
    }

    fun getManualAddress(): String {
        return sharedPreferences.getString("manual_address", "") ?: ""
    }

    fun hasWindSpeedUnitChanged(newUnit: String): Boolean {
        return getWindSpeedUnit() != newUnit
    }

    fun hasLanguageChanged(newLanguage: String): Boolean {
        return getLanguage() != newLanguage
    }

    fun getApiWindSpeedUnit(): String {
        return when (getWindSpeedUnit()) {
            WIND_METERS_PER_SEC -> "metric"
            WIND_MILES_PER_HOUR -> "imperial"
            else -> "metric"
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled) }
    }

    fun areNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
    }

    fun getAllSettings(): Map<String, Any> {
        return mapOf(
            "location_method" to getLocationMethod(),
            "temperature_unit" to getTemperatureUnit(),
            "wind_speed_unit" to getWindSpeedUnit(),
            "language" to getLanguage(),
            "notifications_enabled" to areNotificationsEnabled()
        )
    }
}