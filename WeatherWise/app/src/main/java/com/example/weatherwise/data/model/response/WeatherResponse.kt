package com.example.weatherwise.data.model.response

import com.example.weatherwise.data.model.response.pojo.City
import com.example.weatherwise.data.model.response.pojo.Forecast


data class WeatherResponse(val cod: String, val message: Int, val cnt: Int, val list: List<Forecast>, val city: City)