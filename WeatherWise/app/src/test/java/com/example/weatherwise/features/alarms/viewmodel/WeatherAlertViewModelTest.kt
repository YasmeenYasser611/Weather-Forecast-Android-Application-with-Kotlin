package com.example.weatherwise.features.alarms.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.weatherwise.data.model.entity.WeatherAlert
import com.example.weatherwise.data.repository.IWeatherRepository

import getOrAwaitValue
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*

@ExperimentalCoroutinesApi
class WeatherAlertViewModelTest {

 @get:Rule
 val instantTaskExecutorRule = InstantTaskExecutorRule()

 private val testDispatcher = StandardTestDispatcher()
 private lateinit var repository: IWeatherRepository
 private lateinit var viewModel: WeatherAlertViewModel

 @Before
 fun setup() {
  Dispatchers.setMain(testDispatcher)
  repository = mockk(relaxed = true)
  viewModel = WeatherAlertViewModel(repository)
 }

 @After
 fun tearDown() {
  Dispatchers.resetMain()
 }

 @Test
 fun `addAlert should update alertsUpdated to true`() = runTest {
  val alert = WeatherAlert(
   id = "1",
   type = "Rain",
   startTime = 1620000000L,
   notificationType = "Sound",
   customSoundUri = null,
   isActive = true
  )

  coEvery { repository.saveAlert(alert) } returns Unit

  viewModel.addAlert(alert)

  advanceUntilIdle()

  Assert.assertTrue(viewModel.alertsUpdated.getOrAwaitValue())
 }


 @Test
 fun `deleteAlert should set errorMessage on failure`() = runTest {
  val exceptionMessage = "Network error"
  coEvery { repository.deleteAlert("1") } throws Exception(exceptionMessage)

  viewModel.deleteAlert("1")
  advanceUntilIdle()

  val actualMessage = viewModel.errorMessage.getOrAwaitValue()
  Assert.assertEquals("Failed to delete alert: $exceptionMessage", actualMessage)
  Assert.assertFalse(viewModel.isLoading.getOrAwaitValue())
 }

 @Test
 fun `updateAlert should set alertsUpdated to true`() = runTest {
  val alert = WeatherAlert(
   id = "2",
   type = "Storm",
   startTime = 1620000000L,
   notificationType = "Vibration",
   customSoundUri = null,
   isActive = true
  )

  coEvery { repository.updateAlert(alert) } returns Unit

  viewModel.updateAlert(alert)

  advanceUntilIdle()

  Assert.assertTrue(viewModel.alertsUpdated.getOrAwaitValue())
 }

 @Test
 fun `clearErrorMessage sets errorMessage to null`() {
  viewModel.clearErrorMessage()
  Assert.assertNull(viewModel.errorMessage.getOrAwaitValue())
 }

 @Test
 fun `clearAlertsUpdated sets alertsUpdated to false`() {
  viewModel.clearAlertsUpdated()
  Assert.assertFalse(viewModel.alertsUpdated.getOrAwaitValue())
 }
}
