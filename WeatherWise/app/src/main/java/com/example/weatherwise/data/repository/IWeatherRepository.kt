package com.example.weatherwise.data.repository
import androidx.lifecycle.LiveData
import com.example.weatherwise.data.model.domain.LocationWithWeather
import com.example.weatherwise.data.model.entity.LocationWithWeatherDB
import com.example.weatherwise.data.model.entity.WeatherAlert
import com.example.weatherwise.data.model.response.GeocodingResponse


interface IWeatherRepository {
    suspend fun setCurrentLocation(lat: Double, lon: Double)
    suspend fun getCurrentLocationWithWeather(forceRefresh: Boolean, isNetworkAvailable: Boolean): LocationWithWeather?
    suspend fun getCurrentLocationId(): String?

    suspend fun addFavoriteLocation(lat: Double, lon: Double, name: String): Boolean
    suspend fun removeFavoriteLocation(locationId: String)
    suspend fun getFavoriteLocationsWithWeather(): List<LocationWithWeather>
    suspend fun refreshLocation(locationId: String): Boolean
    suspend fun deleteLocation(locationId: String)

    fun getPreferredUnits(): String

    fun getManualLocation(): Pair<Double, Double>?
    fun getLocationMethod(): String
    suspend fun setManualLocation(lat: Double, lon: Double, address: String)
    fun getManualLocationWithAddress(): Triple<Double, Double, String>?
    suspend fun getReverseGeocoding(lat: Double, lon: Double): List<GeocodingResponse>?
    suspend fun getLocationWithWeather(locationId: String): LocationWithWeather?

    suspend fun saveAlert(alert: WeatherAlert)
    suspend fun getAlert(alertId: String): WeatherAlert?
    fun getAllAlerts(): LiveData<List<WeatherAlert>>
    suspend fun updateAlert(alert: WeatherAlert)
    suspend fun deleteAlert(alertId: String)

    suspend fun getActiveAlerts(currentTime: Long): List<WeatherAlert>
}