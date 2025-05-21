package com.example.weatherwise.mainscreen.viewmodel

object WeatherIconMapper {
    fun getLottieAnimationForIcon(iconCode: String?): String {
        return when (iconCode) {
            "01d", "01n" -> "weather_sunny.json"       // clear sky
            "02d", "02n" -> "weather_partly_cloudy.json"
            "03d", "03n", "04d", "04n" -> "weather_cloudy.json"  // clouds
            "09d", "09n" -> "weather_rainy.json"       // showers
            "10d", "10n" -> "weather_heavy_rain.json"  // rain
            "11d", "11n" -> "weather_storm.json"       // thunderstorm
            "13d", "13n" -> "weather_snow.json"        // snow
            "50d", "50n" -> "weather_fog.json"         // mist/fog
            else -> "weather_unknown.json"             // default
        }
    }
}