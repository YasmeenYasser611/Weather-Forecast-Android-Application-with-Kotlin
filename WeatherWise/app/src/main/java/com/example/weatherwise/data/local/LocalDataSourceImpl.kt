package com.example.weatherwise.data.local

import androidx.lifecycle.LiveData
import com.example.weatherwise.data.model.entity.CurrentWeatherEntity
import com.example.weatherwise.data.model.entity.ForecastWeatherEntity
import com.example.weatherwise.data.model.entity.LocationEntity
import com.example.weatherwise.data.model.domain.LocationWithWeather
import com.example.weatherwise.data.model.entity.WeatherAlert
import com.example.weatherwise.data.model.response.CurrentWeatherResponse
import com.example.weatherwise.data.model.response.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalDataSourceImpl(private val weatherDao: WeatherDao) : ILocalDataSource {

    override suspend fun saveLocation(location: LocationEntity) {
        weatherDao.insertLocation(location)
    }

    override suspend fun getLocation(id: String): LocationEntity? {
        return weatherDao.getLocation(id)
    }



    override suspend fun getCurrentLocation(): LocationEntity? {
        return weatherDao.getCurrentLocation()
    }

    override suspend fun getFavoriteLocations(): List<LocationEntity> {
        return weatherDao.getFavoriteLocations()
    }

    override suspend fun clearCurrentLocationFlag() {
        weatherDao.clearCurrentLocationFlag()
    }

    override suspend fun setCurrentLocation(locationId: String) {
        weatherDao.setCurrentLocation(locationId)
    }

    override suspend fun setFavoriteStatus(locationId: String, isFavorite: Boolean) {
        weatherDao.setFavoriteStatus(locationId, isFavorite)
    }

    override suspend fun updateLocationName(locationId: String, name: String) {
        weatherDao.updateLocationName(locationId, name)
    }

    override suspend fun deleteLocation(locationId: String) {
        weatherDao.deleteLocation(locationId)
    }

    override suspend fun saveCurrentWeather(locationId: String, weather: CurrentWeatherResponse) {
        saveWeatherEntity(CurrentWeatherEntity(locationId, weather)) { weatherDao.insertCurrentWeather(it) }
    }

    override suspend fun getCurrentWeather(locationId: String): CurrentWeatherResponse? {
        return weatherDao.getCurrentWeather(locationId)?.weatherData
    }

    override suspend fun saveForecast(locationId: String, forecast: WeatherResponse) {
        saveWeatherEntity(ForecastWeatherEntity(locationId, forecast)) { weatherDao.insertForecast(it) }
    }

    override suspend fun getForecast(locationId: String): WeatherResponse? {
        return weatherDao.getForecast(locationId)?.forecastData
    }

    override suspend fun getLocationWithWeather(locationId: String): LocationWithWeather? {
        return weatherDao.getLocationWithWeather(locationId)?.let { dbData ->
            LocationWithWeather(
                location = dbData.location,
                currentWeather = dbData.currentWeather?.weatherData,
                forecast = dbData.forecast?.forecastData
            )
        }
    }

    override suspend fun deleteCurrentWeather(locationId: String) {
        weatherDao.deleteCurrentWeather(locationId)
    }

    override suspend fun deleteForecast(locationId: String) {
        weatherDao.deleteForecast(locationId)
    }

    override suspend fun deleteCurrentWeather(currentWeather: CurrentWeatherEntity) {
        weatherDao.deleteCurrentWeather(currentWeather)
    }

    override suspend fun deleteStaleWeather(threshold: Long) {
        weatherDao.deleteStaleWeather(threshold)
    }

    private suspend fun <T> saveWeatherEntity(entity: T, insert: suspend (T) -> Unit) {
        insert(entity)
    }


    override suspend fun findLocationByCoordinates(lat: Double, lon: Double): LocationEntity? {
        return weatherDao.findNearbyLocation(lat, lon)
    }

    // Add this new function
    override suspend fun getFavoriteLocationsWithWeather(): List<LocationWithWeather> {
        return weatherDao.getFavoriteLocations().mapNotNull { location ->
            weatherDao.getLocationWithWeather(location.id)?.let { dbData ->
                LocationWithWeather(
                    location = dbData.location,
                    currentWeather = dbData.currentWeather?.weatherData,
                    forecast = dbData.forecast?.forecastData
                )
            }
        }
    }
    override suspend fun updateLocationAddress(locationId: String, address: String) {
        weatherDao.updateLocationAddress(locationId, address)
    }

    override suspend fun saveAlert(alert: WeatherAlert) {
        weatherDao.insertAlert(alert)
    }

    override suspend fun getAlert(alertId: String): WeatherAlert? {
        return weatherDao.getAlertById(alertId)
    }

    override fun getAllAlerts(): LiveData<List<WeatherAlert>> {
        return weatherDao.getAllAlerts()
    }

    override suspend fun updateAlert(alert: WeatherAlert) {
        weatherDao.updateAlert(alert)
    }

    override suspend fun deleteAlert(alertId: String) {
        weatherDao.deleteAlert(alertId)
    }

    override suspend fun getActiveAlerts(currentTime: Long): List<WeatherAlert> {
        return weatherDao.getActiveAlerts(currentTime)

    }

}