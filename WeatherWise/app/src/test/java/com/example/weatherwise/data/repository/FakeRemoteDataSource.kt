package com.example.weatherwise.data.remote.fake

import com.example.weatherwise.data.model.response.CurrentWeatherResponse
import com.example.weatherwise.data.model.response.GeocodingResponse
import com.example.weatherwise.data.model.response.WeatherResponse
import com.example.weatherwise.data.remote.IWeatherRemoteDataSource

class FakeRemoteDataSource : IWeatherRemoteDataSource {
    // Test data
    private var currentWeatherResponse: CurrentWeatherResponse? = null
    private var weatherResponse: WeatherResponse? = null
    private var geocodingResponses: List<GeocodingResponse> = emptyList()

    // Control variables for testing
    var shouldReturnError = false
    var errorMessage = "Test error"

    fun setCurrentWeatherResponse(response: CurrentWeatherResponse?) {
        currentWeatherResponse = response
    }

    fun setWeatherResponse(response: WeatherResponse?) {
        weatherResponse = response
    }

    fun setGeocodingResponses(responses: List<GeocodingResponse>) {
        geocodingResponses = responses
    }

    override suspend fun getCurrentWeather(
        lat: Double,
        lon: Double,
        units: String,
        lang: String
    ): CurrentWeatherResponse? {
        if (shouldReturnError) throw RuntimeException(errorMessage)
        return currentWeatherResponse
    }

    override suspend fun get5DayForecast(
        lat: Double,
        lon: Double,
        units: String,
        lang: String
    ): WeatherResponse? {
        if (shouldReturnError) throw RuntimeException(errorMessage)
        return weatherResponse
    }

    override suspend fun getReverseGeocoding(lat: Double, lon: Double): List<GeocodingResponse>? {
        if (shouldReturnError) throw RuntimeException(errorMessage)
        return geocodingResponses.ifEmpty { null }
    }
}