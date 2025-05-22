package com.example.weatherwise.features.settings.view

import WeatherService
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.example.weatherwise.features.settings.model.PreferencesManager
import com.example.weatherwise.features.settings.viewmodel.SettingsViewModel
import com.example.weatherwise.features.settings.viewmodel.SettingsViewModelFactory
import com.example.weatherwise.location.LocationHelper
import com.google.android.material.snackbar.Snackbar

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel with dependencies
        val factory = SettingsViewModelFactory(
            LocationHelper(requireContext()),
            WeatherRepositoryImpl.getInstance(
                WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)),
                LocalDataSourceImpl(LocalDatabase.getInstance(requireContext()).weatherDao()),
                PreferencesManager(requireContext())
            ),
            PreferencesManager(requireContext())
        )

        // Create ViewModel using the factory
        // In both SettingsFragment and MapFragment, use:
        viewModel = ViewModelProvider(requireActivity(), factory)[SettingsViewModel::class.java]

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.locationMethod.observe(viewLifecycleOwner) { method ->
            binding.rgLocationMethod.check(
                when (method) {
                    PreferencesManager.LOCATION_GPS -> R.id.rb_gps
                    PreferencesManager.LOCATION_MANUAL -> R.id.rb_manual
                    else -> R.id.rb_gps
                }
            )
            binding.btnSelectLocation.visibility =
                if (method == PreferencesManager.LOCATION_MANUAL) View.VISIBLE else View.GONE

//            if (method == PreferencesManager.LOCATION_MANUAL) {
//                // Show the saved address
//                val address = preferencesManager.getManualAddress()
//                if (address.isNotEmpty()) {
//                    binding.tvManualLocation.text = address
//                    binding.tvManualLocation.visibility = View.VISIBLE
//                }
//            } else {
//                binding.tvManualLocation.visibility = View.GONE
//            }
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
                    PreferencesManager.LANGUAGE_ENGLISH -> R.id.rb_english
                    PreferencesManager.LANGUAGE_ARABIC -> R.id.rb_arabic
                    else -> R.id.rb_english
                }
            )
        }

        viewModel.notificationsEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchNotifications.isChecked = enabled
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.navigateToMap.observe(viewLifecycleOwner) { navigate ->
            if (navigate == true) {
                findNavController().navigate(R.id.action_settings_to_map)
                viewModel.resetNavigateToMap()
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.rgLocationMethod.setOnCheckedChangeListener { _, checkedId ->
            viewModel.setLocationMethod(
                when (checkedId) {
                    R.id.rb_gps -> PreferencesManager.LOCATION_GPS
                    R.id.rb_manual -> PreferencesManager.LOCATION_MANUAL
                    else -> PreferencesManager.LOCATION_GPS
                }
            )
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

        binding.rgLanguage.setOnCheckedChangeListener { _, checkedId ->
            viewModel.setLanguage(
                when (checkedId) {
                    R.id.rb_english -> PreferencesManager.LANGUAGE_ENGLISH
                    R.id.rb_arabic -> PreferencesManager.LANGUAGE_ARABIC
                    else -> PreferencesManager.LANGUAGE_ENGLISH
                }
            )
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setNotificationsEnabled(isChecked)
        }

        binding.btnSelectLocation.setOnClickListener {
            viewModel.selectManualLocation()
        }

        binding.btnSaveSettings.setOnClickListener {
            viewModel.saveSettings()
            Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}