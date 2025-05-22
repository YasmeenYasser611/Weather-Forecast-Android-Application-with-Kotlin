package com.example.weatherwise.data.repository

import android.util.Log
import com.example.weatherwise.data.local.ILocalDataSource
import com.example.weatherwise.data.model.LocationEntity
import com.example.weatherwise.data.model.LocationWithWeather
import com.example.weatherwise.data.remote.IWeatherRemoteDataSource
import com.example.weatherwise.features.settings.model.PreferencesManager
import java.util.UUID

class WeatherRepositoryImpl private constructor(
    private val remoteDataSource: IWeatherRemoteDataSource,
    private val localDataSource: ILocalDataSource,
    private val preferencesManager: PreferencesManager
) : IWeatherRepository {

    companion object {
        @Volatile
        private var instance: WeatherRepositoryImpl? = null

        private const val TAG = "WeatherRepository"

        fun getInstance(
            remoteDataSource: IWeatherRemoteDataSource,
            localDataSource: ILocalDataSource,
            preferencesManager: PreferencesManager
        ): WeatherRepositoryImpl {
            return instance ?: synchronized(this) {
                val temp = WeatherRepositoryImpl(remoteDataSource, localDataSource, preferencesManager)
                instance = temp
                temp
            }
        }
    }



    override suspend fun addFavoriteLocation(lat: Double, lon: Double, name: String): Boolean {
        return try {
            val locationId = getOrCreateLocationId(lat, lon, isFavorite = true)
            Log.d(TAG, "Setting favorite status for $locationId")
            localDataSource.setFavoriteStatus(locationId, true)
            localDataSource.updateLocationName(locationId, name)
            fetchAndSaveWeatherData(locationId, lat, lon)
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

    override suspend fun refreshLocation(locationId: String): Boolean {
        return try {
            val location = localDataSource.getLocation(locationId) ?: return false
            fetchAndSaveWeatherData(locationId, location.latitude, location.longitude)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing location", e)
            false
        }
    }

    override suspend fun deleteLocation(locationId: String) {
        localDataSource.deleteLocation(locationId)
    }

    // In WeatherRepositoryImpl.kt
    private suspend fun fetchAndSaveWeatherData(locationId: String, lat: Double, lon: Double) {
        try {
            // Clear old data first
            localDataSource.deleteCurrentWeather(locationId)
            localDataSource.deleteForecast(locationId)

            // Get preferred units and language from PreferencesManager
            val units = preferencesManager.getApiUnits()
            val lang = preferencesManager.getLanguageCode()

            // Fetch new data with units and language
            val currentWeatherResponse = remoteDataSource.getCurrentWeather(lat, lon, units, lang)
            val forecastResponse = remoteDataSource.get5DayForecast(lat, lon, units, lang)

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
            throw e
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
            val newLocation = LocationEntity(
                id = UUID.randomUUID().toString(),
                name = "Location (${"%.2f".format(lat)}, ${"%.2f".format(lon)})",
                latitude = lat,
                longitude = lon,
                isCurrent = isCurrent,
                isFavorite = isFavorite
            )
            Log.d(TAG, "Creating new location: ${newLocation.id}")
            localDataSource.saveLocation(newLocation)
            newLocation.id
        }
    }

    override suspend fun getCurrentLocationId(): String? {
        return localDataSource.getCurrentLocation()?.id
    }

    override fun getPreferredUnits(): String {
        return preferencesManager.getApiUnits()
    }

    override  fun getManualLocation(): Pair<Double, Double>? {
        return preferencesManager.getManualLocation()
    }

    override fun getLocationMethod(): String {
        return preferencesManager.getLocationMethod()
    }


    override fun getManualLocationWithAddress(): Triple<Double, Double, String>? {
        return preferencesManager.getManualLocationWithAddress()
    }

    override suspend fun setCurrentLocation(lat: Double, lon: Double) {
        localDataSource.clearCurrentLocationFlag()
        val locationId = getOrCreateLocationId(lat, lon, isCurrent = true)


        // Update location name if it's a manual location with address
        if (preferencesManager.getLocationMethod() == PreferencesManager.LOCATION_MANUAL) {
            getManualLocationWithAddress()?.let { (_, _, address) ->
                Log.d("LocationDebug", " from current locationAddress fetched: $address")
                if (address.isNotEmpty()) {
                    localDataSource.updateLocationName(locationId, address)
                }
            }
        }

        fetchAndSaveWeatherData(locationId, lat, lon)
        localDataSource.setCurrentLocation(locationId)
    }

    override suspend fun getCurrentLocationWithWeather(forceRefresh: Boolean, isNetworkAvailable: Boolean): LocationWithWeather? {
        return when (preferencesManager.getLocationMethod()) {
            PreferencesManager.LOCATION_MANUAL -> {
                getManualLocationWithAddress()?.let { (lat, lon, address) ->
                    handleManualLocation(lat, lon, address, forceRefresh, isNetworkAvailable)
                }
            }
            PreferencesManager.LOCATION_GPS -> {
                handleGpsLocation(forceRefresh, isNetworkAvailable)
            }
            else -> null
        }
    }

    private suspend fun handleManualLocation(
        lat: Double,
        lon: Double,
        address: String,
        forceRefresh: Boolean,
        isNetworkAvailable: Boolean
    ): LocationWithWeather? {
        // Ensure we have a current location set
        val currentLocation = localDataSource.getCurrentLocation() ?: run {
            setCurrentLocation(lat, lon)
            localDataSource.getCurrentLocation()
        } ?: return null

        // Update location name if we have an address
        if (address.isNotEmpty()) {
            localDataSource.updateLocationName(currentLocation.id, address)
        }

        return fetchLocationWithWeather(currentLocation, forceRefresh, isNetworkAvailable)
    }

    private suspend fun handleGpsLocation(
        forceRefresh: Boolean,
        isNetworkAvailable: Boolean
    ): LocationWithWeather? {
        val currentLocation = localDataSource.getCurrentLocation() ?: return null
        return fetchLocationWithWeather(currentLocation, forceRefresh, isNetworkAvailable)
    }

    private suspend fun fetchLocationWithWeather(
        location: LocationEntity,
        forceRefresh: Boolean,
        isNetworkAvailable: Boolean
    ): LocationWithWeather? {
        return try {
            if (forceRefresh && isNetworkAvailable) {
                // Delete and recreate for fresh data
                localDataSource.deleteLocation(location.id)
                val newLocationId = getOrCreateLocationId(location.latitude, location.longitude, isCurrent = true)
                fetchAndSaveWeatherData(newLocationId, location.latitude, location.longitude)
                localDataSource.getLocationWithWeather(newLocationId)
            } else {
                // Check if we need to refresh
                if (isNetworkAvailable &&
                    (localDataSource.getCurrentWeather(location.id) == null ||
                            localDataSource.getForecast(location.id) == null ||
                            preferencesManager.hasTemperatureUnitChanged(getPreferredUnits()))) {
                    fetchAndSaveWeatherData(location.id, location.latitude, location.longitude)
                }
                localDataSource.getLocationWithWeather(location.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location with weather", e)
            null
        }
    }

    override suspend fun setManualLocation(lat: Double, lon: Double, address: String) {
        // 1. Save to SharedPrefs
        preferencesManager.setManualLocation(lat, lon, address)

        // 2. Get/Create location ID
        val locationId = getOrCreateLocationId(lat, lon, isCurrent = true)

        // 3. Update name in DB
        localDataSource.updateLocationName(locationId, address)

        // 4. DEBUG: Verify update
        val updatedLocation = localDataSource.getLocation(locationId)
        Log.d("RepoDebug", "Updated location: ${updatedLocation?.name}")

        // 5. Fetch weather
        fetchAndSaveWeatherData(locationId, lat, lon)
    }


}