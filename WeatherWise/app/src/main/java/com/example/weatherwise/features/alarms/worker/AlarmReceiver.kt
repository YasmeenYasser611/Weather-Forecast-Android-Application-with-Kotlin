package com.example.weatherwise.features.alarms.worker

import WeatherService
import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.weatherwise.R
import com.example.weatherwise.data.local.LocalDataSourceImpl
import com.example.weatherwise.data.local.LocalDatabase
import com.example.weatherwise.data.remote.RetrofitHelper
import com.example.weatherwise.data.remote.WeatherRemoteDataSourceImpl
import com.example.weatherwise.data.repository.WeatherRepositoryImpl
import com.example.weatherwise.databinding.FragmentAlarmBinding
import com.example.weatherwise.features.alarms.worker.AlarmService
import com.example.weatherwise.main.MainActivity
import com.example.weatherwise.features.settings.model.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        AlarmService.createNotificationChannel(context)

        val alertId = intent.getStringExtra("alert_id") ?: run {
            Log.e("AlarmReceiver", "No alert_id provided in intent")
            return
        }

        val alertType = intent.getStringExtra("alert_type") ?: run {
            Log.e("AlarmReceiver", "No alert_type provided in intent")
            return
        }
        val endTime = intent.getLongExtra("end_time", 0L)
        val notificationType = intent.getStringExtra("notification_type")?.uppercase() ?: "NOTIFICATION"

        Log.d("AlarmReceiver", "Received intent with alert_id: $alertId, type: $notificationType")

        when (notificationType) {
            "ALARM" -> {
                // Start foreground service for continuous alarm sound
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtras(intent.extras ?: Bundle())
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                showAlarmDialog(context, alertId, alertType)
            }
            "NOTIFICATION" -> {
                showSilentNotification(context, alertId, alertType, endTime)
            }
            else -> {
                Log.e("AlarmReceiver", "Unknown notification type: $notificationType")
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showSilentNotification(
        context: Context,
        alertId: String,
        alertType: String,
        endTime: Long
    ) {
        // Create notification channel for silent notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "weather_alerts_silent",
                "Weather Silent Alerts",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for silent weather alerts"
                setSound(null, null) // No sound
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        // Intent for notification click
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("alert_id", alertId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build silent notification
        val notification = NotificationCompat.Builder(context, "weather_alerts_silent")
            .setSmallIcon(R.drawable.ic_notifications_active)
            .setContentTitle("Weather Alert: $alertType")
            .setContentText("Active until ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(endTime))}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSilent(true) // Make sure it's silent
            .build()

        NotificationManagerCompat.from(context)
            .notify(alertId.hashCode(), notification)
    }

    private fun showAlarmDialog(context: Context, alertId: String, alertType: String) {
        // Inflate the dialog layout
        val dialogBinding = FragmentAlarmBinding.inflate(LayoutInflater.from(context))
        dialogBinding.tvAlertTitle.text = "Weather Alarm: $alertType"

        // Create the AlertDialog
        val dialog = AlertDialog.Builder(context, R.style.AlertDialogTheme)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        // Set window flags to show over lockscreen
        dialog.window?.let { window ->
            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // Handle dismiss button click
        dialogBinding.btnDismiss.setOnClickListener {
            // Stop the alarm service
            context.stopService(Intent(context, AlarmService::class.java))

            // Update the alert status in database
            updateAlertStatus(context, alertId)

            // Dismiss the dialog
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateAlertStatus(context: Context, alertId: String) {
        val repo = WeatherRepositoryImpl.getInstance(
            WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)),
            LocalDataSourceImpl(LocalDatabase.getInstance(context).weatherDao()),
            PreferencesManager(context)
        )

        CoroutineScope(Dispatchers.IO).launch {
            val alert = repo.getAlert(alertId)
            alert?.let {
                repo.saveAlert(it.copy(isActive = false))
            }
        }
    }
}