package com.example.weatherwise.data.model.domain

import com.example.weatherwise.data.model.response.WeatherResponse
import com.example.weatherwise.data.model.response.CurrentWeatherResponse

data class WeatherData(
    val currentWeather: CurrentWeatherResponse?,
    val forecast: WeatherResponse?,
    val hourlyForecast: List<HourlyForecast>?,
    val dailyForecast: List<DailyForecast>?
)