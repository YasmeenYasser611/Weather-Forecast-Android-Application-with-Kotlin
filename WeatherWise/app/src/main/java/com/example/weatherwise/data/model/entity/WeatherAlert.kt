package com.example.weatherwise.data.model.entity


import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID



@Entity(tableName = "weather_alerts")
data class WeatherAlert(
    @PrimaryKey
    val id: String,
    val type: String,
    val startTime: Long,
    val notificationType: String,
    val customSoundUri: String? = null,
    val isActive: Boolean
)