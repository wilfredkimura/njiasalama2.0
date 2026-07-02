package com.njiasalama.data

import com.njiasalama.domain.model.DangerPin
import com.njiasalama.domain.model.HazardType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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

    override suspend fun createPin(request: CreatePinRequest): DangerPin {
        if (shouldFail) throw java.io.IOException("Socket connection refused")
        return DangerPin(
            id = "generated-uuid-xyz",
            title = request.title,
            description = request.description,
            latitude = request.latitude,
            longitude = request.longitude,
            type = request.type,
            reportedBy = request.reportedBy
        )
    }
}

/**
 * Unit tests verifying that PinRepositoryImpl manages network connections safely.
 */
class PinRepositoryTest {

    @Test
    fun testGetPinsReturnsSuccessResult() = runTest {
        // Arrange: Setup api mock to return success pins list
        val expectedPins = listOf(
            DangerPin("1", "Pothole", "Deep pothole", -1.2925, 36.8225, HazardType.POTHOLE, "User1")
        )
        val apiDouble = FakeNjiaSalamaApi(shouldFail = false, responsePins = expectedPins)
        val repository = PinRepositoryImpl(apiDouble)

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
        val repository = PinRepositoryImpl(apiDouble)

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
        val repository = PinRepositoryImpl(apiDouble)

        // Act: Report road hazard pin
        val result = repository.reportPin(
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
