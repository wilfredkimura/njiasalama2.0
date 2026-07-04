package com.njiasalama.ui.map

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.njiasalama.domain.model.HazardType
import coil.compose.AsyncImage

/**
 * A dialog window allowing cyclists to input hazard details (Title, Description, Type, and optional Photo)
 * when they long-press a location on the map.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPinDialog(
    latLng: LatLng,
    onDismiss: () -> Unit,
    onSubmit: (title: String, description: String, type: HazardType, imageUri: android.net.Uri?) -> Unit
) {
    // Local state holders for the text form inputs
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(HazardType.POTHOLE) }
    var selectedImageUri by rememberSaveable { mutableStateOf<android.net.Uri?>(null) }

    // Launcher contract to open the system file picker to select images
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        selectedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Report Road Hazard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show captured coordinates in a subtle premium grey card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "LOCATION COORDINATES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lat: ${String.format("%.5f", latLng.latitude)}, Lng: ${String.format("%.5f", latLng.longitude)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Input field for Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Hazard Title") },
                    placeholder = { Text("e.g. Huge Pothole, Oil Spill") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Input field for Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g. Hard to spot at night, avoid right lane") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Selection block for Hazard Types
                Text(
                    text = "Hazard Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Display options as dynamic chips that flow into multiple lines automatically
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HazardType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name.replace("_", " ")) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Premium MD3 Image Picker Block
                Text(
                    text = "Hazard Photo (Optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (selectedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Photo Preview",
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(50))
                                .padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Photo",
                                tint = Color.Red
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Choose Photo")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSubmit(title, description, selectedType, selectedImageUri)
                    }
                },
                enabled = title.isNotBlank() // Prevent submitting empty hazards
            ) {
                Text("Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
