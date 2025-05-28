package com.example.weatherwise.features.map


import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherwise.data.local.LocalDataSourceImpl
import com.example.weatherwise.data.local.LocalDatabase
import com.example.weatherwise.data.remote.RetrofitHelper
import com.example.weatherwise.data.remote.WeatherRemoteDataSourceImpl
import com.example.weatherwise.data.remote.WeatherService
import com.example.weatherwise.data.repository.WeatherRepositoryImpl
import com.example.weatherwise.databinding.FragmentMapBinding
import com.example.weatherwise.features.fav.viewmodel.FavoritesViewModel
import com.example.weatherwise.features.fav.viewmodel.FavoritesViewModelFactory
import com.example.weatherwise.features.map.view.LocationListAdapter
import com.example.weatherwise.features.map.model.MapRepositoryImpl
import com.example.weatherwise.features.map.model.MapState
import com.example.weatherwise.features.map.viewmodel.MapViewModel
import com.example.weatherwise.features.map.viewmodel.MapViewModelFactory
import com.example.weatherwise.features.settings.model.PreferencesManager

import com.example.weatherwise.features.settings.viewmodel.SettingsViewModel
import com.example.weatherwise.features.settings.viewmodel.SettingsViewModelFactory
import com.example.weatherwise.utils.LocationHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var favoritesViewModel: FavoritesViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private var selectedMarker: Marker? = null
    private lateinit var locationAdapter: LocationListAdapter

    private val viewModel: MapViewModel by viewModels {


        val favoritesFactory = FavoritesViewModelFactory(WeatherRepositoryImpl.getInstance(
            WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)),
            LocalDataSourceImpl(LocalDatabase.getInstance(requireContext()).weatherDao()),
            PreferencesManager(requireContext())
        ), LocationHelper(requireContext()))
        favoritesViewModel = ViewModelProvider(this, favoritesFactory)[FavoritesViewModel::class.java]

        val settingsFactory = SettingsViewModelFactory(
            LocationHelper(requireContext()),
            WeatherRepositoryImpl.getInstance(
                WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)),
                LocalDataSourceImpl(LocalDatabase.getInstance(requireContext()).weatherDao()),
                PreferencesManager(requireContext())
            ),
            PreferencesManager(requireContext()),
            requireContext()
        )
        settingsViewModel = ViewModelProvider(requireActivity(), settingsFactory)[SettingsViewModel::class.java]

        val mapRepository = MapRepositoryImpl(Dispatchers.IO, favoritesViewModel, settingsViewModel)
        MapViewModelFactory(mapRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearch()
        setupRecyclerView()
        setupMap()
        setupListeners()
        observeViewModel()
    }

    private fun setupSearch() {
        binding.searchContainer.setEndIconOnClickListener {
            binding.searchInput.text?.clear()
            binding.resultsRecyclerView.visibility = View.GONE
        }

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.length >= 3) {
                    viewModel.searchLocation(query)
                } else {
                    binding.resultsRecyclerView.visibility = View.GONE
                }
            }
        })

        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchInput.text.toString().trim()
                if (query.isNotEmpty()) {
                    viewModel.searchLocation(query)
                    true
                } else {
                    Snackbar.make(binding.root, "Please enter a location", Snackbar.LENGTH_SHORT).show()
                    false
                }
            } else {
                false
            }
        }
    }

    private fun setupRecyclerView() {
        locationAdapter = LocationListAdapter { location ->
            viewModel.geocodeLocation(location.displayName)
            binding.searchInput.setText(location.displayName)
            binding.resultsRecyclerView.visibility = View.GONE
        }
        binding.resultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = locationAdapter
            visibility = View.GONE
        }
    }

    private fun setupMap() {
        try {
            binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
            binding.mapView.setMultiTouchControls(true)

            val mapController = binding.mapView.controller
            mapController.setZoom(3.0)
            mapController.setCenter(GeoPoint(0.0, 0.0))

            val mapEventsOverlay = MapEventsOverlay(object: MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    p?.let { viewModel.onMapClick(it) }
                    return true
                }

                override fun longPressHelper(p: GeoPoint?): Boolean = false
            })
            binding.mapView.overlays.add(mapEventsOverlay)
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Failed to initialize map", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun setupListeners() {
        binding.btnSaveLocation.setOnClickListener {
            selectedMarker?.position?.let { geoPoint ->
                viewModel.saveLocation(geoPoint, arguments?.getBoolean("for_favorite") == true)
            } ?: run {
                Snackbar.make(binding.root, "Please select a location", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewModel.mapState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MapState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.statusText.text = state.message
                }
                is MapState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.statusText.text = state.message
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                }
                is MapState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.statusText.text = state.message
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                }
                is MapState.LocationSelected -> {
                    binding.progressBar.visibility = View.GONE
                    binding.statusText.text = state.message
                    updateSelectedLocation(state.geoPoint)
                    binding.mapView.controller.animateTo(state.geoPoint)
                    binding.mapView.controller.setZoom(15.0)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            locationAdapter.submitList(results)
            binding.resultsRecyclerView.visibility = if (results.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.saveComplete.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().navigateUp()
            }
        }
    }

    private fun updateSelectedLocation(geoPoint: GeoPoint) {
        selectedMarker?.let { marker ->
            binding.mapView.overlays.remove(marker)
        }

        selectedMarker = Marker(binding.mapView).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Selected Location"
        }
        binding.mapView.overlays.add(selectedMarker)
        binding.mapView.invalidate()
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
        binding.mapView.overlays.clear()
        _binding = null
    }
}

