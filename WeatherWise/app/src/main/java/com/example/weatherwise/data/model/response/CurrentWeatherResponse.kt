package com.example.weatherwise.data.model.response

data class CurrentWeatherResponse(
    val coord: Coordinates,
    val weather: List<Weather>,
    val base: String,
    val main: Main,
    val visibility: Int,
    val wind: Wind,
    val rain: Rain? = null,
    val snow: Snow? = null,
    val clouds: Clouds,
    val dt: Long,
    val sys: Sys,
    val timezone: Int,
    val id: Long,
    val name: String,
    val cod: Int
)



