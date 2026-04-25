package com.sandeep.silentzone.utils

import android.app.Activity
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings

class AppUpdateHelper(private val activity: Activity) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    var isImmediateUpdateInProgress = false
        private set

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600 // Fetch every hour
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(mapOf("force_update_version" to 0L))
    }

    private val updateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showUpdateSnackbar()
        }
    }

    fun checkForUpdates() {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("AppUpdateHelper", "Remote Config fetch successful")
            } else {
                Log.e("AppUpdateHelper", "Remote Config fetch failed")
            }
            performUpdateCheck()
        }
    }

    private fun performUpdateCheck() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val availability = appUpdateInfo.updateAvailability()
            val forceUpdateVersion = remoteConfig.getLong("force_update_version")
            val currentVersion = com.sandeep.silentzone.BuildConfig.VERSION_CODE
            
            Log.d("AppUpdateHelper", "Checking for updates...")
            Log.d("AppUpdateHelper", "Update Availability: $availability")
            Log.d("AppUpdateHelper", "Force Update Version (Remote): $forceUpdateVersion")
            Log.d("AppUpdateHelper", "Current Version: $currentVersion")

            if (availability == UpdateAvailability.UPDATE_AVAILABLE) {
                // Decision Logic
                if (currentVersion < forceUpdateVersion && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    Log.d("AppUpdateHelper", "Triggering IMMEDIATE update (Reason: Current version $currentVersion < Forced version $forceUpdateVersion)")
                    startUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE)
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    Log.d("AppUpdateHelper", "Triggering FLEXIBLE update")
                    appUpdateManager.registerListener(updateListener)
                    startUpdate(appUpdateInfo, AppUpdateType.FLEXIBLE)
                }
            } else {
                Log.d("AppUpdateHelper", "No update available (Availability: $availability)")
            }
        }.addOnFailureListener { e ->
            Log.e("AppUpdateHelper", "Failed to get appUpdateInfo: ${e.message}", e)
        }
    }

    private fun startUpdate(appUpdateInfo: com.google.android.play.core.appupdate.AppUpdateInfo, updateType: Int) {
        try {
            if (updateType == AppUpdateType.IMMEDIATE) {
                isImmediateUpdateInProgress = true
            }
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(updateType).build(),
                UPDATE_REQUEST_CODE
            )
        } catch (e: Exception) {
            Log.e("AppUpdateHelper", "Update flow failed", e)
        }
    }

    /**
     * Check if a flexible update was downloaded but not installed while app was in background
     */
    fun checkPendingUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            val status = appUpdateInfo.installStatus()
            val availability = appUpdateInfo.updateAvailability()

            Log.d("AppUpdateHelper", "Checking pending updates... Status: $status, Availability: $availability")

            if (status == InstallStatus.DOWNLOADED) {
                Log.d("AppUpdateHelper", "Update already downloaded, showing snackbar")
                showUpdateSnackbar()
            }
            
            // If flexible update is in progress, re-register listener
            if (status == InstallStatus.DOWNLOADING || status == InstallStatus.PENDING) {
                Log.d("AppUpdateHelper", "Flexible update in progress, re-registering listener")
                appUpdateManager.registerListener(updateListener)
            }

            // Resume immediate update if it was interrupted
            if (availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                Log.d("AppUpdateHelper", "Immediate update in progress, resuming flow")
                startUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE)
            }
        }.addOnFailureListener { e ->
            Log.e("AppUpdateHelper", "Failed to check pending updates: ${e.message}", e)
        }
    }

    private fun showUpdateSnackbar() {
        try {
            if (activity.isFinishing || activity.isDestroyed) {
                Log.w("AppUpdateHelper", "Activity is not in a valid state to show Snackbar")
                return
            }

            val rootView = activity.findViewById<android.view.View>(android.R.id.content)
            if (rootView != null) {
                Snackbar.make(
                    rootView,
                    "An update has just been downloaded.",
                    Snackbar.LENGTH_INDEFINITE
                ).apply {
                    setAction("RESTART") { 
                        Log.d("AppUpdateHelper", "Complete update clicked")
                        appUpdateManager.completeUpdate() 
                    }
                    show()
                }
            } else {
                Log.e("AppUpdateHelper", "Root view not found for Snackbar")
            }
        } catch (e: Exception) {
            Log.e("AppUpdateHelper", "Failed to show Snackbar, falling back to Toast", e)
            android.widget.Toast.makeText(activity, "Update downloaded. Please restart the app.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun unregisterListener() {
        appUpdateManager.unregisterListener(updateListener)
    }

    companion object {
        const val UPDATE_REQUEST_CODE = 123
    }
}
