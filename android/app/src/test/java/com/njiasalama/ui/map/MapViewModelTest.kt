package com.njiasalama.ui.map

import com.google.android.gms.maps.model.LatLng // Importing LatLng to represent location coordinates.
import com.njiasalama.data.LocationProvider // Importing LocationProvider to mock it.
import com.njiasalama.domain.model.DangerPin
import com.njiasalama.domain.model.HazardType
import com.njiasalama.domain.repository.PinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow // Importing Flow for mock data stream.
import kotlinx.coroutines.flow.flowOf // Importing flowOf to generate immediate test streams.
import kotlinx.coroutines.flow.emptyFlow
import com.njiasalama.data.websocket.SocketManager
import com.njiasalama.domain.repository.AuthRepository
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * A test double implementing the LocationProvider contract.
 * Emits Nairobi coordinates immediately to simulate user location updates.
 */
class FakeLocationProvider : LocationProvider {
    override fun getLocationUpdates(intervalMillis: Long): Flow<LatLng> {
        return flowOf(LatLng(-1.2921, 36.8219))
    }
}

class FakePinRepository : PinRepository {
    private val pins = mutableListOf(
        DangerPin(
            id = "1", 
            title = "Deep Pothole", 
            description = "Left side of the road, hard to spot at speed", 
            latitude = -1.2925, 
            longitude = 36.8225, 
            type = HazardType.POTHOLE, 
            reportedBy = "User1"
        ),
        DangerPin(
            id = "2", 
            title = "Broken Streetlight", 
            description = "Completely dark corner right after the curve", 
            latitude = -1.2910, 
            longitude = 36.8200, 
            type = HazardType.UNLIT_STREET, 
            reportedBy = "User2"
        )
    )

    override suspend fun getPins(): Result<List<DangerPin>> {
        return Result.success(pins.toList())
    }

    override suspend fun getNearbyPins(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int
    ): Result<List<DangerPin>> {
        return Result.success(pins.toList())
    }

    override suspend fun reportPin(
        token: String,
        title: String,
        description: String,
        type: HazardType,
        latitude: Double,
        longitude: Double,
        reportedBy: String
    ): Result<DangerPin> {
        val newPin = DangerPin(
            id = "pin-uuid-mock",
            title = title,
            description = description,
            latitude = latitude,
            longitude = longitude,
            type = type,
            reportedBy = reportedBy
        )
        pins.add(newPin)
        return Result.success(newPin)
    }
}

class FakeSocketManager : SocketManager {
    override fun connect() {}
    override fun disconnect() {}
    override fun getNewPinFlow(): Flow<DangerPin> = emptyFlow()
}

class FakeAuthRepository : AuthRepository {
    private val _isLoggedIn = kotlinx.coroutines.flow.MutableStateFlow(true)
    override val isLoggedIn = _isLoggedIn.asStateFlow()

    override suspend fun login(email: String, password: String) = Result.success(com.njiasalama.domain.model.AuthResponse("fake-jwt"))
    override suspend fun signUp(email: String, password: String, name: String) = Result.success(com.njiasalama.domain.model.AuthResponse("fake-jwt"))
    override suspend fun googleLogin(idToken: String) = Result.success(com.njiasalama.domain.model.AuthResponse("fake-jwt"))
    override fun getToken(): String? = "fake-jwt"
    override fun getUserName(): String? = "Fake Cyclist"
    override fun saveToken(token: String) { _isLoggedIn.value = true }
    override fun logout() { _isLoggedIn.value = false }
}

