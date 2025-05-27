package com.example.weatherwise.features.mainscreen.usecase


import com.example.weatherwise.data.model.domain.LocationData
import com.example.weatherwise.data.model.domain.WeatherData
import com.example.weatherwise.data.repository.IWeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetWeatherDataUseCase(
    private val repository: IWeatherRepository,
    private val processForecastUseCase: ProcessForecastUseCase
) {
    suspend operator fun invoke(
        lat: Double?,
        lon: Double?,
        forceRefresh: Boolean,
        isNetworkAvailable: Boolean
    ): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            if (lat == null || lon == null) {
                return@withContext Result.failure(IllegalStateException("No location available"))
            }
            repository.setCurrentLocation(lat, lon)
            val weatherData = repository.getCurrentLocationWithWeather(
                forceRefresh = forceRefresh,
                isNetworkAvailable = isNetworkAvailable
            ) ?: return@withContext Result.failure(IllegalStateException("No weather data available"))

            val currentTime = System.currentTimeMillis() / 1000L
            val hourlyForecast = processForecastUseCase.processHourlyForecast(weatherData.forecast, currentTime)
            val dailyForecast = processForecastUseCase.processDailyForecast(weatherData.forecast)

            Result.success(
                WeatherData(
                    currentWeather = weatherData.currentWeather,
                    forecast = weatherData.forecast,
                    hourlyForecast = hourlyForecast,
                    dailyForecast = dailyForecast
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}