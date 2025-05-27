package com.example.weatherwise.features.map.model


import org.osmdroid.util.GeoPoint

sealed class MapState {
    data class Loading(val message: String) : MapState()
    data class Success(val message: String) : MapState()
    data class Error(val message: String) : MapState()
    data class LocationSelected(val geoPoint: GeoPoint, val message: String) : MapState()
}

