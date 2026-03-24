package com.sandeep.silentzone.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
    setVibrate: () -> Unit,
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
    onAddLocationZone: (RingerMode, Float) -> Unit,
    onMapZonesSelected: (List<MapZone>, RingerMode, Float) -> Unit,
    onDeleteLocationZone: (String) -> Unit,
    initialUserLocation: com.google.android.gms.maps.model.LatLng?,
    importantContacts: List<ImportantContact>,
    onPickContact: () -> Unit,
    onDeleteContact: (String) -> Unit
) {
    var selectedScreen by remember { mutableStateOf(0) } // 0 = Home, 1 = Zones, 2 = Contacts
    var showMapSelection by remember { mutableStateOf(false) }
    var showAddTypeDialog by remember { mutableStateOf(false) }
    var pendingSsid by remember { mutableStateOf<String?>(null) }
    var showWifiSelection by remember { mutableStateOf(false) }
    
    // Radius logic
    var showRadiusDialog by remember { mutableStateOf(false) }
    var radiusSource by remember { mutableStateOf<RadiusSource?>(null) }

    if (showMapSelection) {
        val startLocation = initialUserLocation ?: com.google.android.gms.maps.model.LatLng(20.5937, 78.9629)
        MapSelectionScreen(
            initialLocation = startLocation,
            onZonesSelected = { zones ->
                radiusSource = RadiusSource.Map(zones)
                showRadiusDialog = true
                showMapSelection = false
            },
            onCancel = { showMapSelection = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when(selectedScreen) {
                                0 -> "SilentZone"
                                1 -> "Smart Zones"
                                else -> "Whitelist"
                            },
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedScreen == 0,
                        onClick = { selectedScreen = 0 },
                        icon = { Icon(if (selectedScreen == 0) Icons.Default.Home else Icons.Default.Home, null) },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = selectedScreen == 1,
                        onClick = { selectedScreen = 1 },
                        icon = { Icon(Icons.Default.Dashboard, null) },
                        label = { Text("Zones") }
                    )
                    NavigationBarItem(
                        selected = selectedScreen == 2,
                        onClick = { selectedScreen = 2 },
                        icon = { Icon(Icons.Default.Person, null) },
                        label = { Text("Contacts") }
                    )
                }
            },
            floatingActionButton = {
                if (selectedScreen != 0) {
                    FloatingActionButton(
                        onClick = {
                            if (selectedScreen == 1) showAddTypeDialog = true else onPickContact()
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = selectedScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.padding(innerPadding)
            ) { target ->
                when (target) {
                    0 -> DashboardScreen(
                        accessGranted = accessGranted,
                        mode = mode,
                        onGrantAccess = onGrantAccess,
                        setSilent = setSilent,
                        setVibrate = setVibrate,
                        setNormal = setNormal,
                        wifiPermissionGranted = wifiPermissionGranted,
                        currentWifiSsid = currentWifiSsid
                    )
                    1 -> ZonesScreen(
                        silentSsids = silentSsids,
                        vibrateSsids = vibrateSsids,
                        locationZones = locationZones,
                        onDeleteSsid = onDeleteSsid,
                        onDeleteLocationZone = onDeleteLocationZone
                    )
                    2 -> ContactsScreen(
                        contacts = importantContacts,
                        onDeleteContact = onDeleteContact
                    )
                }
            }
        }

        // Dialogs
        if (showAddTypeDialog) {
            AddZoneTypeDialog(
                onCurrentLocation = { 
                    radiusSource = RadiusSource.CurrentLocation
                    showRadiusDialog = true
                    showAddTypeDialog = false 
                },
                onSelectMap = { showMapSelection = true; showAddTypeDialog = false },
                onWifi = { addZone(); showWifiSelection = true; showAddTypeDialog = false },
                onDismiss = { showAddTypeDialog = false }
            )
        }

        if (showRadiusDialog) {
            RadiusSelectionDialog(
                onRadiusSelected = { radius ->
                    when (val source = radiusSource) {
                        is RadiusSource.CurrentLocation -> onAddLocationZone(RingerMode.SILENT, radius)
                        is RadiusSource.Map -> onMapZonesSelected(source.zones, RingerMode.SILENT, radius)
                        null -> {}
                    }
                    showRadiusDialog = false
                    radiusSource = null
                },
                onDismiss = {
                    showRadiusDialog = false
                    radiusSource = null
                }
            )
        }

        if (showWifiSelection && availableSsidList.isNotEmpty()) {
            SsidSelectionDialog(
                ssids = availableSsidList,
                onSsidSelected = { ssid ->
                    pendingSsid = ssid
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
                onModeSelected = { m ->
                    onSelectedSsid(pendingSsid!!, m)
                    pendingSsid = null
                },
                onDismiss = { pendingSsid = null }
            )
        }
    }
}

sealed class RadiusSource {
    object CurrentLocation : RadiusSource()
    data class Map(val zones: List<MapZone>) : RadiusSource()
}

@Composable
fun DashboardScreen(
    accessGranted: Boolean,
    mode: RingerMode,
    onGrantAccess: () -> Unit,
    setSilent: () -> Unit,
    setVibrate: () -> Unit,
    setNormal: () -> Unit,
    wifiPermissionGranted: Boolean,
    currentWifiSsid: String?
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Premium Animated Header
        PulseStatusHeader(mode = mode)

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (!accessGranted) {
                PermissionWarningCard(onGrantAccess = onGrantAccess)
            }

            DashboardSectionHeader("Quick Controls")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ModeToggleCard(
                    title = "Normal",
                    icon = Icons.Default.NotificationsActive,
                    isActive = mode == RingerMode.NORMAL,
                    onClick = setNormal,
                    activeColor = MaterialTheme.colorScheme.primary
                )
                ModeToggleCard(
                    title = "Vibrate",
                    icon = Icons.Default.Vibration,
                    isActive = mode == RingerMode.VIBRATE,
                    onClick = setVibrate,
                    activeColor = MaterialTheme.colorScheme.secondary
                )
                ModeToggleCard(
                    title = "Silent",
                    icon = Icons.Default.DoNotDisturbOn,
                    isActive = mode == RingerMode.SILENT,
                    onClick = setSilent,
                    activeColor = MaterialTheme.colorScheme.tertiary
                )
            }

            DashboardSectionHeader("Current Status")
            
            PermissionStatusCard(
                wifiPermissionGranted = wifiPermissionGranted,
                onRequestWifiPermission = {}
            )
            
            if (currentWifiSsid != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wifi, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Connected to: $currentWifiSsid", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ZonesScreen(
    silentSsids: Set<String>,
    vibrateSsids: Set<String>,
    locationZones: List<LocationZone>,
    onDeleteSsid: (String) -> Unit,
    onDeleteLocationZone: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (locationZones.isEmpty() && silentSsids.isEmpty() && vibrateSsids.isEmpty()) {
            item {
                EmptyStateText("No zones added yet.\nTap '+' to create your first silent zone.")
            }
        }

        if (locationZones.isNotEmpty()) {
            item { DashboardSectionHeader("Location Zones") }
            items(locationZones) { zone ->
                LocationZoneItemCard(zone = zone, onDelete = { onDeleteLocationZone(zone.id) })
            }
        }

        if (silentSsids.isNotEmpty() || vibrateSsids.isNotEmpty()) {
            item { DashboardSectionHeader("WiFi Zones") }
            items(silentSsids.toList()) { ssid ->
                ZoneItemCard(ssid = ssid, onDelete = { onDeleteSsid(ssid) })
            }
            items(vibrateSsids.toList()) { ssid ->
                ZoneItemCard(ssid = ssid, onDelete = { onDeleteSsid(ssid) })
            }
        }
    }
}

@Composable
fun ContactsScreen(
    contacts: List<ImportantContact>,
    onDeleteContact: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (contacts.isEmpty()) {
            item {
                EmptyStateText("No important contacts.\nCalls from whitelist will always ring.")
            }
        } else {
            item { DashboardSectionHeader("Whitelisted Contacts") }
            items(contacts) { contact ->
                ImportantContactItemCard(contact = contact, onDelete = { onDeleteContact(contact.phoneNumber) })
            }
        }
    }
}

@Composable
fun AddZoneTypeDialog(
    onCurrentLocation: () -> Unit,
    onSelectMap: () -> Unit,
    onWifi: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Zone Type", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onCurrentLocation,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.LocationOn, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Current Location")
                }
                OutlinedButton(
                    onClick = onSelectMap,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Map, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Select on Map")
                }
                OutlinedButton(
                    onClick = onWifi,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Wifi, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("WiFi Network")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", fontWeight = FontWeight.Bold) }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun EmptyStateText(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.NotificationsActive,
            null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