/**
 * Unit tests for the MapViewModel.
 * Unit tests are automated checks that test a single class in isolation.
 * They run on your computer (JVM) without needing a physical phone or emulator.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    // A Test Dispatcher simulates the Android Main UI Thread.
    // UnconfinedTestDispatcher runs coroutine tasks immediately on the test thread,
    // which makes testing asynchronous code simple and direct.
    private val testDispatcher = UnconfinedTestDispatcher()

    /**
     * The @Before function runs automatically before each test is executed.
     * We use it to set up our mock environment.
     */
    @Before
    fun setUp() {
        // Direct Kotlin Coroutines to use our simulated test thread
        // instead of the real Android UI Main thread (which doesn't exist in unit tests).
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * The @After function runs automatically after each test completes.
     * We use it to clean up the environment.
     */
    @After
    fun tearDown() {
        // Reset the main dispatcher to its original state to avoid affecting other tests.
        Dispatchers.resetMain()
    }

    /**
     * This is the test case. It creates the ViewModel and asserts that
     * the list of mock danger pins is successfully loaded and matches our details.
     */
    @Test
    fun testMockPinsLoadedSuccessfully() = kotlinx.coroutines.test.runTest(testDispatcher) {
        // 1. Arrange & Act: Create the ViewModel.
        // Upon initialization, it runs its 'init' block which calls 'loadPins()'.
        val viewModel = MapViewModel(FakeLocationProvider(), FakePinRepository(), FakeSocketManager(), FakeAuthRepository())

        // Let the initialized coroutines (loadPins) complete
        this.testScheduler.advanceUntilIdle()

        // 2. Assert: Verify the resulting state.
        val currentState = viewModel.uiState.value

        // Confirm that the UI state has successfully transitioned to Success
        assertTrue("UI State should be MapUiState.Success", currentState is MapUiState.Success)

        // Safely treat the state as a Success class so we can look at the pins
        val successState = currentState as MapUiState.Success
        
        // Assert that we have exactly 2 mock pins loaded
        assertEquals(2, successState.pins.size)

        // Verify the details of the first danger pin
        val firstPin = successState.pins[0]
        assertEquals("1", firstPin.id)
        assertEquals("Deep Pothole", firstPin.title)
        assertEquals(HazardType.POTHOLE, firstPin.type)
        assertEquals("User1", firstPin.reportedBy)

        // Verify the details of the second danger pin
        val secondPin = successState.pins[1]
        assertEquals("2", secondPin.id)
        assertEquals("Broken Streetlight", secondPin.title)
        assertEquals(HazardType.UNLIT_STREET, secondPin.type)
        assertEquals("User2", secondPin.reportedBy)
    }

    /**
     * Test case to verify that starting location updates correctly fetches
     * GPS coordinates from the provider and updates the ViewModel's userLocation flow state.
     */
    @Test
    fun testLocationUpdatesUpdateUserLocation() = kotlinx.coroutines.test.runTest(testDispatcher) {
        // 1. Arrange: Create the ViewModel with our fake location provider.
        val viewModel = MapViewModel(FakeLocationProvider(), FakePinRepository(), FakeSocketManager(), FakeAuthRepository())
        this.testScheduler.advanceUntilIdle()

        // 2. Act: Request location updates to start streaming.
        viewModel.startLocationUpdates()
        this.testScheduler.advanceUntilIdle()

        // 3. Assert: Confirm that the userLocation flow gets updated with our fake coordinates.
        val userLocation = viewModel.userLocation.value
        assertTrue("User location should not be null", userLocation != null)
        assertEquals(-1.2921, userLocation!!.latitude, 0.0)
        assertEquals(36.8219, userLocation.longitude, 0.0)
    }

    /**
     * Test case to verify that adding a hazard pin locally appends the new pin
     * successfully to the ViewModel's Success UI state list and increments its size.
     */
    @Test
    fun testAddDangerPinLocallyAppendsPinSuccessfully() = kotlinx.coroutines.test.runTest(testDispatcher) {
        // 1. Arrange: Create the ViewModel with our fake location provider.
        val viewModel = MapViewModel(FakeLocationProvider(), FakePinRepository(), FakeSocketManager(), FakeAuthRepository())
        this.testScheduler.advanceUntilIdle()

        // Retrieve initial Success state list (should have 2 mock pins)
        val initialState = viewModel.uiState.value as MapUiState.Success
        assertEquals(2, initialState.pins.size)

        // 2. Act: Call addDangerPinLocally to append a third pin
        viewModel.addDangerPinLocally(
            title = "Dangerous Traffic",
            description = "Construction zone blocks path",
            latitude = -1.3000,
            longitude = 36.8300,
            type = HazardType.DANGEROUS_TRAFFIC
        )
        // Let the asynchronous report coroutine finish
        this.testScheduler.advanceUntilIdle()

        // 3. Assert: Verify the state has updated with 3 pins
        val updatedState = viewModel.uiState.value as MapUiState.Success
        assertEquals(3, updatedState.pins.size)

        // Confirm the details of the newly appended pin
        val newlyAddedPin = updatedState.pins.last()
        assertEquals("Dangerous Traffic", newlyAddedPin.title)
        assertEquals("Construction zone blocks path", newlyAddedPin.description)
        assertEquals(-1.3000, newlyAddedPin.latitude, 0.0)
        assertEquals(36.8300, newlyAddedPin.longitude, 0.0)
        assertEquals(HazardType.DANGEROUS_TRAFFIC, newlyAddedPin.type)
        assertEquals("Fake Cyclist", newlyAddedPin.reportedBy)
    }

    /**
     * Test case to verify that when SocketManager emits a new danger pin via its Flow,
     * the ViewModel consumes it and appends it to its Success UI state list.
     */
    @Test
    fun testWebSocketBroadcastAppendsPinSuccessfully() = kotlinx.coroutines.test.runTest(testDispatcher) {
        // Arrange: Prepare the live pin to be broadcasted
        val livePin = DangerPin(
            id = "socket-pin-999",
            title = "Live Road Hazard",
            description = "Construction debris",
            latitude = -1.3500,
            longitude = 36.8500,
            type = HazardType.OTHER,
            reportedBy = "LiveBroadcaster"
        )
        
        // Define a shared flow to simulate live socket events dynamically
        val liveFlow = kotlinx.coroutines.flow.MutableSharedFlow<DangerPin>()
        val customSocketManager = object : SocketManager {
            override fun connect() {}
            override fun disconnect() {}
            override fun getNewPinFlow(): Flow<DangerPin> = liveFlow
        }
        
        val viewModel = MapViewModel(FakeLocationProvider(), FakePinRepository(), customSocketManager, FakeAuthRepository())
        this.testScheduler.advanceUntilIdle()
        
        val initialState = viewModel.uiState.value as MapUiState.Success
        assertEquals(2, initialState.pins.size)
        
        // Act: Emit the pin from the WebSocket stream
        liveFlow.emit(livePin)
        this.testScheduler.advanceUntilIdle()
        
        // Assert: Confirm that the UI state has successfully updated to include the broadcasted pin
        val updatedState = viewModel.uiState.value as MapUiState.Success
        assertEquals(3, updatedState.pins.size)
        
        val addedPin = updatedState.pins.last()
        assertEquals("socket-pin-999", addedPin.id)
        assertEquals("Live Road Hazard", addedPin.title)
        assertEquals(HazardType.OTHER, addedPin.type)
    }
}
