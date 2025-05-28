package com.example.weatherwise.features.alarms.view



import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherwise.R
import com.example.weatherwise.data.model.entity.WeatherAlert
import com.example.weatherwise.databinding.ItemWeatherAlertBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherAlertsAdapter(
    private val onToggle: (WeatherAlert) -> Unit,
    private val onDelete: (WeatherAlert) -> Unit
) : ListAdapter<WeatherAlert, WeatherAlertsAdapter.AlertViewHolder>(DiffCallback()) {

    inner class AlertViewHolder(private val binding: ItemWeatherAlertBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(alert: WeatherAlert) {
            binding.tvAlertTitle.text = alert.type
            binding.tvTimeRange.text = buildString {
                append(SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(alert.startTime)))
//                append(" - ")
//                append(SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(alert.endTime)))
            }

            binding.ivActive.setImageResource(
                if (alert.isActive) R.drawable.ic_notifications_active
                else R.drawable.ic_notifications_off
            )

            binding.btnToggle.text = if (alert.isActive) "Turn Off" else "Turn On"
            binding.btnToggle.setOnClickListener { onToggle(alert) }

            // Set appropriate icon based on alert type
            val iconRes = when(alert.type.lowercase(Locale.getDefault())) {
                "rain" -> R.drawable.rain
                "snow" -> R.drawable.nightrain
                "storm" -> R.drawable.heavyrainandstorm
                else -> R.drawable.ic_notifications_active
            }
            binding.ivAlertType.setImageResource(iconRes)

            binding.root.setOnLongClickListener {
                onDelete(alert)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemWeatherAlertBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<WeatherAlert>() {
        override fun areItemsTheSame(oldItem: WeatherAlert, newItem: WeatherAlert) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: WeatherAlert, newItem: WeatherAlert) =
            oldItem == newItem
    }
}