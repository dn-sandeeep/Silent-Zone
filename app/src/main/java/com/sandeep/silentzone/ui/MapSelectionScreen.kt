package com.sandeep.silentzone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.location.Geocoder
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

data class MapZone(val latLng: LatLng, val name: String, val radius: Float = 100f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapSelectionScreen(
    initialLocation: LatLng,
    onZonesSelected: (List<MapZone>) -> Unit,
    onCancel: () -> Unit
) {
    val selectedZones = remember { mutableStateListOf<MapZone>() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Handle system back button
    BackHandler {
        onCancel()
    }

    // Bottom Sheet State
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var tempLatLng by remember { mutableStateOf<LatLng?>(null) }
    var tempName by remember { mutableStateOf("") }
    var tempRadius by remember { mutableFloatStateOf(100f) }

    var mapType by remember { mutableStateOf(MapType.SATELLITE) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 17f)
    }

    fun reverseGeocode(latLng: LatLng) {
        scope.launch(Dispatchers.IO) {
            try {
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val name = address.featureName ?: address.thoroughfare ?: "New Zone"
                    withContext(Dispatchers.Main) {
                        tempName = name
                    }
                }
            } catch (e: Exception) {
                // Fallback to coordinates or generic name
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                // Map Style Toggle
                FloatingActionButton(
                    onClick = {
                        mapType =
                            if (mapType == MapType.SATELLITE) MapType.NORMAL else MapType.SATELLITE
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Toggle Map Type")
                }

                // My Location Button
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(initialLocation, 18f)
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 16.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                }

                FloatingActionButton(
                    onClick = onCancel,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedZones.isNotEmpty(),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {

                Button(
                    onClick = { onZonesSelected(selectedZones) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Confirm ${selectedZones.size} Zones",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }

            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    tiltGesturesEnabled = false
                ),
                properties = MapProperties(
                    isMyLocationEnabled = true,
                    mapType = mapType
                ),
                onMapClick = { latLng ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(latLng, 18f),
                            500
                        )
                    }
                    tempLatLng = latLng
                    tempRadius = 150f
                    reverseGeocode(latLng)
                    showBottomSheet = true
                }
            ) {
                // Live Preview of Current Tapped Location (while Bottom Sheet is open)
                if (showBottomSheet && tempLatLng != null) {
                    val markerState = remember(tempLatLng) { MarkerState(position = tempLatLng!!) }

                    // Sync geocode only when dragging stops
                    LaunchedEffect(markerState.isDragging) {
                        if (!markerState.isDragging && tempLatLng != markerState.position) {
                            tempLatLng = markerState.position
                            reverseGeocode(markerState.position)
                        }
                    }

                    Marker(
                        state = markerState,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        draggable = true
                    )
                    Circle(
                        center = markerState.position,
                        radius = tempRadius.toDouble(),
                        fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        strokeColor = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3f
                    )
                }

                selectedZones.forEach { zone ->
                    // Marker for the center
                    Marker(
                        state = MarkerState(position = zone.latLng),
                        title = zone.name,
                        snippet = "Radius: ${zone.radius.toInt()}m"
                    )

                    // Circle to visualize the geofence
                    Circle(
                        center = zone.latLng,
                        radius = zone.radius.toDouble(),
                        fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        strokeColor = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2f
                    )
                }
            }

            // Floating Search Bar & Glassmorphic Banner
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Global Search Bar
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search for place...") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val results =
                                                    geocoder.getFromLocationName(searchQuery, 1)
                                                if (!results.isNullOrEmpty()) {
                                                    val res = results[0]
                                                    val pos = LatLng(res.latitude, res.longitude)
                                                    withContext(Dispatchers.Main) {
                                                        cameraPositionState.animate(
                                                            CameraUpdateFactory.newLatLngZoom(
                                                                pos,
                                                                18f
                                                            )
                                                        )
                                                        tempLatLng = pos
                                                        tempRadius = 150f
                                                        tempName =
                                                            res.featureName ?: "Searched Zone"
                                                        showBottomSheet = true
                                                        searchQuery = ""
                                                    }
                                                }
                                            } catch (e: Exception) {
                                            }
                                        }
                                    }) {
                                        Icon(Icons.Default.Check, null)
                                    }
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = !showBottomSheet,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        tonalElevation = 4.dp,
                        shadowElevation = 2.dp,
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = if (selectedZones.isEmpty()) "Tap or search to create a silence zone" else "${selectedZones.size} zones ready",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
    }

    // Modern Bottom Sheet for Zone Configuration
    if (showBottomSheet && tempLatLng != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            ) {
                Text(
                    "Configure Smart Zone",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Zone Name") },
                    placeholder = { Text("e.g. Office, Library, Gym") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.background,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.2f
                        ),
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Geofence Radius",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "${tempRadius.toInt()} meters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = tempRadius,
                    onValueChange = {
                        tempRadius = it
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    valueRange = 50f..500f,
                    steps = 8, // Increments of 50m approx
                    modifier = Modifier.padding(vertical = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.background,
                        activeTrackColor = MaterialTheme.colorScheme.background,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        activeTickColor = MaterialTheme.colorScheme.onBackground,
                        inactiveTickColor = MaterialTheme.colorScheme.background
                    )
                )

                Text(
                    "This area will automatically trigger your silent mode profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val name =
                            if (tempName.isBlank()) "Zone ${selectedZones.size + 1}" else tempName
                        selectedZones.add(MapZone(tempLatLng!!, name, tempRadius))
                        showBottomSheet = false
                        tempLatLng = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    )
                ) {
                    Text("Add Smart Zone", fontSize = 16.sp)
                }
            }
        }
    }
}

