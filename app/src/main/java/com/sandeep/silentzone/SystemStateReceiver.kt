package com.sandeep.silentzone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Listens for system-level state changes:
 * 1. GPS/Location Provider toggled ON/OFF
 * 2. WiFi toggled ON/OFF
 *
 * When these events fire, we trigger a full state sync so the ringer mode
 * updates immediately without waiting for the next Geofence or WiFi event.
 */
@AndroidEntryPoint
class SystemStateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: SilentModeRepository

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            LocationManager.PROVIDERS_CHANGED_ACTION -> {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isLocationEnabled =
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                Log.d("SystemStateReceiver", "Location providers changed. Enabled: $isLocationEnabled")

                if (isLocationEnabled) {
                    // GPS turned ON -> immediately check if we're inside any zone
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.syncCurrentState()
                    }
                } else {
                    // GPS turned OFF -> we can no longer detect zones; restore original mode
                    Log.d("SystemStateReceiver", "Location disabled. Restoring mode.")
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.onLocationProviderDisabled()
                    }
                }
            }

            android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                val wifiState = intent.getIntExtra(
                    android.net.wifi.WifiManager.EXTRA_WIFI_STATE,
                    android.net.wifi.WifiManager.WIFI_STATE_UNKNOWN
                )
                Log.d("SystemStateReceiver", "WiFi state changed: $wifiState")

                if (wifiState == android.net.wifi.WifiManager.WIFI_STATE_ENABLED) {
                    // WiFi just turned ON → give it a moment to connect, then sync
                    CoroutineScope(Dispatchers.IO).launch {
                        kotlinx.coroutines.delay(2500)
                        repository.syncCurrentState()
                    }
                } else if (wifiState == android.net.wifi.WifiManager.WIFI_STATE_DISABLED) {
                    // WiFi turned OFF → treat as disconnected from any WiFi zone
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.onWifiChanged(null)
                    }
                }
            }
        }
    }
}
