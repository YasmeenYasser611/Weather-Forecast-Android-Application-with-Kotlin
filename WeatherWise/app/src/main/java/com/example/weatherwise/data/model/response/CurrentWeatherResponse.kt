package com.example.weatherwise.data.model.response

import com.example.weatherwise.data.model.response.pojo.Clouds
import com.example.weatherwise.data.model.response.pojo.Coordinates
import com.example.weatherwise.data.model.response.pojo.Main
import com.example.weatherwise.data.model.response.pojo.Rain
import com.example.weatherwise.data.model.response.pojo.Snow
import com.example.weatherwise.data.model.response.pojo.Sys
import com.example.weatherwise.data.model.response.pojo.Weather
import com.example.weatherwise.data.model.response.pojo.Wind

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