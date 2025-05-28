package com.example.weatherwise.features.settings.view

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.weatherwise.R
import com.example.weatherwise.data.local.LocalDataSourceImpl
import com.example.weatherwise.data.local.LocalDatabase
import com.example.weatherwise.data.remote.RetrofitHelper
import com.example.weatherwise.data.remote.WeatherRemoteDataSourceImpl
import com.example.weatherwise.data.repository.WeatherRepositoryImpl
import com.example.weatherwise.databinding.FragmentSettingsBinding
import com.example.weatherwise.features.main.MainActivity
import com.example.weatherwise.features.settings.model.PreferencesManager
import com.example.weatherwise.features.settings.viewmodel.SettingsViewModel
import com.example.weatherwise.features.settings.viewmodel.SettingsViewModelFactory
import com.example.weatherwise.utils.LocationHelper
import com.google.android.material.snackbar.Snackbar
import  com.example.weatherwise.data.remote.WeatherService
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SettingsViewModel
    private lateinit var notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private lateinit var overlayPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            viewModel.onNotificationPermissionResult(isGranted)
        }
        overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                viewModel.onOverlayPermissionResult(Settings.canDrawOverlays(requireContext()))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val factory = SettingsViewModelFactory(
            LocationHelper(requireContext()),
            WeatherRepositoryImpl.getInstance(
                WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)),
                LocalDataSourceImpl(LocalDatabase.getInstance(requireContext()).weatherDao()),
                PreferencesManager(requireContext())
            ),
            PreferencesManager(requireContext()),
            requireContext()
        )
        viewModel = ViewModelProvider(requireActivity(), factory)[SettingsViewModel::class.java]
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.requestOverlayPermission.observe(viewLifecycleOwner) { shouldRequest ->
            if (shouldRequest && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${requireContext().packageName}"))
                overlayPermissionLauncher.launch(intent)
            }
        }
        viewModel.locationMethod.observe(viewLifecycleOwner) { method ->
            binding.rgLocationMethod.check(
                when (method) {
                    PreferencesManager.LOCATION_GPS -> R.id.rb_gps
                    PreferencesManager.LOCATION_MANUAL -> R.id.rb_manual
                    else -> R.id.rb_gps
                }

            )
            binding.btnSelectLocation.visibility = if (method == PreferencesManager.LOCATION_MANUAL) View.VISIBLE else View.GONE
        }
        viewModel.temperatureUnit.observe(viewLifecycleOwner) { unit ->
            binding.rgTempUnit.check(
                when (unit) {
                    PreferencesManager.TEMP_CELSIUS -> R.id.rb_celsius
                    PreferencesManager.TEMP_FAHRENHEIT -> R.id.rb_fahrenheit
                    PreferencesManager.TEMP_KELVIN -> R.id.rb_kelvin
                    else -> R.id.rb_celsius
                }
            )
        }
        viewModel.windSpeedUnit.observe(viewLifecycleOwner) { unit ->
            binding.rgWindUnit.check(
                when (unit) {
                    PreferencesManager.WIND_METERS_PER_SEC -> R.id.rb_meters_sec
                    PreferencesManager.WIND_MILES_PER_HOUR -> R.id.rb_miles_hour
                    else -> R.id.rb_meters_sec
                }
            )
        }
        viewModel.language.observe(viewLifecycleOwner) { language ->
            binding.rgLanguage.check(
                when (language) {
                    "ar" -> R.id.rb_arabic
                    else -> R.id.rb_english
                }
            )
        }
        viewModel.notificationsEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchNotifications.isChecked = enabled
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
        }
        viewModel.navigateToMap.observe(viewLifecycleOwner) { navigate ->
            if (navigate == true) {
                findNavController().navigate(R.id.action_settings_to_map)
                viewModel.resetNavigateToMap()
            }
        }
        viewModel.notificationPermissionRequest.observe(viewLifecycleOwner) { shouldRequest ->
            if (shouldRequest && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.rgLanguage.setOnCheckedChangeListener { _, checkedId ->
            val language = when (checkedId) {
                R.id.rb_arabic -> "ar"
                else -> "en"
            }
            viewModel.setLanguage(language)
        }
        binding.rgTempUnit.setOnCheckedChangeListener { _, checkedId ->
            viewModel.setTemperatureUnit(
                when (checkedId) {
                    R.id.rb_celsius -> PreferencesManager.TEMP_CELSIUS
                    R.id.rb_fahrenheit -> PreferencesManager.TEMP_FAHRENHEIT
                    R.id.rb_kelvin -> PreferencesManager.TEMP_KELVIN
                    else -> PreferencesManager.TEMP_CELSIUS
                }
            )
        }
        binding.rgWindUnit.setOnCheckedChangeListener { _, checkedId ->
            viewModel.setWindSpeedUnit(
                when (checkedId) {
                    R.id.rb_meters_sec -> PreferencesManager.WIND_METERS_PER_SEC
                    R.id.rb_miles_hour -> PreferencesManager.WIND_MILES_PER_HOUR
                    else -> PreferencesManager.WIND_METERS_PER_SEC
                }
            )
        }
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (binding.switchNotifications.isPressed) {
                viewModel.setNotificationsEnabled(isChecked, fromUser = true)
            }
        }
        binding.btnSaveSettings.setOnClickListener {
            viewModel.saveSettings()
            val language = when (binding.rgLanguage.checkedRadioButtonId) {
                R.id.rb_arabic -> "ar"
                else -> "en"
            }

            // Update locale immediately
            (requireActivity() as MainActivity).updateLocale(language)

            // Update this fragment's UI
            updateLanguage()

            // Show toast in the new language
            val toastText = if (language == "ar") "تم حفظ الإعدادات" else "Settings saved"
            Toast.makeText(requireContext(), toastText, Toast.LENGTH_SHORT).show()

            // Navigate back (optional, remove if you want to stay in settings)
            findNavController().navigateUp()
        }
        binding.rgLocationMethod.setOnCheckedChangeListener { _, checkedId ->
            val locationMethod = when (checkedId) {
                R.id.rb_manual -> PreferencesManager.LOCATION_MANUAL
                R.id.rb_gps -> PreferencesManager.LOCATION_GPS
                else -> PreferencesManager.LOCATION_GPS
            }
            viewModel.setLocationMethod(locationMethod)
        }
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (binding.switchNotifications.isPressed) {
                viewModel.setNotificationsEnabled(isChecked, fromUser = true)
            }
        }
        binding.btnSelectLocation.setOnClickListener {
            viewModel.selectManualLocation()
        }
    }

    fun updateLanguage() {
        val languageCode = PreferencesManager(requireContext()).getLanguageCode()
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        binding.rgLanguage.check(
            when (languageCode) {
                "ar" -> R.id.rb_arabic
                else -> R.id.rb_english
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}