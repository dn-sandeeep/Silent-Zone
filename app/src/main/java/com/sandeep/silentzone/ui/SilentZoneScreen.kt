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
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
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
import com.sandeep.silentzone.ImportantContact
import com.sandeep.silentzone.LocationZone
import com.sandeep.silentzone.RingerMode

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
    onSelectedSsid: (String, RingerMode) -> Unit,
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
    initialUserLocation: com.google.android.gms.maps.model.LatLng?,
    importantContacts: List<ImportantContact>,
    onPickContact: () -> Unit,
    onDeleteContact: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Silent, 1 = Vibrate, 2 = Important
    var pendingSsid by remember { mutableStateOf<String?>(null) }
    var showManualInput by remember { mutableStateOf(false) }
    var showAddTypeDialog by remember { mutableStateOf(false) }

    var showMapSelection by remember { mutableStateOf(false) }
    var showWifiSelection by remember { mutableStateOf(false) }

    if (showMapSelection) {
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
                SilentZoneAnimation(mode = mode)

                if (!accessGranted) {
                    PermissionWarningCard(onGrantAccess = onGrantAccess)
                } else {
                    PermissionStatusCard(
                        wifiPermissionGranted = wifiPermissionGranted,
                        onRequestWifiPermission = {}
                    )
                    
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Silent") },
                            icon = { Icon(Icons.Default.DoNotDisturbOn, contentDescription = null) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Vibrate") },
                            icon = { Icon(Icons.Default.Vibration, contentDescription = null) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Important") },
                            icon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
                    ) {
                        when (selectedTab) {
                            0, 1 -> {
                                val targetMode = if (selectedTab == 0) RingerMode.SILENT else RingerMode.VIBRATE
                                val currentWifiList = if (selectedTab == 0) silentSsids else vibrateSsids
                                val currentLocationList = locationZones.filter { it.mode == targetMode }

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

                                if (currentLocationList.isEmpty() && currentWifiList.isEmpty()) {
                                    item {
                                        EmptyStateText("No zones added for this mode")
                                    }
                                }
                            }
                            2 -> {
                                if (importantContacts.isNotEmpty()) {
                                    items(importantContacts) { contact ->
                                        ImportantContactItemCard(
                                            contact = contact,
                                            onDelete = { onDeleteContact(contact.phoneNumber) }
                                        )
                                    }
                                } else {
                                    item {
                                        EmptyStateText("No important contacts added")
                                    }
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = {
                                if (selectedTab == 2) onPickContact() else showAddTypeDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                if (selectedTab == 2) Icons.Default.Person else Icons.Default.DoNotDisturbOn,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedTab == 2) "Add Important Contact" else "Add New Zone")
                        }
                    }
                }
            }
        }

        if (showWifiSelection && availableSsidList.isNotEmpty()) {
            SsidSelectionDialog(
                ssids = availableSsidList,
                onSsidSelected = { ssid ->
                    pendingSsid = ssid
                    onDismissDialog()
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

        if (showAddTypeDialog) {
            val targetMode = if (selectedTab == 0) RingerMode.SILENT else RingerMode.VIBRATE
            AlertDialog(
                onDismissRequest = { showAddTypeDialog = false },
                title = { Text("Select Zone Type") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
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
                                showMapSelection = true
                                showAddTypeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select on Map")
                        }
                        OutlinedButton(
                            onClick = {
                                addZone()
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

@Composable
fun EmptyStateText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
