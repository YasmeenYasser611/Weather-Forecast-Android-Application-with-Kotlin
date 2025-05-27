

package com.example.weatherwise.features.mainscreen.viewmodel

import android.net.ConnectivityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.weatherwise.data.repository.IWeatherRepository
import com.example.weatherwise.features.mainscreen.usecase.GetWeatherDataUseCase
import com.example.weatherwise.features.mainscreen.usecase.HandleLocationUseCase
import com.example.weatherwise.features.mainscreen.usecase.ProcessForecastUseCase
import com.example.weatherwise.utils.LocationHelper





class HomeViewModelFactory(
    private val repository: IWeatherRepository,
    private val locationHelper: LocationHelper,
    private val connectivityManager: ConnectivityManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                repository,
                HandleLocationUseCase(repository, locationHelper),
                GetWeatherDataUseCase(repository, ProcessForecastUseCase()),
                connectivityManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}