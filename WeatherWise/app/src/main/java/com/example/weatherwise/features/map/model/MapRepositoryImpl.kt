package com.example.weatherwise.features.map.model

import com.example.weatherwise.features.fav.viewmodel.FavoritesViewModel
import com.example.weatherwise.features.settings.viewmodel.SettingsViewModel
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.util.GeoPoint
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.CoroutineContext

class MapRepositoryImpl(
    private val ioDispatcher: CoroutineContext,
    private val favoritesViewModel: FavoritesViewModel,
    private val settingsViewModel: SettingsViewModel
) : MapRepository {

    override suspend fun searchLocation(query: String): List<SearchResult> {
        return withContext(ioDispatcher) {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?format=json&q=$encodedQuery&limit=10")
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "WeatherWiseApp/1.0")

            try {
                val responseCode = connection.responseCode
                if (responseCode != HttpsURLConnection.HTTP_OK) {
                    return@withContext emptyList()
                }
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)
                val results = mutableListOf<SearchResult>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    results.add(
                        SearchResult(
                            displayName = item.optString("display_name", ""),
                            lat = item.optString("lat").toDoubleOrNull(),
                            lon = item.optString("lon").toDoubleOrNull()
                        )
                    )
                }
                results.filter { it.displayName.isNotEmpty() }
            } catch (e: Exception) {
                emptyList()
            } finally {
                connection.disconnect()
            }
        }
    }

    override suspend fun geocodeLocation(locationName: String): GeoPoint? {
        return withContext(ioDispatcher) {
            val encodedLocation = URLEncoder.encode(locationName, "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?format=json&q=$encodedLocation&limit=1")
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "WeatherWiseApp/1.0")

            try {
                val responseCode = connection.responseCode
                if (responseCode != HttpsURLConnection.HTTP_OK) {
                    return@withContext null
                }
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)

                if (jsonArray.length() > 0) {
                    val firstResult = jsonArray.getJSONObject(0)
                    val lat = firstResult.optString("lat").toDoubleOrNull()
                    val lon = firstResult.optString("lon").toDoubleOrNull()
                    if (lat != null && lon != null) GeoPoint(lat, lon) else null
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        }
    }

    override suspend fun saveAsFavorite(geoPoint: GeoPoint): Boolean {
        return try {
            val address = favoritesViewModel.getAddressForCoordinates(geoPoint.latitude, geoPoint.longitude)
            val locationName = address ?: "Location (${"%.2f".format(geoPoint.latitude)}, ${"%.2f".format(geoPoint.longitude)})"
            favoritesViewModel.addFavoriteLocation(geoPoint.latitude, geoPoint.longitude, locationName)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun saveManualLocation(geoPoint: GeoPoint): Boolean {
        return try {
            settingsViewModel.setManualLocationCoordinates(geoPoint.latitude, geoPoint.longitude)
            true
        } catch (e: Exception) {
            false
        }
    }
}