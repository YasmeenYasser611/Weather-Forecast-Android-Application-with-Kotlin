package com.example.weatherwise.data.model.response.pojo

data class Main(var temp: Double, val feels_like: Double, var temp_min: Double, var temp_max: Double, val pressure: Int, val humidity: Int, val sea_level: Int? = null, val grnd_level: Int? = null)