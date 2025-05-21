package com.example.weatherwise.data.remote

import com.example.weatherwise.data.model.CurrentWeatherResponse
import com.example.weatherwise.data.model.WeatherResponse


interface IWeatherRemoteDataSource {
    suspend fun get5DayForecast(lat: Double, lon: Double, units: String, lang: String = "en"): WeatherResponse

    suspend fun getCurrentWeather(lat: Double, lon: Double, units: String, lang: String = "en"): CurrentWeatherResponse
}