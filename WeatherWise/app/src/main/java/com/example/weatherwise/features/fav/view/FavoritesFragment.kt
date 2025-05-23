package com.example.weatherwise.features.fav.view

import WeatherService
import android.annotation.SuppressLint
import android.graphics.Color
import com.example.weatherwise.databinding.FragmentFavBinding



import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherwise.R
import com.example.weatherwise.data.local.LocalDataSourceImpl
import com.example.weatherwise.data.local.LocalDatabase
import com.example.weatherwise.data.model.domain.LocationWithWeather
import com.example.weatherwise.data.remote.RetrofitHelper
import com.example.weatherwise.data.remote.WeatherRemoteDataSourceImpl
import com.example.weatherwise.data.repository.WeatherRepositoryImpl
import com.example.weatherwise.features.fav.viewmodel.FavoritesViewModel
import com.example.weatherwise.features.fav.viewmodel.FavoritesViewModelFactory
import com.example.weatherwise.features.settings.model.PreferencesManager
import com.example.weatherwise.features.settings.viewmodel.SettingsViewModel
import com.example.weatherwise.features.settings.viewmodel.SettingsViewModelFactory
import com.example.weatherwise.location.LocationHelper
import com.google.android.material.snackbar.Snackbar

class FavoritesFragment : Fragment() {
    private var _binding: FragmentFavBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FavoritesViewModel
    private lateinit var adapter: FavoritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        val factory = FavoritesViewModelFactory(
            WeatherRepositoryImpl.getInstance(
                WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)),
                LocalDataSourceImpl(LocalDatabase.getInstance(requireContext()).weatherDao()),
                PreferencesManager(requireContext())
            ),
            LocationHelper(requireContext()),
        )

        // Create ViewModel using the factory
        // In both SettingsFragment and MapFragment, use:
        viewModel = ViewModelProvider(requireActivity(), factory)[FavoritesViewModel::class.java]
        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Load initial data
        viewModel.loadFavorites()
    }

    private fun setupRecyclerView() {

        adapter = FavoritesAdapter(
            showUndoDeleteSnackbar = { removedItem, originalPosition ->
                showCustomSnackbar(removedItem, originalPosition)
            },
            onItemClick = { locationWithWeather ->
                navigateToFavoriteDetails(locationWithWeather)
            }
        )

        binding.rvFavorites.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FavoritesFragment.adapter
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    LinearLayoutManager.VERTICAL
                ).apply {
                    setDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.divider
                        )!!
                    )
                }
            )
        }
    }


    private fun navigateToFavoriteDetails(locationWithWeather: LocationWithWeather) {
        findNavController().navigate(
            R.id.action_favoritesFragment_to_favoriteDetailFragment,
            Bundle().apply {
                putString("location_id", locationWithWeather.location.id)
            }
        )
    }
    private fun setupObservers() {
        viewModel.favorites.observe(viewLifecycleOwner) { favorites ->
            adapter.submitList(favorites)
            binding.emptyState.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.rvFavorites.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        }
    }



    private fun setupListeners() {
        binding.fabAddFavorite.setOnClickListener {
            navigateToMapForFavorite()
        }

        binding.btnAddFirstFavorite.setOnClickListener {
            navigateToMapForFavorite()
        }
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }


    private fun navigateToMapForFavorite() {
        findNavController().navigate(
            R.id.action_favoritesFragment_to_mapFragment,
            Bundle().apply {
                putBoolean("for_favorite", true)
            }
        )
    }

    @SuppressLint("RestrictedApi")
    private fun showCustomSnackbar(removedItem: LocationWithWeather, originalPosition: Int) {
        // Inflate custom layout
        val snackView = layoutInflater.inflate(R.layout.custom_snackbar, null)

        // Create and configure Snackbar
        val snackbar = Snackbar.make(binding.root, "", Snackbar.LENGTH_INDEFINITE).apply {
            view.setBackgroundColor(Color.TRANSPARENT)
            (view as? Snackbar.SnackbarLayout)?.apply {
                removeAllViews()
                addView(snackView, 0)
            }
        }

        // Configure custom view
        snackView.apply {
            findViewById<TextView>(R.id.snackbar_text).text =
                "Deleted ${removedItem.location.address ?: "location"}"

            findViewById<Button>(R.id.snackbar_undo).setOnClickListener {
                // Undo action - reinsert item
                val currentList = adapter.currentList.toMutableList()
                currentList.add(originalPosition, removedItem)
                adapter.submitList(currentList)
                snackbar.dismiss()
            }

            findViewById<Button>(R.id.snackbar_cancel).setOnClickListener {
                // Confirm deletion
                viewModel.removeFavorite(removedItem.location.id)
                snackbar.dismiss()
            }
        }

        // Show snackbar with 10 second timeout as fallback
        snackbar.show()
        snackView.postDelayed({
            if (snackbar.isShown) {
                // If still showing after timeout, confirm deletion
                viewModel.removeFavorite(removedItem.location.id)
                snackbar.dismiss()
            }
        }, 10000)
    }
    private fun navigateToWeatherDetail(locationWithWeather: LocationWithWeather) {
        // Implement navigation to weather detail
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFavorites()
    }
}