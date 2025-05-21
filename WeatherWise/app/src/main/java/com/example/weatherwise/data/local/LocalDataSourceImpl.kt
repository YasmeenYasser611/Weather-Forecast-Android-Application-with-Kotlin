package com.example.weatherwise.data.local

import com.example.weatherwise.data.model.CurrentWeatherEntity
import com.example.weatherwise.data.model.CurrentWeatherResponse
import com.example.weatherwise.data.model.ForecastWeatherEntity
import com.example.weatherwise.data.model.LocationEntity
import com.example.weatherwise.data.model.LocationWithWeather
import com.example.weatherwise.data.model.WeatherResponse

class LocalDataSourceImpl(private val weatherDao: WeatherDao) : ILocalDataSource {

    override suspend fun saveLocation(location: LocationEntity) {
        weatherDao.insertLocation(location)
    }

    override suspend fun getLocation(id: String): LocationEntity? {
        return weatherDao.getLocation(id)
    }

    override suspend fun findLocationByCoordinates(lat: Double, lon: Double): LocationEntity? {
        return weatherDao.findLocationByCoordinates(lat, lon)
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
        weatherDao.insertCurrentWeather(CurrentWeatherEntity(locationId = locationId, weatherData = weather))
    }

    override suspend fun getCurrentWeather(locationId: String): CurrentWeatherResponse? {
        return weatherDao.getCurrentWeather(locationId)?.weatherData
    }

    override suspend fun saveForecast(locationId: String, forecast: WeatherResponse) {
        weatherDao.insertForecast(
            ForecastWeatherEntity(locationId = locationId, forecastData = forecast)
        )
    }

    override suspend fun getForecast(locationId: String): WeatherResponse? {
        return weatherDao.getForecast(locationId)?.forecastData
    }

    override suspend fun getLocationWithWeather(locationId: String): LocationWithWeather? {
        return weatherDao.getLocationWithWeather(locationId)?.let { dbData ->
            LocationWithWeather(location = dbData.location, currentWeather = dbData.currentWeather?.weatherData, forecast = dbData.forecast?.forecastData)
        }
    }
}
