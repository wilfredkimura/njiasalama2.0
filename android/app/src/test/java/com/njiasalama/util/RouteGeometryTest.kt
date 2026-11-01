package com.njiasalama.util

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Test

class RouteGeometryTest {

    @Test
    fun test_haversine_distance_accuracy() {
        // Nairobi CBD points (~500m apart)
        val lat1 = -1.286389
        val lon1 = 36.817223
        val lat2 = -1.283333
        val lon2 = 36.816667

        val distance = RouteGeometry.haversineDistanceMeters(lat1, lon1, lat2, lon2)
        assertEquals(345.0, distance, 15.0)
    }

    @Test
    fun test_orthogonal_projection_on_straight_segment() {
        // Line segment from (0, 0) to (0, 0.001)
        val a = LatLng(0.0, 0.0)
        val b = LatLng(0.0, 0.001)

        // Point offset slightly to the right of the midpoint
        val tap = LatLng(0.0001, 0.0005)

        val projected = RouteGeometry.projectPointOnSegment(tap, a, b)

        // Snapped point should lie directly on the segment (lat = 0.0, lon ~ 0.0005)
        assertEquals(0.0, projected.latitude, 0.00001)
        assertEquals(0.0005, projected.longitude, 0.00001)
    }

    @Test
    fun test_snap_to_nearest_route_point_within_threshold() {
        val route = listOf(
            LatLng(-1.2860, 36.8170),
            LatLng(-1.2870, 36.8170),
            LatLng(-1.2880, 36.8170)
        )

        // Tap 10m off to the east of the route
        val tapNear = LatLng(-1.2865, 36.81709)
        val snapped = RouteGeometry.snapToNearestPointOnRoute(tapNear, route, maxDistanceMeters = 50.0)

        assertNotNull(snapped)
        assertEquals(36.8170, snapped!!.longitude, 0.00005)
    }

    @Test
    fun test_tap_far_from_route_returns_null() {
        val route = listOf(
            LatLng(-1.2860, 36.8170),
            LatLng(-1.2870, 36.8170)
        )

        // Tap 500m away
        val tapFar = LatLng(-1.2800, 36.8250)
        val snapped = RouteGeometry.snapToNearestPointOnRoute(tapFar, route, maxDistanceMeters = 50.0)

        assertNull(snapped)
    }

    @Test
    fun test_compute_bearing_accuracy() {
        // Straight North: bearing 0 deg
        val bearingNorth = RouteGeometry.computeBearing(0.0, 0.0, 1.0, 0.0)
        assertEquals(0f, bearingNorth, 1.0f)

        // Straight East: bearing 90 deg
        val bearingEast = RouteGeometry.computeBearing(0.0, 0.0, 0.0, 1.0)
        assertEquals(90f, bearingEast, 1.0f)

        // Straight South: bearing 180 deg
        val bearingSouth = RouteGeometry.computeBearing(1.0, 0.0, 0.0, 0.0)
        assertEquals(180f, bearingSouth, 1.0f)
    }
}

