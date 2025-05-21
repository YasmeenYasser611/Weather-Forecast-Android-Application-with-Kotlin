package com.example.weatherwise.data.local

import com.example.weatherwise.data.model.CurrentWeatherEntity
import com.example.weatherwise.data.model.CurrentWeatherResponse
import com.example.weatherwise.data.model.LocationEntity
import com.example.weatherwise.data.model.LocationWithWeather
import com.example.weatherwise.data.model.WeatherResponse

interface ILocalDataSource {
    // Location operations
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

    // Weather operations
    suspend fun saveCurrentWeather(locationId: String, weather: CurrentWeatherResponse)
    suspend fun getCurrentWeather(locationId: String): CurrentWeatherResponse?

    // Forecast operations
    suspend fun saveForecast(locationId: String, forecast: WeatherResponse)
    suspend fun getForecast(locationId: String): WeatherResponse?

    // Combined operations
    suspend fun getLocationWithWeather(locationId: String): LocationWithWeather?

    suspend fun deleteCurrentWeather(locationId: String)
    suspend fun deleteForecast(locationId: String)

    // If you need the entity-based delete as well:
    suspend fun deleteCurrentWeather(currentWeather: CurrentWeatherEntity)
}