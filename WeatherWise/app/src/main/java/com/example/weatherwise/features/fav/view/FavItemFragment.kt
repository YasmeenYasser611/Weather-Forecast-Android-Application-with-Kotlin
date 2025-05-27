package com.example.weatherwise.features.fav.view

import WeatherService
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherwise.R
import com.example.weatherwise.data.local.LocalDataSourceImpl
import com.example.weatherwise.data.local.LocalDatabase
import com.example.weatherwise.data.model.domain.WeatherData
import com.example.weatherwise.data.remote.RetrofitHelper
import com.example.weatherwise.data.remote.WeatherRemoteDataSourceImpl
import com.example.weatherwise.data.repository.WeatherRepositoryImpl
import com.example.weatherwise.databinding.FragmentFavItemBinding
import com.example.weatherwise.features.mainscreen.view.dailyforecat.DailyForecastAdapter
import com.example.weatherwise.features.mainscreen.view.hourlyforecast.HourlyForecastAdapter

import com.example.weatherwise.features.mainscreen.viewmodel.HomeViewModel
import com.example.weatherwise.features.mainscreen.viewmodel.HomeViewModelFactory
import com.example.weatherwise.utils.WeatherIconMapper
import com.example.weatherwise.features.settings.model.PreferencesManager
import com.example.weatherwise.utils.LocationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class FavItemFragment : Fragment()
{

        private var _binding: FragmentFavItemBinding? = null
        private val binding get() = _binding!!
        private lateinit var viewModel: HomeViewModel
        private lateinit var hourlyAdapter: HourlyForecastAdapter
        private lateinit var dailyAdapter: DailyForecastAdapter
    private lateinit var preferencesManager: PreferencesManager

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            _binding = FragmentFavItemBinding.inflate(inflater, container, false)
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val locationId = arguments?.getString("location_id") ?: run {
                findNavController().navigateUp()
                return
            }

            preferencesManager = PreferencesManager(requireContext())
            setupViewModel()
            setupAdapters()
            setupObservers()
            setupListeners()

            viewModel.loadFavoriteDetails(locationId)
        }

        private fun setupViewModel() {
            val factory = HomeViewModelFactory(
                repository = WeatherRepositoryImpl.getInstance(
                    WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)),
                    LocalDataSourceImpl(LocalDatabase.getInstance(requireContext()).weatherDao()),
                    preferencesManager
                ),
                locationHelper = LocationHelper(requireContext()),
                connectivityManager = requireContext().getSystemService(ConnectivityManager::class.java)
            )
            viewModel = ViewModelProvider(this, factory).get(HomeViewModel::class.java)
        }

    private fun setupAdapters() {
        hourlyAdapter = HourlyForecastAdapter()
        dailyAdapter = DailyForecastAdapter()

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
                hourlyAdapter.submitList(it.hourlyForecast ?: emptyList())
                dailyAdapter.submitList(it.dailyForecast ?: emptyList())
            }
        }

        viewModel.locationData.observe(viewLifecycleOwner) {
            binding.tvCityName.text = it.address
        }

        viewModel.loading.observe(viewLifecycleOwner) {
            binding.swipeRefreshLayout.isRefreshing = it
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }

    }

        private fun setupListeners() {
            binding.btnBack.setOnClickListener {
                findNavController().navigateUp()
            }

            binding.tabHourly.setOnClickListener {
                switchToHourlyForecast()
            }

            binding.tabWeekly.setOnClickListener {
                switchToDailyForecast()
            }
        }

    private fun updateWeatherUI(weatherData: WeatherData) {
        weatherData.currentWeather?.let { current ->
            binding.tvTemperature.text = "${current.main.temp.toInt()}°"

            val description = current.weather.firstOrNull()?.description?.capitalizeFirst() ?: "N/A"
            binding.tvWeatherDescription.text =
                "$description  H:${current.main.temp_max.toInt()}° L:${current.main.temp_min.toInt()}°"

            current.weather.firstOrNull()?.icon?.let { iconCode ->
                binding.weatherAnimation.apply {
                    setAnimation(WeatherIconMapper.getLottieAnimationForIcon(iconCode))
                    playAnimation()
                }
            }

            binding.tvPressure.text = "${current.main.pressure} hPa"
            binding.tvHumidity.text = "${current.main.humidity}%"
            binding.tvWindSpeed.text = formatWindSpeed(current.wind.speed)
            binding.tvCloudCover.text = "${current.clouds.all}%"
            binding.tvVisibility.text = "${current.visibility} m"
            binding.tvUvIndex.text = "N/A"

            current.dt?.let { timestamp ->
                binding.tvDateTime.text = SimpleDateFormat("MMM d, yyyy  hh:mm a", Locale.getDefault())
                    .format(Date(timestamp * 1000))
            }
        }
    }

    private fun String.capitalizeFirst() = replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

    private fun formatWindSpeed(speed: Double): String {
        val convertedSpeed = when (preferencesManager.getWindSpeedUnit()) {
            PreferencesManager.WIND_MILES_PER_HOUR -> speed * 2.23694
            else -> speed
        }
        return "%.1f %s".format(convertedSpeed, preferencesManager.getWindSpeedUnitSymbol())
    }
        private fun switchToHourlyForecast() {
            binding.rvHourlyForecast.visibility = View.VISIBLE
            binding.rvWeeklyForecast.visibility = View.GONE
            binding.tabHourly.background = ContextCompat.getDrawable(requireContext(), R.drawable.tab_selected_background)
            binding.tabWeekly.background = null
        }

        private fun switchToDailyForecast() {
            binding.rvHourlyForecast.visibility = View.GONE
            binding.rvWeeklyForecast.visibility = View.VISIBLE
            binding.tabWeekly.background = ContextCompat.getDrawable(requireContext(), R.drawable.tab_selected_background)
            binding.tabHourly.background = null
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }