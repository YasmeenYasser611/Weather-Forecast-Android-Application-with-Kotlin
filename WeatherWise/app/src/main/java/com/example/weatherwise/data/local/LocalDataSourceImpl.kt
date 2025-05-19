package com.example.weatherwise.data.local

import com.example.weatherwise.data.model.CurrentWeatherData
import com.example.weatherwise.data.model.FavouritePlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

class LocalDataSourceImpl(
    private val favoriteLocationDao: FavoriteLocationDao,
    private val weatherDataDao: WeatherDataDao
) : ILocalDataSource {

    // Existing implementations
    override suspend fun insertFavoriteLocation(location: FavouritePlace) {
        withContext(Dispatchers.IO) {
            favoriteLocationDao.insertLocation(location)
        }
    }

    override suspend fun deleteFavoriteLocation(location: FavouritePlace) {
        withContext(Dispatchers.IO) {
            favoriteLocationDao.deleteLocation(location)
            weatherDataDao.deleteWeatherDataForLocation(location.id)
        }
    }

    override suspend fun getAllFavoriteLocations(): List<FavouritePlace> {
        return withContext(Dispatchers.IO) {
            favoriteLocationDao.getAllFavoriteLocations()
        }
    }

    override suspend fun getFavoriteLocationById(id: Int): FavouritePlace? {
        return withContext(Dispatchers.IO) {
            favoriteLocationDao.getFavoriteLocationById(id)
        }
    }

    override suspend fun saveWeatherDataForLocation(weatherData: CurrentWeatherData) {
        withContext(Dispatchers.IO) {
            weatherDataDao.insertWeatherData(weatherData)
        }
    }

    override suspend fun getWeatherDataForLocation(locationId: Int): CurrentWeatherData? {
        return withContext(Dispatchers.IO) {
            weatherDataDao.getWeatherDataForLocation(locationId)
        }
    }

    override suspend fun deleteWeatherDataForLocation(locationId: Int) {
        withContext(Dispatchers.IO) {
            weatherDataDao.deleteWeatherDataForLocation(locationId)
        }
    }

    // Updated coordinate comparison with delta
    override suspend fun findLocationByCoordinates(lat: Double, lon: Double): FavouritePlace? {
        val locations = getAllFavoriteLocations()
        return locations.firstOrNull { location ->
            location.latitude.approxEquals(lat) &&
                    location.longitude.approxEquals(lon) &&
                    location.isCurrentLocation  // Prioritize current location
        } ?: locations.firstOrNull { location ->
            location.latitude.approxEquals(lat) &&
                    location.longitude.approxEquals(lon)
        }
    }

    // New implementations for current location handling
    override suspend fun getCurrentLocation(): FavouritePlace? {
        return withContext(Dispatchers.IO) {
            favoriteLocationDao.getCurrentLocation()
        }
    }

    override suspend fun setCurrentLocation(location: FavouritePlace) {
        withContext(Dispatchers.IO) {
            // First clear any existing current location flag
            favoriteLocationDao.clearCurrentLocationFlag()
            // Then set the new current location
            favoriteLocationDao.updateLocation(location.copy(isCurrentLocation = true))
        }
    }

    override suspend fun clearCurrentLocationFlag() {
        withContext(Dispatchers.IO) {
            favoriteLocationDao.clearCurrentLocationFlag()
        }
    }

    override suspend fun getMostRecentWeatherData(): CurrentWeatherData? {
        return withContext(Dispatchers.IO) {
            // Get weather data ordered by lastUpdated descending, take the first
            weatherDataDao.getMostRecentWeatherData()
        }
    }
}

// Extension function for coordinate comparison
fun Double.approxEquals(other: Double, epsilon: Double = 0.0001): Boolean {
    return abs(this - other) < epsilon
}