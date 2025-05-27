package com.example.weatherwise.data.model.domain

import com.example.weatherwise.data.model.response.WeatherResponse
import com.example.weatherwise.data.model.response.CurrentWeatherResponse
import com.example.weatherwise.features.settings.model.PreferencesManager

data class WeatherData(
    val currentWeather: CurrentWeatherResponse?,
    val forecast: WeatherResponse?,
    val hourlyForecast: List<HourlyForecast>?,
    val dailyForecast: List<DailyForecast>?,
    val temperatureUnit: String = PreferencesManager.TEMP_CELSIUS,
    val windSpeedUnit: String = PreferencesManager.WIND_METERS_PER_SEC 
)