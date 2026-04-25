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

class AppUpdateHelper(private val activity: Activity) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private val updateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showUpdateSnackbar()
        }
    }

    fun checkForUpdates() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val availability = appUpdateInfo.updateAvailability()
            val priority = appUpdateInfo.updatePriority()
            
            Log.d("AppUpdateHelper", "Checking for updates...")
            Log.d("AppUpdateHelper", "Update Availability: $availability")
            Log.d("AppUpdateHelper", "Update Priority: $priority")
            Log.d("AppUpdateHelper", "Flexible Allowed: ${appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)}")
            Log.d("AppUpdateHelper", "Immediate Allowed: ${appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)}")

            if (availability == UpdateAvailability.UPDATE_AVAILABLE) {
                val currentVersion = com.sandeep.silentzone.BuildConfig.VERSION_CODE
                val newVersion = appUpdateInfo.availableVersionCode()
                val versionDiff = newVersion - currentVersion

                Log.d("AppUpdateHelper", "Current Version: $currentVersion, New Version: $newVersion, Diff: $versionDiff")

                // Agar version gap 2 ya usse zyada hai, toh IMMEDIATE update trigger karo
                if ((versionDiff >= 2 || priority >= 4) && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    Log.d("AppUpdateHelper", "Triggering IMMEDIATE update (Reason: Critical Gap or High Priority)")
                    startUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE)
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    Log.d("AppUpdateHelper", "Triggering FLEXIBLE update (Reason: Minor Gap)")
                    appUpdateManager.registerListener(updateListener)
                    startUpdate(appUpdateInfo, AppUpdateType.FLEXIBLE)
                }
            } else {
                Log.d("AppUpdateHelper", "No update available (Availability: $availability)")
            }
        }.addOnFailureListener { e ->
            Log.e("AppUpdateHelper", "Failed to check for updates: ${e.message}", e)
        }
    }

    private fun startUpdate(appUpdateInfo: com.google.android.play.core.appupdate.AppUpdateInfo, updateType: Int) {
        try {
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
