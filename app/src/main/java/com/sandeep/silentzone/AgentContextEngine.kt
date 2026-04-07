package com.sandeep.silentzone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentContextEngine @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    fun isUserInMeeting(): Boolean {
        // 1. Check Calendar first
        if (checkCalendarForMeeting()) {
            return true
        }
        
        // 2. TODO: Check Ambient Noise (Audio Classifier)
        // For now, return false
        return false
    }

    private fun checkCalendarForMeeting(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_CALENDAR permission not granted")
            return false
        }

        val projection = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.AVAILABILITY,
            CalendarContract.Instances.TITLE
        )

        val startMillis = System.currentTimeMillis()
        val endMillis = startMillis + 60 * 1000 // Check next 1 minute specifically (are we in a meeting NOW?)

        // Construct the query with the desired date range.
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, startMillis)
        android.content.ContentUris.appendId(builder, endMillis)

        val cursor: Cursor? = context.contentResolver.query(
            builder.build(),
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val availability = it.getInt(2)
                //val title = it.getString(3)
                
                // AVAILABILITY_BUSY = 0, AVAILABILITY_FREE = 1, AVAILABILITY_TENTATIVE = 2
                // We care about BUSY
                if (availability == CalendarContract.Instances.AVAILABILITY_BUSY) {
                    return true
                }
            }
        }
        return false
    }

    companion object {
        private const val TAG = "AgentContextEngine"
    }
}
