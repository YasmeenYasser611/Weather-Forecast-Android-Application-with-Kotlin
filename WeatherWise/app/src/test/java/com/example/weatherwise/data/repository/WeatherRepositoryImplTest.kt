package com.example.weatherwise.data.repository

import com.example.weatherwise.data.model.domain.LocationWithWeather
import com.example.weatherwise.data.model.entity.LocationEntity
import com.example.weatherwise.data.model.entity.WeatherAlert
import com.example.weatherwise.data.model.response.*
import com.example.weatherwise.data.model.response.pojo.*
import com.example.weatherwise.features.settings.model.PreferencesManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherRepositoryImplTest {
 private val testDispatcher = StandardTestDispatcher()

 // Sample data
 private val currentWeatherNY = CurrentWeatherResponse(
  coord = Coordinates(lon = -74.0, lat = 40.0),
  weather = listOf(Weather(id = 800, main = "Clear", description = "clear sky", icon = "01d")),
  base = "stations",
  main = Main(temp = 25.0, feels_like = 26.0, temp_min = 24.0, temp_max = 26.0,
   pressure = 1012, humidity = 50, sea_level = 1012, grnd_level = 1000),
  visibility = 10000,
  wind = Wind(speed = 5.0, deg = 180, gust = 7.0),
  rain = null,
  snow = null,
  clouds = Clouds(all = 0),
  dt = System.currentTimeMillis() / 1000,
  sys = Sys(type = 1, id = 1234, country = "US", sunrise = 1627893600, sunset = 1627944000),
  timezone = -14400,
  id = 5128581,
  name = "New York",
  cod = 200
 )

 private val forecastNY = WeatherResponse(
  cod = "200",
  message = 0,
  cnt = 40,
  list = listOf(
   Forecast(
    dt = System.currentTimeMillis() / 1000,
    main = Main(temp = 24.0, feels_like = 25.0, temp_min = 23.0, temp_max = 25.0,
     pressure = 1012, humidity = 50, sea_level = 1012, grnd_level = 1000),
    weather = listOf(Weather(id = 801, main = "Clouds", description = "few clouds", icon = "02d")),
    clouds = Clouds(all = 20),
    wind = Wind(speed = 4.5, deg = 170, gust = 6.0),
    visibility = 10000,
    pop = 0.1,
    rain = Rain(`3h` = 0.1),
    snow = null,
    sys = ForecastSys(pod = "d"),
    dt_txt = "2025-05-28 12:00:00"
   )
  ),
  city = City(id = 5128581, name = "New York", coord = Coordinates(lat = 40.0, lon = -74.0),
   country = "US", population = 8175133, timezone = -14400,
   sunrise = 1627893600, sunset = 1627944000)
 )

 private val location1 = LocationEntity(
  id = UUID.randomUUID().toString(),
  name = "New York",
  latitude = 40.0,
  longitude = -74.0,
  isCurrent = true,
  isFavorite = false,
  address = "New York, NY",
  timestamp = System.currentTimeMillis()
 )

 private val alert1 = WeatherAlert(
  id = UUID.randomUUID().toString(),
  type = "Storm",
  startTime = System.currentTimeMillis() - 1000,
  notificationType = "Popup",
  customSoundUri = null,
  isActive = true
 )

 private lateinit var fakeDataSource: FakeWeatherDataSource
 private lateinit var fakePreferencesManager: FakePreferencesManager
 private lateinit var repository: WeatherRepositoryImpl

 @Before
 fun setup() {
  fakeDataSource = FakeWeatherDataSource().apply {
   addLocalLocation(location1)
   addLocalWeatherData(location1.id, currentWeatherNY, forecastNY)
   addRemoteWeatherData(location1.latitude, location1.longitude, currentWeatherNY, forecastNY)
  }

  fakePreferencesManager = FakePreferencesManager().apply {
   setLocationMethod(PreferencesManager.LOCATION_GPS)
   setApiUnits("metric")
   setLanguageCode("en")
  }

  repository = WeatherRepositoryImpl(
   fakeDataSource,
   fakeDataSource,
   fakePreferencesManager
  )
 }

 @After
 fun tearDown() {
  // Clean up if needed
 }

 @Test
 fun setCurrentLocation_savesLocationAndFetchesWeather() = runTest {
  val lat = 40.0
  val lon = -74.0

  repository.setCurrentLocation(lat, lon)

  val savedLocation = fakeDataSource.findLocationByCoordinates(lat, lon)
  assertThat(savedLocation?.isCurrent, `is`(true))
  assertThat(fakeDataSource.getCurrentWeather(savedLocation?.id ?: "")?.cod,
   `is`(equalTo(currentWeatherNY.cod)))
 }

 @Test
 fun getCurrentLocationWithWeather_manualLocation_updatesNameAndReturnsWeather() = runTest {
  val lat = 40.0
  val lon = -74.0
  val address = "New York, NY"
  fakePreferencesManager.setLocationMethod(PreferencesManager.LOCATION_MANUAL)
  fakePreferencesManager.setManualLocation(lat, lon, address)

  val result = repository.getCurrentLocationWithWeather(false, false)

  assertThat(result?.location?.name, `is`(address))

 }

 @Test
 fun getCurrentLocationWithWeather_networkFailure_fallsBackToCachedData() = runTest {
  fakeDataSource.shouldThrowNetworkError = true

  val result = repository.getCurrentLocationWithWeather(true, true)

  assertThat(result?.location?.id, `is`(location1.id))
  
 }

 @Test
 fun addFavoriteLocation_savesLocationAndSetsFavorite() = runTest {
  val lat = 35.0
  val lon = 139.0
  val name = "Tokyo"

  val result = repository.addFavoriteLocation(lat, lon, name)

  assertThat(result, `is`(true))
  val savedLocation = fakeDataSource.findLocationByCoordinates(lat, lon)
  assertThat(savedLocation?.isFavorite, `is`(true))
  assertThat(savedLocation?.name, `is`(name))
 }

 @Test
 fun removeFavoriteLocation_clearsFavoriteStatus() = runTest {
  // First add a favorite location
  val lat = 35.0
  val lon = 139.0
  val name = "Tokyo"
  repository.addFavoriteLocation(lat, lon, name)

  val location = fakeDataSource.findLocationByCoordinates(lat, lon)!!
  repository.removeFavoriteLocation(location.id)

  val updatedLocation = fakeDataSource.getLocation(location.id)
  assertThat(updatedLocation?.isFavorite, `is`(false))
 }

 @Test
 fun refreshLocation_fetchesAndSavesWeatherData() = runTest {
  val locationId = location1.id
  fakePreferencesManager.setTemperatureUnitChanged(true)

  val result = repository.refreshLocation(locationId)

  assertThat(result, `is`(true))
  assertThat(fakeDataSource.getCurrentWeather(locationId)?.cod,
   `is`(equalTo(currentWeatherNY.cod)))
 }

 @Test
 fun setManualLocation_updatesPreferencesAndFetchesWeather() = runTest {
  val lat = 40.0
  val lon = -74.0
  val address = "New York, NY"

  repository.setManualLocation(lat, lon, address)

  assertThat(fakePreferencesManager.getManualLocationWithAddress(),
   `is`(equalTo(Triple(lat, lon, address))))
 }

 @Test
 fun saveAndGetAlert_storesAndRetrievesAlert() = runTest {
  val alert = WeatherAlert(
   id = UUID.randomUUID().toString(),
   type = "Flood",
   startTime = System.currentTimeMillis(),
   notificationType = "Notification",
   customSoundUri = null,
   isActive = true
  )

  repository.saveAlert(alert)
  val retrievedAlert = repository.getAlert(alert.id)

  assertThat(retrievedAlert, `is`(equalTo(alert)))
 }

 @Test
 fun deleteLocation_removesLocationAndWeatherData() = runTest {
  repository.deleteLocation(location1.id)

  assertThat(fakeDataSource.getLocation(location1.id), `is`(null))
 }
}