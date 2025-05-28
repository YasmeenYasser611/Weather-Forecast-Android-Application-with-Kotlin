package com.example.weatherwise.features.fav.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherwise.data.model.domain.LocationWithWeather
import com.example.weatherwise.data.repository.IWeatherRepository
import com.example.weatherwise.utils.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesViewModel(private val repository: IWeatherRepository, private val locationHelper: LocationHelper,) : ViewModel() {

    private val _favorites = MutableLiveData<List<LocationWithWeather>>()
    val favorites: LiveData<List<LocationWithWeather>> = _favorites

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _favorites.value = repository.getFavoriteLocationsWithWeather()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeFavorite(locationId: String) {
        viewModelScope.launch {
            repository.removeFavoriteLocation(locationId)
            loadFavorites()
        }
    }

    fun refreshFavorite(locationId: String) {
        viewModelScope.launch {
            repository.refreshLocation(locationId)
            loadFavorites()
        }
    }
    suspend fun getAddressForCoordinates(lat: Double, lon: Double): String? {
        return try {
            val response = repository.getReverseGeocoding(lat, lon)
            response?.firstOrNull()?.getAddressName()
        } catch (e: Exception) {
            null
        }
    }

    fun refreshAllFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val favorites = repository.getFavoriteLocationsWithWeather()
                favorites.forEach { favorite ->
                    repository.refreshLocation(favorite.location.id)
                }
                _favorites.value = repository.getFavoriteLocationsWithWeather()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    suspend fun addFavoriteLocation(lat: Double, lon: Double, name: String): Boolean {
        return try {
            val address = withContext(Dispatchers.IO) {
                locationHelper.getLocationAddress(lat, lon).first
            }
            Log.i("add", "addFavoriteLocation: $address")
            repository.addFavoriteLocation(lat, lon, address)
            true
        } catch (e: Exception) {
            false
        }
    }
}