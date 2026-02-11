package com.sandeep.silentzone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

data class MapZone(val latLng: LatLng, val name: String)

@Composable
fun MapSelectionScreen(
    initialLocation: LatLng,
    onZonesSelected: (List<MapZone>) -> Unit,
    onCancel: () -> Unit
) {
    val selectedZones = remember { mutableStateListOf<MapZone>() }
    val scope = rememberCoroutineScope()
    
    // State for the dialog
    var showNameDialog by remember { mutableStateOf(false) }
    var tempLatLng by remember { mutableStateOf<LatLng?>(null) }
    var tempName by remember { mutableStateOf("") }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 15f)
    }

    // Effect to move camera to initial location if it changes
    LaunchedEffect(initialLocation) {
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(initialLocation, 15f)
        )
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (selectedZones.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { onZonesSelected(selectedZones) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Confirm Selection")
                    }
                }
                
                FloatingActionButton(
                    onClick = onCancel,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false),
                onMapClick = { latLng ->
                    // 1. Zoom in to the tapped location
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(latLng, 18f),
                            500
                        )
                    }
                    // 2. Show dialog to name the zone
                    tempLatLng = latLng
                    tempName = "" // Reset name
                    showNameDialog = true
                }
            ) {
                // Show markers for selected zones
                selectedZones.forEach { zone ->
                    Marker(
                        state = MarkerState(position = zone.latLng),
                        title = zone.name,
                        snippet = "Lat: ${zone.latLng.latitude}, Lng: ${zone.latLng.longitude}"
                    )
                }
            }

            // Overlay Instruction
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = CircleShape
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (selectedZones.isEmpty()) "Tap on map to add a zone" else "${selectedZones.size} zones selected",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showNameDialog && tempLatLng != null) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Name this Location") },
            text = {
                Column {
                    Text("Enter a name for this silent zone (e.g., Office, Home)")
                    TextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        placeholder = { Text("Zone Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = if (tempName.isBlank()) "Zone ${selectedZones.size + 1}" else tempName
                        selectedZones.add(MapZone(tempLatLng!!, name))
                        showNameDialog = false
                        tempLatLng = null
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
