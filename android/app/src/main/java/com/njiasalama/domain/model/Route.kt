package com.njiasalama.domain.model

import java.io.Serializable

enum class SurfaceType {
    ROAD,
    GRAVEL
}

data class Route(
    val id: String,
    val name: String,
    val points: List<RoutePoint>,
    val surfaceType: SurfaceType,
    val distanceKm: Double,
    val dangerPins: List<DangerPin>
) : Serializable
