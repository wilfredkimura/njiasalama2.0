package com.njiasalama.data

import com.njiasalama.domain.model.DangerPin
import com.njiasalama.domain.model.HazardType
import com.njiasalama.domain.repository.PinRepository
import com.njiasalama.domain.model.Route
import com.njiasalama.domain.model.SavedRoute
import com.njiasalama.domain.model.GeocodeLocation

/**
 * Concrete implementation of PinRepository managing network data calls to the NjiaSalama API.
 * Uses Kotlin's runCatching block to safely catch exceptions (such as timeouts or connection issues).
 */
class PinRepositoryImpl(
    private val cacheDir: java.io.File,
    private val api: NjiaSalamaApi
) : PinRepository {

    private val gson = com.google.gson.Gson()
    private val cacheFileName = "cached_pins.json"

    private fun getCachedPins(): List<DangerPin> {
        return try {
            val file = java.io.File(cacheDir, cacheFileName)
            if (!file.exists()) return emptyList()
            val json = file.readText()
            val type = object : com.google.gson.reflect.TypeToken<List<DangerPin>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun cachePins(pins: List<DangerPin>) {
        try {
            val file = java.io.File(cacheDir, cacheFileName)
            val json = gson.toJson(pins)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun getPins(): Result<List<DangerPin>> = runCatching {
        try {
            val pins = api.getPins()
            cachePins(pins)
            pins
        } catch (e: Exception) {
            val cached = getCachedPins()
            if (cached.isNotEmpty()) {
                cached
            } else {
                throw e
            }
        }
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

    override suspend fun getRoutes(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        waypoints: String?
    ): Result<List<Route>> = runCatching {
        api.getRoutes(startLat, startLng, endLat, endLng, waypoints)
    }

    override suspend fun geocode(query: String): Result<List<GeocodeLocation>> = runCatching {
        api.geocode(query)
    }

    override suspend fun saveRoute(
        token: String,
        name: String,
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        points: List<com.njiasalama.domain.model.RoutePoint>,
        surfaceType: com.njiasalama.domain.model.SurfaceType,
        distanceKm: Double
    ): Result<SavedRoute> = runCatching {
        val request = SavedRouteRequest(
            name = name,
            startLat = startLat,
            startLng = startLng,
            endLat = endLat,
            endLng = endLng,
            points = points,
            surfaceType = surfaceType,
            distanceKm = distanceKm
        )
        api.saveRoute("Bearer $token", request)
    }

    override suspend fun getSavedRoutes(token: String): Result<List<SavedRoute>> = runCatching {
        api.getSavedRoutes("Bearer $token")
    }

    override suspend fun deleteSavedRoute(token: String, id: String): Result<Boolean> = runCatching {
        val response = api.deleteSavedRoute("Bearer $token", id)
        response["success"] == true
    }
}
