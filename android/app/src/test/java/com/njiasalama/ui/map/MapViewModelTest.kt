package com.njiasalama.ui.map

import com.google.android.gms.maps.model.LatLng // Importing LatLng to represent location coordinates.
import com.njiasalama.data.LocationProvider // Importing LocationProvider to mock it.
import com.njiasalama.domain.model.HazardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow // Importing Flow for mock data stream.
import kotlinx.coroutines.flow.flowOf // Importing flowOf to generate immediate test streams.
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
    fun testMockPinsLoadedSuccessfully() {
        // 1. Arrange & Act: Create the ViewModel.
        // Upon initialization, it runs its 'init' block which calls 'loadMockPins()'.
        val viewModel = MapViewModel(FakeLocationProvider())

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
    fun testLocationUpdatesUpdateUserLocation() {
        // 1. Arrange: Create the ViewModel with our fake location provider.
        val viewModel = MapViewModel(FakeLocationProvider())

        // 2. Act: Request location updates to start streaming.
        viewModel.startLocationUpdates()

        // 3. Assert: Confirm that the userLocation flow gets updated with our fake coordinates.
        val userLocation = viewModel.userLocation.value
        assertTrue("User location should not be null", userLocation != null)
        assertEquals(-1.2921, userLocation!!.latitude, 0.0)
        assertEquals(36.8219, userLocation.longitude, 0.0)
    }
}
