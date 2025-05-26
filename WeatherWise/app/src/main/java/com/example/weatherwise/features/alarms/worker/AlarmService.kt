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
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.weatherwise.R
class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null

    companion object {
        const val ALARM_CHANNEL_ID = "weather_alerts_alarm"
        const val ALARM_CHANNEL_NAME = "Weather Alarm Alerts"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    ALARM_CHANNEL_ID,
                    ALARM_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for weather alarm alerts"
                    setSound(null, null) // We'll handle sound separately with MediaPlayer
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create notification to keep service running
        val notification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_active)
            .setContentTitle("Weather Alarm Active")
            .setContentText("Tap to dismiss the alarm")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(true)
            .build()

        // Start as foreground service
        startForeground(1, notification)

        // Play alarm sound
        mediaPlayer = MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)).apply {
            isLooping = true
            start()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}