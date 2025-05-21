package com.example.weatherwise

import WeatherService
import android.Manifest
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.example.weatherwise.data.local.LocalDataSourceImpl
import com.example.weatherwise.data.local.LocalDatabase
import com.example.weatherwise.data.model.CurrentWeatherResponse
import com.example.weatherwise.data.model.WeatherResponse
import com.example.weatherwise.data.remote.RetrofitHelper
import com.example.weatherwise.data.remote.WeatherRemoteDataSourceImpl
import com.example.weatherwise.data.repository.WeatherRepositoryImpl
import com.example.weatherwise.databinding.ActivityMainBinding
//import com.example.weatherwise.databinding.ActivityWeatherBinding
import com.example.weatherwise.location.LocationHelper
import com.example.weatherwise.mainscreen.viewmodel.HomeViewModel
import com.example.weatherwise.mainscreen.viewmodel.HomeViewModelFactory
import com.example.weatherwise.mainscreen.viewmodel.WeatherIconMapper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: HomeViewModel
    private lateinit var vmFactory: HomeViewModelFactory
//    private lateinit var hourlyAdapter: HourlyForecastAdapter
//    private lateinit var weeklyAdapter: WeeklyForecastAdapter

    companion object {
        const val MY_LOCATION_PERMISSION_ID = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Initialize ViewModel with factory (replace with your repository implementation)
        vmFactory = HomeViewModelFactory(
            repository = WeatherRepositoryImpl.getInstance(
                WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)),
                LocalDataSourceImpl(
                    LocalDatabase.getInstance(this).weatherDao()
                )
            ),
                locationHelper = LocationHelper(this),
            connectivityManager = getSystemService(ConnectivityManager::class.java)
        )
        viewModel = ViewModelProvider(this, vmFactory)[HomeViewModel::class.java]

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshWeatherData()
        }


        viewModel.loading.observe(this) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }
        viewModel.loading.observe(this) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
            if (!isLoading) {
                // Ensure swipe refresh is stopped when loading completes
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        // Setup RecyclerViews
//        setupRecyclerViews()

        // Observe LiveData
        viewModel.locationData.observe(this) { locationData ->
            binding.tvCityName.text = locationData.address
        }

        viewModel.weatherData.observe(this) { weatherData ->
            updateWeatherUI(weatherData)
        }

        viewModel.error.observe(this) { error ->
            error?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        }


        // Setup forecast tabs
        binding.tabHourly.setOnClickListener {
            binding.tabHourly.background = ContextCompat.getDrawable(this, R.drawable.tab_selected_background)
            binding.tabWeekly.background = null
            binding.rvHourlyForecast.visibility = View.VISIBLE
            binding.rvWeeklyForecast.visibility = View.GONE
        }

        binding.tabWeekly.setOnClickListener {
            binding.tabWeekly.background = ContextCompat.getDrawable(this, R.drawable.tab_selected_background)
            binding.tabHourly.background = null
            binding.rvWeeklyForecast.visibility = View.VISIBLE
            binding.rvHourlyForecast.visibility = View.GONE
        }

        // Trigger location fetch
        viewModel.getFreshLocation()
    }




    override fun onStart() {
        super.onStart()
        if (viewModel.checkLocationPermissions()) {
            if (viewModel.isLocationEnabled()) {
                viewModel.getFreshLocation()
            } else {
                viewModel.enableLocationServices()
                Toast.makeText(this, "Please turn on location", Toast.LENGTH_LONG).show()
            }
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            if (viewModel.isLocationEnabled()) {
                viewModel.getFreshLocation()
            } else {
                viewModel.enableLocationServices()
                Toast.makeText(this, "Please turn on location", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Location permissions denied", Toast.LENGTH_LONG).show()
        }
    }


    private fun updateWeatherUI(weatherData: HomeViewModel.WeatherData) {
        weatherData.currentWeather?.let { current ->
            // Update temperature
            binding.tvTemperature.text = "${current.main.temp.toInt()}°"

            // Update weather description and high/low
            val description = current.weather.firstOrNull()?.description?.capitalize(Locale.getDefault()) ?: "N/A"
            binding.tvWeatherDescription.text = "$description  H:${current.main.temp_max.toInt()}° L:${current.main.temp_min.toInt()}°"

            // Update weather icon
            current.weather.firstOrNull()?.icon?.let { iconCode ->
                val animationFile = WeatherIconMapper.getLottieAnimationForIcon(iconCode)
                binding.weatherAnimation.setAnimation(animationFile)
                binding.weatherAnimation.playAnimation()
            }

            // Update weather details
            binding.tvPressure.text = "${current.main.pressure} hPa"
            binding.tvHumidity.text = "${current.main.humidity}%"
            binding.tvWindSpeed.text = "${current.wind.speed} m/s"
            binding.tvCloudCover.text = "${current.clouds.all}%"
            binding.tvVisibility.text = "${current.visibility} m"
            binding.tvUvIndex.text = "N/A" // UV Index not in CurrentWeatherResponse
        }

        // Update date and time
        weatherData.currentWeather?.dt?.let { timestamp ->
            val dateFormat = SimpleDateFormat("MMM d, yyyy  hh:mm a", Locale.getDefault())
            binding.tvDateTime.text = dateFormat.format(Date(timestamp * 1000))
        }

        // Update Lottie animation
        weatherData.currentWeather?.weather?.firstOrNull()?.main?.let { condition ->
            val animationFile = when (condition.lowercase()) {
                "clear" -> "clear_weather.json"
                "clouds" -> "cloudy_weather.json"
                "rain" -> "rainy_weather.json"
                "snow" -> "snowy_weather.json"
                else -> "weather_animation.json"
            }
//            binding.ivWeatherBackground.setAnimation(animationFile)
        }


    }

    private fun refreshWeatherData() {
        viewModel.refreshCurrentWeather()
    }
}