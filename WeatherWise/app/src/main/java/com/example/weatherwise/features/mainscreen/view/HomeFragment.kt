package com.example.weatherwise.features.mainscreen.view


import android.Manifest
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherwise.features.main.MainActivity
import com.example.weatherwise.R
import com.example.weatherwise.data.local.LocalDataSourceImpl
import com.example.weatherwise.data.local.LocalDatabase
import com.example.weatherwise.data.model.domain.WeatherData
import com.example.weatherwise.data.remote.RetrofitHelper
import com.example.weatherwise.data.remote.WeatherRemoteDataSourceImpl
import com.example.weatherwise.data.remote.WeatherService
import com.example.weatherwise.data.repository.WeatherRepositoryImpl
import com.example.weatherwise.databinding.FragmentWeatherBinding
import com.example.weatherwise.features.mainscreen.view.dailyforecast.DailyForecastAdapter
import com.example.weatherwise.features.mainscreen.view.hourlyforecast.HourlyForecastAdapter
import com.example.weatherwise.utils.LocationHelper
import com.example.weatherwise.features.mainscreen.viewmodel.*
import com.example.weatherwise.features.settings.model.PreferencesManager
import com.example.weatherwise.features.settings.viewmodel.SettingsViewModel
import com.example.weatherwise.utils.WeatherIconMapper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var hourlyAdapter: HourlyForecastAdapter
    private lateinit var dailyAdapter: DailyForecastAdapter
    private lateinit var preferencesManager: PreferencesManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionResult(permissions)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("UnsafeRepeatOnLifecycleDetector")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupViewModel()
        setupAdapters()
        setupObservers()
        setupListeners()
        viewModel.getFreshLocation()
        setupSettingsObserver()

    }

    private fun initViews() {
        preferencesManager = PreferencesManager(requireContext())
    }

    // In HomeFragment's setupViewModel()
    private fun setupViewModel() {
        val vmFactory = HomeViewModelFactory(
            repository = WeatherRepositoryImpl.getInstance(
                WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)),
                LocalDataSourceImpl(LocalDatabase.getInstance(requireContext()).weatherDao()),
                preferencesManager
            ),
            locationHelper = LocationHelper(requireContext()),
            connectivityManager = requireContext().getSystemService(ConnectivityManager::class.java)
            ,  PreferencesManager(requireContext())
        )

        viewModel = ViewModelProvider(this, vmFactory)[HomeViewModel::class.java]

        // Check if we're showing a temporary favorite location
        arguments?.let { args ->
            if (args.getBoolean("is_temporary", false)) {
                args.getString("location_id")?.let { locationId ->
                    viewModel.loadTemporaryLocation(locationId)
                    return@setupViewModel
                }
            }
        }

        // Normal behavior if not showing a temporary location
        viewModel.getFreshLocation()
    }

    private fun setupAdapters() {
        hourlyAdapter = HourlyForecastAdapter()
        dailyAdapter = DailyForecastAdapter()

        binding.rvHourlyForecast.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                .apply {
                    // Add this to prevent initial measurement issues
                    initialPrefetchItemCount = 24 // or your expected item count
                }
            adapter = hourlyAdapter
            setHasFixedSize(true)
            // Add this to prevent clipping
            setItemViewCacheSize(24) // or your expected item count
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
//                hourlyAdapter.submitList(it.hourlyForecast ?: emptyList())
                hourlyAdapter.submitList(it.hourlyForecast ?: emptyList()) {
                    binding.rvHourlyForecast.post {
                        binding.rvHourlyForecast.requestLayout()
                    }
                }
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
    fun updateLanguage() {
        val languageCode = preferencesManager.getLanguageCode()
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        binding.tvCityName.text = preferencesManager.getManualAddress()
        viewModel.weatherData.value?.currentWeather?.let { current ->
            binding.tvTemperature.text = "${current.main.temp.toInt()}°"
            val description = current.weather.firstOrNull()?.description?.capitalizeFirst() ?: "N/A"
            binding.tvWeatherDescription.text = "$description  H:${current.main.temp_max.toInt()}° L:${current.main.temp_min.toInt()}°"
        }
    }

    private fun setupListeners() {
        binding.tabHourly.setOnClickListener { switchToHourlyForecast() }
        binding.tabWeekly.setOnClickListener { switchToDailyForecast() }
        binding.swipeRefreshLayout.setOnRefreshListener { viewModel.refreshCurrentWeather() }
        binding.btnMenu.setOnClickListener {
            val isRtl = preferencesManager.getLanguage() == PreferencesManager.LANGUAGE_ARABIC
            val drawerGravity = if (isRtl) GravityCompat.END else GravityCompat.START
            (requireActivity() as MainActivity).drawerLayout.openDrawer(drawerGravity)
        }
    }

    private fun setupSettingsObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                SettingsViewModel.SettingsEventBus.settingsChanged.collect {
                    viewModel.refreshWeatherData()
                }
            }
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

    private fun handlePermissionResult(permissions: Map<String, Boolean>) {
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                if (viewModel.isLocationEnabled()) {
                    viewModel.getFreshLocation()
                } else {
                    viewModel.enableLocationServices()
                    showToast("Please turn on location")
                }
            }
            else -> showToast("Location permissions denied")
        }
    }

    override fun onStart() {
        super.onStart()
        checkLocationAvailability()
    }

    private fun checkLocationAvailability() {
        if (viewModel.checkLocationPermissions()) {
            if (viewModel.isLocationEnabled()) {
                viewModel.getFreshLocation()
            } else {
                viewModel.enableLocationServices()
                showToast("Please turn on location")
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

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}