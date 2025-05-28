package com.example.weatherwise.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    val latitude: Double,
    val longitude: Double,
    var isCurrent: Boolean = false,
    var isFavorite: Boolean = false,
    var address: String? = null,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val timestamp: Long = System.currentTimeMillis()
)