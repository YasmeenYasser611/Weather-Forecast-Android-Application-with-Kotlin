package com.example.weatherwise.features.alarms.worker

import android.Manifest
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import com.example.weatherwise.R
import com.example.weatherwise.data.local.LocalDataSourceImpl
import com.example.weatherwise.data.local.LocalDatabase
import com.example.weatherwise.data.remote.RetrofitHelper
import com.example.weatherwise.data.remote.WeatherRemoteDataSourceImpl
import com.example.weatherwise.data.remote.WeatherService
import com.example.weatherwise.data.repository.WeatherRepositoryImpl
import com.example.weatherwise.databinding.FragmentAlarmBinding
import com.example.weatherwise.main.MainActivity
import com.example.weatherwise.features.settings.model.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    @RequiresPermission(allOf = [Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.SYSTEM_ALERT_WINDOW])
    override fun onReceive(context: Context, intent: Intent) {
        val alertId = intent.getStringExtra("alert_id") ?: return
        val alertType = intent.getStringExtra("alert_type") ?: return
        val notificationType = intent.getStringExtra("notification_type")?.uppercase() ?: "SILENT"
        val customSoundUri = intent.getStringExtra("custom_sound_uri")

        Log.d("AlarmReceiver", "Received intent with alert_id: $alertId, type: $notificationType, customSoundUri: $customSoundUri")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            Log.e("AlarmReceiver", "Notifications are disabled for alert: $alertId")
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !Settings.canDrawOverlays(context)) {
            Log.e("AlarmReceiver", "Overlay permission not granted for alert: $alertId")
            return
        }


        if (notificationType == "ALARM" || notificationType == "SOUND") {
            AlarmService.createNotificationChannel(context)
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtra("alert_id", alertId)
                putExtra("alert_type", alertType)
                putExtra("notification_type", notificationType)
                putExtra("custom_sound_uri", customSoundUri)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }

        showOverlayDialog(context, alertId, alertType, notificationType)
    }

    private fun showOverlayDialog(context: Context, alertId: String, alertType: String, notificationType: String) {
        val dialogBinding = FragmentAlarmBinding.inflate(LayoutInflater.from(context))
        dialogBinding.tvAlertTitle.text = "Weather $notificationType: $alertType"

        val dialog = AlertDialog.Builder(context, R.style.AlertDialogTheme)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialog.window?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            } else {
                setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
            }
            addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        dialogBinding.buttonDismiss.setOnClickListener {

            if (notificationType == "ALARM" || notificationType == "SOUND") {
                context.stopService(Intent(context, AlarmService::class.java))
            }

            updateAlertStatus(context, alertId)
            dialog.dismiss()
        }


        dialogBinding.buttonDismiss.setOnLongClickListener {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("alert_id", alertId)
            }
            context.startActivity(intent)
            dialog.dismiss()
            true
        }

        dialog.show()
        Log.d("AlarmReceiver", "Overlay dialog shown for alert: $alertId, type: $notificationType")
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