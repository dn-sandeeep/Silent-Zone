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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.sandeep.silentzone.ui.SilentScreen
import com.sandeep.silentzone.ui.theme.SilentZoneTheme
import kotlinx.coroutines.launch

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
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            startWifiScan()
            checkAndRequestBackgroundLocation()
        }

    private val backgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Background Location Granted!", Toast.LENGTH_SHORT).show()
            }
        }

    private val wifiScanReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val success = intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false) ?: false
            if (success) {
                val wifiManager = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                wifiManager?.let {
                    val ssidList = it.scanResults.map { result -> result.SSID }.filter { ssid ->
                        ssid.isNotBlank() && ssid != WifiManager.UNKNOWN_SSID
                    }.distinct()
                    vm.updateSsidList(ssidList)
                }
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

        val savedSilentSsids = prefs.getStringSet(PREF_KEY_SILENT_SSIDS, emptySet()) ?: emptySet()
        val savedVibrateSsids = prefs.getStringSet(PREF_KEY_VIBRATE_SSIDS, emptySet()) ?: emptySet()

        vm.updateSavedSilentSsids(savedSilentSsids)
        vm.updateSavedVibrateSsids(savedVibrateSsids)

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
        
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wifiScanReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wifiScanReceiver, intentFilter)
        }

        registerReceiver(ringerModeReceiver, IntentFilter(android.media.AudioManager.RINGER_MODE_CHANGED_ACTION))

        setContent {
            SilentZoneTheme {
                MaterialTheme {
                    Surface(Modifier.fillMaxSize()) {
                        val state = vm.uiStateFlow.collectAsStateWithLifecycle().value
                        val availableSsidList = vm.availableSsidList.collectAsStateWithLifecycle().value
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
                            addZone = { checkPermissionAndStartScan() },
                            availableSsidList = availableSsidList,
                            onSelectedSsid = { ssid, mode -> saveSsid(ssid, mode) },
                            onDismissDialog = { vm.clearSsidList() },
                            silentSsids = silentSsids,
                            vibrateSsids = vibrateSsids,
                            onDeleteSsid = { ssid ->
                                vm.removeSsid(ssid)
                                if (ssid == currentWifiSsid) vm.setNormal()
                            },
                            wifiPermissionGranted = wifiPermissionGranted(),
                            currentWifiSsid = currentWifiSsid,
                            locationZones = locationZones,
                            onAddLocationZone = { mode -> vm.addCurrentLocationZone(mode) },
                            onMapZonesSelected = { zones, mode ->
                                zones.forEach { zone ->
                                    vm.addLocationZone(zone.latLng.latitude, zone.latLng.longitude, zone.name, mode)
                                }
                            },
                            onDeleteLocationZone = { id -> vm.removeLocationZone(id) },
                            initialUserLocation = currentLocation
                        )
                    }
                }
            }
        }
    }

    private fun checkPermissionAndStartScan() {
        if (!wifiPermissionGranted()) {
            val permissions = mutableListOf<String>()
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            startWifiScan()
            checkAndRequestBackgroundLocation()
        }
    }

    private fun checkAndRequestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackground = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasBackground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { currentLocation = com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude) }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startWifiScan() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager.isWifiEnabled) {
            wifiManager.startScan()
        }
    }

    private fun wifiPermissionGranted(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineLocation) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun registerWiFiNetworkCallback() {
        val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                checkCurrentConnection()
            }
            override fun onLost(network: android.net.Network) {
                if (vm.uiStateFlow.value.accessGranted) runOnUiThread { vm.setNormal() }
            }
        }
        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }

    private fun checkCurrentConnection() {
        if (!vm.uiStateFlow.value.accessGranted) return
        val ssid = getCurrentSsid() ?: return
        runOnUiThread {
            when {
                vm.savedSilentSsids.value.contains(ssid) -> vm.setSilent()
                vm.savedVibrateSsids.value.contains(ssid) -> vm.setVibrate()
                else -> vm.setNormal()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentSsid(): String? {
        if (!wifiPermissionGranted()) return null
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo
            if (info.ssid == WifiManager.UNKNOWN_SSID) null
            else info.ssid.trim('"')
        } catch (e: Exception) {
            null
        }
    }

    private fun saveSsid(ssid: String?, mode: RingerMode) {
        ssid?.let {
            if (mode == RingerMode.SILENT) vm.addSilentSsid(it)
            else if (mode == RingerMode.VIBRATE) vm.addVibrateSsid(it)
            checkCurrentConnection()
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
        if (wifiPermissionGranted()) {
            registerWiFiNetworkCallback()
            checkCurrentConnection()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wifiScanReceiver)
        try { unregisterReceiver(ringerModeReceiver) } catch (e: Exception) {}
        networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
    }
}
