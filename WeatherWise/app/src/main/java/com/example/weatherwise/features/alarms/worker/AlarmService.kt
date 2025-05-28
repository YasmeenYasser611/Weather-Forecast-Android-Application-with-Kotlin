package com.example.weatherwise.features.alarms.worker

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.weatherwise.R

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null

    companion object {
        const val ALARM_CHANNEL_ID = "weather_alerts_alarm"
        const val ALARM_CHANNEL_NAME = "Weather Alerts"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    ALARM_CHANNEL_ID,
                    ALARM_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for weather alerts"
                    setSound(null, null) // Sound handled by MediaPlayer
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                (context.getSystemService(NotificationManager::class.java))
                    .createNotificationChannel(channel)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alertType = intent?.getStringExtra("alert_type") ?: "Weather Alert"
        val notificationType = intent?.getStringExtra("notification_type")?.uppercase() ?: "ALARM"
        val customSoundUri = intent?.getStringExtra("custom_sound_uri")

        val notification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_active)
            .setContentTitle("Weather $notificationType: $alertType")
            .setContentText("Tap to view or dismiss the alert")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(true)
            .build()

        startForeground(1, notification)

        // Play sound based on notification type
        if (notificationType == "ALARM" || notificationType == "SOUND") {
            try {
                when (notificationType) {
                    "ALARM" -> {
                        mediaPlayer = MediaPlayer.create(this, R.raw.alarm)
                        if (mediaPlayer == null) {
                            Log.e("AlarmService", "Failed to create MediaPlayer for R.raw.alarm")
                            throw Exception("MediaPlayer creation failed for ALARM")
                        }
                        mediaPlayer?.isLooping = true
                        mediaPlayer?.start()
                        Log.d("AlarmService", "Playing alarm.mp3 for ALARM notification")
                    }
                    "SOUND" -> {
                        val soundUri = try {
                            if (!customSoundUri.isNullOrBlank()) {
                                Uri.parse(customSoundUri).takeIf { isValidUri(it, this) }
                                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                            } else {
                                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                            }
                        } catch (e: Exception) {
                            Log.e("AlarmService", "Invalid customSoundUri: $customSoundUri", e)
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        }
                        mediaPlayer = MediaPlayer.create(this, soundUri)
                        if (mediaPlayer == null) {
                            Log.e("AlarmService", "Failed to create MediaPlayer for soundUri: $soundUri")
                            throw Exception("MediaPlayer creation failed for SOUND")
                        }
                        mediaPlayer?.isLooping = false
                        mediaPlayer?.start()
                        Log.d("AlarmService", "Playing sound for SOUND notification with URI: $soundUri")
                    }
                }
            } catch (e: Exception) {
                Log.e("AlarmService", "Error playing sound for $notificationType", e)
                // Fallback to default notification sound
                try {
                    mediaPlayer = MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    mediaPlayer?.isLooping = notificationType == "ALARM"
                    mediaPlayer?.start()
                    Log.d("AlarmService", "Fallback to default notification sound for $notificationType")
                } catch (fallbackError: Exception) {
                    Log.e("AlarmService", "Fallback sound playback failed", fallbackError)
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("AlarmService", "Error releasing MediaPlayer", e)
        }
        mediaPlayer = null
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun isValidUri(uri: Uri, context: Context): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.close()
            true
        } catch (e: Exception) {
            Log.e("AlarmService", "Invalid URI: $uri", e)
            false
        }
    }
}