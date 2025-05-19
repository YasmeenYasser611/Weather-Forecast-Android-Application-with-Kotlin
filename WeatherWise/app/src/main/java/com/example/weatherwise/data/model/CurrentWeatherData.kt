package com.example.weatherwise.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "current_weather_data")
data class CurrentWeatherData(
    @PrimaryKey val locationId: Int,
    val temperature: Double,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val weatherDescription: String,
    val weatherIcon: String,
    val lastUpdated: Long = System.currentTimeMillis()
): Serializable