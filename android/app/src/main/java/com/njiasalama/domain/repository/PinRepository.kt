package com.njiasalama.domain.repository

import com.njiasalama.domain.model.DangerPin
import com.njiasalama.domain.model.HazardType
import com.njiasalama.domain.model.Route
import com.njiasalama.domain.model.SavedRoute
import com.njiasalama.domain.model.GeocodeLocation

/**
 * Interface defining the contract for database and network operations on road hazard pins.
 * Decoupling database/network clients from viewmodels makes testing and codebase changes easy.
 */
interface PinRepository {

    /**
     * Retrieves all danger pins from the backend server.
     */
    suspend fun getPins(): Result<List<DangerPin>>

    /**
     * Retrieves danger pins located close to the cyclist's coordinates.
     */
    suspend fun getNearbyPins(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int
    ): Result<List<DangerPin>>

    /**
     * Submits a new cyclist-reported road hazard to the backend database.
     */
    suspend fun reportPin(
        token: String,
        title: String,
        description: String,
        type: HazardType,
        latitude: Double,
        longitude: Double,
        reportedBy: String,
        imageUrl: String? = null
    ): Result<DangerPin>

    /**
     * Retrieves alternative route paths between start and end coordinates with hazard details.
     */
    suspend fun getRoutes(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        waypoints: String? = null
    ): Result<List<Route>>

    suspend fun geocode(query: String): Result<List<GeocodeLocation>>

    suspend fun saveRoute(
        token: String,
        name: String,
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        points: List<com.njiasalama.domain.model.RoutePoint>,
        surfaceType: com.njiasalama.domain.model.SurfaceType,
        distanceKm: Double
    ): Result<SavedRoute>

    suspend fun getSavedRoutes(token: String): Result<List<SavedRoute>>

    suspend fun deleteSavedRoute(token: String, id: String): Result<Boolean>
}
