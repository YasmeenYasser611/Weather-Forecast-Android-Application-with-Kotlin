package com.example.weatherwise.data.repository

import com.example.weatherwise.data.model.WeatherResponse
import com.example.weatherwise.data.remote.IWeatherRemoteDataSource


class WeatherRepositoryImpl private constructor(private val remoteDataSource: IWeatherRemoteDataSource) : IWeatherRepository {

    companion object {
        @Volatile
        private var instance: WeatherRepositoryImpl? = null

        fun getInstance(remoteDataSource: IWeatherRemoteDataSource): WeatherRepositoryImpl
        {
            return instance ?: synchronized(this) {
                instance ?: WeatherRepositoryImpl(remoteDataSource).also {
                    instance = it
                }
            }
        }
    }

    override suspend fun get5DayForecast(lat: Double, lon: Double, units: String): WeatherResponse?
    {
        return remoteDataSource.get5DayForecast(lat, lon, units)
    }
}