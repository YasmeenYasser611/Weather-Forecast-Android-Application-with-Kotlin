package com.example.weatherwise.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LocationHelper(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var locationCallback: LocationCallback? = null

    fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun enableLocationServices() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    fun getFreshLocation(
        onLocationResult: (latitude: Double, longitude: Double, address: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        if (!checkPermissions()) {
            onError("Location permission not granted")
            return
        }

        if (!isLocationEnabled()) {
            onError("Location services are disabled")
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            0 // Immediate update
        ).setMaxUpdates(1) // Stop after one update
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    getAddressFromLocation(location, onLocationResult)
                } else {
                    onError("Unable to get location")
                }
                stopLocationUpdates()
            }
        }

        try {
            fusedClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            onError("Location permission error: ${e.message}")
            stopLocationUpdates()
        }
    }

    private fun getAddressFromLocation(
        location: Location,
        onLocationResult: (latitude: Double, longitude: Double, address: String) -> Unit
    ) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // Modern Geocoder API for Android 13+
                Geocoder(context, Locale.getDefault()).getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                ) { addresses ->
                    val address = addresses.firstOrNull()?.getAddressLine(0) ?: "Unknown address"
                    onLocationResult(location.latitude, location.longitude, address)
                }
            } else {
                // Legacy Geocoder API
                @Suppress("DEPRECATION")
                val addresses = Geocoder(context, Locale.getDefault())
                    .getFromLocation(location.latitude, location.longitude, 1)
                val address = addresses?.firstOrNull()?.getAddressLine(0) ?: "Unknown address"
                onLocationResult(location.latitude, location.longitude, address)
            }
        } catch (e: Exception) {
            onLocationResult(location.latitude, location.longitude, "Unknown address")
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    // Existing function - keeping exactly as is
    fun getAddressFromLocation(
        lat: Double,
        lon: Double,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0].getAddressLine(0) ?: "Unknown address"
                onSuccess(address)
            } else {
                onError("No address found")
            }
        } catch (e: Exception) {
            onError(e.message ?: "Error getting address")
        }
    }

    // Existing function - keeping exactly as is
    suspend fun getAddressFromLocation(lat: Double, lon: Double, any: Any): String {
        return suspendCoroutine { continuation ->
            this.getAddressFromLocation(lat, lon,
                onSuccess = { address ->
                    continuation.resume(address)
                },
                onError = {
                    continuation.resume("Selected Location")
                }
            )
        }
    }

    /**
     * NEW FUNCTION: Gets the address from latitude and longitude coordinates
     * @param latitude The latitude of the location
     * @param longitude The longitude of the location
     * @return Pair containing (address: String, error: String?) where error is null if successful
     */
    // Make sure this function is properly implemented
    fun getLocationAddress(latitude: Double, longitude: Double): Pair<String, String?> {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1)
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)
            }

            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0].getAddressLine(0) ?: "Unknown address"
                Pair(address, null)
            } else {
                Pair("Unknown address", "No address found")
            }
        } catch (e: Exception) {
            Pair("Unknown address", e.message ?: "Error getting address")
        }
    }
}