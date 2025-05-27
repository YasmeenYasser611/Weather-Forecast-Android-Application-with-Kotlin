package com.example.weatherwise.features.map

import WeatherService
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherwise.data.local.LocalDataSourceImpl
import com.example.weatherwise.data.local.LocalDatabase
import com.example.weatherwise.data.remote.RetrofitHelper
import com.example.weatherwise.data.remote.WeatherRemoteDataSourceImpl
import com.example.weatherwise.data.repository.WeatherRepositoryImpl
import com.example.weatherwise.databinding.FragmentMapBinding
import com.example.weatherwise.features.fav.viewmodel.FavoritesViewModel
import com.example.weatherwise.features.fav.viewmodel.FavoritesViewModelFactory
import com.example.weatherwise.features.map.view.LocationAdapter
import com.example.weatherwise.features.settings.model.PreferencesManager
import com.example.weatherwise.features.settings.viewmodel.SettingsViewModel
import com.example.weatherwise.features.settings.viewmodel.SettingsViewModelFactory
import com.example.weatherwise.utils.LocationHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var favoritesViewModel: FavoritesViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private var selectedMarker: Marker? = null
    private var searchResults = mutableListOf<String>()
    private lateinit var locationAdapter: LocationAdapter
    private var lastSearchTime = 0L
    private val searchDelay = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // Initialize ViewModels
        val repository = WeatherRepositoryImpl.getInstance(
            WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)),
            LocalDataSourceImpl(LocalDatabase.getInstance(requireContext()).weatherDao()),
            PreferencesManager(requireContext())
        )

        val favoritesFactory = FavoritesViewModelFactory(repository, LocationHelper(requireContext()))
        favoritesViewModel = ViewModelProvider(this, favoritesFactory)[FavoritesViewModel::class.java]

        val settingsFactory = SettingsViewModelFactory(
            LocationHelper(requireContext()),
            repository,
            PreferencesManager(requireContext()),
            requireContext()
        )
        settingsViewModel = ViewModelProvider(requireActivity(), settingsFactory)[SettingsViewModel::class.java]

        setupSearch()
        setupRecyclerView()
        setupMap()
        setupListeners()

        binding.btnSaveLocation.text = if (arguments?.getBoolean("for_favorite") == true) {
            "Save as Favorite"
        } else {
            "Save Location"
        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.length >= 3) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastSearchTime >= searchDelay) {
                        lastSearchTime = currentTime
                        searchLocation(query)
                    }
                } else {
                    searchResults.clear()
                    locationAdapter.notifyDataSetChanged()
                    binding.resultsRecyclerView.visibility = View.GONE
                    binding.statusText.text = "Enter at least 3 characters to search"
                    binding.progressBar.visibility = View.GONE
                }
            }
        })

        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchInput.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchLocation(query)
                    true
                } else {
                    binding.statusText.text = "Please enter a location"
                    Snackbar.make(binding.root, "Please enter a location", Snackbar.LENGTH_SHORT).show()
                    false
                }
            } else {
                false
            }
        }
    }

    private fun setupRecyclerView() {
        locationAdapter = LocationAdapter(searchResults) { location ->
            geocodeLocation(location)
            binding.searchInput.setText(location)
            binding.resultsRecyclerView.visibility = View.GONE
        }
        binding.resultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = locationAdapter
            visibility = View.GONE // Hidden until results are available
        }
    }

    private fun searchLocation(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.statusText.text = "Searching for \"$query\"..."
                val results = withContext(Dispatchers.IO) {
                    searchLocationNominatim(query)
                }

                searchResults.clear()
                searchResults.addAll(results)
                locationAdapter.notifyDataSetChanged()

                if (results.isEmpty()) {
                    binding.resultsRecyclerView.visibility = View.GONE
                    binding.statusText.text = "No results found for \"$query\""
                    Snackbar.make(binding.root, "No results found", Snackbar.LENGTH_SHORT).show()
                } else {
                    binding.resultsRecyclerView.visibility = View.VISIBLE
                    binding.statusText.text = "Found ${results.size} location(s)"
                }
            } catch (e: Exception) {
                Log.e("MapFragment", "Search failed: ${e.message}", e)
                binding.statusText.text = "Search failed: ${e.message}"
                binding.resultsRecyclerView.visibility = View.GONE
                Snackbar.make(binding.root, "Search failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun searchLocationNominatim(query: String): List<String> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://nominatim.openstreetmap.org/search?format=json&q=$encodedQuery&limit=10")
        val connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "WeatherWiseApp/1.0")

        return try {
            val responseCode = connection.responseCode
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                Log.e("MapFragment", "Nominatim API error: HTTP $responseCode")
                return emptyList()
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(response)
            val results = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val displayName = item.optString("display_name", "")
                if (displayName.isNotEmpty()) {
                    results.add(displayName)
                }
            }
            results
        } catch (e: Exception) {
            Log.e("MapFragment", "Nominatim API parse error: ${e.message}", e)
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    private fun geocodeLocation(locationName: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.statusText.text = "Locating \"$locationName\"..."
                val result = withContext(Dispatchers.IO) {
                    val encodedLocation = URLEncoder.encode(locationName, "UTF-8")
                    val url = URL("https://nominatim.openstreetmap.org/search?format=json&q=$encodedLocation&limit=1")
                    val connection = url.openConnection() as HttpsURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "WeatherWiseApp/1.0")

                    val responseCode = connection.responseCode
                    if (responseCode != HttpsURLConnection.HTTP_OK) {
                        Log.e("MapFragment", "Geocode API error: HTTP $responseCode")
                        return@withContext null
                    }
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)

                    if (jsonArray.length() > 0) {
                        val firstResult = jsonArray.getJSONObject(0)
                        val lat = firstResult.optString("lat").toDoubleOrNull()
                        val lon = firstResult.optString("lon").toDoubleOrNull()
                        if (lat != null && lon != null) GeoPoint(lat, lon) else null
                    } else {
                        null
                    }
                }

                if (result != null) {
                    updateSelectedLocation(result)
                    binding.mapView.controller.animateTo(result)
                    binding.mapView.controller.setZoom(15.0)
                    binding.statusText.text = "Location set: $locationName"
                    Snackbar.make(binding.root, "Location set: $locationName", Snackbar.LENGTH_SHORT).show()
                } else {
                    binding.statusText.text = "Location not found: $locationName"
                    Snackbar.make(binding.root, "Location not found", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MapFragment", "Geocode failed: ${e.message}", e)
                binding.statusText.text = "Geocode failed: ${e.message}"
                Snackbar.make(binding.root, "Geocode failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupMap() {
        try {
            binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
            binding.mapView.setMultiTouchControls(true)

            val mapController = binding.mapView.controller
            mapController.setZoom(3.0)
            mapController.setCenter(GeoPoint(0.0, 0.0))

            val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    if (p != null) {
                        updateSelectedLocation(p)
                        binding.statusText.text = "Map location selected"
                        binding.resultsRecyclerView.visibility = View.GONE
                        Snackbar.make(binding.root, "Map location selected", Snackbar.LENGTH_SHORT).show()
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
            Log.e("MapFragment", "Map setup failed: ${e.message}", e)
            binding.statusText.text = "Map setup failed"
            Snackbar.make(binding.root, "Failed to initialize map", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun setupListeners() {
        binding.btnSaveLocation.setOnClickListener {
            val marker = selectedMarker
            if (marker == null) {
                binding.statusText.text = "No location selected"
                Toast.makeText(requireContext(), "Please select a location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (arguments?.getBoolean("for_favorite") == true) {
                saveAsFavorite(marker.position)
            } else {
                binding.btnSaveLocation.isEnabled = false
                binding.btnSaveLocation.text = "Saving..."
                settingsViewModel.setManualLocationCoordinates(marker.position.latitude, marker.position.longitude)

                settingsViewModel.saveComplete.observe(viewLifecycleOwner) { success ->
                    if (success) {
                        binding.statusText.text = "Location saved"
                        Snackbar.make(binding.root, "Location saved successfully", Snackbar.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } else {
                        binding.btnSaveLocation.isEnabled = true
                        binding.btnSaveLocation.text = "Save Location"
                        binding.statusText.text = "Failed to save location"
                        Snackbar.make(binding.root, "Failed to save location", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun saveAsFavorite(geoPoint: GeoPoint) {
        binding.btnSaveLocation.isEnabled = false
        binding.btnSaveLocation.text = "Saving..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val address = favoritesViewModel.getAddressForCoordinates(geoPoint.latitude, geoPoint.longitude)
                val locationName = address ?: "Location (${"%.2f".format(geoPoint.latitude)}, ${"%.2f".format(geoPoint.longitude)})"

                val success = favoritesViewModel.addFavoriteLocation(
                    geoPoint.latitude,
                    geoPoint.longitude,
                    locationName
                )

                if (success) {
                    binding.statusText.text = "Added to favorites: $locationName"
                    Snackbar.make(binding.root, "Added to favorites: $locationName", Snackbar.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    binding.btnSaveLocation.isEnabled = true
                    binding.btnSaveLocation.text = "Save as Favorite"
                    binding.statusText.text = "Failed to add favorite"
                    Snackbar.make(binding.root, "Failed to add favorite", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MapFragment", "Save favorite failed: ${e.message}", e)
                binding.btnSaveLocation.isEnabled = true
                binding.btnSaveLocation.text = "Save as Favorite"
                binding.statusText.text = "Error saving favorite"
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSelectedLocation(geoPoint: GeoPoint?) {
        if (geoPoint == null) return

        try {
            selectedMarker?.let { binding.mapView.overlays.remove(it) }

            selectedMarker = Marker(binding.mapView).apply {
                position = geoPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Selected Location"
            }
            binding.mapView.overlays.add(selectedMarker)
            binding.mapView.invalidate()
            binding.mapView.controller.setCenter(geoPoint)
        } catch (e: Exception) {
            Log.e("MapFragment", "Update marker failed: ${e.message}", e)
            binding.statusText.text = "Failed to update map marker"
            Snackbar.make(binding.root, "Failed to update map marker", Snackbar.LENGTH_SHORT).show()
        }
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