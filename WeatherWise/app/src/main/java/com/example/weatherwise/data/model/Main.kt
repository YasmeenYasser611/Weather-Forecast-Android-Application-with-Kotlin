package com.example.weatherwise.data.model

data class Main(val temp: Double, val feels_like: Double, val temp_min: Double, val temp_max: Double, val pressure: Int, val humidity: Int, val sea_level: Int? = null, val grnd_level: Int? = null)