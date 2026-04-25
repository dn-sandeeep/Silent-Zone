package com.sandeep.silentzone

import android.app.Application
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import com.microsoft.clarity.models.LogLevel
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.Constraints
import com.sandeep.silentzone.worker.KeepAliveWorker
import com.sandeep.silentzone.worker.InactivityWorker
import android.content.Context
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class SilentZoneApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var repository: SilentModeRepository

    override fun onCreate() {
        super.onCreate()
        
        // Sync state on cold start
        kotlinx.coroutines.MainScope().launch {
            repository.syncCurrentState()
            repository.reportServiceUsage()
        }

        // Microsoft Clarity Initialization
        val config = ClarityConfig(
            projectId = BuildConfig.CLARITY_PROJECT_ID,
            logLevel = LogLevel.Info,
        )
        Clarity.initialize(applicationContext, config)
        //Clarity.setCurrentMaskingLevel(MaskingLevel.Relaxed)
        setupKeepAliveWorker()
        setupInactivityWorker()
        updateLastLaunchTimestamp()
    }

    private fun setupKeepAliveWorker() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
            
        val keepAliveWorkRequest = PeriodicWorkRequestBuilder<KeepAliveWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "silent_zone_keep_alive",
            ExistingPeriodicWorkPolicy.KEEP,
            keepAliveWorkRequest
        )
    }

    private fun setupInactivityWorker() {
        val inactivityWorkRequest = PeriodicWorkRequestBuilder<InactivityWorker>(24, TimeUnit.HOURS)
            .build()
            
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            InactivityWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            inactivityWorkRequest
        )
    }

    private fun updateLastLaunchTimestamp() {
        val sharedPrefs = getSharedPreferences("silent_zone_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putLong("last_launch_timestamp", System.currentTimeMillis()).apply()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
