package com.sandeep.silentzone

import android.app.Application
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import com.microsoft.clarity.models.LogLevel
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SilentZoneApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Microsoft Clarity Initialization
        val config = ClarityConfig(
            projectId = BuildConfig.CLARITY_PROJECT_ID,
            logLevel = LogLevel.Info,
        )
        Clarity.initialize(applicationContext, config)
        //Clarity.setCurrentMaskingLevel(MaskingLevel.Relaxed)

    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
