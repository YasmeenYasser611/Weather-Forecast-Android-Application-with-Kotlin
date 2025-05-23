package com.example.weatherwise.features.mainscreen.view

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherwise.data.model.domain.DailyForecast
import com.example.weatherwise.databinding.ItemDailyForecastBinding
import com.example.weatherwise.features.mainscreen.viewmodel.WeatherIconMapper

class DailyForecastAdapter :
    ListAdapter<DailyForecast, DailyForecastAdapter.DailyForecastViewHolder>(DailyForecastDiffUtil()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyForecastViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemDailyForecastBinding.inflate(inflater, parent, false)
        return DailyForecastViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: DailyForecastViewHolder, position: Int)
    {
        val currentItem = getItem(position)

        holder.binding.tvDay.text = currentItem.day
        holder.binding.tvHighLow.text = "H:${currentItem.highTemperature.toInt()}° L:${currentItem.lowTemperature.toInt()}°"
        holder.binding.tvDescription.text = currentItem.description ?: ""


        val animationFile = WeatherIconMapper.getLottieAnimationForIcon(currentItem.icon)
        holder.binding.ivWeatherIcon.setAnimation(animationFile)
        holder.binding.ivWeatherIcon.playAnimation()
    }


    class DailyForecastViewHolder(val binding: ItemDailyForecastBinding) :
        RecyclerView.ViewHolder(binding.root)
}
