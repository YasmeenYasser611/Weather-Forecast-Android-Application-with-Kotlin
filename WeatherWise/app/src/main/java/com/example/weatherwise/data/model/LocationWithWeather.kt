package com.example.weatherwise.data.model

data class LocationWithWeather(val location: LocationEntity, val currentWeather: CurrentWeatherResponse?, val forecast: WeatherResponse?)