package com.example.weatherwise.features.mainscreen.view.hourlyforecast

import androidx.recyclerview.widget.DiffUtil
import com.example.weatherwise.data.model.domain.HourlyForecast

class HourlyForecastDiffUtil : DiffUtil.ItemCallback<HourlyForecast>() {
    override fun areItemsTheSame(oldItem: HourlyForecast, newItem: HourlyForecast): Boolean {
        return oldItem.timestamp == newItem.timestamp
    }

    override fun areContentsTheSame(oldItem: HourlyForecast, newItem: HourlyForecast): Boolean {
        return oldItem == newItem
    }
}
