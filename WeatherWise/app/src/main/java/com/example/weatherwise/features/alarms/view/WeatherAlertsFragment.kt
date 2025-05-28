package com.example.weatherwise.features.alarms.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherwise.R
import com.example.weatherwise.data.local.LocalDataSourceImpl
import com.example.weatherwise.data.local.LocalDatabase
import com.example.weatherwise.data.model.entity.WeatherAlert
import com.example.weatherwise.data.remote.RetrofitHelper
import com.example.weatherwise.data.remote.WeatherRemoteDataSourceImpl
import com.example.weatherwise.data.remote.WeatherService
import com.example.weatherwise.data.repository.WeatherRepositoryImpl
import com.example.weatherwise.databinding.DialogAddAlertBinding
import com.example.weatherwise.databinding.FragmentAlarmsBinding
import com.example.weatherwise.features.alarms.viewmodel.WeatherAlertViewModel
import com.example.weatherwise.features.alarms.viewmodel.WeatherAlertViewModelFactory
import com.example.weatherwise.features.alarms.worker.AlarmReceiver
import com.example.weatherwise.features.settings.model.PreferencesManager
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeatherAlertsFragment : Fragment() {
    private var _binding: FragmentAlarmsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: WeatherAlertViewModel
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var adapter: WeatherAlertsAdapter
    private var addAlertDialog: AlertDialog? = null

    // Activity result launcher for overlay permission
    private val requestOverlayPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !Settings.canDrawOverlays(requireContext())) {
            Snackbar.make(
                binding.snackbarAnchor,
                "Overlay permission is required for alert dialogs",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlarmsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesManager = PreferencesManager(requireContext())
        viewModel = ViewModelProvider(
            this,
            WeatherAlertViewModelFactory(
                WeatherRepositoryImpl.getInstance(
                    WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)),
                    LocalDataSourceImpl(LocalDatabase.getInstance(requireContext()).weatherDao()),
                    preferencesManager
                )
            )
        ).get(WeatherAlertViewModel::class.java)

        setupUI()
        setupObservers()
        checkNotificationPermission()
    }

    private fun setupUI() {
        adapter = WeatherAlertsAdapter(
            onToggle = { alert ->
                viewModel.updateAlert(alert.copy(isActive = !alert.isActive))
                if (!alert.isActive) {
                    cancelAlarm(alert.id)
                } else {
                    scheduleAlarmIfNeeded(alert)
                }
            },
            onDelete = { alert ->
                // Find the position of the alert in the current list
                val position = adapter.currentList.indexOf(alert)
                if (position != -1) {
                    deleteAlertWithUndo(alert, position)
                }
            }
        )
        binding.rvActiveAlerts.adapter = adapter
        binding.rvActiveAlerts.layoutManager = LinearLayoutManager(requireContext())

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.fabAddAlert.setOnClickListener {
            showAddAlertDialog()
        }

        binding.btnAddFirstAlert.setOnClickListener {
            showAddAlertDialog()
        }
    }

    private fun setupObservers() {
        viewModel.alerts.observe(viewLifecycleOwner) { alerts ->
            if (alerts.isNullOrEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvActiveAlerts.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvActiveAlerts.visibility = View.VISIBLE
                adapter.submitList(alerts)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.snackbarAnchor, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }

        viewModel.alertsUpdated.observe(viewLifecycleOwner) { updated ->
            if (updated) {
                viewModel.alerts.value?.forEach { alert ->
                    if (alert.isActive) {
                        scheduleAlarmIfNeeded(alert)
                    }
                }
                viewModel.clearAlertsUpdated()
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                showNotificationPermissionRationale()
            } else if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission()
            }
        }
        // Check overlay permission for pre-Android 10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !Settings.canDrawOverlays(requireContext())) {
            requestOverlayPermission()
        }
    }

    private fun showNotificationPermissionRationale() {
        AlertDialog.Builder(requireContext())
            .setTitle("Notification Permission Needed")
            .setMessage("This app needs notification permission to alert you about weather changes.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${requireContext().packageName}"))
            requestOverlayPermission.launch(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(
                    binding.snackbarAnchor,
                    "Notifications disabled - permission denied",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showAddAlertDialog() {
        if (!preferencesManager.areNotificationsEnabled()) {
            showEnableNotificationsDialog()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
        ) {
            showNotificationPermissionRationale()
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !Settings.canDrawOverlays(requireContext())) {
            requestOverlayPermission()
            return
        }

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_alert, null)
        val dialogBinding = DialogAddAlertBinding.bind(dialogView)

        val alertTypes = resources.getStringArray(R.array.alert_types)
        val alertTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            alertTypes
        )
        dialogBinding.actvAlertType.setAdapter(alertTypeAdapter)

        val calendar = Calendar.getInstance()
        dialogBinding.etStartTime.setOnClickListener {
            showDateTimePicker { dateTime ->
                calendar.timeInMillis = dateTime
                dialogBinding.etStartTime.setText(
                    SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(dateTime))
                )
            }
        }

        addAlertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Weather Alert")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                saveAlert(dialogBinding)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDateTimePicker(callback: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                TimePickerDialog(
                    requireContext(),
                    { _, hour, minute ->
                        calendar.set(year, month, day, hour, minute)
                        callback(calendar.timeInMillis)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showEnableNotificationsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Notifications Disabled")
            .setMessage("You need to enable notifications in settings to add weather alerts.")
            .setPositiveButton("Go to Settings") { _, _ ->
                findNavController().navigate(R.id.action_weatherAlertsFragment_to_settingsFragment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    // First, add this function to your WeatherAlertsFragment
    @SuppressLint("RestrictedApi")
    private fun showCustomAlertSnackbar(removedAlert: WeatherAlert, originalPosition: Int) {
        // Inflate custom layout
        val snackView = layoutInflater.inflate(R.layout.custom_snackbar, null)

        // Create and configure Snackbar
        val snackbar = Snackbar.make(binding.root, "", Snackbar.LENGTH_INDEFINITE).apply {
            view.setBackgroundColor(Color.TRANSPARENT)
            (view as? Snackbar.SnackbarLayout)?.apply {
                removeAllViews()
                addView(snackView, 0)
            }
        }

        // Configure custom view
        snackView.apply {
            findViewById<TextView>(R.id.snackbar_text).text =
                "Deleted ${removedAlert.type} alert"

            findViewById<Button>(R.id.snackbar_undo).setOnClickListener {
                // Undo action - reinsert item
                val currentList = adapter.currentList.toMutableList()
                currentList.add(originalPosition, removedAlert)
                adapter.submitList(currentList)
                viewModel.addAlert(removedAlert) // Restore in database
                if (removedAlert.isActive) {
                    scheduleAlarmIfNeeded(removedAlert)
                }
                snackbar.dismiss()
            }

            findViewById<Button>(R.id.snackbar_cancel).setOnClickListener {
                // Confirm deletion - no need to do anything since we already deleted
                snackbar.dismiss()
            }
        }

        // Show snackbar with 10 second timeout as fallback
        snackbar.show()
        snackView.postDelayed({
            if (snackbar.isShown) {
                snackbar.dismiss()
            }
        }, 10000)
    }

    // Then modify your deleteAlertWithUndo function to be simpler:
    private fun deleteAlertWithUndo(alert: WeatherAlert, position: Int) {
        // Cancel the alarm and delete from database
        cancelAlarm(alert.id)
        viewModel.deleteAlert(alert.id)

        // Create a mutable copy of the current list and remove the item
        val currentList = adapter.currentList.toMutableList()
        currentList.removeAt(position)
        adapter.submitList(currentList)

        // Show custom Snackbar
        showCustomAlertSnackbar(alert, position)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        addAlertDialog?.dismiss()
        _binding = null
    }

    private fun saveAlert(dialogBinding: DialogAddAlertBinding) {
        val alertType = dialogBinding.actvAlertType.text.toString()
        val startTimeText = dialogBinding.etStartTime.text.toString()
        val notificationType = when (dialogBinding.rgNotificationType.checkedRadioButtonId) {
            R.id.rb_silent_notification -> "SILENT"
            R.id.rb_sound_notification -> "SOUND"
            R.id.rb_alarm_sound -> "ALARM"
            else -> "SILENT"
        }

        if (alertType.isBlank() || startTimeText.isBlank()) {
            Snackbar.make(binding.snackbarAnchor, "Please fill all fields", Snackbar.LENGTH_LONG).show()
            return
        }

        try {
            val startTime = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                .parse(startTimeText)?.time ?: 0

            if (startTime < System.currentTimeMillis()) {
                Snackbar.make(binding.snackbarAnchor, "Start time must be in the future", Snackbar.LENGTH_LONG).show()
                return
            }

            val soundUri = if (notificationType == "ALARM") {
                "android.resource://${requireContext().packageName}/${R.raw.alarm}"
            } else {
                null
            }

            val newAlert = WeatherAlert(
                id = generateAlertId(alertType, startTime),
                type = alertType,
                startTime = startTime,
                notificationType = notificationType,
                isActive = true,
                customSoundUri = soundUri
            )

            viewModel.addAlert(newAlert)
            scheduleAlarmIfNeeded(newAlert)

        } catch (e: Exception) {
            Snackbar.make(binding.snackbarAnchor, "Invalid date format", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun generateAlertId(alertType: String, startTime: Long): String {
        return "${alertType}_${startTime}"
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleAlarmIfNeeded(alert: WeatherAlert) {
        if (alert.isActive) {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
                putExtra("alert_id", alert.id)
                putExtra("alert_type", alert.type)
                putExtra("notification_type", alert.notificationType)
                putExtra("sound_uri", alert.customSoundUri)
            }

            Log.d("WeatherAlertsFragment", "Scheduling alert: ${alert.id}, type: ${alert.notificationType}, sound: ${alert.customSoundUri}")

            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                alert.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                Snackbar.make(
                    binding.snackbarAnchor,
                    "Please allow exact alarms for accurate notifications",
                    Snackbar.LENGTH_LONG
                ).show()
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alert.startTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    alert.startTime,
                    pendingIntent
                )
            }
            Log.d("WeatherAlertsFragment", "Scheduled alert: ${alert.id}")
        }
    }

    private fun cancelAlarm(alertId: String) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            alertId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        fun newInstance() = WeatherAlertsFragment()
    }
}