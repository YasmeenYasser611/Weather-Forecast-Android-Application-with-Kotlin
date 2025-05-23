package com.example.weatherwise.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrent: Boolean = false,
    val isFavorite: Boolean = false,
    val address: String? = null,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val timestamp: Long = System.currentTimeMillis()
)