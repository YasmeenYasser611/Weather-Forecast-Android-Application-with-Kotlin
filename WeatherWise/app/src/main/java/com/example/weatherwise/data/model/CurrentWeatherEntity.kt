package com.example.weatherwise.data.model

import androidx.room.Entity

import androidx.room.PrimaryKey


@Entity(tableName = "current_weather")
data class CurrentWeatherEntity(
    @PrimaryKey val locationId: String,
    val weatherData: CurrentWeatherResponse,
    val lastUpdated: Long = System.currentTimeMillis()
)