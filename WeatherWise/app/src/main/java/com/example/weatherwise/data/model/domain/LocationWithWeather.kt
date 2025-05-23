package com.example.weatherwise.data.model.domain

import com.example.weatherwise.data.model.response.CurrentWeatherResponse
import com.example.weatherwise.data.model.entity.LocationEntity
import com.example.weatherwise.data.model.response.WeatherResponse

data class LocationWithWeather(val location: LocationEntity, val currentWeather: CurrentWeatherResponse?, val forecast: WeatherResponse?)