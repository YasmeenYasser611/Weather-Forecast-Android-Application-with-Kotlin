package com.example.weatherwise.data.local

import com.example.weatherwise.data.model.CurrentWeatherData
import com.example.weatherwise.data.model.FavouritePlace

interface ILocalDataSource
{
    suspend fun insertFavoriteLocation(location: FavouritePlace)
    suspend fun deleteFavoriteLocation(location: FavouritePlace)
    suspend fun getAllFavoriteLocations(): List<FavouritePlace>
    suspend fun getFavoriteLocationById(id: Int): FavouritePlace?


    suspend fun saveWeatherDataForLocation(weatherData: CurrentWeatherData)
    suspend fun getWeatherDataForLocation(locationId: Int): CurrentWeatherData?
    suspend fun deleteWeatherDataForLocation(locationId: Int)

    suspend fun findLocationByCoordinates(lat: Double, lon: Double): FavouritePlace?

    suspend fun getCurrentLocation(): FavouritePlace?
    suspend fun setCurrentLocation(location: FavouritePlace)
    suspend fun getMostRecentWeatherData(): CurrentWeatherData?
    suspend fun clearCurrentLocationFlag()
}