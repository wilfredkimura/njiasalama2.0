package com.njiasalama.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * An interface defining the contract for location providers.
 * Using an interface allows us to easily swap the real GPS provider
 * with a "Fake" one during unit testing, keeping our tests independent.
 */
interface LocationProvider {
    fun getLocationUpdates(intervalMillis: Long = 5000L): Flow<LatLng>
}

/**
 * A helper class that acts as a wrapper around Google Play Services' FusedLocationProviderClient.
 * This class implements the LocationProvider interface to stream real GPS coordinates.
 */
class LocationService(private val context: Context) : LocationProvider {

    // FusedLocationProviderClient is Google's API to fetch geographic location telemetry.
    // It automatically balances GPS, Wi-Fi, and cell signals to optimize battery and speed.
    private val locationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Exposes a continuous stream (Flow) of geographic coordinates (LatLng).
     * By using 'callbackFlow', we can easily convert the traditional listener callbacks from 
     * Google Play Services into a reactive stream that other components can observe.
     */
    @SuppressLint("MissingPermission") // We assume permissions are verified before calling this
    override fun getLocationUpdates(intervalMillis: Long): Flow<LatLng> = callbackFlow {
        
        // 1. Fetch the cached last known location immediately to provide fast startup coordinates
        locationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                trySend(LatLng(it.latitude, it.longitude))
            }
        }

        // 2. Check if the app is allowed to fetch high accuracy fine GPS coordinates.
        // If not, we fall back to balanced accuracy to support coarse (approximate) location tracking.
        val hasFinePermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val priority = if (hasFinePermission) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        // 3. Define the location request criteria.
        val locationRequest = LocationRequest.Builder(priority, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2) // Minimum delay between consecutive updates
            .build()

        // 4. Define the callback that receives new locations from the Android system
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                // Extract the last known location and send it down the flow stream
                locationResult.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    // trySend pushes the new coordinates to any active collector
                    trySend(latLng)
                }
            }
        }

        // 5. Register the callback with the FusedLocationProviderClient.
        locationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // 6. awaitClose is called when the flow's collector stops collecting.
        awaitClose {
            locationClient.removeLocationUpdates(locationCallback)
        }
    }
}
