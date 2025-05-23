package com.example.weatherwise.features.fav.viewmodel

import android.net.ConnectivityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.weatherwise.data.repository.IWeatherRepository
import com.example.weatherwise.features.mainscreen.viewmodel.HomeViewModel
import com.example.weatherwise.location.LocationHelper


class FavoritesViewModelFactory(private val repository: IWeatherRepository,private val locationHelper: LocationHelper,) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoritesViewModel::class.java)) {
            return FavoritesViewModel(repository,locationHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}