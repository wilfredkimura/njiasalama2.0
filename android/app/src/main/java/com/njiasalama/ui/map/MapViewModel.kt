package com.njiasalama.ui.map

import androidx.lifecycle.ViewModel // Importing the standard ViewModel class from lifecycle.
import androidx.lifecycle.viewModelScope // Importing the viewModelScope property to launch coroutines that are tied to the ViewModel's lifecycle.
import com.google.android.gms.maps.model.LatLng // Importing LatLng class to represent latitude/longitude points.
import com.njiasalama.data.LocationProvider // Importing the LocationProvider interface instead of the concrete implementation.
import com.njiasalama.domain.model.DangerPin// Importing the DangerPin data class from the domain model package to represent road hazard pins on the map.
import com.njiasalama.domain.model.HazardType// Importing the HazardType enum class from the domain model package to categorize road hazards.
import kotlinx.coroutines.flow.MutableStateFlow // Importing the MutableStateFlow class from coroutines to create a read-write state flow.
import kotlinx.coroutines.flow.StateFlow // Importing the StateFlow class to represent read-only streams.
import kotlinx.coroutines.flow.asStateFlow // Importing the asStateFlow function to expose a read-only state flow.
import kotlinx.coroutines.launch // Importing the launch function to launch coroutines.

/**
 * Architectural state management layer. It interacts with data sources
 * and exposes clean, lifecycle-aware data flows straight to the UI.
 * We inject the LocationProvider interface to support clean unit testing.
 */
class MapViewModel(private val locationProvider: LocationProvider) : ViewModel() { // Class declaration for the MapViewModel, which is a subclass of ViewModel.

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
        loadMockPins()
    }

    /**
     * Simulates background asynchronous data loading.
     * Uses viewModelScope to ensure that if the user closes the app, 
     * ongoing computational operations are cancelled instantly, avoiding memory leaks.
     */

    //Simulates background asynchronous data loading. Uses viewModelScope to ensure that if the user closes the app,
    // ongoing computational operations are cancelled instantly, avoiding memory leaks.
    private fun loadMockPins() {
        viewModelScope.launch {
            // Setting up immediate local mock data points centered near Nairobi coordinates 
            // for early execution testing before NestJS hooks are operational
            val mockList = listOf(
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
            
            // Updating the flow value. Triggering this shifts our UI layer state completely
            // from 'Loading' into 'Success', passing the mock array instantly.
            _uiState.value = MapUiState.Success(mockList)
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
     * Adds a new DangerPin locally to the UI state.
     * This allows immediate visualization of reported hazards on the map before backend persistence is fully wired.
     */
    fun addDangerPinLocally(
        title: String,
        description: String,
        latitude: Double,
        longitude: Double,
        type: HazardType
    ) {
        val currentState = _uiState.value
        if (currentState is MapUiState.Success) {
            val newPin = DangerPin(
                id = java.util.UUID.randomUUID().toString(), // Generates a unique system ID for tracking
                title = title,
                description = description,
                latitude = latitude,
                longitude = longitude,
                type = type,
                reportedBy = "CurrentUser" // Placeholder user ID until Google Login is integrated
            )
            // Emit the updated list to trigger screen refreshes instantly
            _uiState.value = MapUiState.Success(currentState.pins + newPin)
        }
    }
}