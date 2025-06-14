package com.example.weatherwise.data.remote


import android.util.Log
import com.example.weatherwise.data.model.response.CurrentWeatherResponse
import com.example.weatherwise.data.model.response.GeocodingResponse
import com.example.weatherwise.data.model.response.WeatherResponse

class WeatherRemoteDataSourceImpl(private val weatherService: WeatherService) :
    IWeatherRemoteDataSource {
    override suspend fun getCurrentWeather(
        lat: Double,
        lon: Double,
        units: String,
        lang: String
    ): CurrentWeatherResponse? {
        return try {
            weatherService.getCurrentWeather(lat, lon, units, lang)
        } catch (e: Exception) {
            Log.e("WeatherRemoteDataSource", "Error fetching current weather", e)
            null
        }
    }

    override suspend fun get5DayForecast(
        lat: Double,
        lon: Double,
        units: String,
        lang: String
    ): WeatherResponse? {
        return try {
            weatherService.get5DayForecast(lat, lon, units, lang)
        } catch (e: Exception) {
            Log.e("WeatherRemoteDataSource", "Error fetching forecast", e)
            null
        }
    }

    override suspend fun getReverseGeocoding(lat: Double, lon: Double): List<GeocodingResponse>? {
        return try {
            weatherService.getReverseGeocoding(lat, lon)
        } catch (e: Exception) {
            Log.e("WeatherRemoteDataSource", "Error in reverse geocoding", e)
            null
        }
    }
}