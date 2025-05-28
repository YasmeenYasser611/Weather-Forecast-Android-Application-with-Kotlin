package com.example.weatherwise.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.example.weatherwise.data.local.fake.FakeLocalDataSource
import com.example.weatherwise.data.model.domain.LocationWithWeather
import com.example.weatherwise.data.model.entity.LocationEntity
import com.example.weatherwise.data.model.entity.WeatherAlert
import com.example.weatherwise.data.model.response.CurrentWeatherResponse
import com.example.weatherwise.data.model.response.GeocodingResponse
import com.example.weatherwise.data.model.response.WeatherResponse
import com.example.weatherwise.data.model.response.pojo.*
import com.example.weatherwise.data.remote.fake.FakeRemoteDataSource
import kotlinx.coroutines.runBlocking

import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.nullValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WeatherRepositoryImplTest {

 @get:Rule
 val instantTaskExecutorRule = InstantTaskExecutorRule()

 private lateinit var remoteDataSource: FakeRemoteDataSource
 private lateinit var localDataSource: FakeLocalDataSource
 private lateinit var preferencesManager: FakePreferencesManager
 private lateinit var repository: WeatherRepositoryImpl

 private val testLocation = LocationEntity(
  id = "loc1",
  name = "Cairo",
  latitude = 30.0,
  longitude = 31.0,
  isFavorite = true,
  isCurrent = false,
  address = "Cairo, Egypt"
 )

 private val testCurrentWeather = CurrentWeatherResponse(
  coord = Coordinates(31.0, 30.0),
  weather = listOf(Weather(800, "Clear", "clear sky", "01d")),
  base = "stations",
  main = Main(25.0, 24.0, 23.0, 27.0, 1013, 60),
  visibility = 10000,
  wind = Wind(3.5, 200),
  rain = null,
  snow = null,
  clouds = Clouds(0),
  dt = 1685280000,
  sys = Sys(1, 1, "EG", 1680000000, 1680040000),
  timezone = 7200,
  id = 123456,
  name = "Cairo",
  cod = 200
 )

 private val testWeatherResponse = WeatherResponse(
  cod = "200",
  message = 0,
  cnt = 1,
  list = listOf(
   Forecast(
    dt = 1685283600,
    main = Main(26.0, 25.0, 24.0, 28.0, 1012, 55),
    weather = listOf(Weather(800, "Clear", "clear sky", "01d")),
    clouds = Clouds(0),
    wind = Wind(3.0, 180),
    visibility = 10000,
    pop = 0.0,
    sys = ForecastSys("d"),
    dt_txt = "2025-05-28 12:00:00"
   )
  ),
  city = City(123, "Cairo", Coordinates(31.0, 30.0), "EG", 10000000, 7200, 1680000000, 1680040000)
 )

 @Before
 fun setup() = runBlocking {
  remoteDataSource = FakeRemoteDataSource().apply {
   setCurrentWeatherResponse(testCurrentWeather)
   setWeatherResponse(testWeatherResponse)
  }
  localDataSource = FakeLocalDataSource().apply {
   saveLocation(testLocation) // ✅ Now inside a coroutine
  }
  preferencesManager = FakePreferencesManager().apply {
   setTemperatureUnit("celsius")
   setLanguageCode("en")
  }
  repository = WeatherRepositoryImpl(remoteDataSource, localDataSource, preferencesManager)
 }

 @Test
 fun getCurrentLocationWithWeather_withNetworkAndRefresh_returnsLocationWithWeather() = runTest {
  repository.setCurrentLocation(30.0, 31.0)
  val result = repository.getCurrentLocationWithWeather(true, true)

  assertThat(result?.location?.name, `is`("Cairo"))
  assertThat(result?.currentWeather?.main?.temp, `is`(25.0))
 }

 @Test
 fun getLocationWithWeather_returnsCorrectData() = runTest {
  localDataSource.saveCurrentWeather("loc1", testCurrentWeather)
  val result = repository.getLocationWithWeather("loc1")

  assertThat(result?.location?.id, `is`("loc1"))
  assertThat(result?.currentWeather?.main?.temp, `is`(25.0))
 }

 @Test
 fun saveAndGetAlert_worksCorrectly() = runTest {
  val alert = WeatherAlert("alert1", "Storm warning", 1685290000, "", "loc1" , true)
  repository.saveAlert(alert)
  val fetched = repository.getAlert("alert1")

  assertThat(fetched?.id, `is`("alert1"))
 }


@Test
fun saveMultipleAlertsAndFetchAll_returnsAllAlerts() = runTest {
 val alert1 = WeatherAlert("alertA", "Heat warning", 1686000000, "", "loc1", true)
 val alert2 = WeatherAlert("alertB", "Rain warning", 1686000001, "", "loc1", true)

 repository.saveAlert(alert1)
 repository.saveAlert(alert2)

 val alertsLiveData = repository.getAllAlerts() as MutableLiveData<List<WeatherAlert>>
 val alerts = alertsLiveData.value ?: emptyList()

 assertThat(alerts.size, `is`(2))
 assertThat(alerts.any { it.id == "alertA" }, `is`(true))
 assertThat(alerts.any { it.id == "alertB" }, `is`(true))
}

 @Test
 fun getCurrentLocationWithWeather_withoutNetworkRefresh_returnsCachedData() = runTest {
  repository.setCurrentLocation(30.0, 31.0)
  // Simulate no network refresh
  val result = repository.getCurrentLocationWithWeather(false, false)

  assertThat(result?.location?.name, `is`("Cairo"))
 }

 @Test
 fun setCurrentLocation_updatesCurrentLocation() = runTest {
  // Given
  val newLocation = LocationEntity(
   id = "loc2",
   name = "Alexandria",
   latitude = 31.2,
   longitude = 29.9,
   isFavorite = false,
   isCurrent = false,
   address = "Alexandria, Egypt"
  )
  localDataSource.saveLocation(newLocation)

  // When
  repository.setCurrentLocation(31.2, 29.9)

  // Then
  val currentLocation = localDataSource.getCurrentLocation()
  assertThat(currentLocation?.id, `is`("loc2"))
  assertThat(currentLocation?.isCurrent, `is`(true))
 }

 @Test
 fun addFavoriteLocation_createsNewFavorite() = runTest {
  // When
  val result = repository.addFavoriteLocation(30.1, 31.1, "Giza")

  // Then
  assertThat(result, `is`(true))
  val favorites = localDataSource.getFavoriteLocations()
  assertThat(favorites.any { it.name == "Giza" }, `is`(true))
 }

 @Test
 fun removeFavoriteLocation_removesFromFavorites() = runTest {
  // Given
  repository.addFavoriteLocation(30.0, 31.0, "Cairo")

  // When
  repository.removeFavoriteLocation("loc1")

  // Then
  val favorites = localDataSource.getFavoriteLocations()
  assertThat(favorites.any { it.id == "loc1" }, `is`(false))
 }

 @Test
 fun getFavoriteLocationsWithWeather_returnsCompleteData() = runTest {
  // Given
  localDataSource.saveCurrentWeather("loc1", testCurrentWeather)
  localDataSource.saveForecast("loc1", testWeatherResponse)

  // When
  val favorites = repository.getFavoriteLocationsWithWeather()

  // Then
  assertThat(favorites.size, `is`(1))
  assertThat(favorites[0].location?.name, `is`("Cairo"))
  assertThat(favorites[0].currentWeather?.main?.temp, `is`(25.0))
 }

 @Test
 fun refreshLocation_updatesWeatherData() = runTest {
  // Given
  val newWeather = testCurrentWeather.copy(main = testCurrentWeather.main.copy(temp = 30.0))
  remoteDataSource.setCurrentWeatherResponse(newWeather)

  // When
  val result = repository.refreshLocation("loc1")

  // Then
  assertThat(result, `is`(true))
  val updated = localDataSource.getCurrentWeather("loc1")
  assertThat(updated?.main?.temp, `is`(30.0))
 }

 @Test
 fun deleteLocation_removesAllRelatedData() = runTest {
  // Given
  localDataSource.saveCurrentWeather("loc1", testCurrentWeather)
  localDataSource.saveForecast("loc1", testWeatherResponse)

  // When
  repository.deleteLocation("loc1")

  // Then
  assertThat(localDataSource.locations.any { it.id == "loc1" }, `is`(false))
  assertThat(localDataSource.getCurrentWeather("loc1"), `is`(nullValue()))
  assertThat(localDataSource.getForecast("loc1"), `is`(nullValue()))
 }

 @Test
 fun getPreferredUnits_returnsCorrectUnits() = runTest {
  // Given
  preferencesManager.setTemperatureUnit("fahrenheit")

  // When
  val units = repository.getPreferredUnits()

  // Then
  assertThat(units, `is`("imperial"))
 }

 @Test
 fun getManualLocation_returnsCorrectCoordinates() = runTest {
  // Given
  preferencesManager.setManualLocation(30.1, 31.1, "Test Address")

  // When
  val manualLocation = repository.getManualLocation()

  // Then
  assertThat(manualLocation?.first, `is`(30.1))
  assertThat(manualLocation?.second, `is`(31.1))
 }


 @Test
 fun updateAlert_modifiesExistingAlert() = runTest {
  // Given
  val alert = WeatherAlert("alert1", "Original", 1685290000, "", "loc1", true)
  repository.saveAlert(alert)

  // When
  val updatedAlert = alert.copy(type = "Updated")
  repository.updateAlert(updatedAlert)

  // Then
  val fetched = repository.getAlert("alert1")
  assertThat(fetched?.type, `is`("Updated"))
 }

 @Test
 fun deleteAlert_removesAlert() = runTest {
  // Given
  val alert = WeatherAlert("alert1", "Test", 1685290000, "", "loc1", true)
  repository.saveAlert(alert)

  // When
  repository.deleteAlert("alert1")

  // Then
  assertThat(repository.getAlert("alert1"), `is`(nullValue()))
 }

 @Test
 fun getActiveAlerts_returnsOnlyActiveAlerts() = runTest {
  // Given
  val currentTime = System.currentTimeMillis()
  val activeAlert = WeatherAlert("alert1", "Active", currentTime - 1000, "", "loc1", true)
  val futureAlert = WeatherAlert("alert2", "Future", currentTime + 100000, "", "loc1", true)
  repository.saveAlert(activeAlert)
  repository.saveAlert(futureAlert)

  // When
  val activeAlerts = repository.getActiveAlerts(currentTime)

  // Then
  assertThat(activeAlerts.size, `is`(1))
  assertThat(activeAlerts[0].id, `is`("alert1"))
 }


 @Test
 fun getCurrentLocationId_returnsCurrentLocationId() = runTest {
  // Given
  repository.setCurrentLocation(30.0, 31.0)

  // When
  val locationId = repository.getCurrentLocationId()

  // Then
  assertThat(locationId, `is`("loc1"))
 }
}