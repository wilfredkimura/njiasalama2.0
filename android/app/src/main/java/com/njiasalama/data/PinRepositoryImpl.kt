package com.njiasalama.data

import com.njiasalama.domain.model.DangerPin
import com.njiasalama.domain.model.HazardType
import com.njiasalama.domain.repository.PinRepository

/**
 * Concrete implementation of PinRepository managing network data calls to the NjiaSalama API.
 * Uses Kotlin's runCatching block to safely catch exceptions (such as timeouts or connection issues).
 */
class PinRepositoryImpl(
    private val api: NjiaSalamaApi
) : PinRepository {

    override suspend fun getPins(): Result<List<DangerPin>> = runCatching {
        api.getPins()
    }

    override suspend fun getNearbyPins(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int
    ): Result<List<DangerPin>> = runCatching {
        api.getNearbyPins(latitude, longitude, radiusMeters)
    }

    override suspend fun reportPin(
        token: String,
        title: String,
        description: String,
        type: HazardType,
        latitude: Double,
        longitude: Double,
        reportedBy: String,
        imageUrl: String?
    ): Result<DangerPin> = runCatching {
        val request = CreatePinRequest(
            title = title,
            description = description,
            type = type,
            latitude = latitude,
            longitude = longitude,
            reportedBy = reportedBy,
            imageUrl = imageUrl
        )
        api.createPin("Bearer $token", request)
    }
}
