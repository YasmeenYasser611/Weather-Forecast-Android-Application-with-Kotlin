package com.example.weatherwise.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.weatherwise.data.model.CurrentWeatherData

@Dao
interface WeatherDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherData(weatherData: CurrentWeatherData)

    @Query("SELECT * FROM current_weather_data WHERE locationId = :locationId")
    suspend fun getWeatherDataForLocation(locationId: Int): CurrentWeatherData?

    @Query("DELETE FROM current_weather_data WHERE locationId = :locationId")
    suspend fun deleteWeatherDataForLocation(locationId: Int)

    @Query("SELECT * FROM current_weather_data ORDER BY lastUpdated DESC LIMIT 1")
    suspend fun getMostRecentWeatherData(): CurrentWeatherData?
}