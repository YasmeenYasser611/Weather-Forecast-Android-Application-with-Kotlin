package com.example.weatherwise.data.remote

import WeatherService
import android.util.Log
import com.example.weatherwise.data.model.CurrentWeatherResponse
import com.example.weatherwise.data.model.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class WeatherRemoteDataSourceImpl(private val weatherService: WeatherService) : IWeatherRemoteDataSource
{

    override suspend fun get5DayForecast(lat: Double, lon: Double, units: String, lang: String): WeatherResponse
    {
        return weatherService.get5DayForecast(lat, lon, units, lang)
    }

    override suspend fun getCurrentWeather(lat: Double, lon: Double, units: String, lang: String): CurrentWeatherResponse
    {
        return weatherService.getCurrentWeather(lat, lon, units, lang)
    }
}