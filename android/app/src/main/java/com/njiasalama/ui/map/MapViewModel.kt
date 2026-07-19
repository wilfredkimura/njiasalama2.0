package com.njiasalama.ui.map

import androidx.lifecycle.ViewModel // Importing the standard ViewModel class from lifecycle.
import androidx.lifecycle.viewModelScope // Importing the viewModelScope property to launch coroutines that are tied to the ViewModel's lifecycle.
import com.google.android.gms.maps.model.LatLng // Importing LatLng class to represent latitude/longitude points.
import com.njiasalama.data.LocationProvider // Importing the LocationProvider interface instead of the concrete implementation.
import com.njiasalama.data.websocket.SocketManager // Importing SocketManager to listen to real-time WebSockets.
import com.njiasalama.domain.model.DangerPin// Importing the DangerPin data class from the domain model package to represent road hazard pins on the map.
import com.njiasalama.domain.model.HazardType// Importing the HazardType enum class from the domain model package to categorize road hazards.
import com.njiasalama.domain.repository.PinRepository // Importing PinRepository to query database endpoints.
import com.njiasalama.domain.repository.AuthRepository // Importing AuthRepository to manage session details.
import kotlinx.coroutines.flow.MutableStateFlow // Importing the MutableStateFlow class from coroutines to create a read-write state flow.
import kotlinx.coroutines.flow.StateFlow // Importing the StateFlow class to represent read-only streams.
import kotlinx.coroutines.flow.asStateFlow // Importing the asStateFlow function to expose a read-only state flow.
import kotlinx.coroutines.launch // Importing the launch function to launch coroutines.

/**
 * Architectural state management layer. It interacts with data sources
 * and exposes clean, lifecycle-aware data flows straight to the UI.
 * We inject the LocationProvider, PinRepository, and SocketManager interfaces to support clean unit testing.
 */
