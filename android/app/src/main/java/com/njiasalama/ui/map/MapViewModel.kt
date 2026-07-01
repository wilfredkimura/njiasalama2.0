package com.njiasalama.ui.map

import androidx.lifecycle.ViewModel // Importing the ViewModel class from the AndroidX lifecycle library to create a ViewModel for the Map screen.
import androidx.lifecycle.viewModelScope // Importing the viewModelScope property to launch coroutines that are tied to the ViewModel's lifecycle.
import com.njiasalama.domain.model.DangerPin// Importing the DangerPin data class from the domain model package to represent road hazard pins on the map.
import com.njiasalama.domain.model.HazardType// Importing the HazardType enum class from the domain model package to categorize road hazards.
import kotlinx.flow.MutableStateFlow // Importing the MutableStateFlow class to create a mutable state flow for the Map screen.
import kotlinx.flow.StateFlow // Importing the StateFlow class to create a state flow for the Map screen.
import kotlinx.flow.asStateFlow // Importing the asStateFlow function to convert a mutable state flow to a read-only state flow.
import kotlinx.coroutines.launch // Importing the launch function to launch coroutines.

/**
 * Architectural state management layer. It interacts with data sources
 * and exposes clean, lifecycle-aware data flows straight to the UI.
 */
class MapViewModel : ViewModel() { // Class declaration for the MapViewModel, which is a subclass of ViewModel.

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
}