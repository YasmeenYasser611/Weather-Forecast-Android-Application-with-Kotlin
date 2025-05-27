package com.example.weatherwise.features.mainscreen.usecase


import com.example.weatherwise.data.model.domain.DailyForecast
import com.example.weatherwise.data.model.domain.HourlyForecast
import com.example.weatherwise.data.model.response.WeatherResponse
import java.text.SimpleDateFormat
import java.util.*

class ProcessForecastUseCase {
    fun processHourlyForecast(forecast: WeatherResponse?, currentTime: Long): List<HourlyForecast> {
        val hourFormat = SimpleDateFormat("h a", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val forecastEndTime = currentTime + 172800 // 48 hours in seconds

        return forecast?.list
            ?.sortedBy { it.dt }
            ?.filter { it.dt >= currentTime - 3600 && it.dt <= forecastEndTime }
            ?.take(24)
            ?.map {
                HourlyForecast(
                    timestamp = it.dt,
                    temperature = it.main.temp,
                    icon = it.weather.firstOrNull()?.icon,
                    hour = hourFormat.format(Date(it.dt * 1000L))
                )
            }
            .orEmpty()
    }

    fun processDailyForecast(forecast: WeatherResponse?): List<DailyForecast> {
        if (forecast?.list.isNullOrEmpty()) return emptyList()

        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val grouped = forecast!!.list.groupBy {
            calendar.timeInMillis = it.dt * 1000L
            dateFormat.format(calendar.time)
        }

        return grouped.entries.take(5).map { (day, entries) ->
            val high = entries.maxOfOrNull { it.main.temp_max } ?: 0.0
            val low = entries.minOfOrNull { it.main.temp_min } ?: 0.0
            val noonEntry = entries.minByOrNull {
                calendar.timeInMillis = it.dt * 1000L
                kotlin.math.abs(calendar.get(Calendar.HOUR_OF_DAY) - 12)
            }
            DailyForecast(
                day = day,
                highTemperature = high,
                lowTemperature = low,
                icon = noonEntry?.weather?.firstOrNull()?.icon,
                description = noonEntry?.weather?.firstOrNull()?.description?.capitalizeFirst()
            )
        }.sortedByDay(dateFormat)
    }

    private fun String.capitalizeFirst() = replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

    private fun List<DailyForecast>.sortedByDay(dateFormat: SimpleDateFormat): List<DailyForecast> {
        val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val now = dateFormat.format(System.currentTimeMillis())
        val todayIndex = days.indexOf(now)

        return sortedBy { forecast ->
            val index = days.indexOf(forecast.day.take(3))
            (index - todayIndex + 7) % 7
        }
    }
}