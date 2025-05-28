package com.example.weatherwise.data.local.fake

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.weatherwise.data.local.ILocalDataSource
import com.example.weatherwise.data.model.entity.*
import com.example.weatherwise.data.model.domain.LocationWithWeather
import com.example.weatherwise.data.model.response.CurrentWeatherResponse
import com.example.weatherwise.data.model.response.WeatherResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeLocalDataSource : ILocalDataSource {
    // In-memory storage for testing
    val locations = mutableListOf<LocationEntity>()
    private val currentWeather = mutableMapOf<String, CurrentWeatherEntity>()
    private val forecasts = mutableMapOf<String, ForecastWeatherEntity>()



    private val alerts = mutableListOf<WeatherAlert>()
    private val alertsLiveData = MutableLiveData<List<WeatherAlert>>(alerts)
    var shouldThrowError = false // Flag to simulate errors for testing

    override suspend fun saveLocation(location: LocationEntity) {
        locations.removeAll { it.id == location.id }
        locations.add(location)
    }

    override suspend fun getLocation(id: String): LocationEntity? {
        return locations.find { it.id == id }
    }



    override suspend fun getFavoriteLocations(): List<LocationEntity> {
        return locations.filter { it.isFavorite }
    }

    override suspend fun clearCurrentLocationFlag() {
        locations.forEach { it.isCurrent = false }
    }



    override suspend fun setFavoriteStatus(locationId: String, isFavorite: Boolean) {
        locations.find { it.id == locationId }?.isFavorite = isFavorite
    }

    override suspend fun updateLocationName(locationId: String, name: String) {
        locations.find { it.id == locationId }?.name = name
    }

    override suspend fun deleteLocation(locationId: String) {
        locations.removeAll { it.id == locationId }
        currentWeather.remove(locationId)
        forecasts.remove(locationId)
    }

    override suspend fun saveCurrentWeather(locationId: String, weather: CurrentWeatherResponse) {
        currentWeather[locationId] = CurrentWeatherEntity(locationId, weather)
    }

    override suspend fun getCurrentWeather(locationId: String): CurrentWeatherResponse? {
        return currentWeather[locationId]?.weatherData
    }

    override suspend fun saveForecast(locationId: String, forecast: WeatherResponse) {
        forecasts[locationId] = ForecastWeatherEntity(locationId, forecast)
    }

    override suspend fun getForecast(locationId: String): WeatherResponse? {
        return forecasts[locationId]?.forecastData
    }

    override suspend fun getLocationWithWeather(locationId: String): LocationWithWeather? {
        val location = locations.find { it.id == locationId } ?: return null
        return LocationWithWeather(
            location = location,
            currentWeather = currentWeather[locationId]?.weatherData,
            forecast = forecasts[locationId]?.forecastData
        )
    }

    override suspend fun deleteCurrentWeather(locationId: String) {
        currentWeather.remove(locationId)
    }

    override suspend fun deleteForecast(locationId: String) {
        forecasts.remove(locationId)
    }

    override suspend fun deleteCurrentWeather(currentWeather: CurrentWeatherEntity) {
        this.currentWeather.remove(currentWeather.locationId)
    }

    override suspend fun deleteStaleWeather(threshold: Long) {
        // Implement if needed for tests
    }

    override suspend fun findLocationByCoordinates(lat: Double, lon: Double): LocationEntity? {
        return locations.find { it.latitude == lat && it.longitude == lon }
    }

    override suspend fun getFavoriteLocationsWithWeather(): List<LocationWithWeather> {
        return locations.filter { it.isFavorite }.mapNotNull { location ->
            getLocationWithWeather(location.id)
        }
    }

    override suspend fun updateLocationAddress(locationId: String, address: String) {
        locations.find { it.id == locationId }?.address = address
    }


    // In FakeLocalDataSource.kt
    override suspend fun setCurrentLocation(locationId: String) {
        locations.forEach {
            it.isCurrent = it.id == locationId  // This should work but let's verify
        }
    }

    override suspend fun getCurrentLocation(): LocationEntity? {
        return locations.find { it.isCurrent }  // Make sure this matches
    }


//    override fun getAllAlerts(): Flow<List<WeatherAlert>> {
//        return alertsFlow.asStateFlow()
//    }










    override suspend fun saveAlert(alert: WeatherAlert) {
        if (shouldThrowError) throw RuntimeException("Database error")
        alerts.removeIf { it.id == alert.id } // Replace if exists
        alerts.add(alert)
        alertsLiveData.setValue(alerts) // Synchronous update
    }

    override suspend fun getAlert(alertId: String): WeatherAlert? {
        if (shouldThrowError) throw RuntimeException("Database error")
        return alerts.find { it.id == alertId }
    }

    override fun getAllAlerts(): LiveData<List<WeatherAlert>> {
        return alertsLiveData
    }

    override suspend fun updateAlert(alert: WeatherAlert) {
        if (shouldThrowError) throw RuntimeException("Database error")
        val index = alerts.indexOfFirst { it.id == alert.id }
        if (index != -1) {
            alerts[index] = alert
            alertsLiveData.setValue(alerts) // Synchronous update
        }
    }

    override suspend fun deleteAlert(alertId: String) {
        if (shouldThrowError) throw RuntimeException("Database error")
        alerts.removeIf { it.id == alertId }
        alertsLiveData.setValue(alerts) // Synchronous update
    }

    override suspend fun getActiveAlerts(currentTime: Long): List<WeatherAlert> {
        if (shouldThrowError) throw RuntimeException("Database error")
        return alerts.filter { it.isActive && it.startTime <= currentTime }
    }

    // Helper method for tests to inspect state
    fun getAlerts(): List<WeatherAlert> = alerts
}