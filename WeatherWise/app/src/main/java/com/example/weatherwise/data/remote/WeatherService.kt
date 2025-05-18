package com.example.weatherwise.data.remote



import com.example.weatherwise.data.model.WeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query



interface WeatherService {
    @GET("forecast")
    suspend fun get5DayForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String,
        @Query("appid") apiKey: String = "7744c1d50a26b3850970ce4050886248"
    ): Response<WeatherResponse>
}