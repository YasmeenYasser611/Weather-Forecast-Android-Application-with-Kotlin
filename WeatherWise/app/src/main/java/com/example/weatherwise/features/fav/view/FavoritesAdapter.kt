package com.example.weatherwise.features.fav.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherwise.R
import com.example.weatherwise.data.model.domain.LocationWithWeather
import com.example.weatherwise.databinding.ItemFavoriteBinding
import com.example.weatherwise.features.mainscreen.viewmodel.WeatherIconMapper

class FavoritesAdapter(
    private val showUndoDeleteSnackbar: (LocationWithWeather, Int) -> Unit,
    private val onItemClick: (LocationWithWeather) -> Unit
) : ListAdapter<LocationWithWeather, FavoritesAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(val binding: ItemFavoriteBinding) : RecyclerView.ViewHolder(binding.root)

    private class DiffCallback : DiffUtil.ItemCallback<LocationWithWeather>() {
        override fun areItemsTheSame(oldItem: LocationWithWeather, newItem: LocationWithWeather): Boolean {
            return oldItem.location.id == newItem.location.id
        }

        override fun areContentsTheSame(oldItem: LocationWithWeather, newItem: LocationWithWeather): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFavoriteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.binding.apply {
            // Set location name
            val address = item.location.address?.takeIf { it.isNotBlank() }
                ?: "Unknown Location"
            tvLocationName.text = address

            // Set temperature
            val temperature = item.currentWeather?.main?.temp?.toInt() ?: 0
            tvTemperature.text = "$temperature°"

            // Set weather description
            val description = item.currentWeather?.weather?.firstOrNull()?.description
                ?: "N/A"
            tvWeatherDescription.text = description.capitalize()

            // Set high/low temperatures
            val highTemp = item.currentWeather?.main?.temp_max?.toInt() ?: 0
            val lowTemp = item.currentWeather?.main?.temp_min?.toInt() ?: 0
            tvHighLow.text = "H:$highTemp° L:$lowTemp°"

            // Set weather icon animation
            val animationFile = WeatherIconMapper.getLottieAnimationForIcon(
                item.currentWeather?.weather?.firstOrNull()?.icon
            )
            ivWeatherIcon.setAnimation(animationFile)
            ivWeatherIcon.playAnimation()

            // Set click listeners
            btnRemove.setOnClickListener {
                // First remove the item from the list
                val removedItem = getItem(position)
                val tempList = currentList.toMutableList().apply { removeAt(position) }
                submitList(tempList)

                // Show custom Snackbar with both actions
                showUndoDeleteSnackbar(removedItem, position)
            }
            root.setOnClickListener { onItemClick(item) }
        }
    }
}