package com.sandeep.silentzone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.sandeep.silentzone.ImportantContact
import com.sandeep.silentzone.LocationZone
import com.sandeep.silentzone.RingerMode

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
    normalSsids: Set<String>,
    onDeleteSsid: (String) -> Unit,
    currentWifiSsid: String?,
    locationZones: List<LocationZone>,
    onAddLocationZone: (RingerMode, Float) -> Unit,
    onMapZonesSelected: (List<MapZone>, RingerMode) -> Unit,
    onDeleteLocationZone: (String) -> Unit,
    initialUserLocation: LatLng?,
    importantContacts: List<ImportantContact>,
    onPickContact: () -> Unit,
    onDeleteContact: (String) -> Unit,
    onRequestPermission: (() -> Unit) -> Unit,
    onDisableBatteryOptimization: () -> Unit,
    hasBackgroundLocation: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    zoneCount: Int = 0,
    contactCount: Int = 0
) {
    var selectedScreen by remember { mutableIntStateOf(0) }
    
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
        val startLocation = initialUserLocation ?: LatLng(20.5937, 78.9629)
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
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (selectedScreen) {
                                0 -> "Home"
                                1 -> "Zones"
                                2 -> "Safe"
                                else -> "SilentZone"
                            },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { /* App icon/Drawer maybe? */ }) {
                            Icon(
                                Icons.Default.Adjust, 
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* History */ }) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                        IconButton(onClick = { /* Settings */ }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
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
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                        NavigationBarItem(
                            selected = selectedScreen == 1,
                            onClick = { selectedScreen = 1 },
                            icon = { Icon(Icons.Default.GridView, null) },
                            label = { Text("Zones", fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.secondary,
                                selectedTextColor = MaterialTheme.colorScheme.secondary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                            )
                        )
                        NavigationBarItem(
                            selected = selectedScreen == 2,
                            onClick = { selectedScreen = 2 },
                            icon = { Icon(Icons.Default.VerifiedUser, null) },
                            label = { Text("Safe", fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
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
                        containerColor = if (selectedScreen == 1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
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
                    .background(MaterialTheme.colorScheme.background)
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
                            currentWifiSsid = currentWifiSsid,
                            hasBackgroundLocation = hasBackgroundLocation,
                            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                            onRequestBackgroundLocation = { onRequestPermission { /* Logic handled by manager */ } }, // This needs bridge
                            onDisableBatteryOptimization = onDisableBatteryOptimization,
                            zoneCount = zoneCount,
                            contactCount = contactCount
                        )
                        1 -> ZonesScreen(
                            silentSsids = silentSsids,
                            vibrateSsids = vibrateSsids,
                            normalSsids = normalSsids,
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

        // Bottom Sheets (Replaced Dialogs)
        if (showAddTypeDialog) {
            AddZoneTypeBottomSheet(
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
            RadiusSelectionBottomSheet(
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
            SsidSelectionBottomSheet(
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
            ModeSelectionBottomSheet(
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
    currentWifiSsid: String?,
    hasBackgroundLocation: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    onRequestBackgroundLocation: () -> Unit,
    onDisableBatteryOptimization: () -> Unit,
    zoneCount: Int,
    contactCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PulseStatusHeader(mode = mode, isFallback = isFallback)

        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // New Stat Bubbles Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    StatBubble(
                        label = "Zones",
                        value = zoneCount.toString(),
                        icon = Icons.Default.Map,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    StatBubble(
                        label = "Whitelist",
                        value = contactCount.toString(),
                        icon = Icons.Default.VerifiedUser,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    StatBubble(
                        label = "Security",
                        value = if (accessGranted) "On" else "Off",
                        icon = if (accessGranted) Icons.Default.GppGood else Icons.Default.GppMaybe,
                        color = if (accessGranted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
            }

            if (!accessGranted) {
                if (isFallback) {
                    DndActionCard(onGrantAccess = onGrantAccess)
                } else {
                    PermissionWarningCard(onGrantAccess = onGrantAccess)
                }
            }

            SystemStatusSection(
                hasBackgroundLocation = hasBackgroundLocation,
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                onRequestBackgroundLocation = onRequestBackgroundLocation,
                onDisableBatteryOptimization = onDisableBatteryOptimization
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardSectionHeader("Quick Controls")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ModeToggleCard(
                            title = "Normal",
                            icon = Icons.Default.NotificationsActive,
                            isActive = mode == RingerMode.NORMAL,
                            onClick = setNormal,
                            activeColor = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ModeToggleCard(
                            title = "Vibrate",
                            icon = Icons.Default.Vibration,
                            isActive = mode == RingerMode.VIBRATE,
                            onClick = setVibrate,
                            activeColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ModeToggleCard(
                            title = "Silent",
                            icon = Icons.Default.DoNotDisturbOn,
                            isActive = mode == RingerMode.SILENT,
                            onClick = setSilent,
                            activeColor = MaterialTheme.colorScheme.error
                        )
                    }
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
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Wifi, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Connected to Wi-Fi",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    currentWifiSsid,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Recent Activity Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardSectionHeader("Recent Activity")
                GlassCard {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActivityLogItem(
                            title = "System Ready",
                            time = "Active Now",
                            icon = Icons.Default.AutoAwesome,
                            iconColor = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ActivityLogItem(
                            title = "Smart Zone Tracking",
                            time = "Monitoring in Background",
                            icon = Icons.Default.LocationOn,
                            iconColor = MaterialTheme.colorScheme.secondary
                        )
                        if (mode != RingerMode.NORMAL) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            ActivityLogItem(
                                title = "Auto Mode Engaged",
                                time = "Currently ${when(mode) { RingerMode.SILENT -> "Silent"; RingerMode.VIBRATE -> "Vibrate"; else -> "Normal" }}",
                                icon = Icons.Default.SmartButton,
                                iconColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ZonesScreen(
    silentSsids: Set<String>,
    vibrateSsids: Set<String>,
    normalSsids: Set<String>,
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section 1: Geofence Areas
        item { DashboardSectionHeader("Geofence Areas") }
        if (locationZones.isEmpty()) {
            item {
                MiniEmptyState(
                    icon = Icons.Default.LocationOn,
                    title = "No Locations Added",
                    subtitle = "Tap the + button to create your first geofence.",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            items(locationZones) { zone ->
                LocationZoneItemCard(zone = zone, onDelete = { onDeleteLocationZone(zone.id) })
            }
        }

        // Section 2: Wi-Fi Networks
        item { DashboardSectionHeader("Wi-Fi Networks") }
        if (silentSsids.isEmpty() && vibrateSsids.isEmpty() && normalSsids.isEmpty()) {
            item {
                MiniEmptyState(
                    icon = Icons.Default.Wifi,
                    title = "No Wi-Fi Zones",
                    subtitle = "Add a network to silence your phone automatically.",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else {
            items(silentSsids.toList()) { ssid ->
                ZoneItemCard(ssid = ssid, onDelete = { onDeleteSsid(ssid) })
            }
            items(vibrateSsids.toList()) { ssid ->
                ZoneItemCard(ssid = ssid, onDelete = { onDeleteSsid(ssid) })
            }
            items(normalSsids.toList()) { ssid ->
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (contacts.isEmpty()) {
            item {
                SafeEmptyState()
            }
        } else {
            items(contacts) { contact ->
                ImportantContactItemCard(contact = contact, onDelete = { onDeleteContact(contact.phoneNumber) })
            }
        }
    }
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
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun SafeEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        // Title + subtitle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Emergency Whitelist",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Your phone is on silent, but some calls shouldn't be blocked? Add them here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }


        // Hint
        Text(
            "👇  Press the + button below and choose a contact.",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun SafeHowItWorksRow(step: String, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                step,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }
    }
}

@Composable
fun SystemStatusSection(
    hasBackgroundLocation: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    onRequestBackgroundLocation: () -> Unit,
    onDisableBatteryOptimization: () -> Unit
) {
    if (!hasBackgroundLocation || !isIgnoringBatteryOptimizations) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardSectionHeader("System Health")
            
            if (!hasBackgroundLocation) {
                StatusCard(
                    title = "Background Location",
                    description = "Required to detect zones when app is closed.",
                    actionText = "Allow All The Time",
                    onClick = onRequestBackgroundLocation,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            if (!isIgnoringBatteryOptimizations) {
                StatusCard(
                    title = "Battery Optimization",
                    description = "System may kill SilentZone. Disable for 100% reliability.",
                    actionText = "Disable Optimization",
                    onClick = onDisableBatteryOptimization,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    description: String,
    actionText: String,
    onClick: () -> Unit,
    color: Color
) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.GppMaybe, 
                        null, 
                        tint = color, 
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.2f), contentColor = color),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(actionText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

sealed class RadiusSource {
    object CurrentLocation : RadiusSource()
}
