package com.sandeep.silentzone.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: ComponentActivity) {

    private var onPermissionGrantedAction: (() -> Unit)? = null

    private val requestPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.all { it.value }) {
                onPermissionGrantedAction?.invoke()
                onPermissionGrantedAction = null
            } else {
                onPermissionGrantedAction = null
                val permanentlyDenied = results.filter { !it.value }.keys.any {
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
                }
                if (permanentlyDenied) {
                    showSettingsDialog("Permissions are required for standard features. Please enable them in settings.")
                } else {
                    Toast.makeText(activity, "Permissions are required for some features.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val backgroundLocationLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(activity, "Background Location Granted!", Toast.LENGTH_SHORT).show()
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    showSettingsDialog("To detect Location zones even when the app is closed, please select 'Allow all the time' in the system settings.")
                } else {
                    Toast.makeText(activity, "Background location is required for automation.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    fun requestInitialPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
    }

    fun requestLocationPermissions(action: () -> Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            onPermissionGrantedAction = action
            showProminentLocationDisclosure {
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        } else {
            action()
        }
    }

    private fun showProminentLocationDisclosure(onAccept: () -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle("Prominent Disclosure")
            .setMessage("Silent Zone collects location data to enable automatic ringer mode switching (Silent/Vibrate) based on your location, even when the app is closed or not in use.")
            .setPositiveButton("I Agree") { _, _ ->
                onAccept()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                onPermissionGrantedAction = null
            }
            .setCancelable(false)
            .show()
    }

    fun requestContactPermissions(action: () -> Unit) {
        val permissions = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE
        )
        val missing = permissions.filter { 
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED 
        }
        
        if (missing.isNotEmpty()) {
            onPermissionGrantedAction = action
            requestPermissionLauncher.launch(missing.toTypedArray())
        } else {
            action()
        }
    }

    fun checkAndRequestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackground = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasBackground) {
                showBackgroundLocationRationale(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            }
        }
    }

    private fun showBackgroundLocationRationale(requiresSettingsNavigation: Boolean) {
        val extraInstruction = if (requiresSettingsNavigation) {
            "\n\nTo enable this, please select 'Allow all the time' in the system settings."
        } else {
            ""
        }
        AlertDialog.Builder(activity)
            .setTitle("Background Location Required")
            .setMessage("Silent Zone collects location data to enable automatic ringer mode switching (Silent/Vibrate) based on your location, even when the app is closed or not in use." + extraInstruction)
            .setPositiveButton("I Agree") { _, _ ->
                 try { backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) } catch (e: Exception) {}
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    fun showSettingsDialog(message: String) {
        AlertDialog.Builder(activity)
            .setTitle("Permission Required")
            .setMessage(message)
            .setPositiveButton("Settings") { _, _ -> openAppSettings() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }
    
    fun hasBackgroundPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun wifiPermissionGranted(): Boolean {
        val fine = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun isDndAccessGranted(): Boolean {
        val notifManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        return notifManager.isNotificationPolicyAccessGranted
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
            return pm.isIgnoringBatteryOptimizations(activity.packageName)
        }
        return true
    }

    fun requestDisableBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val listIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                activity.startActivity(listIntent)
            } catch (e: Exception) {
                Toast.makeText(activity, "Unable to open battery settings", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
