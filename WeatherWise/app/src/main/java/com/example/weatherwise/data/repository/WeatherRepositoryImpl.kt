package com.example.weatherwise.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.weatherwise.data.local.ILocalDataSource
import com.example.weatherwise.data.model.entity.LocationEntity
import com.example.weatherwise.data.model.domain.LocationWithWeather
import com.example.weatherwise.data.model.entity.LocationWithWeatherDB
import com.example.weatherwise.data.model.entity.WeatherAlert
import com.example.weatherwise.data.model.response.CurrentWeatherResponse
import com.example.weatherwise.data.model.response.GeocodingResponse
import com.example.weatherwise.data.model.response.WeatherResponse
import com.example.weatherwise.data.remote.IWeatherRemoteDataSource
import com.example.weatherwise.features.settings.model.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class WeatherRepositoryImpl private constructor(private val remoteDataSource: IWeatherRemoteDataSource, private val localDataSource: ILocalDataSource, private val preferencesManager: PreferencesManager) : IWeatherRepository
{

    companion object {
        @Volatile
        private var instance: WeatherRepositoryImpl? = null
        private const val TAG = "WeatherRepository"
        private const val STALE_DATA_THRESHOLD = 24 * 60 * 60 * 1000L // 24 hours

        fun getInstance(remoteDataSource: IWeatherRemoteDataSource, localDataSource: ILocalDataSource, preferencesManager: PreferencesManager): WeatherRepositoryImpl
        {
            return instance ?: synchronized(this) {
                val temp = WeatherRepositoryImpl(remoteDataSource, localDataSource, preferencesManager)
                instance = temp
                temp
            }
        }
    }

    /********************************************************/


    override suspend fun setCurrentLocation(lat: Double, lon: Double) {
        localDataSource.clearCurrentLocationFlag()
        val locationId = getOrCreateLocationId(lat, lon, isCurrent = true)
        updateLocationWithManualAddress(locationId)
        fetchAndSaveWeatherData(locationId, lat, lon)
        localDataSource.setCurrentLocation(locationId)
    }
    private suspend fun updateLocationWithManualAddress(locationId: String) {
        if (preferencesManager.getLocationMethod() == PreferencesManager.LOCATION_MANUAL) {
            preferencesManager.getManualLocationWithAddress()?.let { (_, _, address) ->
                if (address.isNotEmpty()) {
                    localDataSource.updateLocationName(locationId, address)
                    Log.d(TAG, "Updated location name to $address for ID: $locationId")
                }
            }
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
    private suspend fun fetchAndSaveWeatherData(locationId: String, lat: Double, lon: Double): Boolean {
        return try {
            val units = preferencesManager.getApiUnits()
            val lang = preferencesManager.getLanguageCode()

            val currentWeather = remoteDataSource.getCurrentWeather(lat, lon, units, lang)
                ?: return false
            val forecast = remoteDataSource.get5DayForecast(lat, lon, units, lang)
                ?: return false

            withContext(Dispatchers.IO) {
                localDataSource.apply {
                    deleteCurrentWeather(locationId)
                    deleteForecast(locationId)
                    saveCurrentWeather(locationId, currentWeather)
                    saveForecast(locationId, forecast)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Fetch failed, using cached data", e)
            false
        }
    }

    override suspend fun getCurrentLocationWithWeather(
        forceRefresh: Boolean,
        isNetworkAvailable: Boolean
    ): LocationWithWeather? {
        return try {
            val locationId = when (preferencesManager.getLocationMethod()) {
                PreferencesManager.LOCATION_MANUAL -> {
                    preferencesManager.getManualLocationWithAddress()?.let { (lat, lon, address) ->
                        getOrCreateLocationId(lat, lon, isCurrent = true).also { id ->
                            if (address.isNotEmpty()) {
                                localDataSource.updateLocationName(id, address)
                            }
                        }
                    }
                }
                PreferencesManager.LOCATION_GPS -> {
                    localDataSource.getCurrentLocation()?.id
                }
                else -> null
            } ?: return null

            // Try network if available and needed
            if (isNetworkAvailable && (forceRefresh || needsRefresh(locationId))) {
                val success = fetchAndSaveWeatherData(locationId,
                    localDataSource.getLocation(locationId)?.latitude ?: 0.0,
                    localDataSource.getLocation(locationId)?.longitude ?: 0.0
                )
                if (!success && forceRefresh) {
                    Log.w(TAG, "Force refresh failed, using cached data")
                }
            }

            // Always fall back to local data
            localDataSource.getLocationWithWeather(locationId)?.also {
                Log.d(TAG, "Returning ${if (isNetworkAvailable) "network" else "cached"} data")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting weather data", e)
            null
        }
    }




    private suspend fun fetchLocationWithWeather(locationId: String, forceRefresh: Boolean, isNetworkAvailable: Boolean): LocationWithWeather? {
        return try {
            val location = localDataSource.getLocation(locationId) ?: return null

            if (isNetworkAvailable) {
                try {
                    if (forceRefresh || needsRefresh(locationId)) {
                        fetchAndSaveWeatherData(locationId, location.latitude, location.longitude)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Network operation failed, falling back to local data", e)
                    // Continue with local data
                }
            }

            // Always return whatever local data we have
            localDataSource.getLocationWithWeather(locationId)?.also {
                if (!isNetworkAvailable) {
                    Log.d(TAG, "Returning cached data (offline mode) for $locationId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchLocationWithWeather", e)
            null
        }
    }


    private suspend fun handleGpsLocation(forceRefresh: Boolean, isNetworkAvailable: Boolean): LocationWithWeather? {
        val currentLocation = localDataSource.getCurrentLocation() ?: return null
        return fetchLocationWithWeather(currentLocation.id, forceRefresh, isNetworkAvailable)
    }


    /****************************************************************/


    override suspend fun getLocationWithWeather(locationId: String): LocationWithWeather? {
        return localDataSource.getLocationWithWeather(locationId)
    }


    override suspend fun addFavoriteLocation(lat: Double, lon: Double, name: String): Boolean {
        return try {
            val locationId = getOrCreateLocationId(lat, lon, isFavorite = true)
            Log.d(TAG, "Setting favorite status for $locationId")
            localDataSource.setFavoriteStatus(locationId, true)
            localDataSource.updateLocationName(locationId, name)
            localDataSource.updateLocationAddress(locationId, name)
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

    override suspend fun getCurrentLocationId(): String? {
        return localDataSource.getCurrentLocation()?.id
    }

    override fun getPreferredUnits(): String {
        return preferencesManager.getApiUnits()
    }

    override fun getManualLocation(): Pair<Double, Double>? {
        return preferencesManager.getManualLocation()
    }

    override fun getLocationMethod(): String {
        return preferencesManager.getLocationMethod()
    }

    override suspend fun setManualLocation(lat: Double, lon: Double, address: String) {
        preferencesManager.setManualLocation(lat, lon, address)
        val locationId = getOrCreateLocationId(lat, lon, isCurrent = true)
        localDataSource.updateLocationName(locationId, address)
        Log.d(TAG, "Set manual location: $address, ID: $locationId")
        fetchAndSaveWeatherData(locationId, lat, lon)
    }

    override fun getManualLocationWithAddress(): Triple<Double, Double, String>? {
        return preferencesManager.getManualLocationWithAddress()
    }

    private suspend fun handleManualLocation(lat: Double, lon: Double, address: String, forceRefresh: Boolean, isNetworkAvailable: Boolean): LocationWithWeather? {
        val locationId = getOrCreateLocationId(lat, lon, isCurrent = true)
        if (address.isNotEmpty()) {
            localDataSource.updateLocationName(locationId, address)
        }
        return fetchLocationWithWeather(locationId, forceRefresh, isNetworkAvailable)
    }







    override suspend fun getReverseGeocoding(lat: Double, lon: Double): List<GeocodingResponse>? {
        return remoteDataSource.getReverseGeocoding(lat, lon)
    }



    private suspend fun needsRefresh(locationId: String): Boolean {
        val currentWeather = localDataSource.getCurrentWeather(locationId)
        val forecast = localDataSource.getForecast(locationId)
        return currentWeather == null || forecast == null || preferencesManager.hasTemperatureUnitChanged(getPreferredUnits())
    }


    override suspend fun saveAlert(alert: WeatherAlert) {
        localDataSource.saveAlert(alert)
    }

    override suspend fun getAlert(alertId: String): WeatherAlert? {
        return localDataSource.getAlert(alertId)
    }

    override fun getAllAlerts(): LiveData<List<WeatherAlert>> {
        return localDataSource.getAllAlerts()
    }

    override suspend fun updateAlert(alert: WeatherAlert) {
        localDataSource.updateAlert(alert)
    }

    override suspend fun deleteAlert(alertId: String) {
        localDataSource.deleteAlert(alertId)
    }

    override suspend fun getActiveAlerts(currentTime: Long): List<WeatherAlert> {
        return withContext(Dispatchers.IO) {
            localDataSource.getActiveAlerts(currentTime)
        }
    }

}