package com.sandeep.silentzone.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sandeep.silentzone.SilentModeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class KeepAliveWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: SilentModeRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("KeepAliveWorker", "Executing KeepAlive periodic check.")
            repository.syncCurrentState()
            repository.reportServiceUsage()
            repository.reportServiceUptimeHeartbeat()
            Result.success()
        } catch (e: Exception) {
            Log.e("KeepAliveWorker", "Failed to sync state in KeepAliveWorker: ${e.message}")
            Result.retry()
        }
    }
}
