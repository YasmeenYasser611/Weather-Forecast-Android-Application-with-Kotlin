package com.example.weatherwise.data.model.entity

import androidx.room.Entity

import androidx.room.PrimaryKey
import com.example.weatherwise.data.model.response.CurrentWeatherResponse


@Entity(tableName = "current_weather")
data class CurrentWeatherEntity(
    @PrimaryKey val locationId: String,
    val weatherData: CurrentWeatherResponse,
    val lastUpdated: Long = System.currentTimeMillis()
)