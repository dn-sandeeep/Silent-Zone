package com.sandeep.silentzone

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.sandeep.silentzone.ui.theme.SilentZoneTheme

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    private val vm: SilentModeViewModel by viewModels { SilentModeViewModelFactory(this) }
    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback = ConnectivityManager.NetworkCallback()
    private lateinit var prefs: SharedPreferences
    private var redirectedToSettings = false

    companion object {
        private const val REQUEST_CODE_WIFI_PERMISSION: Int = 1001
        private const val PREFS_NAME = "silent_prefs"
        private const val PREF_KEY_SELECTED_SSID = "pref_selected_ssid"
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        setContent {
            SilentZoneTheme {
                MaterialTheme {
                    Surface(Modifier.fillMaxSize()) {
                        val state = vm.uiStateFlow.collectAsStateWithLifecycle().value
                        SilentScreen(
                            accessGranted = state.accessGranted,
                            mode = state.currentMode,
                            message = state.message,
                            onGrantAccess = {
                                //startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                                val intent =
                                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                startActivity(intent)
                            },
                            setSilent = vm::setSilent,
                            setNormal = vm::setNormal,
                            addZone = {
                                handleSetSilentWiFiclick()
                            })

                    }
                }
            }
        }
        checkWiFiPermissionAndResister()
    }


    private fun openAppPermissionSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun wifiPermissionGranted(): Boolean {
        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestWifiPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES),
                REQUEST_CODE_WIFI_PERMISSION
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_WIFI_PERMISSION
            )
        }
    }

    fun checkWiFiPermissionAndResister() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.NEARBY_WIFI_DEVICES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES),
                    REQUEST_CODE_WIFI_PERMISSION
                )
                return
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_CODE_WIFI_PERMISSION
                )
                return
            }
        }
        registerWiFiNetworkCallback()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_WIFI_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                registerWiFiNetworkCallback()
            } else {
                if (!redirectedToSettings) {
                    redirectedToSettings = true
                    Toast.makeText(
                        this,
                        "Please grant location permission to enable silent mode",
                        Toast.LENGTH_SHORT
                    ).show()
                    openAppPermissionSettings()
                }
            }
        }
    }
    private fun registerWiFiNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                super.onAvailable(network)
                val ssid = getCurrentSsid()
                val saved = getSelectedSsid()
                if (!ssid.isNullOrBlank() && ssid == saved) {
                    runOnUiThread { vm.setSilent() }
                } else {
                    runOnUiThread { vm.setNormal() }
                }
            }

            override fun onLost(network: android.net.Network) {
                super.onLost(network)
                runOnUiThread { vm.setNormal() }
            }
        }
        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }

    private fun getCurrentSsid(): String? {

        try {
            val wifiManager =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager

            @Suppress("DEPRECATION")
            val info = wifiManager.connectionInfo
            var ssid = info?.ssid ?: return null
            if (ssid == WifiManager.UNKNOWN_SSID) return null
            ssid = ssid.replace("\"", "")
            return ssid

        } catch (e: Exception) {
            return null
        }
    }

    private fun saveSelectedSsid(ssid: String?) {
        if (ssid.isNullOrBlank()) {
            Toast.makeText(this, "No SSID found", Toast.LENGTH_SHORT).show()
            return
        }
        prefs.edit().putString(PREF_KEY_SELECTED_SSID, ssid).apply()
        val current = getCurrentSsid()
        if (current == ssid) {
            vm.setSilent()
        } else {
            vm.setNormal()
        }
    }

    private fun getSelectedSsid(): String? =
        prefs.getString(PREF_KEY_SELECTED_SSID, null)

    private fun handleSetSilentWiFiclick() {
        if (!wifiPermissionGranted()) {
            requestWifiPermission()
            return
        }
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.startScan()
        val scanResult = wifiManager.scanResults
        val ssidList = scanResult.map { it.SSID }.filter { it.isNotBlank() }

        if (ssidList.isEmpty()){
            Toast.makeText(this, "No WiFi networks found", Toast.LENGTH_SHORT).show()
            return
        }
    }
}

@Composable
fun SilentScreen(
    accessGranted: Boolean,
    mode: RingerMode,
    message: String?,
    onGrantAccess: () -> Unit,
    setSilent: () -> Unit,
    setNormal: () -> Unit,
    addZone: () -> Unit

) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Silent Mode Controller", style = MaterialTheme.typography.headlineMedium)

        AssistChip(label = {
            Text(
                if (accessGranted) "DND Access: Granted" else "DND Access: Not Granted"
            )
        }, onClick = {})
        AssistChip(label = { Text("Current Mode: $mode") }, onClick = {})

        if (!accessGranted) {
            Text("Please grant notification policy access to enable silent mode")
            Button(onClick = onGrantAccess) { "Grant DND Access" }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = setSilent) {
                    Text("Device Silent")
                }
                Button(onClick = setNormal) {
                    Text("Device Normal")
                }
            }
        }
        if (accessGranted) {
            Button(onClick = {
                addZone()
            }) {
                Text("Set silent WiFi")
            }
        }
        if (!message.isNullOrBlank()) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

