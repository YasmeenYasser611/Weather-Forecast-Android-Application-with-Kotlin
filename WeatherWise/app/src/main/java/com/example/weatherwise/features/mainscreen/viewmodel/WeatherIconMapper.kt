package com.example.weatherwise.features.mainscreen.viewmodel



object WeatherIconMapper {
    fun getLottieAnimationForIcon(iconCode: String?): String {
        return when (iconCode) {
            // Clear Sky
            "01d" -> "weather_sunny.json"
            "01n" -> "clear_night.json"

            // Few Clouds
            "02d" -> "partly_cloudy_day.json"
            "02n" -> "partly_cloudy_night.json"

            // Scattered Clouds
            "03d", "03n" -> "scattered_clouds.json"

            // Broken Clouds
            "04d", "04n" -> "broken_cloudy.json"

            // Shower Rain
            "09d", "09n" -> "weather_heavy_rain.json"

            // Rain
            "10d" -> "rain_day.json"
            "10n" -> "rain_night.json"

            // Thunderstorm
            "11d", "11n" -> "thunderstorm.json"

            // Snow
            "13d", "13n" -> "weather_snow.json"

            // Mist / Fog
            "50d" -> "mist_day.json"
            "50n" -> "weather_fog.json"

            // Fallback
            else -> "weather_unknown.json"
        }
    }
}
