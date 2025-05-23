package com.example.weatherwise.data.local

import com.example.weatherwise.data.model.entity.CurrentWeatherEntity
import com.example.weatherwise.data.model.entity.LocationEntity
import com.example.weatherwise.data.model.domain.LocationWithWeather
import com.example.weatherwise.data.model.response.CurrentWeatherResponse
import com.example.weatherwise.data.model.response.WeatherResponse

interface ILocalDataSource {
    suspend fun saveLocation(location: LocationEntity)
    suspend fun getLocation(id: String): LocationEntity?
    suspend fun findLocationByCoordinates(lat: Double, lon: Double): LocationEntity?
    suspend fun getCurrentLocation(): LocationEntity?
    suspend fun getFavoriteLocations(): List<LocationEntity>
    suspend fun clearCurrentLocationFlag()
    suspend fun setCurrentLocation(locationId: String)
    suspend fun setFavoriteStatus(locationId: String, isFavorite: Boolean)
    suspend fun updateLocationName(locationId: String, name: String)
    suspend fun deleteLocation(locationId: String)
    suspend fun saveCurrentWeather(locationId: String, weather: CurrentWeatherResponse)
    suspend fun getCurrentWeather(locationId: String): CurrentWeatherResponse?
    suspend fun saveForecast(locationId: String, forecast: WeatherResponse)
    suspend fun getForecast(locationId: String): WeatherResponse?
    suspend fun getLocationWithWeather(locationId: String): LocationWithWeather?
    suspend fun deleteCurrentWeather(locationId: String)
    suspend fun deleteForecast(locationId: String)
    suspend fun deleteCurrentWeather(currentWeather: CurrentWeatherEntity)
    suspend fun deleteStaleWeather(threshold: Long)
}