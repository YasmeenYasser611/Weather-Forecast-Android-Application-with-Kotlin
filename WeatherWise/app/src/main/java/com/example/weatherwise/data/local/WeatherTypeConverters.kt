package com.example.weatherwise.data.local

import androidx.room.TypeConverter
import com.example.weatherwise.data.model.response.CurrentWeatherResponse
import com.example.weatherwise.data.model.response.WeatherResponse
import com.google.gson.Gson

class WeatherTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromCurrentWeatherResponse(value: CurrentWeatherResponse?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCurrentWeatherResponse(value: String?): CurrentWeatherResponse? {
        return value?.let { gson.fromJson(it, CurrentWeatherResponse::class.java) }
    }

    @TypeConverter
    fun fromForecastWeatherResponse(value: WeatherResponse?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toForecastWeatherResponse(value: String?): WeatherResponse? {
        return value?.let { gson.fromJson(it, WeatherResponse::class.java) }
    }
}