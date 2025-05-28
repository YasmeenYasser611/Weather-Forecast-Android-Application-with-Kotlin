package com.example.weatherwise.features.map.viewmodel



import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherwise.features.map.model.MapRepository
import com.example.weatherwise.features.map.model.MapState
import com.example.weatherwise.features.map.model.SearchResult

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint

class MapViewModel(private val repository: MapRepository) : ViewModel() {

    private val _mapState = MutableLiveData<MapState>()
    val mapState: LiveData<MapState> = _mapState

    private val _searchResults = MutableLiveData<List<SearchResult>>()
    val searchResults: LiveData<List<SearchResult>> = _searchResults

    private val _saveComplete = MutableLiveData<Boolean>()
    val saveComplete: LiveData<Boolean> = _saveComplete

    private var searchJob: Job? = null

    fun searchLocation(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _mapState.postValue(MapState.Loading("Searching for \"$query\"..."))

            delay(500)

            try {
                val results = withContext(Dispatchers.IO) {
                    repository.searchLocation(query)
                }
                _searchResults.postValue(results)
                _mapState.postValue(
                    if (results.isEmpty()) {
                        MapState.Error("No results found for \"$query\"")
                    } else {
                        MapState.Success("Found ${results.size} location(s)")
                    }
                )
            } catch (e: Exception) {
                _mapState.postValue(MapState.Error("Search failed: ${e.message}"))
                _searchResults.postValue(emptyList())
            }
        }
    }

    fun geocodeLocation(locationName: String) {
        viewModelScope.launch {
            _mapState.postValue(MapState.Loading("Locating \"$locationName\"..."))
            try {
                val point = withContext(Dispatchers.IO) {
                    repository.geocodeLocation(locationName)
                }
                if (point != null) {
                    _mapState.postValue(MapState.LocationSelected(point, "Location set: $locationName"))
                } else {
                    _mapState.postValue(MapState.Error("Location not found: $locationName"))
                }
            } catch (e: Exception) {
                _mapState.postValue(MapState.Error("Geocoding failed: ${e.message}"))
            }
        }
    }

    fun saveLocation(geoPoint: GeoPoint, isFavorite: Boolean) {
        viewModelScope.launch {
            _mapState.postValue(MapState.Loading("Saving location..."))
            try {
                val success = if (isFavorite) {
                    repository.saveAsFavorite(geoPoint)
                } else {
                    repository.saveManualLocation(geoPoint)
                }
                _saveComplete.postValue(success)
                _mapState.postValue(
                    if (success) {
                        MapState.Success("Location saved successfully")
                    } else {
                        MapState.Error("Failed to save location")
                    }
                )
            } catch (e: Exception) {
                _mapState.postValue(MapState.Error("Save failed: ${e.message}"))
                _saveComplete.postValue(false)
            }
        }
    }

    fun onMapClick(geoPoint: GeoPoint) {
        _mapState.postValue(MapState.LocationSelected(geoPoint, "Map location selected"))
    }
}