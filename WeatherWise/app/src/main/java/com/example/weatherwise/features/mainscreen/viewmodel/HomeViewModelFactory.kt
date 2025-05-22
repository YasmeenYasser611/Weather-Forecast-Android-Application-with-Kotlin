

package com.example.weatherwise.features.mainscreen.viewmodel

import android.net.ConnectivityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.weatherwise.data.repository.IWeatherRepository
import com.example.weatherwise.location.LocationHelper

class HomeViewModelFactory(
    private val repository: IWeatherRepository,
    private val locationHelper: LocationHelper,
    private val connectivityManager: ConnectivityManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository, locationHelper, connectivityManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}