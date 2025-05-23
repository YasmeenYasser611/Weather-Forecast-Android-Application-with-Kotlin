package com.example.weatherwise.data.model.entity

import androidx.room.Entity

import androidx.room.PrimaryKey
import com.example.weatherwise.data.model.response.WeatherResponse


@Entity(tableName = "forecast_weather")
data class ForecastWeatherEntity(
    @PrimaryKey val locationId: String,
    val forecastData: WeatherResponse,
    val lastUpdated: Long = System.currentTimeMillis()
)