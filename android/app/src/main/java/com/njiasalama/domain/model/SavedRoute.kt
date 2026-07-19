package com.njiasalama.domain.model

import java.io.Serializable

data class SavedRoute(
    val id: String,
    val name: String,
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val points: List<RoutePoint>,
    val surfaceType: SurfaceType,
    val distanceKm: Double,
    val createdAt: String
) : Serializable
