package com.example.weatherwise.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.example.weatherwise.data.local.fake.FakeLocalDataSource
import com.example.weatherwise.data.model.domain.LocationWithWeather
import com.example.weatherwise.data.model.entity.LocationEntity
import com.example.weatherwise.data.model.entity.WeatherAlert
import com.example.weatherwise.data.model.response.CurrentWeatherResponse
import com.example.weatherwise.data.model.response.WeatherResponse
import com.example.weatherwise.data.model.response.pojo.*
import com.example.weatherwise.data.remote.fake.FakeRemoteDataSource
import kotlinx.coroutines.runBlocking

import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
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

// @Test
// fun getAllAlerts_returnsLiveData() {
//  val alert = WeatherAlert("alert2", "Flood warning", 1685290000, "", "loc1" , true)
//  localDataSource.saveAlert(alert)
//  val alertsLiveData = repository.getAllAlerts() as MutableLiveData<List<WeatherAlert>>
//
//  assertThat(alertsLiveData.value?.first()?.id, `is`("alert2"))
// }
}