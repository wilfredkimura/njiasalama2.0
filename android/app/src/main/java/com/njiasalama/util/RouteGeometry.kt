package com.njiasalama.util

import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

/**
 * Geometric utility calculations for bicycle navigation, route tracking, and obstacle proximity.
 */
object RouteGeometry {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates the spherical surface distance in meters between two coordinates using the Haversine formula.
     */
    fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Finds the nearest point along a traversed polyline by projecting the tap coordinate
     * onto each adjacent segment. If the orthogonal distance to the closest segment is within
     * [maxDistanceMeters], the snapped coordinate is returned; otherwise null.
     */
    fun snapToNearestPointOnRoute(
        tap: LatLng,
        routePoints: List<LatLng>,
        maxDistanceMeters: Double = 50.0
    ): LatLng? {
        if (routePoints.isEmpty()) return null
        if (routePoints.size == 1) {
            val dist = haversineDistanceMeters(tap.latitude, tap.longitude, routePoints[0].latitude, routePoints[0].longitude)
            return if (dist <= maxDistanceMeters) routePoints[0] else null
        }

        var closestPoint: LatLng? = null
        var minDistance = Double.MAX_VALUE

        for (i in 0 until routePoints.size - 1) {
            val a = routePoints[i]
            val b = routePoints[i + 1]

            val projected = projectPointOnSegment(tap, a, b)
            val dist = haversineDistanceMeters(tap.latitude, tap.longitude, projected.latitude, projected.longitude)

            if (dist < minDistance) {
                minDistance = dist
                closestPoint = projected
            }
        }

        return if (minDistance <= maxDistanceMeters) closestPoint else null
    }

    /**
     * Projects a coordinate point [p] orthogonally onto the segment [a] -> [b].
     */
    fun projectPointOnSegment(p: LatLng, a: LatLng, b: LatLng): LatLng {
        val midLat = Math.toRadians((a.latitude + b.latitude) / 2.0)
        val cosMidLat = cos(midLat)

        // Equirectangular local Cartesian projection in meters
        val ax = Math.toRadians(a.longitude) * cosMidLat * EARTH_RADIUS_METERS
        val ay = Math.toRadians(a.latitude) * EARTH_RADIUS_METERS

        val bx = Math.toRadians(b.longitude) * cosMidLat * EARTH_RADIUS_METERS
        val by = Math.toRadians(b.latitude) * EARTH_RADIUS_METERS

        val px = Math.toRadians(p.longitude) * cosMidLat * EARTH_RADIUS_METERS
        val py = Math.toRadians(p.latitude) * EARTH_RADIUS_METERS

        val vx = bx - ax
        val vy = by - ay

        val segmentLengthSq = vx * vx + vy * vy
        if (segmentLengthSq == 0.0) return a

        // Scalar projection parameter t along segment
        val t = ((px - ax) * vx + (py - ay) * vy) / segmentLengthSq

        val clampedT = t.coerceIn(0.0, 1.0)
        val sx = ax + clampedT * vx
        val sy = ay + clampedT * vy

        // Convert back to latitude and longitude degrees
        val snappedLat = Math.toDegrees(sy / EARTH_RADIUS_METERS)
        val snappedLon = Math.toDegrees(sx / (cosMidLat * EARTH_RADIUS_METERS))

        return LatLng(snappedLat, snappedLon)
    }

    /**
     * Computes the forward compass bearing angle in degrees [0, 360) from [lat1, lon1] to [lat2, lon2].
     */
    fun computeBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360.0) % 360.0).toFloat()
    }
}

