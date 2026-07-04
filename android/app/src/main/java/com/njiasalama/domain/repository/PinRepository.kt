package com.njiasalama.domain.repository

import com.njiasalama.domain.model.DangerPin
import com.njiasalama.domain.model.HazardType

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
}
