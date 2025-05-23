package com.example.weatherwise.features.mainscreen.view


import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherwise.data.model.domain.HourlyForecast
import com.example.weatherwise.databinding.ItemHourlyForecastBinding
import com.example.weatherwise.features.mainscreen.viewmodel.WeatherIconMapper

class HourlyForecastAdapter :
    ListAdapter<HourlyForecast, HourlyForecastAdapter.HourlyForecastViewHolder>(
        HourlyForecastDiffUtil()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyForecastViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemHourlyForecastBinding.inflate(inflater, parent, false)
        return HourlyForecastViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: HourlyForecastViewHolder, position: Int) {
        val currentItem = getItem(position)

        holder.binding.tvHour.text = currentItem.hour
        holder.binding.tvTemperature.text = "${currentItem.temperature.toInt()}°"

        val animationFile = WeatherIconMapper.getLottieAnimationForIcon(currentItem.icon)
        holder.binding.ivWeatherIcon.setAnimation(animationFile)
        holder.binding.ivWeatherIcon.playAnimation()

    }

    class HourlyForecastViewHolder(val binding: ItemHourlyForecastBinding) :
        RecyclerView.ViewHolder(binding.root)
}
