package com.njiasalama.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Close
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.njiasalama.data.LocationService
import com.njiasalama.data.RetrofitClient // Import our networking client singleton container
import com.njiasalama.domain.model.DangerPin
import coil.compose.AsyncImage

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
                val appContext = context.applicationContext
                // Initialize MapViewModel with filesDir, LocationService, Retrofit PinRepository, SocketManager, and AuthRepository
                MapViewModel(
                    filesDir = appContext.filesDir,
                    locationProvider = LocationService(appContext),
                    pinRepository = RetrofitClient.getPinRepository(appContext),
                    socketManager = RetrofitClient.socketManager,
                    authRepository = RetrofitClient.getAuthRepository(appContext)
                )
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
    var selectedLatLngForNewPin by rememberSaveable { mutableStateOf<LatLng?>(null) }

    // State to hold currently selected pin for showing detailed bottom card
    var selectedPin by remember { mutableStateOf<DangerPin?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var radiusKm by remember { mutableStateOf(5.0f) }
    var panelExpanded by remember { mutableStateOf(false) }

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
    // We disable the default location layer to draw our own customizable azure location pin instead.
    val mapProperties = remember {
        MapProperties(isMyLocationEnabled = false)
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
                // Reactive Zoom-based pin clustering
                val currentZoom = cameraPositionState.position.zoom
                val clusters = remember(state.pins, currentZoom) {
                    clusterPins(state.pins, currentZoom)
                }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    // Intercept long press gestures on the map layout to capture coordinate positions
                    onMapLongClick = { latLng ->
                        selectedLatLngForNewPin = latLng
                    }
                ) {
                    // Render custom Azure cyclist location marker
                    userLocation?.let { cyclistLoc ->
                        Marker(
                            state = remember(cyclistLoc) { MarkerState(position = cyclistLoc) },
                            title = "My Current Location",
                            snippet = "Click to zoom 2x2 km viewport",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            onClick = {
                                val latDelta = 0.009
                                val lngDelta = 0.009
                                val bounds = LatLngBounds(
                                    LatLng(cyclistLoc.latitude - latDelta, cyclistLoc.longitude - lngDelta),
                                    LatLng(cyclistLoc.latitude + latDelta, cyclistLoc.longitude + lngDelta)
                                )
                                coroutineScope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 0))
                                }
                                true
                            }
                        )
                    }

                    // Render clustered markers or individual pins
                    clusters.forEach { cluster ->
                        if (cluster.pins.size == 1) {
                            val pin = cluster.pins.first()
                            Marker(
                                state = remember(pin.id) { MarkerState(position = LatLng(pin.latitude, pin.longitude)) },
                                title = "${pin.type.name}: ${pin.title}", // e.g., "POTHOLE: Deep Pothole"
                                snippet = pin.description,                 // Subtitle showing description
                                onClick = { marker ->
                                    selectedPin = pin
                                    marker.showInfoWindow()
                                    val latDelta = 0.009
                                    val lngDelta = 0.009
                                    val bounds = LatLngBounds(
                                        LatLng(pin.latitude - latDelta, pin.longitude - lngDelta),
                                        LatLng(pin.latitude + latDelta, pin.longitude + lngDelta)
                                    )
                                    coroutineScope.launch {
                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 0))
                                    }
                                    true
                                }
                            )
                        } else {
                            Marker(
                                state = remember(cluster.id) { MarkerState(position = cluster.center) },
                                title = "${cluster.pins.size} Hazards Clustered",
                                snippet = "Zoom in or click to expand",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                                onClick = { marker ->
                                    marker.showInfoWindow()
                                    coroutineScope.launch {
                                        val newZoom = cameraPositionState.position.zoom + 2f
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(cluster.center, newZoom)
                                        )
                                    }
                                    true
                                }
                            )
                        }
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
                onSubmit = { title, description, type, imageUri ->
                    coroutineScope.launch {
                        val base64Image = imageUri?.let { uri ->
                            com.njiasalama.data.ImageUtils.compressUriToBase64(context, uri)
                        }
                        viewModel.addDangerPinLocally(
                            title = title,
                            description = description,
                            latitude = latLng.latitude,
                            longitude = latLng.longitude,
                            type = type,
                            base64Image = base64Image
                        )
                    }
                    selectedLatLngForNewPin = null
                }
            )
        }

        // Top Left Header Title & Expandable Panel
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .width(280.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // App Name Title Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "NjiaSalama",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expandable Nearby Hazards Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header Row (Clickable to toggle expansion)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { panelExpanded = !panelExpanded }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nearby Hazards (${String.format("%.1f", radiusKm)} km)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (panelExpanded) "▲" else "▼",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (panelExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Slider to adjust radius
                        Text(
                            text = "Adjust Radius: ${String.format("%.1f", radiusKm)} km",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Slider(
                            value = radiusKm,
                            onValueChange = { radiusKm = it },
                            valueRange = 1f..20f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Filter hazards
                        if (uiState is MapUiState.Success) {
                            val successState = uiState as MapUiState.Success
                            val nearby = userLocation?.let { loc ->
                                successState.pins.map { pin ->
                                    val dist = calculateDistance(loc.latitude, loc.longitude, pin.latitude, pin.longitude)
                                    pin to dist
                                }.filter { it.second <= radiusKm }
                                 .sortedBy { it.second }
                            } ?: emptyList()

                            if (nearby.isEmpty()) {
                                Text(
                                    text = if (userLocation == null) "Retrieving location..." else "No hazards nearby",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                ) {
                                    items(nearby) { (pin, dist) ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedPin = pin // Also select pin when list item clicked
                                                    // Zoom to hazard viewport
                                                    val latDelta = 0.009
                                                    val lngDelta = 0.009
                                                    val bounds = LatLngBounds(
                                                        LatLng(pin.latitude - latDelta, pin.longitude - lngDelta),
                                                        LatLng(pin.latitude + latDelta, pin.longitude + lngDelta)
                                                    )
                                                    coroutineScope.launch {
                                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 0))
                                                    }
                                                }
                                                .padding(vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "${pin.type.name}: ${pin.title}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = String.format("%.2f km away", dist),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Right Centering Floating Action Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    userLocation?.let { location ->
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngZoom(location, 15f)
                            )
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Center on current location",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Profile and Logout Overlay Menu
        var showProfileMenu by remember { mutableStateOf(false) }
        val cyclistName = viewModel.currentUserName

        // Top right floating profile action button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { showProfileMenu = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "User Profile",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Profile details / Logout popup dialog
        if (showProfileMenu) {
            AlertDialog(
                onDismissRequest = { showProfileMenu = false },
                title = { Text("Cyclist Profile") },
                text = {
                    Column {
                        Text("Logged in as:", fontWeight = FontWeight.SemiBold)
                        Text(cyclistName, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showProfileMenu = false
                            viewModel.logout()
                        }
                    ) {
                        Text("Sign Out", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showProfileMenu = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Details panel for selected road hazard pin (MD3 compliant)
        selectedPin?.let { pin ->
            ElevatedCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 80.dp) // Adjusted offset to not overlap with FAB or maps UI
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pin.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pin.type.name.replace("_", " "),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = { selectedPin = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close details"
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = pin.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reported by: ${pin.reportedBy}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    pin.imageUrl?.let { imgUrl ->
                        if (imgUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            AsyncImage(
                                model = getCoilModel(imgUrl),
                                contentDescription = "Hazard Picture",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val results = FloatArray(1)
    android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0] / 1000f // Convert to km
}

private data class Cluster(
    val id: String,
    val center: LatLng,
    val pins: List<DangerPin>
)

private fun clusterPins(pins: List<DangerPin>, zoom: Float): List<Cluster> {
    if (pins.isEmpty()) return emptyList()
    
    // Zoom-dependent grid grouping threshold in coordinate degrees
    val threshold = when {
        zoom < 5f -> 4.0
        zoom < 7f -> 2.0
        zoom < 9f -> 0.8
        zoom < 11f -> 0.25
        zoom < 13f -> 0.05
        else -> 0.0 // No clustering for zoom >= 13
    }
    
    if (threshold == 0.0) {
        return pins.map { pin ->
            Cluster(
                id = pin.id,
                center = LatLng(pin.latitude, pin.longitude),
                pins = listOf(pin)
            )
        }
    }
    
    val clusters = mutableListOf<Cluster>()
    
    for (pin in pins) {
        val pinLatLng = LatLng(pin.latitude, pin.longitude)
        var matched = false
        for (i in clusters.indices) {
            val cluster = clusters[i]
            val dLat = cluster.center.latitude - pinLatLng.latitude
            val dLng = cluster.center.longitude - pinLatLng.longitude
            val dist = Math.sqrt(dLat * dLat + dLng * dLng)
            if (dist < threshold) {
                val updatedPins = cluster.pins + pin
                val avgLat = updatedPins.map { it.latitude }.average()
                val avgLng = updatedPins.map { it.longitude }.average()
                clusters[i] = Cluster(
                    id = cluster.id,
                    center = LatLng(avgLat, avgLng),
                    pins = updatedPins
                )
                matched = true
                break
            }
        }
        if (!matched) {
            clusters.add(
                Cluster(
                    id = pin.id,
                    center = pinLatLng,
                    pins = listOf(pin)
                )
            )
        }
    }
    
    return clusters
}

private fun getCoilModel(model: String?): Any? {
    if (model == null) return null
    if (model.startsWith("data:image/") && model.contains("base64,")) {
        return try {
            val base64Content = model.substringAfter("base64,")
            android.util.Base64.decode(base64Content, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            model
        }
    }
    return model
}
