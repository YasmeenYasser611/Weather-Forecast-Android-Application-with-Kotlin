package com.example.weatherwise.data.model

import java.io.Serializable


data class WeatherResponse(val cod: String, val list: List<Forecast>, val city: City) : Serializable