package com.example.weatherwise.data.remote


import com.example.weatherwise.data.model.WeatherResponse




interface IWeatherRemoteDataSource {
    suspend fun get5DayForecast(lat: Double, lon: Double, units: String): WeatherResponse?
    suspend fun getCurrentWeather(lat: Double, lon: Double, units: String): WeatherResponse?
}