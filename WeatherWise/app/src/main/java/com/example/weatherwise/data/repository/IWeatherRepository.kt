package com.example.weatherwise.data.repository




import com.example.weatherwise.data.model.CurrentWeatherData
import com.example.weatherwise.data.model.FavouritePlace
import com.example.weatherwise.data.model.WeatherResponse



interface IWeatherRepository {


    // Remote data operations
    suspend fun get5DayForecast(lat: Double, lon: Double, units: String): WeatherResponse?

    suspend fun getCurrentWeather(lat: Double, lon: Double, units: String, forceRefresh: Boolean = false, isNetworkAvailable: Boolean): CurrentWeatherData?

    // Local data operations
    suspend fun getFavoriteLocations(): List<FavouritePlace>
    suspend fun addFavoriteLocation(location: FavouritePlace, weatherData: CurrentWeatherData)
    suspend fun removeFavoriteLocation(location: FavouritePlace)
    suspend fun getWeatherForFavorite(locationId: Int): CurrentWeatherData?

}