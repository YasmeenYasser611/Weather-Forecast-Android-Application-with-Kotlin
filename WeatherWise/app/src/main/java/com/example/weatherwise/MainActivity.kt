package com.example.weatherwise
import WeatherService
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.weatherwise.R
import com.example.weatherwise.data.local.LocalDataSourceImpl
import com.example.weatherwise.data.local.LocalDatabase
import com.example.weatherwise.data.model.LocationWithWeather
import com.example.weatherwise.data.remote.IWeatherRemoteDataSource
import com.example.weatherwise.data.remote.RetrofitHelper
import com.example.weatherwise.data.remote.WeatherRemoteDataSourceImpl
import com.example.weatherwise.data.repository.WeatherRepositoryImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var repository: WeatherRepositoryImpl

    private val testLocations = listOf(
        TestLocation(50.7749, -12.4194, " Francisco"),
        TestLocation(60.7128, -70.0060, " York"),
        TestLocation(71.5074, -1.1278, "ondon")
    )
    private val testUnits = "metric"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = WeatherRepositoryImpl.getInstance(
            WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)),
            LocalDataSourceImpl(
                LocalDatabase.getInstance(this).weatherDao()
            )
        )

        lifecycleScope.launch {
            runAllTests()
        }
    }

    private suspend fun runAllTests() {
        Log.d("WeatherTest", "===== STARTING TEST SUITE =====")

        // Clear any previous test data
        clearTestData()

        // Test current location functionality
        testCurrentLocationOperations()

        // Test favorite locations functionality
        testFavoriteLocationOperations()

        // Test refresh and delete operations
        testRefreshAndDeleteOperations()

        // Test offline behavior
        testOfflineBehavior()

        Log.d("WeatherTest", "===== TEST SUITE COMPLETED =====")
    }

    private suspend fun clearTestData() {
        Log.d("WeatherTest", "Clearing previous test data...")
        repository.getFavoriteLocationsWithWeather().forEach {
            repository.deleteLocation(it.location.id)
        }
    }

    private suspend fun testCurrentLocationOperations() {
        Log.d("WeatherTest", "--- Testing Current Location ---")

        // 1. Set current location
        val testLocation = testLocations[0]
        repository.setCurrentLocation(testLocation.lat, testLocation.lon, testUnits)
        Log.d("WeatherTest", "1. Current location set")

        // 2. Test cached data
        val cached = repository.getCurrentLocationWithWeather(false, true)
        Log.d("WeatherTest", "2. Cached data - Current: ${cached?.currentWeather != null}, " +
                "Forecast: ${cached?.forecast != null}")

        // 3. Test fresh data
        val fresh = repository.getCurrentLocationWithWeather(true, true)
        Log.d("WeatherTest", "3. Fresh data - Current: ${fresh?.currentWeather != null}, " +
                "Forecast: ${fresh?.forecast != null}")

        // 4. Test offline fallback
        val offline = repository.getCurrentLocationWithWeather(false, false)
        Log.d("WeatherTest", "4. Offline fallback - Data exists: ${offline != null}")

        if (offline != null) {
            Log.d("WeatherTest", "Offline data - Current: ${offline.currentWeather != null}, " +
                    "Forecast: ${offline.forecast != null}")
        }
    }

    private suspend fun testFavoriteLocationOperations() {
        Log.d("WeatherTest", "--- Testing Favorite Locations ---")

        // 1. Add favorite
        val testLocation = testLocations[1]
        val success = repository.addFavoriteLocation(
            testLocation.lat,
            testLocation.lon,
            testLocation.name,
            testUnits
        )
        Log.d("WeatherTest", "1. Add favorite success: $success")

        // 2. Verify count
        val favorites = repository.getFavoriteLocationsWithWeather()
        Log.d("WeatherTest", "2. Favorite count: ${favorites.size}")

        if (favorites.isNotEmpty()) {
            Log.d("WeatherTest", "First favorite: ${favorites[0].location.name} " +
                    "(ID: ${favorites[0].location.id})")
            Log.d("WeatherTest", "Has weather: ${favorites[0].currentWeather != null}")
        }
    }

    private suspend fun testRefreshAndDeleteOperations() {
        Log.d("WeatherTest", "===== TESTING REFRESH AND DELETE =====")

        // Get a location to test with (use first favorite if exists)
        val testLocation = repository.getFavoriteLocationsWithWeather().firstOrNull()
            ?: run {
                // If no favorites, create one
                val loc = testLocations[1]
                repository.addFavoriteLocation(loc.lat, loc.lon, loc.name, testUnits)
                delay(1000)
                repository.getFavoriteLocationsWithWeather().first()
            }

        // Test refresh
        val success = repository.refreshLocation(testLocation.location.id, testUnits)
        if (success) {
            Log.d("WeatherTest", "Refresh successful for ${testLocation.location.name}")
            val refreshed = repository.getFavoriteLocationsWithWeather()
                .first { it.location.id == testLocation.location.id }
            Log.d("WeatherTest", "Refreshed temp: ${refreshed.currentWeather?.main?.temp}°C")
        } else {
            Log.e("WeatherTest", "Refresh failed for ${testLocation.location.name}")
        }

        // Test delete
        repository.deleteLocation(testLocation.location.id)
        Log.d("WeatherTest", "Deleted location: ${testLocation.location.name}")

        // Verify deletion
        val remaining = repository.getFavoriteLocationsWithWeather()
        if (remaining.none { it.location.id == testLocation.location.id }) {
            Log.d("WeatherTest", "Deletion verified successfully")
        } else {
            Log.e("WeatherTest", "Deletion verification failed")
        }
    }

    private suspend fun testOfflineBehavior() {
        Log.d("WeatherTest", "===== TESTING OFFLINE BEHAVIOR =====")

        // Get current location data while "online"
        val onlineData = repository.getCurrentLocationWithWeather(false, true)
        if (onlineData == null) {
            Log.e("WeatherTest", "No current location to test offline behavior")
            return
        }

        Log.d("WeatherTest", "Online data: ${onlineData.currentWeather?.main?.temp}°C")

        // Simulate offline mode (pass isNetworkAvailable = false)
        val offlineData = repository.getCurrentLocationWithWeather(false, false)
        if (offlineData != null) {
            Log.d("WeatherTest", "Offline data: ${offlineData.currentWeather?.main?.temp}°C")
            if (offlineData.currentWeather?.dt == onlineData.currentWeather?.dt) {
                Log.d("WeatherTest", "Offline mode correctly returned cached data")
            } else {
                Log.e("WeatherTest", "Offline data doesn't match last online data")
            }
        } else {
            Log.e("WeatherTest", "Failed to get data in offline mode")
        }

        // Test force refresh in offline mode (should fail gracefully)
        val forceRefreshOffline = repository.getCurrentLocationWithWeather(true, false)
        if (forceRefreshOffline != null) {
            Log.d("WeatherTest", "Force refresh in offline mode fell back to cached data")
        } else {
            Log.e("WeatherTest", "Force refresh in offline mode failed completely")
        }
    }

    private data class TestLocation(
        val lat: Double,
        val lon: Double,
        val name: String
    )
}