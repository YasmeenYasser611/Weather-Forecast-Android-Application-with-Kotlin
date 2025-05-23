package com.example.weatherwise.data.model.domain
data class DailyForecast(
    val day: String,
    val highTemperature: Double,
    val lowTemperature: Double,
    val icon: String?,
    val description: String?
)