package com.example.weatherwise.features.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.weatherwise.data.repository.IWeatherRepository
import com.example.weatherwise.features.settings.model.PreferencesManager
import com.example.weatherwise.utils.LocationHelper

class SettingsViewModelFactory(
    private val locationHelper: LocationHelper,
    private val weatherRepository: IWeatherRepository,
    private val preferencesManager: PreferencesManager,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T
    {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(locationHelper, weatherRepository ,preferencesManager ,context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}