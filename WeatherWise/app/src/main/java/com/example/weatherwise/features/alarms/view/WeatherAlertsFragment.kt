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
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
import com.example.weatherwise.features.alarms.worker.AlertWorker
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

        // Initialize dependencies
        preferencesManager = PreferencesManager(requireContext())
        viewModel = ViewModelProvider(this, WeatherAlertViewModelFactory(
            WeatherRepositoryImpl.getInstance(
                WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)),
                LocalDataSourceImpl(LocalDatabase.getInstance(requireContext()).weatherDao()),
                preferencesManager
            )
        )).get(WeatherAlertViewModel::class.java)

        setupUI()
        setupObservers()
        checkNotificationPermission()

        // Start periodic alert checks
        AlertWorker.schedulePeriodicCheck(requireContext())
    }

    private fun setupUI() {
        // Setup RecyclerView
        adapter = WeatherAlertsAdapter(
            onToggle = { alert ->
                viewModel.updateAlert(alert.copy(isActive = !alert.isActive))
                if (!alert.isActive && alert.notificationType == "ALARM") {
                    cancelAlarm(alert.id)
                }
            },
            onDelete = { alert ->
                if (alert.notificationType == "ALARM") {
                    cancelAlarm(alert.id)
                }
                showDeleteConfirmation(alert)
            }
        )
        binding.rvActiveAlerts.adapter = adapter
        binding.rvActiveAlerts.layoutManager = LinearLayoutManager(requireContext())

        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Add Alert FAB
        binding.fabAddAlert.setOnClickListener {
            showAddAlertDialog()
        }

        // Empty state button
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
                // Reschedule alarms if needed
                viewModel.alerts.value?.forEach { alert ->
                    if (alert.notificationType == "ALARM" && alert.isActive) {
                        scheduleAlarmIfNeeded(alert)
                    }
                }
                viewModel.clearAlertsUpdated()
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted, check if we should show rationale
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    showNotificationPermissionRationale()
                } else {
                    // Directly request the permission
                    requestNotificationPermission()
                }
            }
        }
    }

    private fun showNotificationPermissionRationale() {
        AlertDialog.Builder(requireContext())
            .setTitle("Notification Permission Needed")
            .setMessage("This app needs notification permission to alert you about weather changes.")
            .setPositiveButton("OK") { _, _ ->
                requestNotificationPermission()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Snackbar.make(
                    binding.snackbarAnchor,
                    "Notifications disabled - permission denied",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showAddAlertDialog() {
        // First check if notifications are enabled in preferences
        if (!preferencesManager.areNotificationsEnabled()) {
            showEnableNotificationsDialog()
            return
        }

        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showNotificationPermissionRationale()
            return
        }

        // Inflate the dialog layout
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_alert, null)
        val dialogBinding = DialogAddAlertBinding.bind(dialogView)

        // Setup alert type dropdown
        val alertTypes = resources.getStringArray(R.array.alert_types)
        val alertTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            alertTypes
        )
        dialogBinding.actvAlertType.setAdapter(alertTypeAdapter)

        // Setup time pickers
        val calendar = Calendar.getInstance()
        dialogBinding.etStartTime.setOnClickListener {
            showDateTimePicker(true) { dateTime ->
                calendar.timeInMillis = dateTime
                dialogBinding.etStartTime.setText(
                    SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                        .format(Date(dateTime)))
            }
        }

        dialogBinding.etEndTime.setOnClickListener {
            showDateTimePicker(false) { dateTime ->
                calendar.timeInMillis = dateTime
                dialogBinding.etEndTime.setText(
                    SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                        .format(Date(dateTime))
                )
            }
        }

        // Setup notification type radio group
        dialogBinding.rgNotificationType.setOnCheckedChangeListener { _, checkedId ->
            dialogBinding.tilCustomSound.visibility = when (checkedId) {
                R.id.rb_alarm_sound -> View.VISIBLE
                else -> View.GONE
            }
        }

        // Setup custom sound picker
        dialogBinding.etCustomSound.setOnClickListener {
            // Implement sound picker intent here
        }

        // Build and show the dialog
        addAlertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Weather Alert")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                saveAlert(dialogBinding)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDateTimePicker(isStartTime: Boolean, callback: (Long) -> Unit) {
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

    private fun showDeleteConfirmation(alert: WeatherAlert) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Alert")
            .setMessage("Are you sure you want to delete this alert?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteAlert(alert.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        addAlertDialog?.dismiss()
        _binding = null
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        fun newInstance() = WeatherAlertsFragment()
    }

    private fun saveAlert(dialogBinding: DialogAddAlertBinding) {
        val alertType = dialogBinding.actvAlertType.text.toString()
        val startTimeText = dialogBinding.etStartTime.text.toString()
        val endTimeText = dialogBinding.etEndTime.text.toString()
        val notificationType = when (dialogBinding.rgNotificationType.checkedRadioButtonId) {
            R.id.rb_silent_notification -> "SILENT"
            R.id.rb_sound_notification -> "SOUND"
            R.id.rb_alarm_sound -> "ALARM"
            else -> "SILENT"
        }
        val customSoundUri = dialogBinding.etCustomSound.text?.toString()

        // Validate inputs
        if (alertType.isBlank() || startTimeText.isBlank() || endTimeText.isBlank()) {
            Snackbar.make(binding.snackbarAnchor, "Please fill all fields", Snackbar.LENGTH_LONG).show()
            return
        }

        try {
            val startTime = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                .parse(startTimeText)?.time ?: 0
            val endTime = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                .parse(endTimeText)?.time ?: 0

            if (startTime >= endTime) {
                Snackbar.make(binding.snackbarAnchor, "End time must be after start time", Snackbar.LENGTH_LONG).show()
                return
            }

            val newAlert = WeatherAlert(
                id = generateAlertId(alertType, startTime),
                type = alertType,
                startTime = startTime,
                endTime = endTime,
                notificationType = notificationType,
                customSoundUri = customSoundUri,
                isActive = true
            )

            viewModel.addAlert(newAlert)

            // If it's an alarm type, schedule it immediately
            if (notificationType == "ALARM") {
                scheduleAlarmIfNeeded(newAlert)
            }

        } catch (e: Exception) {
            Snackbar.make(binding.snackbarAnchor, "Invalid date format", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun generateAlertId(alertType: String, startTime: Long): String {
        return "${alertType}_${startTime}"
    }


    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleAlarmIfNeeded(alert: WeatherAlert) {
        if (alert.notificationType == "ALARM" && alert.isActive) {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
                putExtra("alert_id", alert.id)
                putExtra("alert_type", alert.type)
                putExtra("end_time", alert.endTime)
                putExtra("notification_type", alert.notificationType)
            }

            Log.d("WeatherAlertsFragment", "Scheduling alarm for alert: ${alert.id}, notification_type: ${alert.notificationType}")

            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                alert.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

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
            Log.d("WeatherAlertsFragment", "Scheduled alarm for alert: ${alert.id}")
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
}