package com.example.weatherwise.data.local



import androidx.room.TypeConverter
import com.example.weatherwise.data.model.CurrentWeatherResponse
import com.example.weatherwise.data.model.WeatherResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class WeatherTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromCurrentWeatherResponse(value: CurrentWeatherResponse?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCurrentWeatherResponse(value: String?): CurrentWeatherResponse? {
        return gson.fromJson(value, object : TypeToken<CurrentWeatherResponse>() {}.type)
    }

    @TypeConverter
    fun fromForecastWeatherResponse(value: WeatherResponse?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toForecastWeatherResponse(value: String?): WeatherResponse? {
        return gson.fromJson(value, object : TypeToken<WeatherResponse>() {}.type)
    }
}
