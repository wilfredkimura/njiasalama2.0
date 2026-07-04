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
    private val locationProvider: LocationProvider,
    private val pinRepository: PinRepository,
    private val socketManager: SocketManager,
    private val authRepository: AuthRepository
) : ViewModel() {

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

    // Constructor block executed automatically as soon as the ViewModel is initialized
    init {
        loadPins()
        listenToWebSocketUpdates()
    }

    /**
     * Fetches all road hazard pins from the database using our repository.
     * Updates uiState Flow to Success(pins) on success, or Error on connection failure.
     */
    fun loadPins() {
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            pinRepository.getPins()
                .onSuccess { pinsList ->
                    _uiState.value = MapUiState.Success(pinsList)
                }
                .onFailure { exception ->
                    _uiState.value = MapUiState.Error(
                        exception.message ?: "Failed to connect to the backend server"
                    )
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
     * If the API call fails (e.g. offline), we fall back to displaying the pin locally.
     */
    fun addDangerPinLocally(
        title: String,
        description: String,
        latitude: Double,
        longitude: Double,
        type: HazardType
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
                reportedBy = currentUserName
            ).onSuccess { newPin ->
                val currentState = _uiState.value
                if (currentState is MapUiState.Success) {
                    // Update state flow with the returned persisted pin details
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
                        reportedBy = "$currentUserName (Offline)"
                    )
                    _uiState.value = MapUiState.Success(currentState.pins + localFallbackPin)
                }
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