class MapViewModel(
    private val filesDir: java.io.File,
    private val locationProvider: LocationProvider,
    private val pinRepository: PinRepository,
    private val socketManager: SocketManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val gson = com.google.gson.Gson()
    private val offlineFileName = "offline_pins.json"

    // Reads queued offline-reported hazards from local storage file
    private fun getOfflinePins(): List<DangerPin> {
        return try {
            val file = java.io.File(filesDir, offlineFileName)
            if (!file.exists()) return emptyList()
            val json = file.readText()
            val type = object : com.google.gson.reflect.TypeToken<List<DangerPin>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Appends an offline hazard pin to the local file storage queue
    private fun saveOfflinePin(pin: DangerPin) {
        try {
            val file = java.io.File(filesDir, offlineFileName)
            val currentList = getOfflinePins()
            val updatedList = currentList + pin
            val json = gson.toJson(updatedList)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Expose the logged-in cyclist's name for displaying on the Profile Icon overlay
    val currentUserName: String
        get() = authRepository.getUserName() ?: "User"

    /**
     * Triggers logout flow, which updates SharedPreferences and redirects the user to the AuthScreen
     */
    fun logout() {
        authRepository.logout()
    }

    // Private read-write flow tracking the cyclist's current GPS location. Starts as null.
    private val _userLocation = MutableStateFlow<LatLng?>(null)

    // Public read-only flow that Compose observers can listen to for camera-centering
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()

    /**
     * An internal, read-write state container utilizing StateFlow.
     * Defaults strictly to 'Loading' as the structural starting point.
     * Kept private to protect state corruption directly from UI components.
     */
    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)

    /**
     * A public, read-only state pipeline exposed directly to Compose observers.
     * Using asStateFlow() transforms mutable layers securely to eliminate direct manipulation.
     */
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    // --- Start of Routing State Properties ---

    private val _startPoint = MutableStateFlow<LatLng?>(null)
    val startPoint: StateFlow<LatLng?> = _startPoint.asStateFlow()

    private val _endPoint = MutableStateFlow<LatLng?>(null)
    val endPoint: StateFlow<LatLng?> = _endPoint.asStateFlow()

    private val _plannedRoutes = MutableStateFlow<List<com.njiasalama.domain.model.Route>>(emptyList())
    val plannedRoutes: StateFlow<List<com.njiasalama.domain.model.Route>> = _plannedRoutes.asStateFlow()

    private val _selectedRoute = MutableStateFlow<com.njiasalama.domain.model.Route?>(null)
    val selectedRoute: StateFlow<com.njiasalama.domain.model.Route?> = _selectedRoute.asStateFlow()

    private val _surfaceCriteria = MutableStateFlow<com.njiasalama.domain.model.SurfaceType?>(null)
    val surfaceCriteria: StateFlow<com.njiasalama.domain.model.SurfaceType?> = _surfaceCriteria.asStateFlow()

    private val _safetyCriteria = MutableStateFlow<Boolean>(false)
    val safetyCriteria: StateFlow<Boolean> = _safetyCriteria.asStateFlow()

    private val _routeSafetyStatus = MutableStateFlow<com.njiasalama.domain.model.RouteSafetyStatus?>(null)
    val routeSafetyStatus: StateFlow<com.njiasalama.domain.model.RouteSafetyStatus?> = _routeSafetyStatus.asStateFlow()

    private val _waypoints = MutableStateFlow<List<LatLng>>(emptyList())
    val waypoints: StateFlow<List<LatLng>> = _waypoints.asStateFlow()

    private val _savedRoutes = MutableStateFlow<List<com.njiasalama.domain.model.SavedRoute>>(emptyList())
    val savedRoutes: StateFlow<List<com.njiasalama.domain.model.SavedRoute>> = _savedRoutes.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<com.njiasalama.domain.model.GeocodeLocation>>(emptyList())
    val searchSuggestions: StateFlow<List<com.njiasalama.domain.model.GeocodeLocation>> = _searchSuggestions.asStateFlow()

    // --- End of Routing State Properties ---

    // Constructor block executed automatically as soon as the ViewModel is initialized
    init {
        loadPins()
        listenToWebSocketUpdates()
    }

    /**
     * Fetches all road hazard pins from the database using our repository.
     * Updates uiState Flow to Success(pins) on success, or Error on connection failure.
     * Combines retrieved pins with offline queued pins.
     */
    fun loadPins() {
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            pinRepository.getPins()
                .onSuccess { pinsList ->
                    val offlinePins = getOfflinePins()
                    _uiState.value = MapUiState.Success(pinsList + offlinePins)
                }
                .onFailure { exception ->
                    val offlinePins = getOfflinePins()
                    if (offlinePins.isNotEmpty()) {
                        _uiState.value = MapUiState.Success(offlinePins)
                    } else {
                        _uiState.value = MapUiState.Error(
                            exception.message ?: "Failed to connect to the backend server"
                        )
                    }
                }
        }
    }

    /**
     * Subscribes to the live Socket.io channel via SocketManager.
     * Appends broadcasted hazard updates directly to the Success UI state map list.
     */
    private fun listenToWebSocketUpdates() {
        viewModelScope.launch {
            // Connect to server
            socketManager.connect()
            
            // Collect live events emitted by SocketManager Flow
            socketManager.getNewPinFlow().collect { newPin ->
                val currentState = _uiState.value
                if (currentState is MapUiState.Success) {
                    // Prevent duplicate insertions if the pin was already created locally or retrieved via HTTP REST
                    val pinAlreadyExists = currentState.pins.any { it.id == newPin.id }
                    if (!pinAlreadyExists) {
                        _uiState.value = MapUiState.Success(currentState.pins + newPin)
                    }
                }
            }
        }
    }

    /**
     * Starts requesting continuous GPS location updates from the LocationProvider.
     * The updates are collected asynchronously inside the viewModelScope and posted to _userLocation.
     */
    fun startLocationUpdates() {
        viewModelScope.launch {
            // Collect is a terminal flow operator that listens to all values emitted by the location flow
            locationProvider.getLocationUpdates().collect { location ->
                _userLocation.value = location
            }
        }
    }

    /**
     * Submits a new road hazard pin to the server repository.
     * Compress photo as Base64. If API call fails, fall back to offline storage queue.
     */
    fun addDangerPinLocally(
        title: String,
        description: String,
        latitude: Double,
        longitude: Double,
        type: HazardType,
        base64Image: String?
    ) {
        viewModelScope.launch {
            val token = authRepository.getToken() ?: ""
            pinRepository.reportPin(
                token = token,
                title = title,
                description = description,
                latitude = latitude,
                longitude = longitude,
                type = type,
                reportedBy = currentUserName,
                imageUrl = base64Image
            ).onSuccess { newPin ->
                val currentState = _uiState.value
                if (currentState is MapUiState.Success) {
                    _uiState.value = MapUiState.Success(currentState.pins + newPin)
                }
            }.onFailure {
                // Offline fallback mechanism: Place pin locally so cyclist gets instant visual confirmation
                val currentState = _uiState.value
                if (currentState is MapUiState.Success) {
                    val localFallbackPin = DangerPin(
                        id = java.util.UUID.randomUUID().toString(),
                        title = title,
                        description = description,
                        latitude = latitude,
                        longitude = longitude,
                        type = type,
                        reportedBy = "$currentUserName (Offline)",
                        imageUrl = base64Image
                    )
                    saveOfflinePin(localFallbackPin)
                    _uiState.value = MapUiState.Success(currentState.pins + localFallbackPin)
                }
            }
        }
    }

    fun setStartPoint(latLng: LatLng?) {
        _startPoint.value = latLng
        triggerRouteSearch()
    }

    fun setEndPoint(latLng: LatLng?) {
        _endPoint.value = latLng
        triggerRouteSearch()
    }

    fun setSurfaceCriteria(type: com.njiasalama.domain.model.SurfaceType?) {
        _surfaceCriteria.value = type
        updateSelectedRoute()
    }

    fun setSafetyCriteria(enabled: Boolean) {
        _safetyCriteria.value = enabled
        updateSelectedRoute()
    }

    fun selectRoute(route: com.njiasalama.domain.model.Route) {
        _selectedRoute.value = route
        _routeSafetyStatus.value = when {
            route.dangerPins.isEmpty() -> com.njiasalama.domain.model.RouteSafetyStatus.SAFE
            route.dangerPins.size <= 3 -> com.njiasalama.domain.model.RouteSafetyStatus.CAUTION
            else -> com.njiasalama.domain.model.RouteSafetyStatus.DANGEROUS
        }
    }

    fun clearRoute() {
        _startPoint.value = null
        _endPoint.value = null
        _waypoints.value = emptyList()
        _plannedRoutes.value = emptyList()
        _selectedRoute.value = null
        _surfaceCriteria.value = null
        _safetyCriteria.value = false
        _routeSafetyStatus.value = null
    }

    fun searchLocations(query: String) {
        if (query.trim().isEmpty()) {
            _searchSuggestions.value = emptyList()
            return
        }
        viewModelScope.launch {
            pinRepository.geocode(query)
                .onSuccess { suggestions ->
                    _searchSuggestions.value = suggestions
                }
                .onFailure {
                    _searchSuggestions.value = emptyList()
                }
        }
    }

    fun clearSuggestions() {
        _searchSuggestions.value = emptyList()
    }

    private fun triggerRouteSearch() {
        val start = _startPoint.value
        val end = _endPoint.value
        if (start != null && end != null) {
            viewModelScope.launch {
                pinRepository.getRoutes(
                    startLat = start.latitude,
                    startLng = start.longitude,
                    endLat = end.latitude,
                    endLng = end.longitude
                ).onSuccess { routes ->
                    _plannedRoutes.value = routes
                    updateSelectedRoute()
                }.onFailure {
                    _plannedRoutes.value = emptyList()
                    _selectedRoute.value = null
                    _routeSafetyStatus.value = null
                }
            }
        } else {
            _plannedRoutes.value = emptyList()
            _selectedRoute.value = null
            _routeSafetyStatus.value = null
        }
    }

    private fun getRecommendedRoute(): com.njiasalama.domain.model.Route? {
        val routes = _plannedRoutes.value
        if (routes.isEmpty()) return null

        val surfacePref = _surfaceCriteria.value
        val preferSafest = _safetyCriteria.value

        val filteredRoutes = if (surfacePref != null) {
            routes.filter { it.surfaceType == surfacePref }
        } else {
            routes
        }

        val candidates = if (filteredRoutes.isEmpty()) routes else filteredRoutes

        return if (preferSafest) {
            candidates.minByOrNull { it.dangerPins.size }
        } else {
            candidates.firstOrNull()
        }
    }

    private fun updateSelectedRoute() {
        val recommended = getRecommendedRoute()
        _selectedRoute.value = recommended
        _routeSafetyStatus.value = recommended?.let { route ->
            when {
                route.dangerPins.isEmpty() -> com.njiasalama.domain.model.RouteSafetyStatus.SAFE
                route.dangerPins.size <= 3 -> com.njiasalama.domain.model.RouteSafetyStatus.CAUTION
                else -> com.njiasalama.domain.model.RouteSafetyStatus.DANGEROUS
            }
        }
    }

    /**
     * Lifecycle method invoked automatically when the ViewModel is destroyed.
     * We close our active WebSockets connection here to release memory and network connections.
     */
    override fun onCleared() {
        super.onCleared()
        socketManager.disconnect()
    }
}