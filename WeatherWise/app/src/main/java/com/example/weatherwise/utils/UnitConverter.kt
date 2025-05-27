package com.example.weatherwise.utils

import com.example.weatherwise.features.settings.model.PreferencesManager

object UnitConverter {
    fun convertTemperature(temp: Double, fromUnit: String, toUnit: String): Double {
        return when {
            fromUnit == toUnit -> temp
            fromUnit == PreferencesManager.TEMP_CELSIUS && toUnit == PreferencesManager.TEMP_FAHRENHEIT -> (temp * 9/5) + 32
            fromUnit == PreferencesManager.TEMP_CELSIUS && toUnit == PreferencesManager.TEMP_KELVIN -> temp + 273.15
            fromUnit == PreferencesManager.TEMP_FAHRENHEIT && toUnit == PreferencesManager.TEMP_CELSIUS -> (temp - 32) * 5/9
            fromUnit == PreferencesManager.TEMP_FAHRENHEIT && toUnit == PreferencesManager.TEMP_KELVIN -> (temp - 32) * 5/9 + 273.15
            fromUnit == PreferencesManager.TEMP_KELVIN && toUnit == PreferencesManager.TEMP_CELSIUS -> temp - 273.15
            fromUnit == PreferencesManager.TEMP_KELVIN && toUnit == PreferencesManager.TEMP_FAHRENHEIT -> (temp - 273.15) * 9/5 + 32
            else -> temp
        }
    }

    fun convertWindSpeed(speed: Double, fromUnit: String, toUnit: String): Double {
        return when {
            fromUnit == toUnit -> speed
            fromUnit == PreferencesManager.WIND_METERS_PER_SEC && toUnit == PreferencesManager.WIND_MILES_PER_HOUR -> speed * 2.23694
            fromUnit == PreferencesManager.WIND_MILES_PER_HOUR && toUnit == PreferencesManager.WIND_METERS_PER_SEC -> speed / 2.23694
            else -> speed
        }
    }
}