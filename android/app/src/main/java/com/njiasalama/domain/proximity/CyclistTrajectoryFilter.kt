package com.njiasalama.domain.proximity

import com.google.android.gms.maps.model.LatLng
import com.njiasalama.domain.model.DangerPin
import com.njiasalama.domain.model.HazardType
import com.njiasalama.util.RouteGeometry
import kotlin.math.abs

/**
 * Severity level of an upcoming road hazard for a cyclist.
 */
enum class ProximityAlertLevel {
    NONE,
    NOTICE,   // ~100m–120m: Prepare to check shoulder and maneuver safely
    URGENT    // ~35m–45m: Immediate road hazard right ahead (brake/swerve)
}

/**
 * Filter result indicating if an alert should be triggered for a hazard.
 */
data class ProximityAlert(
    val pin: DangerPin,
    val distanceMeters: Double,
    val relativeBearingDegrees: Float,
    val level: ProximityAlertLevel
)

/**
 * Evaluates whether a danger pin poses an immediate collision or safety risk to a moving cyclist
 * based on speed, course trajectory, and distance.
 */
object CyclistTrajectoryFilter {

    const val NOTICE_DISTANCE_METERS = 120.0
    const val URGENT_DISTANCE_METERS = 45.0
    const val CONE_HALF_ANGLE_DEGREES = 50.0f
    const val MIN_CYCLING_SPEED_MPS = 0.83f // ~3 km/h (below this is walking/stopped)

    /**
     * Determines the alert level for a single hazard given current cyclist GPS location,
     * speed, and forward GPS course bearing.
     */
    fun evaluateHazard(
        currentLocation: LatLng,
        currentSpeedMps: Float,
        currentBearing: Float,
        pin: DangerPin
    ): ProximityAlert? {
        // Suppress alerts if cyclist is stopped at a traffic light or walking slowly
        if (currentSpeedMps < MIN_CYCLING_SPEED_MPS) {
            return null
        }

        val distance = RouteGeometry.haversineDistanceMeters(
            currentLocation.latitude,
            currentLocation.longitude,
            pin.latitude,
            pin.longitude
        )

        // Beyond alert horizon
        if (distance > NOTICE_DISTANCE_METERS) {
            return null
        }

        // Compute bearing from cyclist to hazard
        val bearingToPin = RouteGeometry.computeBearing(
            currentLocation.latitude,
            currentLocation.longitude,
            pin.latitude,
            pin.longitude
        )

        // Angular difference between cycling movement vector and hazard direction
        val diff = abs(currentBearing - bearingToPin)
        val relativeBearing = if (diff > 180f) 360f - diff else diff

        // If hazard is outside forward traveling cone (e.g. behind cyclist or on adjacent street), ignore
        if (relativeBearing > CONE_HALF_ANGLE_DEGREES) {
            return null
        }

        val level = if (distance <= URGENT_DISTANCE_METERS) {
            ProximityAlertLevel.URGENT
        } else {
            ProximityAlertLevel.NOTICE
        }

        return ProximityAlert(
            pin = pin,
            distanceMeters = distance,
            relativeBearingDegrees = relativeBearing,
            level = level
        )
    }
}

