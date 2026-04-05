package com.sandeep.silentzone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandeep.silentzone.ImportantContact
import com.sandeep.silentzone.LocationZone
import com.sandeep.silentzone.RingerMode
import com.sandeep.silentzone.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SilentScreen(
    accessGranted: Boolean,
    mode: RingerMode,
    isFallback: Boolean,
    operationState: com.sandeep.silentzone.OperationState,
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
    currentWifiSsid: String?,
    locationZones: List<LocationZone>,
    onAddLocationZone: (RingerMode, Float) -> Unit,
    onMapZonesSelected: (List<MapZone>, RingerMode) -> Unit,
    onDeleteLocationZone: (String) -> Unit,
    initialUserLocation: com.google.android.gms.maps.model.LatLng?,
    importantContacts: List<ImportantContact>,
    onPickContact: () -> Unit,
    onDeleteContact: (String) -> Unit,
    onRequestPermission: (() -> Unit) -> Unit
) {
    var selectedScreen by remember { mutableStateOf(0) }
    
    BackHandler(enabled = selectedScreen != 0) {
        selectedScreen = 0
    }
    
    var showMapSelection by remember { mutableStateOf(false) }
    var showAddTypeDialog by remember { mutableStateOf(false) }
    var pendingSsid by remember { mutableStateOf<String?>(null) }
    var showWifiSelection by remember { mutableStateOf(false) }
    var showRadiusDialog by remember { mutableStateOf(false) }
    var radiusSource by remember { mutableStateOf<RadiusSource?>(null) }

    if (showMapSelection) {
        val startLocation = initialUserLocation ?: com.google.android.gms.maps.model.LatLng(20.5937, 78.9629)
        MapSelectionScreen(
            initialLocation = startLocation,
            onZonesSelected = { zones ->
                onMapZonesSelected(zones, RingerMode.SILENT)
                showMapSelection = false
            },
            onCancel = { showMapSelection = false }
        )
    } else {
        Scaffold(
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MidnightBlue.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            selected = selectedScreen == 0,
                            onClick = { selectedScreen = 0 },
                            icon = { Icon(Icons.Default.Home, null) },
                            label = { Text("Home", fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = IndigoAccent,
                                selectedTextColor = IndigoAccent,
                                unselectedIconColor = Color.White.copy(alpha = 0.4f),
                                unselectedTextColor = Color.White.copy(alpha = 0.4f),
                                indicatorColor = IndigoAccent.copy(alpha = 0.1f)
                            )
                        )
                        NavigationBarItem(
                            selected = selectedScreen == 1,
                            onClick = { selectedScreen = 1 },
                            icon = { Icon(Icons.Default.GridView, null) },
                            label = { Text("Zones", fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyanAccent,
                                selectedTextColor = CyanAccent,
                                unselectedIconColor = Color.White.copy(alpha = 0.4f),
                                unselectedTextColor = Color.White.copy(alpha = 0.4f),
                                indicatorColor = CyanAccent.copy(alpha = 0.1f)
                            )
                        )
                        NavigationBarItem(
                            selected = selectedScreen == 2,
                            onClick = { selectedScreen = 2 },
                            icon = { Icon(Icons.Default.VerifiedUser, null) },
                            label = { Text("Safe", fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TealAccent,
                                selectedTextColor = TealAccent,
                                unselectedIconColor = Color.White.copy(alpha = 0.4f),
                                unselectedTextColor = Color.White.copy(alpha = 0.4f),
                                indicatorColor = TealAccent.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            },
            floatingActionButton = {
                if (selectedScreen != 0) {
                    FloatingActionButton(
                        onClick = {
                            if (selectedScreen == 1) showAddTypeDialog = true else onPickContact()
                        },
                        containerColor = if (selectedScreen == 1) CyanAccent else TealAccent,
                        contentColor = MidnightBlue,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(DeepSpace, MidnightBlue)))
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = selectedScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { target ->
                    when (target) {
                        0 -> DashboardScreen(
                            accessGranted = accessGranted,
                            mode = mode,
                            isFallback = isFallback,
                            onGrantAccess = onGrantAccess,
                            setSilent = setSilent,
                            setVibrate = setVibrate,
                            setNormal = setNormal,
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
                // Operation status feedback (Top Layer)
                OperationOverlay(state = operationState)
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
                onDismiss = { showAddTypeDialog = false },
                onRequestPermission = onRequestPermission
            )
        }

        if (showRadiusDialog) {
            RadiusSelectionDialog(
                onRadiusSelected = { radius ->
                    if (radiusSource == RadiusSource.CurrentLocation) {
                        onAddLocationZone(RingerMode.SILENT, radius)
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

@Composable
fun DashboardScreen(
    accessGranted: Boolean,
    mode: RingerMode,
    isFallback: Boolean,
    onGrantAccess: () -> Unit,
    setSilent: () -> Unit,
    setVibrate: () -> Unit,
    setNormal: () -> Unit,
    currentWifiSsid: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PulseStatusHeader(mode = mode, isFallback = isFallback)

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            if (!accessGranted) {
                if (isFallback) {
                    DndActionCard(onGrantAccess = onGrantAccess)
                } else {
                    PermissionWarningCard(onGrantAccess = onGrantAccess)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardSectionHeader("Quick Controls")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModeToggleCard(
                        title = "Normal",
                        icon = Icons.Default.NotificationsActive,
                        isActive = mode == RingerMode.NORMAL,
                        onClick = setNormal,
                        activeColor = IndigoAccent
                    )
                    ModeToggleCard(
                        title = "Vibrate",
                        icon = Icons.Default.Vibration,
                        isActive = mode == RingerMode.VIBRATE,
                        onClick = setVibrate,
                        activeColor = TealAccent
                    )
                    ModeToggleCard(
                        title = "Silent",
                        icon = Icons.Default.DoNotDisturbOn,
                        isActive = mode == RingerMode.SILENT,
                        onClick = setSilent,
                        activeColor = RoseAccent
                    )
                }
            }

            if (currentWifiSsid != null) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardSectionHeader("Current Connection")
                    GlassCard {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(CyanAccent.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Wifi, null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Connected to Wi-Fi",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Text(
                                    currentWifiSsid,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "SMART ZONES",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = Color.White
            )
            Text(
                "Automation rules for your rings",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (locationZones.isEmpty() && silentSsids.isEmpty() && vibrateSsids.isEmpty()) {
            item {
                EmptyStateText("No regions configured.")
            }
        }

        if (locationZones.isNotEmpty()) {
            item { DashboardSectionHeader("Geofence Areas") }
            items(locationZones) { zone ->
                LocationZoneItemCard(zone = zone, onDelete = { onDeleteLocationZone(zone.id) })
            }
        }

        if (silentSsids.isNotEmpty() || vibrateSsids.isNotEmpty()) {
            item { DashboardSectionHeader("Wi-Fi Networks") }
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "WHITELIST",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = Color.White
            )
            Text(
                "Important calls that bypass silence",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (contacts.isEmpty()) {
            item {
                EmptyStateText("No priority contacts.")
            }
        } else {
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
    onDismiss: () -> Unit,
    onRequestPermission: (() -> Unit) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Automate Mode", fontWeight = FontWeight.Black, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onRequestPermission { onCurrentLocation() } },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassWhite)
                ) {
                    Icon(Icons.Default.MyLocation, null, tint = IndigoAccent)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Current Location", color = Color.White)
                }
                Button(
                    onClick = { onRequestPermission { onSelectMap() } },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassWhite)
                ) {
                    Icon(Icons.Default.Map, null, tint = CyanAccent)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Pick on Map", color = Color.White)
                }
                Button(
                    onClick = { onRequestPermission { onWifi() } },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassWhite)
                ) {
                    Icon(Icons.Default.Wifi, null, tint = TealAccent)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Connect to WiFi", color = Color.White)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = RoseAccent, fontWeight = FontWeight.Bold) }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = MidnightBlue
    )
}

@Composable
fun EmptyStateText(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(GlassWhite, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                null,
                modifier = Modifier.size(48.dp),
                tint = Color.White.copy(alpha = 0.2f)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.4f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

sealed class RadiusSource {
    object CurrentLocation : RadiusSource()
}
