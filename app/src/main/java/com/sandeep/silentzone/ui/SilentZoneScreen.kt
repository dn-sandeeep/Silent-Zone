package com.sandeep.silentzone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.sandeep.silentzone.ImportantContact
import com.sandeep.silentzone.LocationZone
import com.sandeep.silentzone.RingerMode
import com.sandeep.silentzone.utils.FeedbackUtils

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
    onUpdateLocationZone: (LocationZone) -> Unit,
    onUpdateWifiZoneMode: (String, RingerMode) -> Unit,
    initialUserLocation: LatLng?,
    importantContacts: List<ImportantContact>,
    onPickContact: () -> Unit,
    onDeleteContact: (String) -> Unit,
    onRequestPermission: (() -> Unit) -> Unit,
    onRequestBackgroundLocation: () -> Unit,
    onDisableBatteryOptimization: () -> Unit,
    onNavigateToZones: () -> Unit = {},
    onLogClickCreateZone: () -> Unit = {},
    onCompleteUpdate: () -> Unit = {},
    updateReadyToInstall: Boolean = false,
    hasForegroundLocation: Boolean,
    hasBackgroundLocation: Boolean,
    hasWifiAutomationPermission: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    zoneCount: Int = 0,
    contactCount: Int = 0,
    dailyPeacefulTime: Long = 0,
    lifetimePeacefulTime: Long = 0,
    activeSession: com.sandeep.silentzone.AnalyticsEvent? = null,
    recentAnalytics: List<com.sandeep.silentzone.AnalyticsEvent> = emptyList(),
    batteryUsage: com.sandeep.silentzone.BatteryUsage =
        com.sandeep.silentzone.BatteryUsage(0.0, 0.0, 0.0, 0.0)
) {
    var selectedScreen by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedScreen) {
        if (selectedScreen == 1) {
            onNavigateToZones()
        }
    }

    BackHandler(enabled = selectedScreen != 0) { selectedScreen = 0 }

    var showMapSelection by remember { mutableStateOf(false) }
    var showAddTypeDialog by remember { mutableStateOf(false) }
    var showLocationAddOptions by remember { mutableStateOf(false) }
    var pendingSsid by remember { mutableStateOf<String?>(null) }
    var pendingCurrentLocationRadius by remember { mutableStateOf<Float?>(null) }
    var pendingMapZones by remember { mutableStateOf<List<MapZone>?>(null) }
    var editingWifiSsid by remember { mutableStateOf<String?>(null) }
    var editingLocationModeZone by remember { mutableStateOf<LocationZone?>(null) }
    var editingLocationRadiusZone by remember { mutableStateOf<LocationZone?>(null) }
    var showWifiSelection by remember { mutableStateOf(false) }
    var showRadiusDialog by remember { mutableStateOf(false) }
    var radiusSource by remember { mutableStateOf<RadiusSource?>(null) }
    var showBatteryDetails by remember { mutableStateOf(false) }
    var showActivityHistory by remember { mutableStateOf(false) }
    var showDndPermissionDialog by remember { mutableStateOf(false) }

    fun requestSilentPermissionOrRun(modeToSave: RingerMode, action: () -> Unit) {
        if (modeToSave == RingerMode.SILENT && !accessGranted) {
            showDndPermissionDialog = true
        } else {
            action()
        }
    }

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(updateReadyToInstall) {
        if (updateReadyToInstall) {
            val result =
                snackbarHostState.showSnackbar(
                    message = "An update has just been downloaded.",
                    actionLabel = "RESTART",
                    duration = androidx.compose.material3.SnackbarDuration.Indefinite
                )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                onCompleteUpdate()
            }
        }
    }

    if (showMapSelection) {
        val startLocation = initialUserLocation ?: LatLng(20.5937, 78.9629)
        MapSelectionScreen(
            initialLocation = startLocation,
            onZonesSelected = { zones ->
                pendingMapZones = zones
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
                            text =
                                when (selectedScreen) {
                                    0 -> "Home"
                                    1 -> "Zones"
                                    2 -> "Contacts"
                                    3 -> "Feedback"
                                    else -> "SilentZone"
                                },
                            style =
                                MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                )
                        )
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor =
                                MaterialTheme.colorScheme.surface.copy(
                                    alpha = 0.8f
                                ),
                            titleContentColor =
                                MaterialTheme.colorScheme.onBackground,
                            actionIconContentColor =
                                MaterialTheme.colorScheme.onBackground.copy(
                                    alpha = 0.7f
                                ),
                            navigationIconContentColor =
                                MaterialTheme.colorScheme.onBackground
                        ),
                    actions = {
                        val context = LocalContext.current
                        IconButton(onClick = { FeedbackUtils.shareApp(context) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share App",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    border =
                        BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                ) {
                    NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                        NavigationBarItem(
                            selected = selectedScreen == 0,
                            onClick = { selectedScreen = 0 },
                            icon = { Icon(Icons.Default.Home, null) },
                            label = { Text("Home", fontWeight = FontWeight.Bold) },
                            colors =
                                NavigationBarItemDefaults.colors(
                                    selectedIconColor =
                                        MaterialTheme.colorScheme.primary,
                                    selectedTextColor =
                                        MaterialTheme.colorScheme.primary,
                                    unselectedIconColor =
                                        MaterialTheme.colorScheme.onSurface
                                            .copy(alpha = 0.4f),
                                    unselectedTextColor =
                                        MaterialTheme.colorScheme.onSurface
                                            .copy(alpha = 0.4f),
                                    indicatorColor =
                                        MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.1f
                                        )
                                )
                        )
                        NavigationBarItem(
                            selected = selectedScreen == 1,
                            onClick = { selectedScreen = 1 },
                            icon = { Icon(Icons.Default.GridView, null) },
                            label = { Text("Zones", fontWeight = FontWeight.Bold) },
                            colors =
                                NavigationBarItemDefaults.colors(
                                    selectedIconColor =
                                        MaterialTheme.colorScheme.secondary,
                                    selectedTextColor =
                                        MaterialTheme.colorScheme.secondary,
                                    unselectedIconColor =
                                        MaterialTheme.colorScheme.onSurface
                                            .copy(alpha = 0.4f),
                                    unselectedTextColor =
                                        MaterialTheme.colorScheme.onSurface
                                            .copy(alpha = 0.4f),
                                    indicatorColor =
                                        MaterialTheme.colorScheme.secondary
                                            .copy(alpha = 0.1f)
                                )
                        )
                        NavigationBarItem(
                            selected = selectedScreen == 2,
                            onClick = { selectedScreen = 2 },
                            icon = { Icon(Icons.Default.Phone, null) },
                            label = { Text("Contacts", fontWeight = FontWeight.Bold) },
                            colors =
                                NavigationBarItemDefaults.colors(
                                    selectedIconColor =
                                        MaterialTheme.colorScheme.primary,
                                    selectedTextColor =
                                        MaterialTheme.colorScheme.primary,
                                    unselectedIconColor =
                                        MaterialTheme.colorScheme.onSurface
                                            .copy(alpha = 0.4f),
                                    unselectedTextColor =
                                        MaterialTheme.colorScheme.onSurface
                                            .copy(alpha = 0.4f),
                                    indicatorColor =
                                        MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.1f
                                        )
                                )
                        )
                        NavigationBarItem(
                            selected = selectedScreen == 3,
                            onClick = { selectedScreen = 3 },
                            icon = { Icon(Icons.Default.Feedback, null) },
                            label = { Text("Feedback", fontWeight = FontWeight.Bold) },
                            colors =
                                NavigationBarItemDefaults.colors(
                                    selectedIconColor =
                                        MaterialTheme.colorScheme.tertiary,
                                    selectedTextColor =
                                        MaterialTheme.colorScheme.tertiary,
                                    unselectedIconColor =
                                        MaterialTheme.colorScheme.onSurface
                                            .copy(alpha = 0.4f),
                                    unselectedTextColor =
                                        MaterialTheme.colorScheme.onSurface
                                            .copy(alpha = 0.4f),
                                    indicatorColor =
                                        MaterialTheme.colorScheme.tertiary.copy(
                                            alpha = 0.1f
                                        )
                                )
                        )
                    }
                }
            },
            floatingActionButton = {
                if (selectedScreen == 1 || selectedScreen == 2) {
                    FloatingActionButton(
                        onClick = {
                            if (selectedScreen == 1) {
                                onLogClickCreateZone()
                                showAddTypeDialog = true
                            } else {
                                onPickContact()
                            }
                        },
                        containerColor =
                            if (selectedScreen == 1) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(20.dp)
                    ) { Icon(Icons.Default.Add, contentDescription = "Add") }
                }
            },
            snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
            ) {
                AnimatedContent(
                    targetState = selectedScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { target ->
                    when (target) {
                        0 ->
                            DashboardScreen(
                                contentPadding = innerPadding,
                                accessGranted = accessGranted,
                                mode = mode,
                                isFallback = isFallback,
                                onGrantAccess = onGrantAccess,
                                setSilent = setSilent,
                                setVibrate = setVibrate,
                                setNormal = setNormal,
                                currentWifiSsid = currentWifiSsid,
                                hasBackgroundLocation = hasBackgroundLocation,
                                isIgnoringBatteryOptimizations =
                                    isIgnoringBatteryOptimizations,
                                onRequestBackgroundLocation = onRequestBackgroundLocation,
                                onDisableBatteryOptimization = onDisableBatteryOptimization,
                                zoneCount = zoneCount,
                                contactCount = contactCount,
                                dailyPeacefulTime = dailyPeacefulTime,
                                lifetimePeacefulTime = lifetimePeacefulTime,
                                activeSession = activeSession,
                                recentAnalytics = recentAnalytics,
                                batteryUsage = batteryUsage,
                                onNavigateToZones = { selectedScreen = 1 },
                                onNavigateToWhitelist = { selectedScreen = 2 },
                                onShowBatteryDetails = { showBatteryDetails = true },
                                onViewAll = { showActivityHistory = true }
                            )

                        1 ->
                            ZonesScreen(
                                contentPadding = innerPadding,
                                silentSsids = silentSsids,
                                vibrateSsids = vibrateSsids,
                                normalSsids = normalSsids,
                                locationZones = locationZones,
                                isLocationAutomationPaused =
                                    !hasForegroundLocation || !hasBackgroundLocation,
                                isWifiAutomationPaused = !hasWifiAutomationPermission,
                                onDeleteSsid = onDeleteSsid,
                                onDeleteLocationZone = onDeleteLocationZone,
                                onEditLocationMode = { zone -> editingLocationModeZone = zone },
                                onEditLocationRadius = { zone -> editingLocationRadiusZone = zone },
                                onEditWifiMode = { ssid -> editingWifiSsid = ssid },
                                onAddLocationFromEmpty = { showLocationAddOptions = true },
                                onAddWifiFromEmpty = {
                                    onRequestPermission {
                                        addZone()
                                        showWifiSelection = true
                                    }
                                }
                            )

                        2 ->
                            ContactsScreen(
                                contentPadding = innerPadding,
                                contacts = importantContacts,
                                onDeleteContact = onDeleteContact
                            )

                        3 -> FeedbackScreen(contentPadding = innerPadding)
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
                onSelectMap = {
                    showMapSelection = true
                    showAddTypeDialog = false
                },
                onWifi = {
                    addZone()
                    showWifiSelection = true
                    showAddTypeDialog = false
                },
                onDismiss = { showAddTypeDialog = false },
                onRequestPermission = onRequestPermission
            )
        }

        if (showLocationAddOptions) {
            LocationAddOptionsBottomSheet(
                onCurrentLocation = {
                    radiusSource = RadiusSource.CurrentLocation
                    showRadiusDialog = true
                    showLocationAddOptions = false
                },
                onSelectMap = {
                    showMapSelection = true
                    showLocationAddOptions = false
                },
                onDismiss = { showLocationAddOptions = false },
                onRequestPermission = onRequestPermission
            )
        }

        if (showRadiusDialog) {
            RadiusSelectionBottomSheet(
                onRadiusSelected = { radius ->
                    if (radiusSource == RadiusSource.CurrentLocation) {
                        pendingCurrentLocationRadius = radius
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

        if (editingLocationRadiusZone != null) {
            val zone = editingLocationRadiusZone!!
            RadiusSelectionBottomSheet(
                initialRadius = zone.radius,
                onRadiusSelected = { radius ->
                    onUpdateLocationZone(zone.copy(radius = radius))
                    editingLocationRadiusZone = null
                },
                onDismiss = { editingLocationRadiusZone = null }
            )
        }

        if (pendingCurrentLocationRadius != null) {
            val radius = pendingCurrentLocationRadius!!
            ModeSelectionBottomSheet(
                targetName = "Current Location",
                onModeSelected = { selectedMode ->
                    requestSilentPermissionOrRun(selectedMode) {
                        onAddLocationZone(selectedMode, radius)
                        pendingCurrentLocationRadius = null
                    }
                },
                onDismiss = { pendingCurrentLocationRadius = null }
            )
        }

        if (pendingMapZones != null) {
            val zones = pendingMapZones!!
            ModeSelectionBottomSheet(
                targetName = if (zones.size == 1) zones.first().name else "${zones.size} Locations",
                onModeSelected = { selectedMode ->
                    requestSilentPermissionOrRun(selectedMode) {
                        onMapZonesSelected(zones, selectedMode)
                        pendingMapZones = null
                    }
                },
                onDismiss = { pendingMapZones = null }
            )
        }

        if (showWifiSelection) {
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
                targetName = pendingSsid!!,
                onModeSelected = { m ->
                    val ssid = pendingSsid!!
                    requestSilentPermissionOrRun(m) {
                        onSelectedSsid(ssid, m)
                        pendingSsid = null
                    }
                },
                onDismiss = { pendingSsid = null }
            )
        }

        if (editingWifiSsid != null) {
            ModeSelectionBottomSheet(
                targetName = editingWifiSsid!!,
                onModeSelected = { mode ->
                    val ssid = editingWifiSsid!!
                    requestSilentPermissionOrRun(mode) {
                        onUpdateWifiZoneMode(ssid, mode)
                        editingWifiSsid = null
                    }
                },
                onDismiss = { editingWifiSsid = null }
            )
        }

        if (editingLocationModeZone != null) {
            val zone = editingLocationModeZone!!
            ModeSelectionBottomSheet(
                targetName = zone.name,
                onModeSelected = { selectedMode ->
                    requestSilentPermissionOrRun(selectedMode) {
                        onUpdateLocationZone(zone.copy(mode = selectedMode))
                        editingLocationModeZone = null
                    }
                },
                onDismiss = { editingLocationModeZone = null }
            )
        }

        if (showBatteryDetails) {
            BatteryDetailsBottomSheet(
                usage = batteryUsage,
                onDismiss = { showBatteryDetails = false }
            )
        }

        if (showActivityHistory) {
            ActivityHistoryBottomSheet(
                events = recentAnalytics,
                onDismiss = { showActivityHistory = false }
            )
        }

        if (showDndPermissionDialog) {
            DndPermissionRequiredDialog(
                onCancel = { showDndPermissionDialog = false },
                onProceed = {
                    showDndPermissionDialog = false
                    onGrantAccess()
                }
            )
        }
    }
}

@Composable
fun DashboardScreen(
    contentPadding: PaddingValues,
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
    contactCount: Int,
    dailyPeacefulTime: Long,
    lifetimePeacefulTime: Long,
    activeSession: com.sandeep.silentzone.AnalyticsEvent?,
    recentAnalytics: List<com.sandeep.silentzone.AnalyticsEvent>,
    batteryUsage: com.sandeep.silentzone.BatteryUsage,
    onNavigateToZones: () -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onShowBatteryDetails: () -> Unit,
    onViewAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(contentPadding.calculateTopPadding()))
        
        PulseStatusHeader(mode = mode, isFallback = isFallback)

        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            // Stats Bubbles (Always visible at the top)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    StatBubble(
                        label = "Zones",
                        value = zoneCount.toString(),
                        icon = Icons.Default.Map,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToZones
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    StatBubble(
                        label = "Contacts",
                        value = contactCount.toString(),
                        icon = Icons.Default.Contacts,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = onNavigateToWhitelist
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    StatBubble(
                        label = "Battery",
                        value = "${"%.3f".format(batteryUsage.totalImpact)}%",
                        icon = Icons.Default.Battery5Bar,
                        color = MaterialTheme.colorScheme.tertiary,
                        onClick = onShowBatteryDetails
                    )
                }
            }

            if (zoneCount == 0) {
                // --- NEW USER ONBOARDING ---
                OnboardingCard(onNavigateToZones = onNavigateToZones)
            }
            
            // --- ADVANCED DASHBOARD ---
            AnalyticsSummaryCard(
                dailyTotalMillis = dailyPeacefulTime,
                lifetimeTotalMillis = lifetimePeacefulTime,
                activeSession = activeSession
            )

            // --- COMMON SETUP CHECKLIST ---
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                DashboardSectionHeader("Setup Permissions")
                GlassCard {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SetupStepItem(
                            title = "Do Not Disturb Access",
                            subtitle = "Required to mute sounds",
                            onClick = onGrantAccess,
                            color = MaterialTheme.colorScheme.error,
                            rationale =
                                "To enable complete silence, Do Not Disturb access is essential. Without this permission, SilentZone can only switch your device to Vibrate mode, but cannot fully silent your phone.",
                            isGranted = accessGranted
                        )
                        SetupStepItem(
                            title = "Background Location",
                            subtitle = "Required to detect zones",
                            onClick = onRequestBackgroundLocation,
                            color = MaterialTheme.colorScheme.primary,
                            rationale =
                                "Continuous location access is required to detect geofences reliably in the background. Without 'Allow all the time' permission, automated location-based silencing will fail when the app is minimized.",
                            isGranted = hasBackgroundLocation
                        )
                        SetupStepItem(
                            title = "Battery Optimization",
                            subtitle = "Disable for 100% reliability",
                            onClick = onDisableBatteryOptimization,
                            color = MaterialTheme.colorScheme.tertiary,
                            rationale =
                                "Android's power management may terminate background services to save battery. Disabling battery optimization for SilentZone guarantees uninterrupted automation and reliable zone detection.",
                            isGranted = isIgnoringBatteryOptimizations
                        )
                    }
                }
            }

            // --- COMMON CONTROLS (Moved to bottom) ---
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                DashboardSectionHeader("Manual Override")
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
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    DashboardSectionHeader("Current Connection")
                    GlassCard {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .background(
                                            MaterialTheme.colorScheme.secondary
                                                .copy(alpha = 0.1f),
                                            CircleShape
                                        ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Wifi,
                                    null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Connected to Wi-Fi",
                                    style = MaterialTheme.typography.labelMedium,
                                    color =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.5f
                                        )
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
            RecentActivityList(events = recentAnalytics, onViewAll = onViewAll)
        }
        
        Spacer(modifier = Modifier.height(contentPadding.calculateBottomPadding() + 20.dp))
    }
}

