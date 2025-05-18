package com.example.weatherwise.data.model



data class WeatherResponse(val cod: String, val list: List<Forecast>, val city: City) {
    data class Forecast(val dt: Long, val main: MainData, val weather: List<Weather>, val wind: Wind)
    {
        data class MainData(val temp: Double, val humidity: Int, val pressure: Int)

        data class Weather(val id: Int, val main: String, val description: String, val icon: String)

        data class Wind(val speed: Double, val deg: Int)
    }

    data class City(val id: Int, val name: String, val country: String)
}