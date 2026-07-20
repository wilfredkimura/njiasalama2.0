package com.njiasalama.data

import com.njiasalama.domain.model.DangerPin
import com.njiasalama.domain.model.HazardType
import com.njiasalama.domain.model.AuthResponse
import com.njiasalama.domain.model.GoogleAuthRequest
import com.njiasalama.domain.model.LoginRequest
import com.njiasalama.domain.model.SignUpRequest
import com.njiasalama.domain.model.Route
import com.njiasalama.domain.model.SavedRoute
import com.njiasalama.domain.model.GeocodeLocation
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Query
import retrofit2.http.Path

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
    val reportedBy: String,
    val imageUrl: String? = null
)

data class SavedRouteRequest(
    val name: String,
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val points: List<com.njiasalama.domain.model.RoutePoint>,
    val surfaceType: com.njiasalama.domain.model.SurfaceType,
    val distanceKm: Double
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
     * Maps to: GET /routes
     * Fetches alternative route lines between point A and point B, complete with hazard overlaps and surface types.
     */
    @GET("routes")
    suspend fun getRoutes(
        @Query("startLat") startLat: Double,
        @Query("startLng") startLng: Double,
        @Query("endLat") endLat: Double,
        @Query("endLng") endLng: Double,
        @Query("waypoints") waypoints: String? = null
    ): List<Route>

    @GET("routes/geocode")
    suspend fun geocode(
        @Query("query") query: String,
        @Query("focusLat") focusLat: Double? = null,
        @Query("focusLng") focusLng: Double? = null
    ): List<GeocodeLocation>

    @POST("routes/save")
    suspend fun saveRoute(
        @Header("Authorization") token: String,
        @Body request: SavedRouteRequest
    ): SavedRoute

    @GET("routes/saved")
    suspend fun getSavedRoutes(
        @Header("Authorization") token: String
    ): List<SavedRoute>

    @DELETE("routes/saved/{id}")
    suspend fun deleteSavedRoute(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Map<String, Boolean>

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
        @Header("Authorization") token: String,
        @Body request: CreatePinRequest
    ): DangerPin

    /**
     * Maps to: POST /auth/signup
     * Sends user registration details to standard email sign up endpoint.
     */
    @POST("auth/signup")
    suspend fun signUp(
        @Body request: SignUpRequest
    ): AuthResponse

    /**
     * Maps to: POST /auth/login
     * Sends email/password credentials to login endpoint.
     */
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): AuthResponse

    /**
     * Maps to: POST /auth/google
     * Sends verified Google ID token to google auth endpoint.
     */
    @POST("auth/google")
    suspend fun googleLogin(
        @Body request: GoogleAuthRequest
    ): AuthResponse
}
