package com.example.weatherwise.features.alarms.worker

import WeatherService
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.weatherwise.R
import com.example.weatherwise.data.local.LocalDataSourceImpl
import com.example.weatherwise.data.local.LocalDatabase
import com.example.weatherwise.data.model.entity.WeatherAlert
import com.example.weatherwise.data.remote.RetrofitHelper
import com.example.weatherwise.data.remote.WeatherRemoteDataSourceImpl
import com.example.weatherwise.data.repository.WeatherRepositoryImpl
import com.example.weatherwise.features.main.MainActivity
import com.example.weatherwise.features.settings.model.PreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AlertWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    companion object {
        const val WORK_TAG = "periodic_alert_check"

        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<AlertWorker>(
                15, TimeUnit.MINUTES // Check every 15 minutes
            ).setConstraints(constraints)
                .addTag(WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.REPLACE, // Use REPLACE to ensure new worker is scheduled
                periodicWorkRequest
            ).also {
                Log.d("AlertWorker", "Periodic work enqueued with ID: ${periodicWorkRequest.id}")
            }
        }
    }

    private val repo by lazy {
        WeatherRepositoryImpl.getInstance(
            WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)),
            LocalDataSourceImpl(LocalDatabase.getInstance(applicationContext).weatherDao()),
            PreferencesManager(applicationContext)
        )
    }

    override suspend fun doWork(): Result {
        Log.d("AlertWorker", "Worker started at ${System.currentTimeMillis()}")
        try {
            val currentTime = System.currentTimeMillis()
            val alerts = repo.getActiveAlerts(currentTime)
            Log.d("AlertWorker", "Retrieved ${alerts.size} active alerts for time: $currentTime")

            if (alerts.isEmpty()) {
                Log.d("AlertWorker", "No active alerts found")
            } else {
                alerts.forEach { alert ->
                    Log.d("AlertWorker", "Processing alert: ${alert.id}, Type: ${alert.type}, " +
                            "Start: ${alert.startTime}, End: ${alert.endTime}, Active: ${alert.isActive}")
                    showNotification(alert)
                }
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("AlertWorker", "Error in doWork: ${e.message}", e)
            return Result.retry() // Retry on failure to handle transient issues
        }
    }

    private fun showNotification(alert: WeatherAlert) {
        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("AlertWorker", "Notification permission not granted for alert: ${alert.id}")
            return
        }

        // Create intent for notification click
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("alert_id", alert.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Determine channel ID and name based on notification type
        val channelId = when (alert.notificationType.uppercase()) {
            "ALARM" -> "weather_alerts_alarm"
            "SOUND" -> "weather_alerts_sound"
            else -> {
                Log.w("AlertWorker", "Unknown notification type: ${alert.notificationType}, defaulting to silent")
                "weather_alerts_silent"
            }
        }

        val channelName = when (alert.notificationType.uppercase()) {
            "ALARM" -> "Weather Alarm Alerts"
            "SOUND" -> "Weather Sound Alerts"
            else -> "Weather Silent Alerts"
        }

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = when (alert.notificationType.uppercase()) {
                "ALARM" -> NotificationManager.IMPORTANCE_HIGH
                "SOUND" -> NotificationManager.IMPORTANCE_DEFAULT
                else -> NotificationManager.IMPORTANCE_LOW
            }

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Channel for ${alert.type} alerts"
                if (alert.notificationType.uppercase() != "SILENT") {
                    val soundUri = try {
                        alert.customSoundUri?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(
                            if (alert.notificationType.uppercase() == "ALARM")
                                RingtoneManager.TYPE_ALARM
                            else
                                RingtoneManager.TYPE_NOTIFICATION
                        )
                    } catch (e: Exception) {
                        Log.e("AlertWorker", "Invalid custom sound URI: ${alert.customSoundUri}, using default")
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    }
                    setSound(soundUri, AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build())
                }
            }
            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
            Log.d("AlertWorker", "Notification channel created: $channelId")
        }

        // Build notification
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notifications_active)
            .setContentTitle("Weather Alert: ${alert.type}")
            .setContentText("Active until ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(alert.endTime))}")
            .setPriority(
                when (alert.notificationType.uppercase()) {
                    "ALARM" -> NotificationCompat.PRIORITY_MAX
                    "SOUND" -> NotificationCompat.PRIORITY_DEFAULT
                    else -> NotificationCompat.PRIORITY_LOW
                }
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .apply {
                if (alert.notificationType.uppercase() == "SILENT") {
                    setSilent(true)
                }
            }
            .build()

        // Post notification
        NotificationManagerCompat.from(applicationContext)
            .notify(alert.id.hashCode(), notification)
        Log.d("AlertWorker", "Notification posted for alert: ${alert.id}")
    }
}