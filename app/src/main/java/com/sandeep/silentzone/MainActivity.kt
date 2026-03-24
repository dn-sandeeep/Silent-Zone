package com.sandeep.silentzone

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.sandeep.silentzone.ui.SilentScreen
import com.sandeep.silentzone.ui.theme.SilentZoneTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: SilentModeViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: com.google.android.gms.maps.model.LatLng? by mutableStateOf(null)

    private val pickContactLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val contactUri = result.data?.data
            if (contactUri != null) {
                processPickedContact(contactUri)
            }
        }
    }

    private val contactPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] ?: (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
        val callLogGranted = permissions[Manifest.permission.READ_CALL_LOG] ?: (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED)
        val phoneStateGranted = permissions[Manifest.permission.READ_PHONE_STATE] ?: (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)

        if (contactsGranted && callLogGranted && phoneStateGranted) {
            openContactPicker()
        } else {
            Toast.makeText(this, "Whitelist feature requires all permissions to work.", Toast.LENGTH_LONG).show()
        }
    }

    private fun openContactPicker() {
        try {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            pickContactLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to open contact picker: ${e.message}")
            Toast.makeText(this, "Could not open contact list", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processPickedContact(contactUri: Uri) {
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    vm.addImportantContact(name, number)
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error reading contact data: ${e.message}")
        }
    }

    private fun handleAddImportantContact() {
        val permissions = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE
        )
        contactPermissionLauncher.launch(permissions)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { 
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
                try {
                    val wifiManager = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    wifiManager?.let {
                        val ssidList = it.scanResults.map { res -> res.SSID }.filter { ssid ->
                            ssid.isNotBlank() && ssid != "unknown ssid" && ssid.length < 32
                        }.distinct()
                        if (ssidList.isNotEmpty()) vm.updateSsidList(ssidList)
                    }
                } catch (e: Exception) {}
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

    private val networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
            runOnUiThread {
                vm.checkWifiConnection(getCurrentSsid())
            }
        }
        override fun onLost(network: android.net.Network) {
            runOnUiThread {
                vm.checkWifiConnection(null)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start the background service
        val serviceIntent = Intent(this, SilentZoneService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        enableEdgeToEdge()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fetchCurrentLocation()

        val scanFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wifiScanReceiver, scanFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wifiScanReceiver, scanFilter)
        }
        registerReceiver(ringerModeReceiver, IntentFilter(android.media.AudioManager.RINGER_MODE_CHANGED_ACTION))

        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val networkRequest = android.net.NetworkRequest.Builder()
                .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to register network callback: ${e.message}")
        }

        setContent {
            SilentZoneTheme {
                MaterialTheme {
                    Surface(Modifier.fillMaxSize()) {
                        val state = vm.uiStateFlow.collectAsStateWithLifecycle().value
                        val availableSsidList = vm.availableSsidList.collectAsStateWithLifecycle().value
                        val wifiZones = vm.wifiZones.collectAsStateWithLifecycle().value
                        val locationZones = vm.locationZones.collectAsStateWithLifecycle().value
                        val importantContacts = vm.importantContacts.collectAsStateWithLifecycle().value
                        val currentWifiSsid = getCurrentSsid()

                        val silentSsids = wifiZones.filter { it.mode == RingerMode.SILENT }.map { it.ssid }.toSet()
                        val vibrateSsids = wifiZones.filter { it.mode == RingerMode.VIBRATE }.map { it.ssid }.toSet()

                        SilentScreen(
                            accessGranted = state.accessGranted,
                            mode = state.currentMode,
                            message = state.message,
                            onGrantAccess = {
                                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                            },
                            setSilent = vm::setSilent,
                            setVibrate = vm::setVibrate,
                            setNormal = vm::setNormal,
                            addZone = { startWifiScan() },
                            availableSsidList = availableSsidList,
                            onSelectedSsid = { ssid, mode -> saveSsid(ssid, mode) },
                            onDismissDialog = { vm.clearSsidList() },
                            silentSsids = silentSsids,
                            vibrateSsids = vibrateSsids,
                            onDeleteSsid = { ssid ->
                                vm.removeWifiZone(ssid)
                            },
                            wifiPermissionGranted = wifiPermissionGranted(),
                            currentWifiSsid = currentWifiSsid,
                            locationZones = locationZones,
                            onAddLocationZone = { mode, radius -> vm.addCurrentLocationZone(mode, radius) },
                            onMapZonesSelected = { zones, mode, radius ->
                                zones.forEach { zone ->
                                    vm.addLocationZone(zone.latLng.latitude, zone.latLng.longitude, zone.name, mode, radius)
                                }
                            },
                            onDeleteLocationZone = { id -> vm.removeLocationZone(id) },
                            initialUserLocation = currentLocation,
                            importantContacts = importantContacts,
                            onPickContact = { handleAddImportantContact() },
                            onDeleteContact = { phoneNumber -> vm.removeImportantContact(phoneNumber) }
                        )
                    }
                }
            }
        }
        checkPermissionAndStartScan()
    }

    private fun checkPermissionAndStartScan() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startWifiScan()
            checkAndRequestBackgroundLocation()
        }
    }

    private fun checkAndRequestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackground = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasBackground) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    showBackgroundLocationRationale()
                } else {
                     backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
        }
    }

    private fun showBackgroundLocationRationale() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Background Location Required")
            .setMessage("To use Location-based zones while the app is closed, please allow 'Allow all the time'.")
            .setPositiveButton("OK") { _, _ ->
                 try { backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) } catch (e: Exception) {}
            }
            .setNegativeButton("No Thanks") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) currentLocation = com.google.android.gms.maps.model.LatLng(location.latitude, location.longitude)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startWifiScan() {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (!wifiManager.isWifiEnabled) return
            if (wifiManager.startScan()) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        val ssidList = wifiManager.scanResults.map { it.SSID }.filter { it.isNotBlank() && it != "unknown ssid" }.distinct()
                        if (ssidList.isNotEmpty()) vm.updateSsidList(ssidList)
                    } catch (e: Exception) {}
                }, 2000)
            }
        } catch (e: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(wifiScanReceiver) } catch (e: Exception) {}
        try { unregisterReceiver(ringerModeReceiver) } catch (e: Exception) {}
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {}
    }

    private fun wifiPermissionGranted(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentSsid(): String? {
        try {
            if (!wifiPermissionGranted()) return null
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo
            return if (info.ssid != null && info.ssid != WifiManager.UNKNOWN_SSID) info.ssid.trim('"') else null
        } catch (e: Exception) { return null }
    }

    private fun saveSsid(ssid: String?, mode: RingerMode) {
        if (ssid.isNullOrBlank()) return
        vm.addWifiZone(ssid, mode)
        vm.checkWifiConnection(getCurrentSsid())
    }
}
