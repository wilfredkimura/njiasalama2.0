package com.njiasalama.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.njiasalama.MainActivity
import com.njiasalama.data.RetrofitClient
import com.njiasalama.data.RideRecorder
import com.njiasalama.domain.model.DangerPin
import com.njiasalama.domain.proximity.CyclistTrajectoryFilter
import com.njiasalama.domain.proximity.ProximityAlertLevel
import com.njiasalama.feedback.AudioAlertManager
import com.njiasalama.feedback.HapticAlertManager
import kotlinx.coroutines.*
import java.io.File

/**
 * Android Foreground Service maintaining continuous high-accuracy GPS tracking,
 * breadcrumb recording, and proximity hazard evaluation while the cyclist has the phone
 * locked away in their pocket or bag.
 */
class RideTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var rideRecorder: RideRecorder
    private lateinit var hapticManager: HapticAlertManager
    private lateinit var audioManager: AudioAlertManager

    private var activePins: List<DangerPin> = emptyList()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        rideRecorder = RideRecorder(filesDir)
        hapticManager = HapticAlertManager(this)
        audioManager = AudioAlertManager(this)

        // Load existing pins into memory for trajectory matching
        serviceScope.launch {
            try {
                val repo = RetrofitClient.getPinRepository(applicationContext)
                val result = repo.getPins()
                activePins = result.getOrDefault(emptyList())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return

                // 1. Log breadcrumb silently for post-ride debrief
                rideRecorder.recordLocation(loc)

                // 2. Evaluate forward trajectory hazards
                if (loc.hasBearing() && loc.hasSpeed()) {
                    val currentLatLng = LatLng(loc.latitude, loc.longitude)
                    for (pin in activePins) {
                        val alert = CyclistTrajectoryFilter.evaluateHazard(
                            currentLocation = currentLatLng,
                            currentSpeedMps = loc.speed,
                            currentBearing = loc.bearing,
                            pin = pin
                        )
                        if (alert != null && alert.level != ProximityAlertLevel.NONE) {
                            hapticManager.triggerAlert(pin.id, alert.level)
                            audioManager.speakHazardAlert(alert)
                            break // Alert on most immediate hazard
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RIDE -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                rideRecorder.startRide()
                hapticManager.resetSession()
                startLocationUpdates()
            }
            ACTION_STOP_RIDE -> {
                stopLocationUpdates()
                rideRecorder.stopRide()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(2.0f)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cyclist Ride Safety Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors road hazards and records ride breadcrumbs in your pocket"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NjiaSalama Ride Active")
            .setContentText("Monitoring road hazards & logging traversed path in your pocket")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        audioManager.shutdown()
        stopLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "njiasalama_ride_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START_RIDE = "com.njiasalama.action.START_RIDE"
        const val ACTION_STOP_RIDE = "com.njiasalama.action.STOP_RIDE"

        fun start(context: Context) {
            val intent = Intent(context, RideTrackingService::class.java).apply {
                action = ACTION_START_RIDE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, RideTrackingService::class.java).apply {
                action = ACTION_STOP_RIDE
            }
            context.startService(intent)
        }
    }
}

