package com.example.weatherwise.features.map.model



import org.osmdroid.util.GeoPoint

interface MapRepository {
    suspend fun searchLocation(query: String): List<SearchResult>
    suspend fun geocodeLocation(locationName: String): GeoPoint?
    suspend fun saveAsFavorite(geoPoint: GeoPoint): Boolean
    suspend fun saveManualLocation(geoPoint: GeoPoint): Boolean
}
