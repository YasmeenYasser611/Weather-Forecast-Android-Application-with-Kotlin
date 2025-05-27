package com.example.weatherwise.features.fav.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.weatherwise.data.repository.IWeatherRepository
import com.example.weatherwise.utils.LocationHelper


class FavoritesViewModelFactory(private val repository: IWeatherRepository, private val locationHelper: LocationHelper,) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoritesViewModel::class.java)) {
            return FavoritesViewModel(repository,locationHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}