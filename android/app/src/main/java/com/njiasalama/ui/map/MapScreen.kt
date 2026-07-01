package com.njiasalama.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.njiasalama.data.LocationService

/**
 * The main UI layout for the application map.
 * In Jetpack Compose, UI elements are created using Composable functions annotated with @Composable.
 * This composable reacts automatically whenever the MapUiState transitions.
 */
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    // Retrieve the LocalContext of the screen layout
    context: android.content.Context = LocalContext.current,
    // Automatically instantiates the MapViewModel passing the custom factory
    viewModel: MapViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                // Initialize MapViewModel with a concrete LocationService wrapper
                MapViewModel(LocationService(context.applicationContext))
            }
        }
    )
) {
    // collectAsStateWithLifecycle extracts data updates from the ViewModel's state flow (pipeline)
    // while checking the lifecycle of the phone screen. If the user minimizes the app,
    // this flow automatically pauses to save battery power.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Observe the cyclist's current GPS location updates from the ViewModel
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()

    // Track whether the user has granted location permission
    var permissionGranted by remember { mutableStateOf(false) }

    // Track whether we have already centered the camera on the user's location initially
    var hasCenteredCamera by remember { mutableStateOf(false) }

    // Temporary state to hold coordinates of a map long-press gesture before showing the add pin dialog
    var selectedLatLngForNewPin by remember { mutableStateOf<LatLng?>(null) }

    // Sets up the map camera position centered over Nairobi coordinates by default
    val defaultLocation = LatLng(-1.2921, 36.8219)
    
    // rememberCameraPositionState creates a camera state that persists across screen refreshes
    val cameraPositionState = rememberCameraPositionState {
        // Position camera to default latitude and longitude at a city-level zoom (14f)
        position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
    }

    // Activity launcher used to request fine and coarse location access permissions dynamically.
    // It captures user consent selections and starts location tracking if accepted.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        permissionGranted = fineLocationGranted || coarseLocationGranted
        if (permissionGranted) {
            // Permission granted! Start location updates streaming in the ViewModel
            viewModel.startLocationUpdates()
        }
    }

    // Check permission state on startup. LaunchedEffect(Unit) runs exactly once when this screen boots.
    LaunchedEffect(Unit) {
        // Initialize the Google Maps SDK immediately. This loads internal maps resources
        // and guarantees CameraUpdateFactory is ready, preventing NullPointerExceptions on startup.
        com.google.android.gms.maps.MapsInitializer.initialize(context)

        val fineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationPermission || coarseLocationPermission) {
            // If already granted, toggle permission state and activate location tracking
            permissionGranted = true
            viewModel.startLocationUpdates()
        } else {
            // If not granted, trigger the Android OS permissions request dialog box
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Automatically center and animate the camera on the user's location when first retrieved.
    // We include uiState as a trigger key to guarantee that if the location is retrieved while
    // the map is still in its loading state, it will try to animate again once the map success loads.
    LaunchedEffect(userLocation, uiState) {
        if (uiState is MapUiState.Success) {
            userLocation?.let { location ->
                if (!hasCenteredCamera) {
                    // animate updates the map viewpoint smoothly with a camera shift zoom (15f)
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(location, 15f)
                    )
                    // Set flag to true so user manual pans are not overridden by subsequent updates
                    hasCenteredCamera = true
                }
            }
        }
    }

    // Configure properties of the map. 
    // Setting isMyLocationEnabled to true draws the standard blue dot and adds the 'My Location' button.
    val mapProperties = remember(permissionGranted) {
        MapProperties(isMyLocationEnabled = permissionGranted)
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
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    // Intercept long press gestures on the map layout to capture coordinate positions
                    onMapLongClick = { latLng ->
                        selectedLatLngForNewPin = latLng
                    }
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

        // Overlay dialog form when cyclist has long-pressed a coordinates point on the map.
        // It provides input fields to submit hazard details and updates the local UI list immediately.
        selectedLatLngForNewPin?.let { latLng ->
            AddPinDialog(
                latLng = latLng,
                onDismiss = { selectedLatLngForNewPin = null },
                onSubmit = { title, description, type ->
                    viewModel.addDangerPinLocally(
                        title = title,
                        description = description,
                        latitude = latLng.latitude,
                        longitude = latLng.longitude,
                        type = type
                    )
                    selectedLatLngForNewPin = null
                }
            )
        }
    }
}
