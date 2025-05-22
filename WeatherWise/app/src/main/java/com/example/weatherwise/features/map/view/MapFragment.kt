package com.example.weatherwise.features.map

import WeatherService
import android.content.Context
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
import com.example.weatherwise.databinding.FragmentMapBinding
import com.example.weatherwise.features.settings.model.PreferencesManager
import com.example.weatherwise.features.settings.viewmodel.SettingsViewModel
import com.example.weatherwise.features.settings.viewmodel.SettingsViewModelFactory
import com.example.weatherwise.location.LocationHelper
import com.google.android.material.snackbar.Snackbar
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SettingsViewModel
    private var selectedMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize osmdroid configuration
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
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

        viewModel = ViewModelProvider(requireActivity(), factory)[SettingsViewModel::class.java]

        setupMap()
        setupListeners()
        setupObservers()
    }

    private fun setupMap() {
        try {
            // Configure the map
            binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
            binding.mapView.setMultiTouchControls(true)

            // Set initial map position (e.g., center of the world)
            val mapController = binding.mapView.controller
            mapController.setZoom(3.0)
            mapController.setCenter(GeoPoint(0.0, 0.0))

            // Add tap event listener
            val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    p?.let { geoPoint ->
                        updateSelectedLocation(geoPoint)
                        return true
                    }
                    return false
                }

                override fun longPressHelper(p: GeoPoint?): Boolean {
                    return false
                }
            })
            binding.mapView.overlays.add(mapEventsOverlay)
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Failed to initialize map: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun setupListeners() {
        binding.btnSaveLocation.setOnClickListener {
            selectedMarker?.let { marker ->
                binding.btnSaveLocation.isEnabled = false
                binding.btnSaveLocation.text = "Saving..."

                viewModel.setManualLocationCoordinates(marker.position.latitude, marker.position.longitude)

                // Observe the save completion
                viewModel.saveComplete.observe(viewLifecycleOwner) { success ->
                    if (success) {
                        findNavController().navigateUp() // Go back to Settings
                    } else {
                        binding.btnSaveLocation.isEnabled = true
                        binding.btnSaveLocation.text = "Save Location"
                    }
                }
            } ?: run {
                Toast.makeText(requireContext(), "Please select a location", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupObservers() {
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun updateSelectedLocation(geoPoint: GeoPoint) {
        // Remove previous marker if exists
        selectedMarker?.let { binding.mapView.overlays.remove(it) }

        // Add new marker
        selectedMarker = Marker(binding.mapView).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Selected Location"
        }
        binding.mapView.overlays.add(selectedMarker)
        binding.mapView.invalidate() // Refresh map to show marker

        // Center map on selected location
        binding.mapView.controller.setCenter(geoPoint)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}