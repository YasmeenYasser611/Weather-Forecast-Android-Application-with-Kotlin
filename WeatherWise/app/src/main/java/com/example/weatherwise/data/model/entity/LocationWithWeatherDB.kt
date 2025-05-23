package com.example.weatherwise.data.model.entity

import androidx.room.Embedded
import androidx.room.Relation


data class LocationWithWeatherDB(
    @Embedded val location: LocationEntity,
    @Relation(parentColumn = "id", entityColumn = "locationId", entity = CurrentWeatherEntity::class)
    val currentWeather: CurrentWeatherEntity?,
    @Relation(parentColumn = "id", entityColumn = "locationId", entity = ForecastWeatherEntity::class)
    val forecast: ForecastWeatherEntity?
)