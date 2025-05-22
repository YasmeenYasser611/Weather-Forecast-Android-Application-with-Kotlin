package com.example.weatherwise.features.mainscreen.view

import androidx.recyclerview.widget.DiffUtil
import com.example.weatherwise.data.model.DailyForecast

class DailyForecastDiffUtil : DiffUtil.ItemCallback<DailyForecast>() {
    override fun areItemsTheSame(oldItem: DailyForecast, newItem: DailyForecast): Boolean {
        return oldItem.day == newItem.day
    }


    override fun areContentsTheSame(oldItem: DailyForecast, newItem: DailyForecast): Boolean {
        return oldItem == newItem
    }
}