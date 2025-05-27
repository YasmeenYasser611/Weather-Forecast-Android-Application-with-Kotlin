package com.example.weatherwise.utils


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
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
            0
        ).setMaxUpdates(1).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    getAddressFromLocation(location) { address ->
                        onLocationResult(location.latitude, location.longitude, address)
                    }
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

    @SuppressLint("MissingPermission")
    suspend fun getFreshLocation(): Triple<Double, Double, String> {
        if (!checkPermissions()) throw Exception("Location permission not granted")
        if (!isLocationEnabled()) throw Exception("Location services disabled")

        return suspendCancellableCoroutine { continuation ->
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                0
            ).setMaxUpdates(1).build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation
                    if (location != null) {
                        getAddressFromLocation(location) { address ->
                            continuation.resume(Triple(location.latitude, location.longitude, address))
                        }
                    } else {
                        continuation.resumeWithException(Exception("Unable to get location"))
                    }
                    fusedClient.removeLocationUpdates(this)
                }
            }

            fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                    fusedClient.removeLocationUpdates(callback)
                }

            continuation.invokeOnCancellation {
                fusedClient.removeLocationUpdates(callback)
            }
        }
    }

    private fun getAddressFromLocation(location: Location, onResult: (String) -> Unit) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                Geocoder(context, Locale.getDefault()).getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                ) { addresses ->
                    val address = addresses?.firstOrNull()?.getAddressLine(0) ?: "Unknown address"
                    onResult(address)
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = Geocoder(context, Locale.getDefault())
                    .getFromLocation(location.latitude, location.longitude, 1)
                val address = addresses?.firstOrNull()?.getAddressLine(0) ?: "Unknown address"
                onResult(address)
            }
        } catch (e: Exception) {
            onResult("Unknown address")
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    suspend fun getAddressFromLocation(lat: Double, lon: Double): String {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    Geocoder(context, Locale.getDefault()).getFromLocation(
                        lat,
                        lon,
                        1
                    ) { addresses ->
                        val address = addresses?.firstOrNull()?.getAddressLine(0) ?: "Unknown address"
                        continuation.resume(address)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)
                addresses?.firstOrNull()?.getAddressLine(0) ?: "Unknown address"
            }
        } catch (e: Exception) {
            "Unknown address"
        }
    }

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