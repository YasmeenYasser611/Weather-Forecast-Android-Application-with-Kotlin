package com.example.weatherwise.data.repository

import com.example.weatherwise.data.local.fake.FakeLocalDataSource
import com.example.weatherwise.data.model.response.CurrentWeatherResponse
import com.example.weatherwise.data.model.response.WeatherResponse
import com.example.weatherwise.data.model.response.pojo.City
import com.example.weatherwise.data.model.response.pojo.Clouds
import com.example.weatherwise.data.model.response.pojo.Coordinates
import com.example.weatherwise.data.model.response.pojo.Main
import com.example.weatherwise.data.model.response.pojo.Sys
import com.example.weatherwise.data.model.response.pojo.Wind
import com.example.weatherwise.data.remote.fake.FakeRemoteDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WeatherRepositoryTest {
 private lateinit var repository: WeatherRepositoryImpl
 private lateinit var fakeLocalDataSource: FakeLocalDataSource
 private lateinit var fakeRemoteDataSource: FakeRemoteDataSource
 private lateinit var fakePreferencesManager: FakePreferencesManager

 @Before
 fun setup() {
  fakeLocalDataSource = FakeLocalDataSource()
  fakeRemoteDataSource = FakeRemoteDataSource()
  fakePreferencesManager = FakePreferencesManager()

  repository = WeatherRepositoryImpl.getInstance(
   fakeRemoteDataSource,
   fakeLocalDataSource,
   fakePreferencesManager
  )
 }

 @Test
 fun `getCurrentLocationWithWeather should return cached data when offline`() = runTest {
  // Given
  val testLat = 40.7128
  val testLon = -74.0060
  repository.setCurrentLocation(testLat, testLon)

  // When (simulate offline)
  val result = repository.getCurrentLocationWithWeather(
   forceRefresh = false,
   isNetworkAvailable = false
  )

  // Then
  assertNotNull(result)
  assertEquals(testLat, result?.location?.latitude)
  assertEquals(testLon, result?.location?.longitude)
 }

 @Test
 fun `addFavoriteLocation should mark location as favorite`() = runTest {
  // Given
  val testLat = 34.0522
  val testLon = -118.2437
  val testName = "Los Angeles"

  // When
  val success = repository.addFavoriteLocation(testLat, testLon, testName)

  // Then
  assertTrue(success)
  val favorites = fakeLocalDataSource.getFavoriteLocations()
  assertEquals(1, favorites.size)
  assertEquals(testName, favorites[0].name)
  assertTrue(favorites[0].isFavorite)
 }
}