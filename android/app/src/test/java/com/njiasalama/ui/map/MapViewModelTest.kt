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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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
        reportedBy: String,
        imageUrl: String?
    ): Result<DangerPin> {
        val newPin = DangerPin(
            id = "pin-uuid-mock",
            title = title,
            description = description,
            latitude = latitude,
            longitude = longitude,
            type = type,
            reportedBy = reportedBy,
            imageUrl = imageUrl
        )
        pins.add(newPin)
        return Result.success(newPin)
    }

    override suspend fun getRoutes(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        waypoints: String?
    ): Result<List<com.njiasalama.domain.model.Route>> {
        val points = listOf(
            com.njiasalama.domain.model.RoutePoint(startLat, startLng),
            com.njiasalama.domain.model.RoutePoint(endLat, endLng)
        )
        val roadRoute = com.njiasalama.domain.model.Route(
            id = "test-road-1",
            name = "Road Route",
            points = points,
            surfaceType = com.njiasalama.domain.model.SurfaceType.ROAD,
            distanceKm = 2.5,
            dangerPins = emptyList()
        )
        val gravelRoute = com.njiasalama.domain.model.Route(
            id = "test-gravel-1",
            name = "Gravel Route",
            points = points,
            surfaceType = com.njiasalama.domain.model.SurfaceType.GRAVEL,
            distanceKm = 3.0,
            dangerPins = listOf(
                DangerPin("1", "Pothole", "Pothole", startLat, startLng, HazardType.POTHOLE, "User1")
            )
        )
        return Result.success(listOf(roadRoute, gravelRoute))
    }

    val geocodeResults = mutableListOf<com.njiasalama.domain.model.GeocodeLocation>()
    val savedRoutesList = mutableListOf<com.njiasalama.domain.model.SavedRoute>()

    override suspend fun geocode(query: String): Result<List<com.njiasalama.domain.model.GeocodeLocation>> {
        return Result.success(geocodeResults)
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
    ): Result<com.njiasalama.domain.model.SavedRoute> {
        val mockSavedRoute = com.njiasalama.domain.model.SavedRoute(
            id = "saved-route-${java.util.UUID.randomUUID()}",
            name = name,
            startLat = startLat,
            startLng = startLng,
            endLat = endLat,
            endLng = endLng,
            points = points,
            surfaceType = surfaceType,
            distanceKm = distanceKm,
            createdAt = "2026-07-19T12:00:00Z"
        )
        savedRoutesList.add(mockSavedRoute)
        return Result.success(mockSavedRoute)
    }

    override suspend fun getSavedRoutes(token: String): Result<List<com.njiasalama.domain.model.SavedRoute>> {
        return Result.success(savedRoutesList.toList())
    }

    override suspend fun deleteSavedRoute(token: String, id: String): Result<Boolean> {
        val removed = savedRoutesList.removeIf { it.id == id }
        return Result.success(removed)
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

    // JUnit Rule to create temporary files directory for mock view model testing
    @get:Rule
    val tempFolder = TemporaryFolder()

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
        val viewModel = MapViewModel(tempFolder.newFolder("files"), FakeLocationProvider(), FakePinRepository(), FakeSocketManager(), FakeAuthRepository())

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
        val viewModel = MapViewModel(tempFolder.newFolder("files"), FakeLocationProvider(), FakePinRepository(), FakeSocketManager(), FakeAuthRepository())
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
        val viewModel = MapViewModel(tempFolder.newFolder("files"), FakeLocationProvider(), FakePinRepository(), FakeSocketManager(), FakeAuthRepository())
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
            type = HazardType.DANGEROUS_TRAFFIC,
            base64Image = null
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
        
        val viewModel = MapViewModel(tempFolder.newFolder("files"), FakeLocationProvider(), FakePinRepository(), customSocketManager, FakeAuthRepository())
        this.testScheduler.advanceUntilIdle()
        
        val initialState = viewModel.uiState.value as MapUiState.Success
        assertEquals(2, initialState.pins.size)
        
        // Act: Emit the pin from the WebSocket stream
        liveFlow.emit(livePin)
        this.testScheduler.advanceUntilIdle()
        
        // Assert: Verify the state has successfully updated to include the broadcasted pin
        val updatedState = viewModel.uiState.value as MapUiState.Success
        assertEquals(3, updatedState.pins.size)

        val addedPin = updatedState.pins.last()
        assertEquals("socket-pin-999", addedPin.id)
        assertEquals("Live Road Hazard", addedPin.title)
        assertEquals(HazardType.OTHER, addedPin.type)
    }

    @Test
    fun testPlanningRouteRetrievesRoutesAndAppliesCriteriaFilters() = kotlinx.coroutines.test.runTest(testDispatcher) {
        val viewModel = MapViewModel(tempFolder.newFolder("files"), FakeLocationProvider(), FakePinRepository(), FakeSocketManager(), FakeAuthRepository())
        this.testScheduler.advanceUntilIdle()

        // Verify initial state is empty
        assertEquals(null, viewModel.startPoint.value)
        assertEquals(null, viewModel.endPoint.value)
        assertTrue(viewModel.plannedRoutes.value.isEmpty())

        // Act: Set Start and End points to trigger routing search
        viewModel.setStartPoint(LatLng(1.0, 1.0))
        viewModel.setEndPoint(LatLng(2.0, 2.0))
        this.testScheduler.advanceUntilIdle()

        // Assert: Verify routes are retrieved successfully
        val routes = viewModel.plannedRoutes.value
        assertEquals(2, routes.size)

        // Road route has 0 hazards, gravel has 1
        val road = routes.find { it.surfaceType == com.njiasalama.domain.model.SurfaceType.ROAD }!!
        val gravel = routes.find { it.surfaceType == com.njiasalama.domain.model.SurfaceType.GRAVEL }!!
        assertEquals(0, road.dangerPins.size)
        assertEquals(1, gravel.dangerPins.size)

        // Default selection should recommend the first route (road)
        assertEquals(road.id, viewModel.selectedRoute.value?.id)
        assertEquals(com.njiasalama.domain.model.RouteSafetyStatus.SAFE, viewModel.routeSafetyStatus.value)

        // Act: Filter by surface criteria (GRAVEL)
        viewModel.setSurfaceCriteria(com.njiasalama.domain.model.SurfaceType.GRAVEL)
        this.testScheduler.advanceUntilIdle()

        // Assert: Selected route updates to gravel, status updates to CAUTION
        assertEquals(gravel.id, viewModel.selectedRoute.value?.id)
        assertEquals(com.njiasalama.domain.model.RouteSafetyStatus.CAUTION, viewModel.routeSafetyStatus.value)

        // Act: Clear criteria, set safetyCriteria to true (prefer safest)
        viewModel.setSurfaceCriteria(null)
        viewModel.setSafetyCriteria(true)
        this.testScheduler.advanceUntilIdle()

        // Assert: Recommends road route because it has 0 pins (safer than gravel with 1 pin)
        assertEquals(road.id, viewModel.selectedRoute.value?.id)
        assertEquals(com.njiasalama.domain.model.RouteSafetyStatus.SAFE, viewModel.routeSafetyStatus.value)

        // Act: Clear route planning
        viewModel.clearRoute()
        assertEquals(null, viewModel.startPoint.value)
        assertEquals(null, viewModel.endPoint.value)
        assertTrue(viewModel.plannedRoutes.value.isEmpty())
        assertEquals(null, viewModel.selectedRoute.value)
        assertEquals(null, viewModel.routeSafetyStatus.value)
    }

    @Test
    fun testWaypointEditingLogic() = kotlinx.coroutines.test.runTest(testDispatcher) {
        val viewModel = MapViewModel(tempFolder.newFolder("files"), FakeLocationProvider(), FakePinRepository(), FakeSocketManager(), FakeAuthRepository())
        this.testScheduler.advanceUntilIdle()

        viewModel.setStartPoint(LatLng(1.0, 1.0))
        viewModel.setEndPoint(LatLng(2.0, 2.0))
        this.testScheduler.advanceUntilIdle()

        assertTrue("Waypoints should start empty", viewModel.waypoints.value.isEmpty())

        // Act: Add a waypoint
        val waypoint = LatLng(1.5, 1.5)
        viewModel.addWaypoint(waypoint)
        this.testScheduler.advanceUntilIdle()

        // Assert: Waypoint is added
        assertEquals(1, viewModel.waypoints.value.size)
        assertEquals(waypoint, viewModel.waypoints.value[0])

        // Act: Reorder/Update/Remove waypoint
        val waypoint2 = LatLng(1.8, 1.8)
        viewModel.addWaypoint(waypoint2)
        this.testScheduler.advanceUntilIdle()
        assertEquals(2, viewModel.waypoints.value.size)

        viewModel.reorderWaypoints(0, 1)
        assertEquals(waypoint2, viewModel.waypoints.value[0])
        assertEquals(waypoint, viewModel.waypoints.value[1])

        viewModel.removeWaypoint(0)
        assertEquals(1, viewModel.waypoints.value.size)
        assertEquals(waypoint, viewModel.waypoints.value[0])

        viewModel.clearWaypoints()
        assertTrue(viewModel.waypoints.value.isEmpty())
    }

    @Test
    fun testSavedRoutesSyncActions() = kotlinx.coroutines.test.runTest(testDispatcher) {
        val fakeRepo = FakePinRepository()
        val viewModel = MapViewModel(tempFolder.newFolder("files"), FakeLocationProvider(), fakeRepo, FakeSocketManager(), FakeAuthRepository())
        this.testScheduler.advanceUntilIdle()

        // Set start/end and planned route
        viewModel.setStartPoint(LatLng(1.0, 1.0))
        viewModel.setEndPoint(LatLng(2.0, 2.0))
        this.testScheduler.advanceUntilIdle()

        // Act: Save active route
        var isSuccess = false
        viewModel.saveActiveRoute("My Custom Route", onSuccess = { isSuccess = true })
        this.testScheduler.advanceUntilIdle()

        assertTrue("Route save should trigger success callback", isSuccess)
        assertEquals(1, viewModel.savedRoutes.value.size)
        assertEquals("My Custom Route", viewModel.savedRoutes.value[0].name)

        // Act: Load the saved route on map
        val saved = viewModel.savedRoutes.value[0]
        viewModel.clearRoute()
        assertEquals(null, viewModel.selectedRoute.value)

        viewModel.loadSavedRouteOnMap(saved)
        assertEquals("My Custom Route", viewModel.selectedRoute.value?.name)
        assertEquals(LatLng(1.0, 1.0), viewModel.startPoint.value)
        assertEquals(LatLng(2.0, 2.0), viewModel.endPoint.value)

        // Act: Delete route
        viewModel.deleteSavedRoute(saved.id)
        this.testScheduler.advanceUntilIdle()
        assertTrue("Saved routes should be empty after deletion", viewModel.savedRoutes.value.isEmpty())
    }
}
