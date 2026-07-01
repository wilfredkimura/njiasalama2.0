package com.njiasalama.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * The main UI layout for the application map.
 * In Jetpack Compose, UI elements are created using Composable functions annotated with @Composable.
 * This composable reacts automatically whenever the MapUiState transitions.
 */
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    // Automatically instantiates and registers the lifecycle-aware MapViewModel
    viewModel: MapViewModel = viewModel()
) {
    // collectAsStateWithLifecycle extracts data updates from the ViewModel's state flow (pipeline)
    // while checking the lifecycle of the phone screen. If the user minimizes the app,
    // this flow automatically pauses to save battery power.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Sets up the map camera position centered over Nairobi coordinates by default
    val defaultLocation = LatLng(-1.2921, 36.8219)
    
    // rememberCameraPositionState creates a camera state that persists across screen refreshes
    val cameraPositionState = rememberCameraPositionState {
        // Position camera to default latitude and longitude at a city-level zoom (14f)
        position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
    }

    // A full-screen Box container. Box lets us overlay items on top of each other.
    Box(modifier = modifier.fillMaxSize()) {
        // Using 'when' in Kotlin behaves like a switch statement, but is type-safe.
        // It checks the current state of our MapUiState.
        when (val state = uiState) {
            // State 1: UI is fetching data. Display a circular loading spinner at the center.
            is MapUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            
            // State 2: Fetch failed. Show the error message at the center.
            is MapUiState.Error -> {
                Text(
                    text = state.message,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // State 3: Data is ready. Render the Google Map.
            is MapUiState.Success -> {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    // Loop through every DangerPin in the list and draw a marker on the map
                    state.pins.forEach { pin ->
                        Marker(
                            state = MarkerState(position = LatLng(pin.latitude, pin.longitude)),
                            title = "${pin.type.name}: ${pin.title}", // e.g., "POTHOLE: Deep Pothole"
                            snippet = pin.description                  // Subtitle showing description
                        )
                    }
                }
            }
        }
    }
}
