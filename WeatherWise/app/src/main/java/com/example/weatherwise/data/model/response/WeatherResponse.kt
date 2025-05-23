package com.example.weatherwise.data.model.response


data class WeatherResponse(val cod: String, val message: Int, val cnt: Int, val list: List<Forecast>, val city: City)



