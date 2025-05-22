package com.example.weatherwise.mainscreen.view

import WeatherService
import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherwise.MainActivity
import com.example.weatherwise.R
import com.example.weatherwise.data.local.LocalDataSourceImpl
import com.example.weatherwise.data.local.LocalDatabase
import com.example.weatherwise.data.remote.RetrofitHelper
import com.example.weatherwise.data.remote.WeatherRemoteDataSourceImpl
import com.example.weatherwise.data.repository.WeatherRepositoryImpl
import com.example.weatherwise.databinding.FragmentWeatherBinding
import com.example.weatherwise.location.LocationHelper
import com.example.weatherwise.mainscreen.viewmodel.HomeViewModel
import com.example.weatherwise.mainscreen.viewmodel.HomeViewModelFactory
import com.example.weatherwise.mainscreen.viewmodel.WeatherIconMapper
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var vmFactory: HomeViewModelFactory
    private lateinit var hourlyAdapter: HourlyForecastAdapter
    private lateinit var dailyAdapter: DailyForecastAdapter
    private lateinit var drawerLayout: DrawerLayout

    companion object {
        const val MY_LOCATION_PERMISSION_ID = 123
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        drawerLayout = requireActivity().findViewById(R.id.drawer_layout)

        // Setup menu button click listener
        binding.btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // ViewModel setup
        vmFactory = HomeViewModelFactory(
            repository = WeatherRepositoryImpl.getInstance(
                WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)),
                LocalDataSourceImpl(LocalDatabase.getInstance(context).weatherDao())
            ),
            locationHelper = LocationHelper(context),
            connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        )
        viewModel = ViewModelProvider(this, vmFactory)[HomeViewModel::class.java]

        hourlyAdapter = HourlyForecastAdapter()
        dailyAdapter = DailyForecastAdapter()

        setupRecyclerViews()
        setupObservers()

        binding.tabHourly.setOnClickListener { switchToHourlyForecast() }
        binding.tabWeekly.setOnClickListener { switchToDailyForecast() }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshCurrentWeather()
        }

        binding.btnMenu.setOnClickListener {
            (requireActivity() as MainActivity).drawerLayout.openDrawer(GravityCompat.START)
        }
        switchToHourlyForecast()
        viewModel.getFreshLocation()
    }

    private fun setupRecyclerViews() {
        binding.rvHourlyForecast.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = hourlyAdapter
            setHasFixedSize(true)
        }

        binding.rvWeeklyForecast.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = dailyAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        viewModel.weatherData.observe(viewLifecycleOwner) { weatherData ->
            weatherData?.let {
                updateWeatherUI(it)
                it.hourlyForecast?.let { hourly -> hourlyAdapter.submitList(hourly) }
                it.dailyForecast?.let { daily -> dailyAdapter.submitList(daily) }
            }
        }

        viewModel.locationData.observe(viewLifecycleOwner) { locationData ->
            binding.tvCityName.text = locationData.address
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun switchToHourlyForecast() {
        binding.tabHourly.background = ContextCompat.getDrawable(requireContext(), R.drawable.tab_selected_background)
        binding.tabWeekly.background = null
        binding.rvHourlyForecast.visibility = View.VISIBLE
        binding.rvWeeklyForecast.visibility = View.GONE
    }

    private fun switchToDailyForecast() {
        binding.tabWeekly.background = ContextCompat.getDrawable(requireContext(), R.drawable.tab_selected_background)
        binding.tabHourly.background = null
        binding.rvWeeklyForecast.visibility = View.VISIBLE
        binding.rvHourlyForecast.visibility = View.GONE
    }

    private fun updateWeatherUI(weatherData: HomeViewModel.WeatherData) {
        weatherData.currentWeather?.let { current ->
            binding.tvTemperature.text = "${current.main.temp.toInt()}°"

            val description = current.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "N/A"
            binding.tvWeatherDescription.text =
                "$description  H:${current.main.temp_max.toInt()}° L:${current.main.temp_min.toInt()}°"

            current.weather.firstOrNull()?.icon?.let { iconCode ->
                val animationFile = WeatherIconMapper.getLottieAnimationForIcon(iconCode)
                binding.weatherAnimation.setAnimation(animationFile)
                binding.weatherAnimation.playAnimation()
            }

            binding.tvPressure.text = "${current.main.pressure} hPa"
            binding.tvHumidity.text = "${current.main.humidity}%"
            binding.tvWindSpeed.text = "${current.wind.speed} m/s"
            binding.tvCloudCover.text = "${current.clouds.all}%"
            binding.tvVisibility.text = "${current.visibility} m"
            binding.tvUvIndex.text = "N/A" // UV Index not in CurrentWeatherResponse
        }

        weatherData.currentWeather?.dt?.let { timestamp ->
            val dateFormat = SimpleDateFormat("MMM d, yyyy  hh:mm a", Locale.getDefault())
            binding.tvDateTime.text = dateFormat.format(Date(timestamp * 1000))
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
                Toast.makeText(requireContext(), "Please turn on location", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "Location permissions denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStart() {
        super.onStart()
        if (viewModel.checkLocationPermissions()) {
            if (viewModel.isLocationEnabled()) {
                viewModel.getFreshLocation()
            } else {
                viewModel.enableLocationServices()
                Toast.makeText(requireContext(), "Please turn on location", Toast.LENGTH_LONG).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}