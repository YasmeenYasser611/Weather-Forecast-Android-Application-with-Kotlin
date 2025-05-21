package com.example.weatherwise.data.local


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.weatherwise.data.model.CurrentWeatherEntity
import com.example.weatherwise.data.model.ForecastWeatherEntity
import com.example.weatherwise.data.model.LocationEntity

@Database(entities = [LocationEntity::class, CurrentWeatherEntity::class, ForecastWeatherEntity::class], version = 1, exportSchema = false)
@TypeConverters(WeatherTypeConverters::class)
abstract class LocalDatabase : RoomDatabase()
{
    abstract fun weatherDao(): WeatherDao

    companion object {
        @Volatile
        private var INSTANCE: LocalDatabase? = null

        fun getInstance(context: Context): LocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext, LocalDatabase::class.java, "weather_database")
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}