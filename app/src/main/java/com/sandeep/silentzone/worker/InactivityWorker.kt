package com.sandeep.silentzone.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sandeep.silentzone.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class InactivityWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sharedPrefs = appContext.getSharedPreferences("silent_zone_prefs", Context.MODE_PRIVATE)
            val lastLaunch = sharedPrefs.getLong("last_launch_timestamp", 0L)
            
            if (lastLaunch == 0L) {
                // First time or no timestamp, set it now
                sharedPrefs.edit().putLong("last_launch_timestamp", System.currentTimeMillis()).apply()
                return@withContext Result.success()
            }

            val currentTime = System.currentTimeMillis()
            val diff = currentTime - lastLaunch
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            Log.d("InactivityWorker", "Days since last launch: $days")

            if (days >= 3) {
                NotificationHelper.showInactivityNotification(appContext)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("InactivityWorker", "Worker failed: ${e.message}")
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "InactivityWorker"
    }
}
