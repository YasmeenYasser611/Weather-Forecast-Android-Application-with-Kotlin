package com.example.weatherwise

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.weatherwise.data.local.LocalDataSourceImpl
import com.example.weatherwise.data.local.LocalDatabase
import com.example.weatherwise.data.model.CurrentWeatherData
import com.example.weatherwise.data.model.FavouritePlace
import com.example.weatherwise.data.remote.RetrofitHelper
import com.example.weatherwise.data.remote.WeatherRemoteDataSourceImpl
import com.example.weatherwise.data.remote.WeatherService
import com.example.weatherwise.data.repository.WeatherRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

lateinit var weatherRepo :WeatherRepositoryImpl
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        weatherRepo = WeatherRepositoryImpl.getInstance(WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)) , LocalDataSourceImpl(
            LocalDatabase.getInstance(this).favoriteLocationDao() , LocalDatabase.getInstance(this).weatherDataDao() ) )

        Log.i("WeatherTest", "onCreate: ")
        testWeatherApi()
        testLocalDatabaseOperations()
        testCurrentLocationHandling()
        testOfflineScenarios()

    }

    private fun testWeatherApi() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Test 1: 5-day forecast
                Log.d("WeatherTest", "===== TEST 1: 5-Day Forecast =====")
                val forecast = weatherRepo.get5DayForecast(
                    lat = 30.0444,  // Cairo coordinates
                    lon = 31.2357,
                    units = "metric"
                )

                launch(Dispatchers.Main) {
                    forecast?.let {
                        Log.d("WeatherTest", "1*Success! ${it.city.name} forecast:")
                        it.list.take(5).forEach { forecast ->
                            Log.d("WeatherTest", " 1-${forecast.dt} - Temp: ${forecast.main.temp}°C, ${forecast.weather[0].description}")
                        }
                    } ?: Log.e("WeatherTest", "1-Null forecast response")
                }

                // Test 2: Current weather
                Log.d("WeatherTest", "\n===== TEST 2: Current Weather (Online) =====")
                val currentWeather = weatherRepo.getCurrentWeather(
                    lat = 30.0444,
                    lon = 31.2357,
                    units = "metric",
                    forceRefresh = true,
                    isNetworkAvailable = true
                )

                currentWeather?.let {
                    Log.d("WeatherTest", "2-Current weather: ${it.temperature}°C, ${it.weatherDescription}")
                    Log.d("WeatherTest", "2-Location ID: ${it.locationId}")
                } ?: Log.e("WeatherTest", "2-Null current weather response")

            } catch (e: Exception) {
                Log.e("WeatherTest", "2-API Error: ${e.message}")
            }
        }
    }

    private fun testLocalDatabaseOperations() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Test data
                val testLocation = FavouritePlace(
                    cityName = "Test City",
                    country = "Test Country",
                    latitude = 30.1234,
                    longitude = 31.5678,
                    isCurrentLocation = false
                )

                val testWeather = CurrentWeatherData(
                    locationId = 1,
                    temperature = 25.0,
                    humidity = 60,
                    pressure = 1012,
                    windSpeed = 5.5,
                    weatherDescription = "Sunny",
                    weatherIcon = "01d"
                )

                // Test 3: Favorite locations
                Log.d("WeatherTest", "3-\n===== TEST 3: Favorite Locations =====")

                // Add favorite
                weatherRepo.addFavoriteLocation(testLocation, testWeather)
                Log.d("WeatherTest", "3-Added favorite location")

                // Get all favorites
                val favorites = weatherRepo.getFavoriteLocations()
                Log.d("WeatherTest", "3-Favorites count: ${favorites.size}")
                favorites.forEach {
                    Log.d("WeatherTest", "3-Favorite: ${it.cityName} (ID: ${it.id}), Current: ${it.isCurrentLocation}")
                }

                // Get weather for favorite
                if (favorites.isNotEmpty()) {
                    val favWeather = weatherRepo.getWeatherForFavorite(favorites.first().id)
                    Log.d("WeatherTest", "3-Favorite weather: ${favWeather?.temperature}°C")
                }

                // Remove favorite
                if (favorites.isNotEmpty()) {
                    weatherRepo.removeFavoriteLocation(favorites.first())
                    Log.d("WeatherTest", "3-Removed favorite location")
                }

            } catch (e: Exception) {
                Log.e("WeatherTest", "3-Database Error: ${e.message}")
            }
        }
    }

    private fun testCurrentLocationHandling() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Test 4: Current location handling
                Log.d("WeatherTest", "\n===== TEST 4: Current Location Handling =====")

                // First get current weather (will mark as current location)
                val cairoWeather = weatherRepo.getCurrentWeather(
                    lat = 30.0444,
                    lon = 31.2357,
                    units = "metric",
                    forceRefresh = true,
                    isNetworkAvailable = true
                )

                // Verify current location was set
                val currentLocation =  LocalDataSourceImpl(
                    LocalDatabase.getInstance(this@MainActivity).favoriteLocationDao() , LocalDatabase.getInstance(this@MainActivity).weatherDataDao() ).getCurrentLocation()

                currentLocation?.let {
                    Log.d("WeatherTest", "4-Current location set to: ${it.cityName} (ID: ${it.id})")
                    Log.d("WeatherTest", "4-Coordinates: ${it.latitude}, ${it.longitude}")
                } ?: Log.e("WeatherTest", "4-No current location set")

                // Test getting weather while offline
                val offlineWeather = weatherRepo.getCurrentWeather(
                    lat = 30.0444,
                    lon = 31.2357,
                    units = "metric",
                    forceRefresh = false,
                    isNetworkAvailable = false
                )

                offlineWeather?.let {
                    Log.d("WeatherTest", "4-Offline weather retrieved: ${it.temperature}°C")
                } ?: Log.e("WeatherTest", "4-Failed to get offline weather")

                // Test coordinate matching
                val nearbyWeather = weatherRepo.getCurrentWeather(
                    lat = 30.0445,  // Slightly different coordinates
                    lon = 31.2356,
                    units = "metric",
                    forceRefresh = false,
                    isNetworkAvailable = false
                )

                nearbyWeather?.let {
                    Log.d("WeatherTest", "4-Nearby coordinates matched existing location")
                } ?: Log.e("WeatherTest", "4-Nearby coordinates didn't match")

            } catch (e: Exception) {
                Log.e("WeatherTest", "4-Current Location Error: ${e.message}")
            }
        }
    }

    private fun testOfflineScenarios() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Test 5: Offline scenarios
                Log.d("WeatherTest", "\n===== TEST 5: Offline Scenarios =====")

                // Test with no network and no existing location
                val unknownLocationWeather = weatherRepo.getCurrentWeather(
                    lat = 40.7128,  // New York (not in DB)
                    lon = -74.0060,
                    units = "metric",
                    forceRefresh = false,
                    isNetworkAvailable = false
                )

                if (unknownLocationWeather == null) {
                    Log.d("WeatherTest", "5-Correctly returned null for unknown offline location")
                } else {
                    Log.e("WeatherTest", "5-Incorrectly returned data for unknown offline location")
                }

                // Test fallback to most recent
                val mostRecent =  LocalDataSourceImpl(
                    LocalDatabase.getInstance(this@MainActivity).favoriteLocationDao() , LocalDatabase.getInstance(this@MainActivity).weatherDataDao() ).getMostRecentWeatherData()

                mostRecent?.let {
                    Log.d("WeatherTest", "5-Most recent weather fallback: ${it.temperature}°C")
                } ?: Log.d("WeatherTest", "5-No recent weather data available")

            } catch (e: Exception) {
                Log.e("WeatherTest", "5-ffline Error: ${e.message}")
            }
        }
    }


}



