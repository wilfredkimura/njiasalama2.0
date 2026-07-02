package com.njiasalama.data

import com.njiasalama.domain.model.DangerPin
import com.njiasalama.domain.model.HazardType
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Data class representing the JSON payload sent when reporting a new hazard.
 * This aligns directly with the CreatePinDto constraints on the NestJS backend.
 */
data class CreatePinRequest(
    val title: String,
    val description: String,
    val type: HazardType,
    val latitude: Double,
    val longitude: Double,
    val reportedBy: String
)

/**
 * Retrofit network interface mapping HTTP routes of our NestJS backend API.
 * Uses Kotlin Coroutines (suspend functions) to perform non-blocking asynchronous networking.
 */
interface NjiaSalamaApi {

    /**
     * Maps to: GET /pins
     * Fetches the complete list of road hazard pins from the database.
     */
    @GET("pins")
    suspend fun getPins(): List<DangerPin>

    /**
     * Maps to: GET /pins/nearby
     * Fetches hazard pins within a specific radius (in meters) of the cyclist.
     */
    @GET("pins/nearby")
    suspend fun getNearbyPins(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radiusMeters: Int? = null
    ): List<DangerPin>

    /**
     * Maps to: POST /pins
     * Sends a new cyclist-reported road hazard pin payload to the backend server.
     */
    @POST("pins")
    suspend fun createPin(
        @Body request: CreatePinRequest
    ): DangerPin
}
