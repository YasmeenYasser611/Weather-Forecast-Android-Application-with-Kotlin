package com.example.weatherwise.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.weatherwise.data.model.FavouritePlace

@Dao
interface FavoriteLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: FavouritePlace)

    @Delete
    suspend fun deleteLocation(location: FavouritePlace)

    @Query("SELECT * FROM favorite_locations ORDER BY cityName ASC")
    suspend fun getAllFavoriteLocations(): List<FavouritePlace>

    @Query("SELECT * FROM favorite_locations WHERE id = :id")
    suspend fun getFavoriteLocationById(id: Int): FavouritePlace?


    // New methods for current location handling
    @Query("SELECT * FROM favorite_locations WHERE isCurrentLocation = 1 LIMIT 1")
    suspend fun getCurrentLocation(): FavouritePlace?

    @Update
    suspend fun updateLocation(location: FavouritePlace)

    @Query("UPDATE favorite_locations SET isCurrentLocation = 0 WHERE isCurrentLocation = 1")
    suspend fun clearCurrentLocationFlag()
}