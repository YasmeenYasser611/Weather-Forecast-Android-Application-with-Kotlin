package com.example.weatherwise.data.model

import androidx.room.Entity

import androidx.room.PrimaryKey


@Entity(tableName = "forecast_weather")
data class ForecastWeatherEntity(
    @PrimaryKey val locationId: String,
    val forecastData: WeatherResponse,
    val lastUpdated: Long = System.currentTimeMillis()
)