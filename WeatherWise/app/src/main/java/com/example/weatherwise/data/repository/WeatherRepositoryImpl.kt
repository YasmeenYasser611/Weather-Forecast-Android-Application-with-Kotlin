

package com.example.weatherwise.data.repository

import android.util.Log
import com.example.weatherwise.data.local.ILocalDataSource
import com.example.weatherwise.data.model.LocationEntity
import com.example.weatherwise.data.model.LocationWithWeather
import com.example.weatherwise.data.model.WeatherResponse
import com.example.weatherwise.data.remote.IWeatherRemoteDataSource
import java.util.UUID

class WeatherRepositoryImpl private constructor(private val remoteDataSource: IWeatherRemoteDataSource, private val localDataSource: ILocalDataSource) : IWeatherRepository {

    companion object {
        @Volatile
        private var instance: WeatherRepositoryImpl? = null

        private const val TAG = "WeatherRepository"
        private const val DEFAULT_UNITS = "metric"

        fun getInstance(remoteDataSource: IWeatherRemoteDataSource, localDataSource: ILocalDataSource): WeatherRepositoryImpl {
            return instance ?: synchronized(this) {
                val temp = WeatherRepositoryImpl(remoteDataSource, localDataSource)
                instance = temp
                temp
            }
        }
    }

    override suspend fun setCurrentLocation(lat: Double, lon: Double, units: String)
    {
        localDataSource.clearCurrentLocationFlag()
        val locationId = getOrCreateLocationId(lat, lon, isCurrent = true)
        fetchAndSaveWeatherData(locationId, lat, lon, units)
        localDataSource.setCurrentLocation(locationId)
    }

    override suspend fun getCurrentLocationWithWeather(forceRefresh: Boolean, isNetworkAvailable: Boolean): LocationWithWeather? {
        val currentLocation = localDataSource.getCurrentLocation() ?: return null

        return try {
            if (forceRefresh && isNetworkAvailable) {
                // Delete current location and its weather data
                localDataSource.deleteLocation(currentLocation.id)

                // Create a new location with same coordinates but fresh data
                val newLocationId = getOrCreateLocationId(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    isCurrent = true
                )

                // Fetch fresh data for the new location
                fetchAndSaveWeatherData(
                    newLocationId,
                    currentLocation.latitude,
                    currentLocation.longitude,
                    DEFAULT_UNITS
                )

                // Get the newly created location with fresh data
                val newLocation = localDataSource.getCurrentLocation() ?: return null
                LocationWithWeather(
                    location = newLocation,
                    currentWeather = localDataSource.getCurrentWeather(newLocation.id),
                    forecast = localDataSource.getForecast(newLocation.id)
                )
            } else {
                // Normal case (no refresh or no network)
                if (isNetworkAvailable &&
                    (localDataSource.getCurrentWeather(currentLocation.id) == null ||
                            localDataSource.getForecast(currentLocation.id) == null)) {
                    fetchAndSaveWeatherData(
                        currentLocation.id,
                        currentLocation.latitude,
                        currentLocation.longitude,
                        DEFAULT_UNITS
                    )
                }

                LocationWithWeather(
                    location = currentLocation,
                    currentWeather = localDataSource.getCurrentWeather(currentLocation.id),
                    forecast = localDataSource.getForecast(currentLocation.id)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location with weather", e)
            null
        }
    }

    override suspend fun addFavoriteLocation(lat: Double, lon: Double, name: String, units: String): Boolean {
        return try {
            val locationId = getOrCreateLocationId(lat, lon, isFavorite = true)
            Log.d(TAG, "Setting favorite status for $locationId")
            localDataSource.setFavoriteStatus(locationId, true)
            localDataSource.updateLocationName(locationId, name)
            fetchAndSaveWeatherData(locationId, lat, lon, units)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding favorite", e)
            false
        }
    }

    override suspend fun removeFavoriteLocation(locationId: String) {
        localDataSource.setFavoriteStatus(locationId, false)
    }

    override suspend fun getFavoriteLocationsWithWeather(): List<LocationWithWeather> {
        return localDataSource.getFavoriteLocations().mapNotNull { location ->
            localDataSource.getLocationWithWeather(location.id)
        }
    }

    override suspend fun refreshLocation(locationId: String, units: String): Boolean {
        return try {
            val location = localDataSource.getLocation(locationId) ?: return false
            fetchAndSaveWeatherData(locationId, location.latitude, location.longitude, units)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing location", e)
            false
        }
    }

    override suspend fun deleteLocation(locationId: String) {
        localDataSource.deleteLocation(locationId)
    }



    private suspend fun fetchAndSaveWeatherData(locationId: String, lat: Double, lon: Double, units: String) {
        try {
            // Clear old data first
            localDataSource.deleteCurrentWeather(locationId)
            localDataSource.deleteForecast(locationId)

            // Fetch new data
            val currentWeatherResponse = remoteDataSource.getCurrentWeather(lat, lon, units)
            val forecastResponse = remoteDataSource.get5DayForecast(lat, lon, units)

            // Save new data
            currentWeatherResponse?.let {
                localDataSource.saveCurrentWeather(locationId, it)
            } ?: run {
                Log.e(TAG, "Null current weather response")
                throw Exception("Failed to fetch current weather")
            }

            forecastResponse?.let {
                localDataSource.saveForecast(locationId, it)
            } ?: run {
                Log.e(TAG, "Null forecast response")
                throw Exception("Failed to fetch forecast")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchAndSaveWeatherData: ${e.message}")
            throw e // Re-throw to handle in ViewModel
        }
    }

    private suspend fun getOrCreateLocationId(lat: Double, lon: Double, isCurrent: Boolean = false, isFavorite: Boolean = false): String {
        Log.d(TAG, "Looking for location at ($lat, $lon)")
        val existing = localDataSource.findLocationByCoordinates(lat, lon)

        return if (existing != null) {
            Log.d(TAG, "Found existing location: ${existing.id}")

            if (isCurrent && !existing.isCurrent) {
                localDataSource.setCurrentLocation(existing.id)
            }

            if (isFavorite && !existing.isFavorite) {
                localDataSource.setFavoriteStatus(existing.id, true)
            }

            existing.id
        } else {
            val newLocation = LocationEntity(id = UUID.randomUUID().toString(), name = "Location (${"%.2f".format(lat)}, ${"%.2f".format(lon)})", latitude = lat, longitude = lon, isCurrent = isCurrent, isFavorite = isFavorite)
            Log.d(TAG, "Creating new location: ${newLocation.id}")
            localDataSource.saveLocation(newLocation)
            newLocation.id
        }
    }

    override suspend fun getCurrentLocationId(): String? {
        return localDataSource.getCurrentLocation()?.id
    }
}
