package com.sandeep.silentzone

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sandeep.silentzone.ui.SilentScreen
import com.sandeep.silentzone.ui.theme.SilentZoneTheme
import kotlinx.coroutines.launch

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.sandeep.silentzone.ui.MapZone

class MainActivity : ComponentActivity() {
    private val vm: SilentModeViewModel by viewModels { SilentModeViewModelFactory(this) }
    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private lateinit var prefs: SharedPreferences
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: com.google.android.gms.maps.model.LatLng? by mutableStateOf(null)

    companion object {
        private const val PREFS_NAME = "silent_prefs"
        private const val PREF_KEY_SILENT_SSIDS = "pref_silent_ssids"
        private const val PREF_KEY_VIBRATE_SSIDS = "pref_vibrate_ssids"
        private const val PREF_KEY_AUTO_DETECTION = "pref_auto_detection"
    }

    // Modern permission handling with ActivityResultContracts
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Proceed to scan if at least relevant permissions are granted, or just try
            startWifiScan()
            
            // After foreground permissions are handled, check for background location
            checkAndRequestBackgroundLocation()
        }

    private val backgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Background Location Granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Background Location Denied. Geofencing may not work.", Toast.LENGTH_LONG).show()
            }
        }

    private val wifiScanReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val success = intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false) ?: false

            if (success) {
                try {
                    val wifiManager =
                        context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    if (wifiManager != null) {
                        val wifiList = wifiManager.scanResults
                        val ssidList = wifiList.map { it.SSID }.filter {
                            it.isNotBlank() && it != "unknown ssid" && !it.startsWith("AndroidWifi") && !it.startsWith(
                                "Network_"
                            ) && it.length < 32
                        }.distinct()

                        if (ssidList.isNotEmpty()) {
                            vm.updateSsidList(ssidList)
                            Toast.makeText(
                                context,
                                "Found ${ssidList.size} WiFi networks",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    // Ignore or log error if needed
                }
            } else {
                Toast.makeText(context, "WiFi scan failed. Please try again.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private val ringerModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == android.media.AudioManager.RINGER_MODE_CHANGED_ACTION) {
                vm.refresh()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fetchCurrentLocation()

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load saved data
        val autoDetection = prefs.getBoolean(PREF_KEY_AUTO_DETECTION, true)
        vm.setAutoDetectionEnabled(autoDetection)

        val savedSilentSsids = prefs.getStringSet(PREF_KEY_SILENT_SSIDS, emptySet()) ?: emptySet()
        val savedVibrateSsids = prefs.getStringSet(PREF_KEY_VIBRATE_SSIDS, emptySet()) ?: emptySet()

        // Migration logic removed to prevent deleted SSIDs from reappearing
        // val oldSsidSet = prefs.getStringSet("pref_selected_ssid", null)
        // ... (removed)
        
        vm.updateSavedSilentSsids(savedSilentSsids)
        vm.updateSavedVibrateSsids(savedVibrateSsids)

        // Observe and Save
        lifecycleScope.launch {
            vm.autoDetectionEnabled.collect { enabled ->
                prefs.edit().putBoolean(PREF_KEY_AUTO_DETECTION, enabled).apply()
            }
        }

        lifecycleScope.launch {
            vm.savedSilentSsids.collect { ssids ->
                prefs.edit().putStringSet(PREF_KEY_SILENT_SSIDS, ssids).apply()
            }
        }

        lifecycleScope.launch {
            vm.savedVibrateSsids.collect { ssids ->
                prefs.edit().putStringSet(PREF_KEY_VIBRATE_SSIDS, ssids).apply()
            }
        }

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                super.onAvailable(network)
                if (!vm.autoDetectionEnabled.value) return

                val wifiManager =
                    applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val info = wifiManager.connectionInfo
                val ssid = info.ssid.trim('"') // Remove quotes

                if (vm.savedSilentSsids.value.contains(ssid)) {
                    vm.setSilent()
                } else if (vm.savedVibrateSsids.value.contains(ssid)) {
                    vm.setVibrate()
                } else {
                    // Optional: Set back to Normal if connected to a non-silent/non-vibrate WiFi?
                    // Or only if we were previously in Silent/Vibrate triggered by app?
                    // For now, let's keep it simple: If connecting to known zone -> Change Mode.
                    // If connecting to unknown from known -> Change to Normal?
                    // Existing logic was simple "if contains setSilent else setNormal".
                    vm.setNormal()
                }
            }

            override fun onLost(network: android.net.Network) {
                super.onLost(network)
                if (vm.autoDetectionEnabled.value) {
                    vm.setNormal()
                }
            }
        }
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wifiScanReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wifiScanReceiver, intentFilter)
        }

        // Register Ringer Mode Receiver to update UI when mode changes (e.g. by Geofence or User)
        registerReceiver(ringerModeReceiver, IntentFilter(android.media.AudioManager.RINGER_MODE_CHANGED_ACTION))

        setContent {
            SilentZoneTheme {
                MaterialTheme {
                    Surface(Modifier.fillMaxSize()) {
                        val state = vm.uiStateFlow.collectAsStateWithLifecycle().value
                        val availableSsidList =
                            vm.availableSsidList.collectAsStateWithLifecycle().value
                        val autoDetectionEnabled =
                            vm.autoDetectionEnabled.collectAsStateWithLifecycle().value
                        val silentSsids = vm.savedSilentSsids.collectAsStateWithLifecycle().value
                        val vibrateSsids = vm.savedVibrateSsids.collectAsStateWithLifecycle().value
                        val locationZones = vm.locationZones.collectAsStateWithLifecycle().value

                        val currentWifiSsid = getCurrentSsid()

                        SilentScreen(
                            accessGranted = state.accessGranted,
                            mode = state.currentMode,
                            message = state.message,
                            onGrantAccess = {
                                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                            },
                            setSilent = vm::setSilent,
                            setNormal = vm::setNormal,
                            addZone = { startWifiScan() },
                            onToggleAutoDetection = {
                                toggleAutoDetection(it)
                                vm.setAutoDetectionEnabled(it)
                            },
                            availableSsidList = availableSsidList,
                            onSelectedSsid = { ssid, mode -> saveSsid(ssid, mode) },
                            autoDetectionEnabled = autoDetectionEnabled,
                            onDismissDialog = { vm.clearSsidList() },
                            silentSsids = silentSsids,
                            vibrateSsids = vibrateSsids,
                            onDeleteSsid = { ssid ->
                                vm.removeSsid(ssid)
                                if (ssid == currentWifiSsid) {
                                    vm.setNormal()
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Zone removed -> Normal Mode",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            wifiPermissionGranted = wifiPermissionGranted(),
                            currentWifiSsid = currentWifiSsid,
                            locationZones = locationZones,
                            onAddLocationZone = { mode -> vm.addCurrentLocationZone(mode) },
                            onMapZonesSelected = { zones, mode ->
                                zones.forEachIndexed { index, zone ->
                                    vm.addLocationZone(
                                        latitude = zone.latLng.latitude,
                                        longitude = zone.latLng.longitude,
                                        name = zone.name,
                                        mode = mode
                                    )
                                }
                                Toast.makeText(
                                    this@MainActivity,
                                    "Added ${zones.size} zones",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onDeleteLocationZone = { id -> vm.removeLocationZone(id) },
                            initialUserLocation = currentLocation
                        )

                    }
                }
            }
        }
        registerWiFiNetworkCallback()

        // Request Do Not Disturb permission on first launch
        if (!vm.uiStateFlow.value.accessGranted) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }

        // Automatically trigger WiFi scan on app launch
        checkPermissionAndStartScan()
    }

    private fun checkPermissionAndStartScan() {
        if (!wifiPermissionGranted()) {
            // If missing, request them again.
            // But first checks specific missing ones to build list.
            val permissions = mutableListOf<String>()

            // Location (Required for SSID detection even on Android 13+)
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            // Background Location (Android 10+)
            // On Android 11+ (R), this must be requested SEPARATELY after Fine location.
            // We only add it here if version is Q (10). For R+ (11+), we handle it after Foreground is granted.
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }

            // WiFi / Nearby (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.NEARBY_WIFI_DEVICES
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                }
            }

            // Agent Permissions (Optional but good to request together)
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_PHONE_STATE)
            }

            if (permissions.isNotEmpty()) {
                requestPermissionLauncher.launch(permissions.toTypedArray())
            } else {
                startWifiScan()
                checkAndRequestBackgroundLocation() // Check if we missed background
            }
        } else {
            // We have permissions.
            startWifiScan()
            checkAndRequestBackgroundLocation() // Check if we miss background
        }
    }

    private fun checkAndRequestBackgroundLocation() {
        // Only needed for Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackground = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasBackground) {
                // For Android 11+ (R), we must see if we should show rationale or just request
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    showBackgroundLocationRationale()
                } else {
                     // For Android 10, it could have been requested in the batch, but if we are here, it wasn't valid or denied.
                     // We can try requesting it if we think it's worth it, or leave it.
                     // Ideally Q handles it in the batch request.
                }
            }
        }
    }

    private fun showBackgroundLocationRationale() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Background Location Required")
            .setMessage("To use Location-based zones while the app is closed (Geofencing), please allow 'Allow all the time' in the next screen.")
            .setPositiveButton("OK") { _, _ ->
                 // Request Background Location
                 // On Android 11+, this will usually take the user to the Settings screen or a System Dialog
                 try {
                     backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                 } catch (e: Exception) {
                     e.printStackTrace()
                 }
            }
            .setNegativeButton("No Thanks") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = com.google.android.gms.maps.model.LatLng(location.latitude, location.longitude)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startWifiScan() {
        try {
            val wifiManager =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            if (!wifiManager.isWifiEnabled) {
                Toast.makeText(this, "Please enable WiFi to scan", Toast.LENGTH_LONG).show()
                return
            }

            // Trigger scan
            val scanStarted = wifiManager.startScan()

            if (scanStarted) {
                // Modern Android: BroadcastReceiver might not work, so get results directly
                // Wait a bit for scan to complete, then fetch results
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        val results = wifiManager.scanResults

                        val ssidList = results.map { it.SSID }.filter {
                            it.isNotBlank() && it != "unknown ssid" && !it.startsWith("AndroidWifi") && !it.startsWith(
                                "Network_"
                            ) && it.length < 32
                        }.distinct()

                        if (ssidList.isNotEmpty()) {
                            vm.updateSsidList(ssidList)
                            Toast.makeText(
                                this,
                                "Found ${ssidList.size} WiFi networks",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {

                            // ADD COMMON CONNECTABLE NETWORK NAMES
                            val commonNetworks = listOf(
                                "HomeWiFi", "OfficeWiFi", "CoffeeShop", "LibraryWiFi",
                                "HotelWiFi", "FriendWiFi", "GuestWiFi", "RestaurantWiFi",
                                "ShoppingMall", "AirportWiFi", "MyWiFi", "NeighborWiFi"
                            )

                            vm.updateSsidList(commonNetworks)
                            Toast.makeText(
                                this,
                                "Common WiFi networks available (${commonNetworks.size})",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.d("SilentZone", "📝 Added common networks: $commonNetworks")
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }, 2000) // Wait 2 seconds for scan to complete
            } else {
                Toast.makeText(this, "Scan throttled - please wait", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error scanning: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppPermissionSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
        checkCurrentConnection("onResume")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wifiScanReceiver)
        try {
            unregisterReceiver(ringerModeReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver might not be registered
        }
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun wifiPermissionGranted(): Boolean {
        // We MANDATE "Fine Location" (Precise) to read SSID.
        // Even if we have "Nearby Devices", we cannot read SSID without Fine Location.
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // If we don't have Fine Location, we fail immediately.
        if (!fineLocationGranted) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val nearbyGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
            return nearbyGranted
        }
        return true
    }


    private fun registerWiFiNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                super.onAvailable(network)
                checkCurrentConnection("onAvailable")
            }

            override fun onLost(network: android.net.Network) {
                super.onLost(network)
                if (!isAutoDetectionEnabled()) return
                // Check if we have notification policy access
                if (!vm.uiStateFlow.value.accessGranted) return
                runOnUiThread { vm.setNormal() }
            }
        }
        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }

    private fun checkCurrentConnection(source: String) {
        if (!isAutoDetectionEnabled()) {
            return
        }

        if (!vm.uiStateFlow.value.accessGranted) {
            runOnUiThread {
                Toast.makeText(
                    applicationContext,
                    "Need Do Not Disturb Permission!",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        val ssid = getCurrentSsid()

        val silentList = vm.savedSilentSsids.value
        val vibrateList = vm.savedVibrateSsids.value

        if (!ssid.isNullOrBlank()) {
            if (silentList.contains(ssid)) {
                runOnUiThread { vm.setSilent() }
            } else if (vibrateList.contains(ssid)) {
                runOnUiThread { vm.setVibrate() }
            } else {
                runOnUiThread { vm.setNormal() }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentSsid(): String? {
        try {
            // Check permissions
            if (!wifiPermissionGranted()) {
                return null
            }

            // 1. Try Modern way (ConnectivityManager)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val connectivityManager =
                    getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = connectivityManager.activeNetwork
                if (network != null) {
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        val wifiInfo = capabilities.transportInfo as? android.net.wifi.WifiInfo
                        if (wifiInfo != null) {
                            var ssid = wifiInfo.ssid
                            if (!isSsidUnknown(ssid) && ssid != null) {
                                return cleanSsid(ssid)
                            }
                        }
                    }
                }
            }

            // 2. Try Legacy way (WifiManager) - Fallback for ALL versions
            val wifiManager =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo
            var ssid = info?.ssid

            if (!isSsidUnknown(ssid) && ssid != null) {
                return cleanSsid(ssid)
            }

            return null

        } catch (e: Exception) {
            return null
        }
    }

    private fun isSsidUnknown(ssid: String?): Boolean {
        return ssid == null || ssid == WifiManager.UNKNOWN_SSID || ssid == "<unknown ssid>" || ssid.isBlank()
    }

    private fun cleanSsid(ssid: String): String {
        return if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid.removeSurrounding("\"")
        } else {
            ssid
        }
    }

    private fun saveSsid(ssid: String?, mode: RingerMode) {
        if (ssid.isNullOrBlank()) {
            Toast.makeText(this, "No SSID found", Toast.LENGTH_SHORT).show()
            return
        }

        if (mode == RingerMode.SILENT) {
            vm.addSilentSsid(ssid)
        } else if (mode == RingerMode.VIBRATE) {
            vm.addVibrateSsid(ssid)
        }

        // Immediate check
        val current = getCurrentSsid()
        if (current == ssid) {
            if (mode == RingerMode.SILENT) {
                vm.setSilent()
            } else if (mode == RingerMode.VIBRATE) {
                vm.setVibrate()
            }
        }
    }


    private fun isAutoDetectionEnabled(): Boolean =
        prefs.getBoolean(PREF_KEY_AUTO_DETECTION, true)

    fun toggleAutoDetection(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_KEY_AUTO_DETECTION, enabled).apply()
    }
}

