package com.example.weatherwise

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.weatherwise.data.remote.RetrofitHelper
import com.example.weatherwise.data.remote.WeatherRemoteDataSourceImpl
import com.example.weatherwise.data.remote.WeatherService
import com.example.weatherwise.data.repository.WeatherRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        Log.i("WeatherTest", "onCreate: ")
        testWeatherApi()

    }

    private fun testWeatherApi()
    {

        val weatherRepo = WeatherRepositoryImpl.getInstance(WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)))

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = weatherRepo.get5DayForecast(
                    lat = 30.0444,  // Cairo coordinates
                    lon = 31.2357,
                    units = "metric"
                )

                launch(Dispatchers.Main) {
                    if (response != null)
                    {
                        Log.d("WeatherTest", "Success! ${response.city.name} forecast:")
                        response.list.take(5).forEach { forecast ->
                            Log.d("WeatherTest", "${forecast.dt} - Temp: ${forecast.main.temp}°C, " + "${forecast.weather[0].description}")
                        }
                    }
                    else
                    {
                        Log.e("WeatherTest", "Null response from API")
                    }
                }
            } catch (e: Exception) {
                Log.e("WeatherTest", "API Error: ${e.message}")
            }
        }
    }
}