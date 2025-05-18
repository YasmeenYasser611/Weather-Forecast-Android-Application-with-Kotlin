package com.example.weatherwise.data.repository




import com.example.weatherwise.data.model.WeatherResponse



interface IWeatherRepository {
    suspend fun get5DayForecast(lat: Double, lon: Double, units: String): WeatherResponse?
}