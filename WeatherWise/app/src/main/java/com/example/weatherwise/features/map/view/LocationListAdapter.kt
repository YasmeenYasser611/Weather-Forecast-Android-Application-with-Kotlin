package com.example.weatherwise.features.map.view



import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherwise.R

class LocationListAdapter(
    private val onLocationClick: (String) -> Unit
) : ListAdapter<String, LocationListAdapter.LocationViewHolder>(LocationDiffCallback()) {

    class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val locationText: TextView = itemView.findViewById(R.id.location_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location_card, parent, false)
        return LocationViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val location = getItem(position) ?: return
        holder.locationText.text = location
        holder.itemView.setOnClickListener { onLocationClick(location) }
    }

    class LocationDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}