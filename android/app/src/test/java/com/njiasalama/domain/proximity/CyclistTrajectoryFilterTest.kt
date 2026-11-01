package com.njiasalama.domain.proximity

import com.google.android.gms.maps.model.LatLng
import com.njiasalama.domain.model.DangerPin
import org.junit.Assert.*
import org.junit.Test

class CyclistTrajectoryFilterTest {

    private val samplePin = DangerPin(
        id = "pin-1",
        title = "Severe Pothole",
        description = "Deep crater in bike lane",
        type = "POTHOLE",
        latitude = 0.0005, // ~55m North
        longitude = 0.0,
        reportedBy = "Tester",
        createdAt = "2026-11-01"
    )

    @Test
    fun test_cyclist_heading_north_towards_pin_triggers_notice() {
        val currentLoc = LatLng(0.0, 0.0)
        val speedMps = 5.0f // ~18 km/h
        val bearing = 0.0f // Heading North

        val alert = CyclistTrajectoryFilter.evaluateHazard(
            currentLocation = currentLoc,
            currentSpeedMps = speedMps,
            currentBearing = bearing,
            pin = samplePin
        )

        assertNotNull(alert)
        assertEquals(ProximityAlertLevel.NOTICE, alert!!.level)
        assertTrue(alert.distanceMeters in 50.0..60.0)
    }

    @Test
    fun test_cyclist_approaching_urgent_distance_triggers_urgent() {
        // Cyclist 30m South of pin
        val currentLoc = LatLng(0.00023, 0.0)
        val speedMps = 5.0f
        val bearing = 0.0f // Heading North towards pin

        val alert = CyclistTrajectoryFilter.evaluateHazard(
            currentLocation = currentLoc,
            currentSpeedMps = speedMps,
            currentBearing = bearing,
            pin = samplePin
        )

        assertNotNull(alert)
        assertEquals(ProximityAlertLevel.URGENT, alert!!.level)
        assertTrue(alert.distanceMeters < 45.0)
    }

    @Test
    fun test_cyclist_heading_south_away_from_pin_is_ignored() {
        val currentLoc = LatLng(0.0, 0.0)
        val speedMps = 5.0f
        val bearing = 180.0f // Heading South away from pin

        val alert = CyclistTrajectoryFilter.evaluateHazard(
            currentLocation = currentLoc,
            currentSpeedMps = speedMps,
            currentBearing = bearing,
            pin = samplePin
        )

        assertNull(alert) // Pin is behind cyclist!
    }

    @Test
    fun test_stopped_cyclist_suppresses_alerts() {
        val currentLoc = LatLng(0.0, 0.0)
        val speedMps = 0.2f // Stopped at red light
        val bearing = 0.0f

        val alert = CyclistTrajectoryFilter.evaluateHazard(
            currentLocation = currentLoc,
            currentSpeedMps = speedMps,
            currentBearing = bearing,
            pin = samplePin
        )

        assertNull(alert) // Suppressed when stationary
    }
}

