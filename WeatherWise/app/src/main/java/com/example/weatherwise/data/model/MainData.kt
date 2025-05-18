package com.example.weatherwise.data.model

import java.io.Serializable


data class MainData(val temp: Double, val humidity: Int, val pressure: Int) : Serializable