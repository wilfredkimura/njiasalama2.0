package com.njiasalama.data

import android.location.Location
import com.njiasalama.domain.model.BreadcrumbPoint
import com.njiasalama.domain.model.TraversedRide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manages in-memory and on-disk recording of GPS breadcrumbs during active cycling rides.
 * Allows cyclists to preserve their traversed route for post-ride debriefing and hazard reporting.
 */
class RideRecorder(private val filesDir: File) {

    private val gson = Gson()
    private val lastRideFile = File(filesDir, "last_recorded_ride.json")

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _activeBreadcrumbs = MutableStateFlow<List<BreadcrumbPoint>>(emptyList())
    val activeBreadcrumbs: StateFlow<List<BreadcrumbPoint>> = _activeBreadcrumbs.asStateFlow()

    private var rideStartTime: Long = 0L
    private var totalDistanceMeters: Double = 0.0
    private var lastRecordedLocation: Location? = null

    /**
     * Starts a new ride recording session.
     */
    fun startRide() {
        _activeBreadcrumbs.value = emptyList()
        rideStartTime = System.currentTimeMillis()
        totalDistanceMeters = 0.0
        lastRecordedLocation = null
        _isRecording.value = true
    }

    /**
     * Appends a new GPS coordinate breadcrumb if the user has moved at least [minDisplacementMeters].
     */
    fun recordLocation(location: Location, minDisplacementMeters: Float = 3.0f) {
        if (!_isRecording.value) return

        val lastLoc = lastRecordedLocation
        if (lastLoc != null) {
            val dist = lastLoc.distanceTo(location)
            if (dist < minDisplacementMeters) {
                // Ignore micro-jitter to conserve memory and preserve clean route lines
                return
            }
            totalDistanceMeters += dist
        }

        lastRecordedLocation = location
        val point = BreadcrumbPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            speed = location.speed,
            timestamp = System.currentTimeMillis()
        )

        _activeBreadcrumbs.value = _activeBreadcrumbs.value + point
    }

    /**
     * Ends the active ride, persists the session to local cache, and returns the completed TraversedRide.
     */
    fun stopRide(): TraversedRide? {
        if (!_isRecording.value && _activeBreadcrumbs.value.isEmpty()) return null

        _isRecording.value = false
        val points = _activeBreadcrumbs.value
        val completedRide = TraversedRide(
            startTime = rideStartTime,
            endTime = System.currentTimeMillis(),
            points = points,
            totalDistanceMeters = totalDistanceMeters
        )

        // Save last completed ride to disk for seamless recovery if app restarts
        try {
            lastRideFile.writeText(gson.toJson(completedRide))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return completedRide
    }

    /**
     * Retrieves the most recently completed ride from memory or local disk.
     */
    fun getLastCompletedRide(): TraversedRide? {
        return try {
            if (lastRideFile.exists()) {
                val json = lastRideFile.readText()
                val type = object : TypeToken<TraversedRide>() {}.type
                gson.fromJson(json, type)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

