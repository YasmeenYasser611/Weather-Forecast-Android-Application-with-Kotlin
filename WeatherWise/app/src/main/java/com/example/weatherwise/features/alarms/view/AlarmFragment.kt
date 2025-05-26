package com.example.weatherwise.features.alarms.view


import WeatherService
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.weatherwise.data.local.LocalDataSourceImpl
import com.example.weatherwise.data.local.LocalDatabase
import com.example.weatherwise.data.remote.RetrofitHelper
import com.example.weatherwise.data.remote.WeatherRemoteDataSourceImpl
import com.example.weatherwise.data.repository.WeatherRepositoryImpl
import com.example.weatherwise.databinding.FragmentAlarmBinding
import com.example.weatherwise.features.alarms.worker.AlarmService
import com.example.weatherwise.features.settings.model.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmFragment : Fragment() {
    private var _binding: FragmentAlarmBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(extras: Bundle?) = AlarmFragment().apply {
            arguments = extras
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val alertId = arguments?.getString("alert_id") ?: ""
        val alertType = arguments?.getString("alert_type") ?: "Weather Alert"
        binding.tvAlertTitle.text = "Weather Alarm: $alertType"

        binding.btnDismiss.setOnClickListener {
            // Stop the service
            requireActivity().stopService(Intent(requireContext(), AlarmService::class.java))

            // Update the alert status
            updateAlertStatus(alertId, false)

            // Go back
            parentFragmentManager.popBackStack()
        }
    }
    private fun updateAlertStatus(alertId: String, isActive: Boolean) {
        // You'll need to get access to your repository here
        // Similar to how you do it in WeatherAlertsFragment
        // This is a simplified version - implement according to your architecture
        CoroutineScope(Dispatchers.IO).launch {
            val repo = WeatherRepositoryImpl.getInstance(
                WeatherRemoteDataSourceImpl(RetrofitHelper.retrofit.create(WeatherService::class.java)),
                LocalDataSourceImpl(LocalDatabase.getInstance(requireContext()).weatherDao()),
                PreferencesManager(requireContext())
            )
            val alert = repo.getAlert(alertId)
            alert?.let {
                repo.saveAlert(it.copy(isActive = isActive))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}