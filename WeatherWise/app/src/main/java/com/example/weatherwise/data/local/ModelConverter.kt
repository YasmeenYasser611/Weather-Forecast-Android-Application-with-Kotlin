package com.example.weatherwise.data.local

import com.example.weatherwise.data.model.CurrentWeatherData
import com.example.weatherwise.data.model.FavouritePlace
import com.example.weatherwise.data.model.WeatherResponse

// ModelConverter.kt
object ModelConverter {
    fun weatherResponseToFavoriteLocation(weatherResponse: WeatherResponse, lat: Double, lon: Double,isCurrentLocation: Boolean = false): FavouritePlace {
        return FavouritePlace(
            cityName = weatherResponse.city.name,
            country = weatherResponse.city.country,
            latitude = lat,
            longitude = lon,
            isCurrentLocation = isCurrentLocation
        )
    }

    fun weatherResponseToCurrentWeatherData(weatherResponse: WeatherResponse, locationId: Int): CurrentWeatherData {
        val currentWeather = weatherResponse.list.first()
        return CurrentWeatherData(
            locationId = locationId,
            temperature = currentWeather.main.temp,
            humidity = currentWeather.main.humidity,
            pressure = currentWeather.main.pressure,
            windSpeed = currentWeather.wind.speed,
            weatherDescription = currentWeather.weather.first().description,
            weatherIcon = currentWeather.weather.first().icon

        )
    }
}