package com.example.weatherwise.data.remote



import com.example.weatherwise.data.model.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext



class WeatherRemoteDataSourceImpl(private val weatherService: WeatherService) : IWeatherRemoteDataSource
{
    override suspend fun get5DayForecast(lat: Double, lon: Double, units: String): WeatherResponse?
    {
        return withContext(Dispatchers.IO) {
            weatherService.get5DayForecast(lat, lon, units).body()
        }
    }

    override suspend fun getCurrentWeather(lat: Double, lon: Double, units: String): WeatherResponse? {
        return withContext(Dispatchers.IO) {
            weatherService.getCurrentWeather(lat, lon, units).body()
        }
    }
}