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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.sandeep.silentzone.ui.SilentScreen
import com.sandeep.silentzone.ui.theme.SilentZoneTheme
import com.sandeep.silentzone.utils.PermissionManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: SilentModeViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: com.google.android.gms.maps.model.LatLng? by mutableStateOf(null)

    private lateinit var permissionManager: PermissionManager

    private val pickContactLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val contactUri = result.data?.data
            if (contactUri != null) {
                processPickedContact(contactUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        permissionManager = PermissionManager(this)
        
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

        setContent {
            SilentZoneTheme {
                MaterialTheme {
                    Surface(Modifier.fillMaxSize()) {
                        val state = vm.uiStateFlow.collectAsStateWithLifecycle().value
                        val opState = vm.operationState.collectAsStateWithLifecycle().value
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
                            isFallback = state.isFallback,
                            operationState = opState,
                            onGrantAccess = {
                                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                            },
                            setSilent = vm::setSilent,
                            setVibrate = vm::setVibrate,
                            setNormal = vm::setNormal,
                            addZone = { permissionManager.requestLocationPermissions { startWifiScan() } },
                            availableSsidList = availableSsidList,
                            onSelectedSsid = { ssid, mode -> saveSsid(ssid, mode) },
                            onDismissDialog = { vm.clearSsidList() },
                            silentSsids = silentSsids,
                            vibrateSsids = vibrateSsids,
                            onDeleteSsid = { ssid ->
                                vm.removeWifiZone(ssid)
                            },
                            currentWifiSsid = currentWifiSsid,
                            locationZones = locationZones,
                            onAddLocationZone = { mode, radius -> 
                                if (permissionManager.hasBackgroundPermission()) {
                                    vm.addCurrentLocationZone(mode, radius)
                                } else {
                                    permissionManager.checkAndRequestBackgroundLocation()
                                }
                            },
                            onMapZonesSelected = { zones, mode ->
                                if (permissionManager.hasBackgroundPermission()) {
                                    zones.forEach { zone ->
                                        vm.addLocationZone(zone.latLng.latitude, zone.latLng.longitude, zone.name, mode, zone.radius)
                                    }
                                } else {
                                    permissionManager.checkAndRequestBackgroundLocation()
                                }
                            },
                            onDeleteLocationZone = { id -> vm.removeLocationZone(id) },
                            initialUserLocation = currentLocation,
                            importantContacts = importantContacts,
                            onPickContact = { handleAddImportantContact() },
                            onDeleteContact = { phoneNumber -> vm.removeImportantContact(phoneNumber) },
                            onRequestPermission = { action -> permissionManager.requestLocationPermissions { action() } }
                        )
                    }
                }
            }
        }
    }

    private fun handleAddImportantContact() {
        permissionManager.requestContactPermissions {
            openContactPicker()
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

    @SuppressLint("MissingPermission")
    private fun getCurrentSsid(network: android.net.Network? = null): String? {
        return try {
            if (!permissionManager.wifiPermissionGranted()) return null
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            
            // Priority 1: Legacy
            @Suppress("DEPRECATION")
            val info = wifiManager.connectionInfo
            if (info != null && info.ssid != null && info.ssid != WifiManager.UNKNOWN_SSID) {
                return info.ssid.trim('"')
            }
            
            // Priority 2: Modern
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val targetNetwork = network ?: connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(targetNetwork)
            val wifiInfo = capabilities?.transportInfo as? android.net.wifi.WifiInfo
            if (wifiInfo != null && wifiInfo.ssid != null && wifiInfo.ssid != WifiManager.UNKNOWN_SSID) {
                return wifiInfo.ssid.trim('"')
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun saveSsid(ssid: String?, mode: RingerMode) {
        if (ssid.isNullOrBlank()) return
        vm.addWifiZone(ssid, mode)
        vm.checkWifiConnection(getCurrentSsid())
    }
}
