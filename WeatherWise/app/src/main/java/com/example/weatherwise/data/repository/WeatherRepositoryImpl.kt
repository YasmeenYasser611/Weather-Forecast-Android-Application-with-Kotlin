

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

    override suspend fun getCurrentLocationWithWeather(forceRefresh: Boolean, isNetworkAvailable: Boolean): LocationWithWeather?
    {
        val currentLocation = localDataSource.getCurrentLocation() ?: return null

        return try {
            if (forceRefresh && isNetworkAvailable) {
                fetchAndSaveWeatherData(currentLocation.id, currentLocation.latitude, currentLocation.longitude, DEFAULT_UNITS)
            }

            val currentWeather = localDataSource.getCurrentWeather(currentLocation.id)
            val forecast = localDataSource.getForecast(currentLocation.id)

            if (currentWeather == null && forecast == null && isNetworkAvailable)
            {
                fetchAndSaveWeatherData(currentLocation.id, currentLocation.latitude, currentLocation.longitude, DEFAULT_UNITS)
                LocationWithWeather(location = currentLocation, currentWeather = localDataSource.getCurrentWeather(currentLocation.id), forecast = localDataSource.getForecast(currentLocation.id))
            }
            else {
                LocationWithWeather(location = currentLocation, currentWeather = currentWeather, forecast = forecast)
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
            val currentWeatherResponse = remoteDataSource.getCurrentWeather(lat, lon, units)
            val forecastResponse = remoteDataSource.get5DayForecast(lat, lon, units)

            currentWeatherResponse?.let {
                Log.d(TAG, "Saving weather for $locationId")
                localDataSource.saveCurrentWeather(locationId, it)
            } ?: Log.e(TAG, "Null weather response")

            forecastResponse?.let {
                Log.d(TAG, "Saving forecast for $locationId")
                localDataSource.saveForecast(locationId, it)
            } ?: Log.e(TAG, "Null forecast response")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching/saving weather", e)
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
}
