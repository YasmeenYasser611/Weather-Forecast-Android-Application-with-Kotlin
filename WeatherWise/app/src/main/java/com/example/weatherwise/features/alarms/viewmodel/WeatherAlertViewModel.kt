package com.example.weatherwise.features.alarms.viewmodel



import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherwise.data.model.entity.WeatherAlert
import com.example.weatherwise.data.repository.IWeatherRepository
import kotlinx.coroutines.launch

class WeatherAlertViewModel(
    private val repository: IWeatherRepository
) : ViewModel() {  // Regular ViewModel (no context)

    val alerts: LiveData<List<WeatherAlert>> = repository.getAllAlerts()
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Simplified to just trigger DB operations
    fun addAlert(alert: WeatherAlert) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.saveAlert(alert)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add alert: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateAlert(alert: WeatherAlert) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateAlert(alert)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update alert: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAlert(alertId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteAlert(alertId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete alert: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}