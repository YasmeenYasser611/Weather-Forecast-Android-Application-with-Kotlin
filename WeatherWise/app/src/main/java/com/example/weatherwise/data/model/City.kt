package com.example.weatherwise.data.model


data class City(
    val id: Long,
    val name: String,
    val coord: Coordinates,
    val country: String,
    val population: Long,
    val timezone: Int,
    val sunrise: Long,
    val sunset: Long
)