@Composable
fun ZonesScreen(
    contentPadding: PaddingValues,
    silentSsids: Set<String>,
    vibrateSsids: Set<String>,
    normalSsids: Set<String>,
    locationZones: List<LocationZone>,
    isLocationAutomationPaused: Boolean,
    isWifiAutomationPaused: Boolean,
    onDeleteSsid: (String) -> Unit,
    onDeleteLocationZone: (String) -> Unit,
    onEditLocationMode: (LocationZone) -> Unit,
    onEditLocationRadius: (LocationZone) -> Unit,
    onEditWifiMode: (String) -> Unit,
    onAddLocationFromEmpty: () -> Unit,
    onAddWifiFromEmpty: () -> Unit
) {
    var pendingLocationDelete by remember { mutableStateOf<LocationZone?>(null) }
    var pendingWifiDelete by remember { mutableStateOf<String?>(null) }

    pendingLocationDelete?.let { zone ->
        ConfirmDeleteZoneDialog(
            message =
                "Delete \"${zone.name}\"? This zone will no longer change your phone mode automatically.",
            onCancel = { pendingLocationDelete = null },
            onDelete = {
                onDeleteLocationZone(zone.id)
                pendingLocationDelete = null
            }
        )
    }

    pendingWifiDelete?.let { ssid ->
        ConfirmDeleteZoneDialog(
            message =
                "Delete \"$ssid\"? This Wi-Fi network will no longer change your phone mode automatically.",
            onCancel = { pendingWifiDelete = null },
            onDelete = {
                onDeleteSsid(ssid)
                pendingWifiDelete = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 100.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Section 1: Geofence Areas
        item { DashboardSectionHeader("Location Areas") }
        if (isLocationAutomationPaused) {
            item {
                SectionPauseMessage("Paused: Background location permission required")
            }
        }
        if (locationZones.isEmpty()) {
            item {
                MiniEmptyState(
                    icon = Icons.Default.LocationOn,
                    title = "No Locations Added",
                    subtitle = "Tap here to create your first geofence.",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onAddLocationFromEmpty
                )
            }
        } else {
            items(locationZones) { zone ->
                LocationZoneItemCard(
                    zone = zone,
                    onDelete = { pendingLocationDelete = zone },
                    onEditMode = { onEditLocationMode(zone) },
                    onEditRadius = { onEditLocationRadius(zone) }
                )
            }
        }

        // Section 2: Wi-Fi Networks
        item { DashboardSectionHeader("Wi-Fi Networks") }
        if (isWifiAutomationPaused) {
            item {
                SectionPauseMessage("Paused: Location/Nearby Wi-Fi permission required")
            }
        }
        if (silentSsids.isEmpty() && vibrateSsids.isEmpty() && normalSsids.isEmpty()) {
            item {
                MiniEmptyState(
                    icon = Icons.Default.Wifi,
                    title = "No Wi-Fi Zones",
                    subtitle = "Tap here to choose a Wi-Fi network.",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onAddWifiFromEmpty
                )
            }
        } else {
            items(silentSsids.toList()) { ssid ->
                ZoneItemCard(
                    ssid = ssid,
                    mode = RingerMode.SILENT,
                    onDelete = { pendingWifiDelete = ssid },
                    onEditMode = { onEditWifiMode(ssid) }
                )
            }
            items(vibrateSsids.toList()) { ssid ->
                ZoneItemCard(
                    ssid = ssid,
                    mode = RingerMode.VIBRATE,
                    onDelete = { pendingWifiDelete = ssid },
                    onEditMode = { onEditWifiMode(ssid) }
                )
            }
            items(normalSsids.toList()) { ssid ->
                ZoneItemCard(
                    ssid = ssid,
                    mode = RingerMode.NORMAL,
                    onDelete = { pendingWifiDelete = ssid },
                    onEditMode = { onEditWifiMode(ssid) }
                )
            }
        }
    }
}

@Composable
private fun ConfirmDeleteZoneDialog(
    message: String,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                "Delete zone?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = onDelete,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun SectionPauseMessage(text: String) {
    GlassCard(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.GppMaybe,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ContactsScreen(
    contentPadding: PaddingValues,
    contacts: List<ImportantContact>,
    onDeleteContact: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 100.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (contacts.isEmpty()) {
            item { SafeEmptyState() }
        } else {
            items(contacts) { contact ->
                ImportantContactItemCard(
                    contact = contact,
                    onDelete = { onDeleteContact(contact.phoneNumber) }
                )
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
            modifier =
                Modifier
                    .size(100.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        CircleShape
                    ),
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
            modifier =
                Modifier
                    .size(96.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Contacts,
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
                "Important Contacts",
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
            modifier =
                Modifier
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        CircleShape
                    ),
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
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
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
                    modifier =
                        Modifier
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
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = color.copy(alpha = 0.2f),
                        contentColor = color
                    ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) { Text(actionText, fontWeight = FontWeight.Bold) }
        }
    }
}

sealed class RadiusSource {
    object CurrentLocation : RadiusSource()
}

@Composable
fun FeedbackScreen(contentPadding: PaddingValues) {
    var rating by remember { mutableIntStateOf(5) }
    var selectedCategory by remember { mutableStateOf("General") }
    var message by remember { mutableStateOf("") }
    val context = LocalContext.current

    val categories =
        listOf(
            "General" to Icons.Default.Feedback,
            "Bug" to Icons.Default.BugReport,
            "Feature" to Icons.Default.Lightbulb,
            "Support" to Icons.Default.QuestionAnswer
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(contentPadding.calculateTopPadding() + 8.dp))

        // Header Illustration/Icon
        Box(
            modifier =
                Modifier
                    .size(120.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        CircleShape
                    ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Feedback,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "How's your experience?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Your feedback helps us make Silent Zone better.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        // Message Input
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardSectionHeader("Message")
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp),
                placeholder = {
                    Text(
                        "Tell us what's on your mind...",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedBorderColor =
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor =
                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.3f
                            ),
                        unfocusedContainerColor =
                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.3f
                            )
                    )
            )
        }

        // Submit Button
        Button(
            onClick = {
                FeedbackUtils.sendFeedback(context, rating, selectedCategory, message)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                "Send Feedback",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Send, null, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.height(contentPadding.calculateBottomPadding() + 40.dp))
    }
}
