package com.sandeep.silentzone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sandeep.silentzone.LocationZone
import com.sandeep.silentzone.RingerMode

// PermissionStatusCard moved to SilentZoneComponents.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SilentScreen(
    accessGranted: Boolean,
    mode: RingerMode,
    message: String?,
    onGrantAccess: () -> Unit,
    setSilent: () -> Unit,
    setNormal: () -> Unit,
    addZone: () -> Unit,
    availableSsidList: List<String>,
    onSelectedSsid: (String, RingerMode) -> Unit, // Updated callback
    onDismissDialog: () -> Unit,
    silentSsids: Set<String>,
    vibrateSsids: Set<String>,
    onDeleteSsid: (String) -> Unit,
    wifiPermissionGranted: Boolean,
    currentWifiSsid: String?,
    locationZones: List<LocationZone>,
    onAddLocationZone: (RingerMode) -> Unit,
    onMapZonesSelected: (List<MapZone>, RingerMode) -> Unit,
    onDeleteLocationZone: (String) -> Unit,
    initialUserLocation: com.google.android.gms.maps.model.LatLng? // New parameter
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Silent, 1 = Vibrate
    var pendingSsid by remember { mutableStateOf<String?>(null) } // Temp storage for selected SSID
    var showManualInput by remember { mutableStateOf(false) } // Manual input dialog
    var showAddTypeDialog by remember { mutableStateOf(false) } // Dialog to choose between WiFi and Location

    var showMapSelection by remember { mutableStateOf(false) }
    var showWifiSelection by remember { mutableStateOf(false) } // Control when to show the list

    if (showMapSelection) {
        // Use user location or default to India Center
        val startLocation =
            initialUserLocation ?: com.google.android.gms.maps.model.LatLng(20.5937, 78.9629)

        MapSelectionScreen(
            initialLocation = startLocation,
            onZonesSelected = { zones ->
                val targetMode = if (selectedTab == 0) RingerMode.SILENT else RingerMode.VIBRATE
                onMapZonesSelected(zones, targetMode)
                showMapSelection = false
            },
            onCancel = { showMapSelection = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Silent Zone", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animation (Non-scrollable header)
                SilentZoneAnimation(mode = mode)

                if (!accessGranted) {
                    PermissionWarningCard(onGrantAccess = onGrantAccess)
                } else {
                    // Permission Status Card
                    PermissionStatusCard(
                        wifiPermissionGranted = wifiPermissionGranted,
                        onRequestWifiPermission = {
                            // Will trigger permission request via MainActivity
                        }
                    )
                    // Zone Management Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Silent Zones") },
                            icon = { Icon(Icons.Default.DoNotDisturbOn, contentDescription = null) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Vibrate Zones") },
                            icon = {
                                Icon(
                                    Icons.Default.Vibration,
                                    contentDescription = null
                                )
                            }
                        )
                    }

                    // Unified Zone List
                    val targetMode = if (selectedTab == 0) RingerMode.SILENT else RingerMode.VIBRATE
                    val currentWifiList = if (selectedTab == 0) silentSsids else vibrateSsids
                    val currentLocationList = locationZones.filter { it.mode == targetMode }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp) // Space for FAB/Buttons if needed
                    ) {
                        // Location Zones Section
                        if (currentLocationList.isNotEmpty()) {
                            item {
                                Text(
                                    "Location Zones",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(currentLocationList) { zone ->
                                LocationZoneItemCard(
                                    zone = zone,
                                    onDelete = { onDeleteLocationZone(zone.id) })
                            }
                        }

                        // WiFi Zones Section
                        if (currentWifiList.isNotEmpty()) {
                            item {
                                Text(
                                    "WiFi Zones",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(currentWifiList.toList()) { ssid ->
                                ZoneItemCard(ssid = ssid, onDelete = { onDeleteSsid(ssid) })
                            }
                        }

                        // Empty State
                        if (currentLocationList.isEmpty() && currentWifiList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No zones added for this mode",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Controls & Add Button
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showAddTypeDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null
                                ) // Generic icon
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add New Zone")
                            }
                        }
                    }
                }
            }
        }

// Dialogs
        if (showWifiSelection && availableSsidList.isNotEmpty()) {
            SsidSelectionDialog(
                ssids = availableSsidList,
                onSsidSelected = { ssid ->
                    pendingSsid = ssid // Store and show mode dialog next
                    onDismissDialog() // Dismiss list dialog
                    showWifiSelection = false
                },
                onDismiss = {
                    onDismissDialog()
                    showWifiSelection = false
                }
            )
        }

        if (pendingSsid != null) {
            ModeSelectionDialog(
                ssid = pendingSsid!!,
                onModeSelected = { mode ->
                    onSelectedSsid(pendingSsid!!, mode)
                    pendingSsid = null
                },
                onDismiss = { pendingSsid = null }
            )
        }

        // Manual WiFi Input Dialog
        if (showManualInput) {
            var manualSsid by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showManualInput = false },
                title = { Text("Enter WiFi Name") },
                text = {
                    TextField(
                        value = manualSsid,
                        onValueChange = { manualSsid = it },
                        label = { Text("WiFi Network Name (SSID)") },
                        placeholder = { Text("e.g., HomeWiFi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (manualSsid.isNotBlank()) {
                                pendingSsid = manualSsid.trim()
                                showManualInput = false
                            }
                        },
                        enabled = manualSsid.isNotBlank()
                    ) {
                        Text("Next")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualInput = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showAddTypeDialog) {
            val targetMode = if (selectedTab == 0) RingerMode.SILENT else RingerMode.VIBRATE
            AlertDialog(
                onDismissRequest = { showAddTypeDialog = false },
                title = { Text("Select Zone Type") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                // Add current location immediately
                                onAddLocationZone(targetMode)
                                showAddTypeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Current Location")
                        }
                        OutlinedButton(
                            onClick = {
                                // Open Map Selection
                                showMapSelection = true
                                showAddTypeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED.let {
                                // Using generic map icon here
                                androidx.compose.material.icons.Icons.Default.Map
                            }, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select on Map")
                        }
                        OutlinedButton(
                            onClick = {
                                addZone() // Scan start
                                showWifiSelection = true
                                showAddTypeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("WiFi Network")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAddTypeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
// Helper components moved to SilentZoneComponents.kt
