package com.example.weatherwise.data.model

data class WeatherData(
    val currentWeather: CurrentWeatherResponse?,
    val forecast: WeatherResponse?,
    val hourlyForecast: List<HourlyForecast>?,
    val dailyForecast: List<DailyForecast>?
)