package com.example.weatherwise.data.repository

import com.example.weatherwise.data.local.ILocalDataSource
import com.example.weatherwise.data.model.domain.LocationWithWeather
import com.example.weatherwise.data.model.entity.*
import com.example.weatherwise.data.model.response.CurrentWeatherResponse
import com.example.weatherwise.data.model.response.GeocodingResponse
import com.example.weatherwise.data.model.response.WeatherResponse
import com.example.weatherwise.data.remote.IWeatherRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class FakeWeatherDataSource : IWeatherRemoteDataSource, ILocalDataSource {
    // Remote data
    private var remoteCurrentWeather: MutableMap<String, CurrentWeatherResponse> = mutableMapOf()
    private var remoteForecasts: MutableMap<String, WeatherResponse> = mutableMapOf()
    private var remoteGeocoding: MutableMap<Pair<Double, Double>, List<GeocodingResponse>> = mutableMapOf()
    var shouldThrowNetworkError = false

    // Local data
    private val _locations = MutableStateFlow<List<LocationEntity>>(emptyList())
    private val _currentWeather = MutableStateFlow<Map<String, CurrentWeatherResponse>>(emptyMap())
    private val _forecasts = MutableStateFlow<Map<String, WeatherResponse>>(emptyMap())
    private val _alerts = MutableStateFlow<List<WeatherAlert>>(emptyList())
    private var currentLocationId: String? = null

    // Remote Data Source Implementation
    override suspend fun getCurrentWeather(
        lat: Double,
        lon: Double,
        units: String,
        lang: String
    ): CurrentWeatherResponse? {
        if (shouldThrowNetworkError) throw Exception("Network error")
        return remoteCurrentWeather.values.find {
            it.coord.lat == lat && it.coord.lon == lon
        }
    }

    override suspend fun get5DayForecast(
        lat: Double,
        lon: Double,
        units: String,
        lang: String
    ): WeatherResponse? {
        if (shouldThrowNetworkError) throw Exception("Network error")
        return remoteForecasts.values.find {
            it.city.coord.lat == lat && it.city.coord.lon == lon
        }
    }

    override suspend fun getReverseGeocoding(lat: Double, lon: Double): List<GeocodingResponse>? {
        return remoteGeocoding[Pair(lat, lon)]
    }

    // Local Data Source Implementation
    override suspend fun saveLocation(location: LocationEntity) {
        _locations.value = _locations.value.filterNot { it.id == location.id } + location
    }

    override suspend fun getLocation(locationId: String): LocationEntity? {
        return _locations.value.find { it.id == locationId }
    }

    override suspend fun findLocationByCoordinates(lat: Double, lon: Double): LocationEntity? {
        return _locations.value.find { it.latitude == lat && it.longitude == lon }
    }

    override suspend fun setCurrentLocation(locationId: String) {
        currentLocationId = locationId
        _locations.value = _locations.value.map {
            it.copy(isCurrent = it.id == locationId)
        }
    }

    override suspend fun getCurrentLocation(): LocationEntity? {
        return currentLocationId?.let { getLocation(it) }
    }

    override suspend fun clearCurrentLocationFlag() {
        _locations.value = _locations.value.map { it.copy(isCurrent = false) }
        currentLocationId = null
    }

    override suspend fun setFavoriteStatus(locationId: String, isFavorite: Boolean) {
        _locations.value = _locations.value.map {
            if (it.id == locationId) it.copy(isFavorite = isFavorite) else it
        }
    }

    override suspend fun getFavoriteLocations(): List<LocationEntity> {
        return _locations.value.filter { it.isFavorite }
    }

    override suspend fun deleteLocation(locationId: String) {
        _locations.value = _locations.value.filterNot { it.id == locationId }
        _currentWeather.value = _currentWeather.value - locationId
        _forecasts.value = _forecasts.value - locationId
    }

    override suspend fun saveCurrentWeather(locationId: String, weather: CurrentWeatherResponse) {
        _currentWeather.value = _currentWeather.value + (locationId to weather)
    }

    override suspend fun getCurrentWeather(locationId: String): CurrentWeatherResponse? {
        return _currentWeather.value[locationId]
    }

    override suspend fun deleteCurrentWeather(locationId: String) {
        _currentWeather.value = _currentWeather.value - locationId
    }

    override suspend fun deleteCurrentWeather(currentWeather: CurrentWeatherEntity) {
        TODO("Not yet implemented")
    }

    override suspend fun saveForecast(locationId: String, forecast: WeatherResponse) {
        _forecasts.value = _forecasts.value + (locationId to forecast)
    }

    override suspend fun getForecast(locationId: String): WeatherResponse? {
        return _forecasts.value[locationId]
    }

    override suspend fun deleteForecast(locationId: String) {
        _forecasts.value = _forecasts.value - locationId
    }

    override suspend fun deleteStaleWeather(threshold: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun getFavoriteLocationsWithWeather(): List<LocationWithWeather> {
        TODO("Not yet implemented")
    }

    override suspend fun getLocationWithWeather(locationId: String): LocationWithWeatherDB? {
        val location = getLocation(locationId) ?: return null
        val currentWeather = getCurrentWeather(locationId)
        val forecast = getForecast(locationId)
        return if (currentWeather != null && forecast != null) {
            LocationWithWeatherDB(location, currentWeather, forecast)
        } else {
            null
        }
    }

    override suspend fun saveAlert(alert: WeatherAlert) {
        _alerts.value = _alerts.value.filterNot { it.id == alert.id } + alert
    }

    override suspend fun getAlert(alertId: String): WeatherAlert? {
        return _alerts.value.find { it.id == alertId }
    }

    override fun getAllAlerts(): Flow<List<WeatherAlert>> {
        return _alerts.asStateFlow()
    }

    override suspend fun updateAlert(alert: WeatherAlert) {
        _alerts.value = _alerts.value.map { if (it.id == alert.id) alert else it }
    }

    override suspend fun deleteAlert(alertId: String) {
        _alerts.value = _alerts.value.filterNot { it.id == alertId }
    }

    override suspend fun getActiveAlerts(currentTime: Long): List<WeatherAlert> {
        return _alerts.value.filter { it.isActive }
    }

    override suspend fun updateLocationName(locationId: String, name: String) {
        _locations.value = _locations.value.map {
            if (it.id == locationId) it.copy(name = name) else it
        }
    }

    override suspend fun updateLocationAddress(locationId: String, address: String) {
        _locations.value = _locations.value.map {
            if (it.id == locationId) it.copy(address = address) else it
        }
    }

    // Test helper methods
    fun addRemoteWeatherData(lat: Double, lon: Double, current: CurrentWeatherResponse, forecast: WeatherResponse) {
        remoteCurrentWeather[UUID.randomUUID().toString()] = current
        remoteForecasts[UUID.randomUUID().toString()] = forecast
    }

    fun addLocalLocation(location: LocationEntity) {
        _locations.value = _locations.value + location
    }

    fun addLocalWeatherData(locationId: String, current: CurrentWeatherResponse, forecast: WeatherResponse) {
        _currentWeather.value = _currentWeather.value + (locationId to current)
        _forecasts.value = _forecasts.value + (locationId to forecast)
    }
}