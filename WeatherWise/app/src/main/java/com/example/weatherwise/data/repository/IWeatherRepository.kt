package com.example.weatherwise.data.repository
import com.example.weatherwise.data.model.LocationWithWeather




interface IWeatherRepository {

    suspend fun setCurrentLocation(lat: Double, lon: Double, units: String)
    suspend fun getCurrentLocationWithWeather(forceRefresh: Boolean = false, isNetworkAvailable: Boolean = true): LocationWithWeather?


    suspend fun addFavoriteLocation(lat: Double, lon: Double, name: String, units: String): Boolean
    suspend fun removeFavoriteLocation(locationId: String)
    suspend fun getFavoriteLocationsWithWeather(): List<LocationWithWeather>


    suspend fun refreshLocation(locationId: String, units: String): Boolean
    suspend fun deleteLocation(locationId: String)

    suspend fun getCurrentLocationId(): String?
}