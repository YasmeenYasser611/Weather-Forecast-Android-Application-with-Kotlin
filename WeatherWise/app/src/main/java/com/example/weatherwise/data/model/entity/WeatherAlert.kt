package com.example.weatherwise.data.model.entity


import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "weather_alerts")
data class WeatherAlert(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: String, // e.g., "Rain", "Storm"
    val startTime: Long, // milliseconds
    val endTime: Long, // milliseconds
    val notificationType: String, // "SILENT", "SOUND", "ALARM"
    val customSoundUri: String? = null,
    var isActive: Boolean = true,
    val latitude: Double? = null, // Optional location for alert
    val longitude: Double? = null // Optional location for alert
)