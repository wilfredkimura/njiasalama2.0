package com.njiasalama.data

import com.njiasalama.domain.model.DangerPin
import com.njiasalama.domain.model.HazardType
import com.njiasalama.domain.model.AuthResponse
import com.njiasalama.domain.model.GoogleAuthRequest
import com.njiasalama.domain.model.LoginRequest
import com.njiasalama.domain.model.SignUpRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * A test double for the Retrofit API interface.
 * Implements NjiaSalamaApi to simulate network responses and exceptions in unit tests
 * without requiring real HTTP network calls.
 */
class FakeNjiaSalamaApi(
    private val shouldFail: Boolean = false,
    private val responsePins: List<DangerPin> = emptyList()
) : NjiaSalamaApi {

    override suspend fun getPins(): List<DangerPin> {
        if (shouldFail) throw java.io.IOException("Network connection timeout")
        return responsePins
    }

    override suspend fun getNearbyPins(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int?
    ): List<DangerPin> {
        if (shouldFail) throw java.io.IOException("Network connection timeout")
        return responsePins
    }

    override suspend fun createPin(token: String, request: CreatePinRequest): DangerPin {
        if (shouldFail) throw java.io.IOException("Socket connection refused")
        return DangerPin(
            id = "generated-uuid-xyz",
            title = request.title,
            description = request.description,
            latitude = request.latitude,
            longitude = request.longitude,
            type = request.type,
            reportedBy = request.reportedBy,
            imageUrl = request.imageUrl
        )
    }

    override suspend fun signUp(request: SignUpRequest): AuthResponse {
        if (shouldFail) throw java.io.IOException("Auth server error")
        return AuthResponse("fake-jwt-token")
    }

    override suspend fun login(request: LoginRequest): AuthResponse {
        if (shouldFail) throw java.io.IOException("Auth server error")
        return AuthResponse("fake-jwt-token")
    }

    override suspend fun googleLogin(request: GoogleAuthRequest): AuthResponse {
        if (shouldFail) throw java.io.IOException("Auth server error")
        return AuthResponse("fake-jwt-token")
    }

    override suspend fun getRoutes(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        waypoints: String?
    ): List<com.njiasalama.domain.model.Route> {
        if (shouldFail) throw java.io.IOException("Network connection timeout")
        return emptyList()
    }

    override suspend fun geocode(query: String): List<com.njiasalama.domain.model.GeocodeLocation> {
        if (shouldFail) throw java.io.IOException("Network connection timeout")
        return emptyList()
    }

    override suspend fun saveRoute(token: String, request: SavedRouteRequest): com.njiasalama.domain.model.SavedRoute {
        if (shouldFail) throw java.io.IOException("Network connection timeout")
        return com.njiasalama.domain.model.SavedRoute(
            id = "saved-id",
            name = request.name,
            startLat = request.startLat,
            startLng = request.startLng,
            endLat = request.endLat,
            endLng = request.endLng,
            points = request.points,
            surfaceType = request.surfaceType,
            distanceKm = request.distanceKm,
            createdAt = "2026-07-19T12:00:00Z"
        )
    }

    override suspend fun getSavedRoutes(token: String): List<com.njiasalama.domain.model.SavedRoute> {
        if (shouldFail) throw java.io.IOException("Network connection timeout")
        return emptyList()
    }

    override suspend fun deleteSavedRoute(token: String, id: String): Map<String, Boolean> {
        if (shouldFail) throw java.io.IOException("Network connection timeout")
        return mapOf("success" to true)
    }
}

/**
 * Unit tests verifying that PinRepositoryImpl manages network connections safely.
 */
class PinRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testGetPinsReturnsSuccessResult() = runTest {
        // Arrange: Setup api mock to return success pins list
        val expectedPins = listOf(
            DangerPin("1", "Pothole", "Deep pothole", -1.2925, 36.8225, HazardType.POTHOLE, "User1")
        )
        val apiDouble = FakeNjiaSalamaApi(shouldFail = false, responsePins = expectedPins)
        val repository = PinRepositoryImpl(tempFolder.newFolder("cache"), apiDouble)

        // Act: Execute load calls
        val result = repository.getPins()

        // Assert: Verify it wrapped success details correctly
        assertTrue(result.isSuccess)
        assertEquals(expectedPins, result.getOrNull())
    }

    @Test
    fun testGetPinsReturnsFailureResultOnNetworkException() = runTest {
        // Arrange: Setup api mock to raise connectivity issues
        val apiDouble = FakeNjiaSalamaApi(shouldFail = true)
        val repository = PinRepositoryImpl(tempFolder.newFolder("cache"), apiDouble)

        // Act: Execute calls
        val result = repository.getPins()

        // Assert: Verify it caught the network failure safely
        assertTrue(result.isFailure)
        assertEquals("Network connection timeout", result.exceptionOrNull()?.message)
    }

    @Test
    fun testReportPinReturnsSuccessResult() = runTest {
        // Arrange: Setup api mock to save reports successfully
        val apiDouble = FakeNjiaSalamaApi(shouldFail = false)
        val repository = PinRepositoryImpl(tempFolder.newFolder("cache"), apiDouble)

        // Act: Report road hazard pin
        val result = repository.reportPin(
            token = "fake-token",
            title = "Pothole",
            description = "Deep pothole",
            type = HazardType.POTHOLE,
            latitude = -1.2925,
            longitude = 36.8225,
            reportedBy = "User1"
        )

        // Assert: Verify returned entity matches parameters
        assertTrue(result.isSuccess)
        val pin = result.getOrNull()!!
        assertEquals("generated-uuid-xyz", pin.id)
        assertEquals("Pothole", pin.title)
        assertEquals(-1.2925, pin.latitude, 0.0)
    }
}
