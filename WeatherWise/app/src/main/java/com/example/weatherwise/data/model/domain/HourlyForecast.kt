package com.example.weatherwise.data.model.domain

data class HourlyForecast(val timestamp: Long, val temperature: Double, val icon: String? ,val hour: String )