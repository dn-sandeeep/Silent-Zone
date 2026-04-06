package com.sandeep.silentzone.ui

import android.location.Address
import android.location.Geocoder
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // Bottom Sheet State
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var tempLatLng by remember { mutableStateOf<LatLng?>(null) }
    var tempName by remember { mutableStateOf("") }
    var tempRadius by remember { mutableFloatStateOf(100f) }
    var editingZone by remember { mutableStateOf<MapZone?>(null) }

    // Handle system back button
    BackHandler {
        if (showBottomSheet) {
            showBottomSheet = false
            editingZone = null
            tempLatLng = null
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            onCancel()
        }
    }

    var mapType by remember { mutableStateOf(MapType.SATELLITE) }
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Address>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    val focusManager = LocalFocusManager.current

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 17f)
    }

    // Search logic
    fun performSearch(query: String) {
        if (query.isBlank()) return
        isSearching = true
        scope.launch(Dispatchers.IO) {
            try {
                val results = geocoder.getFromLocationName(query, 1)
                withContext(Dispatchers.Main) {
                    isSearching = false
                    if (!results.isNullOrEmpty()) {
                        val res = results[0]
                        val pos = LatLng(res.latitude, res.longitude)
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pos, 18f))
                        tempLatLng = pos
                        tempRadius = 150f
                        tempName = res.featureName ?: res.thoroughfare ?: "Searched Zone"
                        showBottomSheet = true
                        searchQuery = ""
                        suggestions = emptyList()
                        focusManager.clearFocus()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isSearching = false }
            }
        }
    }

    // Debounced suggestion logic
    LaunchedEffect(searchQuery) {
        if (searchQuery.length < 3) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(500)
        isSearching = true
        withContext(Dispatchers.IO) {
            try {
                val results = geocoder.getFromLocationName(searchQuery, 5)
                withContext(Dispatchers.Main) {
                    suggestions = results ?: emptyList()
                    isSearching = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isSearching = false }
            }
        }
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
                        snippet = "Radius: ${zone.radius.toInt()}m",
                        onClick = {
                            editingZone = zone
                            tempLatLng = zone.latLng
                            tempName = zone.name
                            tempRadius = zone.radius
                            showBottomSheet = true
                            true // consume click
                        }
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

            // --- PREMIUM SLIM SEARCH BAR & SUGGESTIONS ---
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Slim Glassmorphic Search Bar
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp) // Slimmer height
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Search location...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { performSearch(searchQuery) })
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { 
                                    searchQuery = ""
                                    suggestions = emptyList()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Suggestions List
                AnimatedVisibility(
                    visible = suggestions.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                    ) {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 240.dp)
                        ) {
                            items(suggestions) { address ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val pos = LatLng(address.latitude, address.longitude)
                                            cameraPositionState.position = CameraPosition.fromLatLngZoom(pos, 18f)
                                            tempLatLng = pos
                                            tempRadius = 150f
                                            tempName = address.featureName ?: address.thoroughfare ?: "Found Location"
                                            showBottomSheet = true
                                            searchQuery = ""
                                            suggestions = emptyList()
                                            focusManager.clearFocus()
                                        }
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = address.featureName ?: "Unknown Place",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val subtitle = address.getAddressLine(0) ?: ""
                                    if (subtitle.isNotEmpty()) {
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (suggestions.last() != address) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = !showBottomSheet && suggestions.isEmpty(),
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

            // --- 3D FLOATING CONFIRM BUTTON ---
            AnimatedVisibility(
                visible = selectedZones.isNotEmpty(),
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp) // Floating above map
            ) {
                Surface(
                    onClick = { onZonesSelected(selectedZones) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shadowElevation = 12.dp,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .height(50.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Confirm ${selectedZones.size} Zones",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
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
            onDismissRequest = { 
                showBottomSheet = false
                editingZone = null
            },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (editingZone == null) "Configure Smart Zone" else "Edit Smart Zone",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (editingZone == null) {
                        TextButton(onClick = {
                            showBottomSheet = false
                            tempLatLng = null
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }) {
                            Text("Discard", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        IconButton(onClick = {
                            selectedZones.remove(editingZone)
                            showBottomSheet = false
                            editingZone = null
                            tempLatLng = null
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

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
                        val name = if (tempName.isBlank()) "Zone ${selectedZones.size + 1}" else tempName
                        val newZone = MapZone(tempLatLng!!, name, tempRadius)
                        
                        if (editingZone != null) {
                            val index = selectedZones.indexOf(editingZone)
                            if (index != -1) {
                                selectedZones[index] = newZone
                            }
                        } else {
                            selectedZones.add(newZone)
                        }
                        
                        showBottomSheet = false
                        editingZone = null
                        tempLatLng = null
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    )
                ) {
                    Text(
                        if (editingZone == null) "Add Smart Zone" else "Update Zone", 
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

