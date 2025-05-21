
package com.example.weatherwise.data.local

import androidx.room.*
import com.example.weatherwise.data.model.CurrentWeatherEntity
import com.example.weatherwise.data.model.ForecastWeatherEntity
import com.example.weatherwise.data.model.LocationEntity
import com.example.weatherwise.data.model.LocationWithWeatherDB

@Dao
interface WeatherDao {

    // --------------------------
    // Location operations
    // --------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Query("SELECT * FROM locations WHERE id = :locationId")
    suspend fun getLocation(locationId: String): LocationEntity?

    @Query("SELECT * FROM locations WHERE latitude = :lat AND longitude = :lon LIMIT 1")
    suspend fun findLocationByCoordinates(lat: Double, lon: Double): LocationEntity?

    @Query("SELECT * FROM locations WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentLocation(): LocationEntity?

    @Query("SELECT * FROM locations WHERE isFavorite = 1")
    suspend fun getFavoriteLocations(): List<LocationEntity>

    @Query("UPDATE locations SET isCurrent = 0")
    suspend fun clearCurrentLocationFlag()

    @Query("UPDATE locations SET isCurrent = 1 WHERE id = :locationId")
    suspend fun setCurrentLocation(locationId: String)

    @Query("UPDATE locations SET isFavorite = :isFavorite WHERE id = :locationId")
    suspend fun setFavoriteStatus(locationId: String, isFavorite: Boolean)

    @Query("UPDATE locations SET name = :name WHERE id = :locationId")
    suspend fun updateLocationName(locationId: String, name: String)

    @Query("DELETE FROM locations WHERE id = :locationId")
    suspend fun deleteLocation(locationId: String)


    // --------------------------
    // Current weather operations
    // --------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrentWeather(weather: CurrentWeatherEntity)

    @Query("SELECT * FROM current_weather WHERE locationId = :locationId")
    suspend fun getCurrentWeather(locationId: String): CurrentWeatherEntity?


    // --------------------------
    // Forecast weather operations
    // --------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForecast(forecast: ForecastWeatherEntity)

    @Query("SELECT * FROM forecast_weather WHERE locationId = :locationId")
    suspend fun getForecast(locationId: String): ForecastWeatherEntity?


    // --------------------------
    // Combined operation
    // --------------------------

    @Transaction
    @Query("SELECT * FROM locations WHERE id = :locationId")
    suspend fun getLocationWithWeather(locationId: String): LocationWithWeatherDB?


    @Delete
    fun deleteCurrentWeather(currentWeather: CurrentWeatherEntity)

    @Query("DELETE FROM current_weather WHERE locationId = :locationId")
    suspend fun deleteCurrentWeather(locationId: String)

    @Query("DELETE FROM forecast_weather WHERE locationId = :locationId")
    suspend fun deleteForecast(locationId: String)
}


