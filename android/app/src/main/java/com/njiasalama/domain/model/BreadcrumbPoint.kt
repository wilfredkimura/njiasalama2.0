package com.njiasalama.domain.model

/**
 * Represents a single GPS breadcrumb recorded during an active ride.
 * @param latitude Geographic latitude
 * @param longitude Geographic longitude
 * @param altitude Elevation in meters (optional)
 * @param speed Speed in meters per second (optional)
 * @param timestamp Epoch timestamp in milliseconds when this coordinate was recorded
 */
data class BreadcrumbPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents a recorded ride session containing all breadcrumbs and summary stats.
 */
data class TraversedRide(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startTime: Long,
    val endTime: Long,
    val points: List<BreadcrumbPoint>,
    val totalDistanceMeters: Double = 0.0
)

