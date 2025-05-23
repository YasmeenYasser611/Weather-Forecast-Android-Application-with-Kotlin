package com.example.weatherwise.data.remote

import com.example.weatherwise.data.model.response.CurrentWeatherResponse
import com.example.weatherwise.data.model.response.WeatherResponse


interface IWeatherRemoteDataSource {
    suspend fun getCurrentWeather(lat: Double, lon: Double, units: String, lang: String = "en"): CurrentWeatherResponse?
    suspend fun get5DayForecast(lat: Double, lon: Double, units: String, lang: String = "en"): WeatherResponse?
}