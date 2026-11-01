package com.njiasalama.ui.ride

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.njiasalama.domain.model.BreadcrumbPoint
import com.njiasalama.domain.model.DangerPin
import com.njiasalama.domain.model.HazardType
import com.njiasalama.domain.model.TraversedRide

/**
 * PostRideDebriefScreen allows cyclists to review the exact route they just traversed,
 * tap anywhere along the route line to pinpoint where a road hazard was encountered,
 * and submit the hazard report in a few seconds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRideDebriefScreen(
    ride: TraversedRide,
    existingPins: List<DangerPin> = emptyList(),
    onClose: () -> Unit,
    onSubmitHazard: (title: String, description: String, type: HazardType, lat: Double, lng: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val routeLatLngs = remember(ride) {
        ride.points.map { LatLng(it.latitude, it.longitude) }
    }

    // Camera setup centered on route
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(routeLatLngs) {
        if (routeLatLngs.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.builder()
            routeLatLngs.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        }
    }

    // Selected point for new hazard report
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var showReportSheet by remember { mutableStateOf(false) }

    // Summary calculations
    val distanceKm = (ride.totalDistanceMeters / 1000.0)
    val durationMinutes = ((ride.endTime - ride.startTime) / 60000.0).coerceAtLeast(1.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Post-Ride Hazard Review", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "%.2f km • %.0f mins".format(distanceKm, durationMinutes),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Interactive Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = true,
                    myLocationButtonEnabled = false
                ),
                onMapClick = { clickedLatLng ->
                    // Snapping to closest point on the route polyline if near enough
                    val snapped = findNearestPointOnRoute(clickedLatLng, routeLatLngs, maxDistanceMeters = 50.0)
                    selectedLocation = snapped ?: clickedLatLng
                    showReportSheet = true
                }
            ) {
                // Traversed Ride Polyline (Green highlight)
                if (routeLatLngs.size >= 2) {
                    Polyline(
                        points = routeLatLngs,
                        color = Color(0xFF059669),
                        width = 14f
                    )
                }

                // Start Marker
                if (routeLatLngs.isNotEmpty()) {
                    Marker(
                        state = MarkerState(position = routeLatLngs.first()),
                        title = "Ride Start",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                    // End Marker
                    Marker(
                        state = MarkerState(position = routeLatLngs.last()),
                        title = "Ride Finish",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )
                }

                // Existing Danger Pins along route
                existingPins.forEach { pin ->
                    Marker(
                        state = MarkerState(position = LatLng(pin.latitude, pin.longitude)),
                        title = pin.title,
                        snippet = pin.description,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }

                // Temporary marker for the point the cyclist tapped to report
                selectedLocation?.let { loc ->
                    Marker(
                        state = MarkerState(position = loc),
                        title = "Selected Hazard Location",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                    )
                }
            }

            // Top Guidance Pill
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tap along your green route to add a hazard",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Bottom Sheet for adding hazard when a point is tapped
            if (showReportSheet && selectedLocation != null) {
                QuickHazardBottomSheet(
                    location = selectedLocation!!,
                    onDismiss = { showReportSheet = false },
                    onSubmit = { title, description, type ->
                        onSubmitHazard(title, description, type, selectedLocation!!.latitude, selectedLocation!!.longitude)
                        showReportSheet = false
                    }
                )
            }
        }
    }
}

/**
 * Fast 1-tap categorization bottom sheet designed for tired cyclists post-ride.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickHazardBottomSheet(
    location: LatLng,
    onDismiss: () -> Unit,
    onSubmit: (title: String, description: String, type: HazardType) -> Unit
) {
    var selectedType by remember { mutableStateOf(HazardType.POTHOLE) }
    var customDescription by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                "Report Road Hazard",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Pinned at %.5f, %.5f on your route".format(location.latitude, location.longitude),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Hazard Type", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            // Quick Selection Chips for Cyclists
            val hazardTypes = listOf(
                HazardType.POTHOLE to "Pothole / Crater",
                HazardType.UNLIT_STREET to "Dark / Unlit Road",
                HazardType.DANGEROUS_TRAFFIC to "Dangerous Traffic",
                HazardType.OTHER to "Road Obstacle / Glass"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                hazardTypes.forEach { (type, label) ->
                    val isSelected = selectedType == type
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF059669).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF059669)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                label,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = customDescription,
                onValueChange = { customDescription = it },
                label = { Text("Details (e.g. Deep crater near left curb)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val defaultTitle = when (selectedType) {
                        HazardType.POTHOLE -> "Pothole on Route"
                        HazardType.UNLIT_STREET -> "Unlit Section"
                        HazardType.DANGEROUS_TRAFFIC -> "Heavy Traffic Hazard"
                        HazardType.OTHER -> "Road Hazard"
                    }
                    val finalDesc = if (customDescription.isNotBlank()) customDescription else "Reported by cyclist post-ride"
                    onSubmit(defaultTitle, finalDesc, selectedType)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publish Hazard Alert", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Snaps a user map tap to the nearest point on the route polyline if within [maxDistanceMeters].
 */
private fun findNearestPointOnRoute(tap: LatLng, route: List<LatLng>, maxDistanceMeters: Double): LatLng? {
    if (route.isEmpty()) return null

    var nearest: LatLng? = null
    var minDistance = Double.MAX_VALUE

    for (pt in route) {
        val dist = haversineDistanceMeters(tap.latitude, tap.longitude, pt.latitude, pt.longitude)
        if (dist < minDistance) {
            minDistance = dist
            nearest = pt
        }
    }

    return if (minDistance <= maxDistanceMeters) nearest else null
}

private fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0 // Earth radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

