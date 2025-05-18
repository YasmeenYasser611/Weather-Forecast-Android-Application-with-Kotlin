package com.example.weatherwise.data.model

import java.io.Serializable


data class Forecast(val dt: Long, val main: MainData, val weather: List<Weather>, val wind: Wind) : Serializable