package com.example.weatherwise.data.repository

import com.example.weatherwise.data.local.ILocalDataSource
import com.example.weatherwise.data.local.ModelConverter
import com.example.weatherwise.data.model.CurrentWeatherData
import com.example.weatherwise.data.model.FavouritePlace
import com.example.weatherwise.data.model.WeatherResponse
import com.example.weatherwise.data.remote.IWeatherRemoteDataSource

class WeatherRepositoryImpl private constructor(
    private val remoteDataSource: IWeatherRemoteDataSource,
    private val localDataSource: ILocalDataSource) : IWeatherRepository {

    companion object {
        @Volatile
        private var instance: WeatherRepositoryImpl? = null

        fun getInstance(
            remoteDataSource: IWeatherRemoteDataSource,
            localDataSource: ILocalDataSource): WeatherRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: WeatherRepositoryImpl(remoteDataSource, localDataSource).also {
                    instance = it
                }
            }
        }
    }


    override suspend fun get5DayForecast(lat: Double, lon: Double, units: String): WeatherResponse? {
        return remoteDataSource.get5DayForecast(lat, lon, units)
    }

    override suspend fun getCurrentWeather(
        lat: Double,
        lon: Double,
        units: String,
        forceRefresh: Boolean,
        isNetworkAvailable: Boolean
    ): CurrentWeatherData? {
        // Try to find matching location (with coordinate tolerance)
        val existingLocation = localDataSource.findLocationByCoordinates(lat, lon)

        // Case 1: Force refresh (only if network available)
        if (forceRefresh) {
            return if (isNetworkAvailable) {
                fetchAndSaveCurrentWeather(lat, lon, units, existingLocation?.id, true)
            } else {
                existingLocation?.id?.let { localDataSource.getWeatherDataForLocation(it) }
            }
        }

        // Case 2: Return local data if available
        existingLocation?.id?.let { locationId ->
            return localDataSource.getWeatherDataForLocation(locationId)
        }

        // Case 3: No existing location - fetch if online
        return if (isNetworkAvailable) {
            fetchAndSaveCurrentWeather(lat, lon, units, null, true)
        } else {
            // Last resort: return most recent data
            localDataSource.getMostRecentWeatherData()
        }
    }

    private suspend fun fetchAndSaveCurrentWeather(
        lat: Double,
        lon: Double,
        units: String,
        locationId: Int?,
        isCurrentLocation: Boolean = false
    ): CurrentWeatherData? {
        return try {
            remoteDataSource.getCurrentWeather(lat, lon, units)?.let { response ->
                // Clear previous current location
                localDataSource.clearCurrentLocationFlag()

                val finalLocationId = locationId ?: run {
                    val newLocation = ModelConverter.weatherResponseToFavoriteLocation(
                        response, lat, lon, isCurrentLocation
                    )
                    localDataSource.insertFavoriteLocation(newLocation)
                    newLocation.id
                }

                // Update current location flag if needed
                if (isCurrentLocation) {
                    localDataSource.getFavoriteLocationById(finalLocationId)?.let { location ->
                        localDataSource.setCurrentLocation(location.copy(isCurrentLocation = true))
                    }
                }

                val weatherData = ModelConverter.weatherResponseToCurrentWeatherData(response, finalLocationId)
                localDataSource.saveWeatherDataForLocation(weatherData)
                weatherData
            }
        } catch (e: Exception) {
            locationId?.let { localDataSource.getWeatherDataForLocation(it) }
        }
    }



    override suspend fun getFavoriteLocations(): List<FavouritePlace> {
        return localDataSource.getAllFavoriteLocations()
    }

    override suspend fun addFavoriteLocation(location: FavouritePlace, weatherData: CurrentWeatherData) {
        localDataSource.insertFavoriteLocation(location)
        localDataSource.saveWeatherDataForLocation(weatherData)
    }

    override suspend fun removeFavoriteLocation(location: FavouritePlace) {
        localDataSource.deleteFavoriteLocation(location)
    }

    override suspend fun getWeatherForFavorite(locationId: Int): CurrentWeatherData? {
        return localDataSource.getWeatherDataForLocation(locationId)
    }



}