package com.sandeep.silentzone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.telephony.TelephonyManager
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CallReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repo: SilentModeRepository
    
    @Inject
    lateinit var actionManager: AgentActionManager
    
    @Inject
    lateinit var contextEngine: AgentContextEngine

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            
            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    handleRinging(context, incomingNumber)
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    restoreMode(context)
                }
            }
        }
    }

    private fun handleRinging(
        context: Context,
        incomingNumber: String?
    ) {
        // 1. Important Contact Bypass
        if (!incomingNumber.isNullOrEmpty() && repo.isImportantContact(incomingNumber)) {
            Log.d(TAG, "Important contact calling: $incomingNumber. Bypassing silent mode.")
            saveCurrentMode(context, repo.getCurrentMode())
            actionManager.normalPhone()
            return
        }

        // 2. Meeting Mode Logic
        if (contextEngine.isUserInMeeting()) {
            Log.d(TAG, "User in meeting. Silencing call.")
            actionManager.silencePhone()
            
            if (!incomingNumber.isNullOrEmpty()) {
                actionManager.sendMeetingSms(incomingNumber)
            }
        }
    }

    private fun restoreMode(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedModeOrdinal = prefs.getInt(PREF_KEY_SAVED_MODE, -1)
        
        if (savedModeOrdinal != -1) {
            val savedMode = RingerMode.values()[savedModeOrdinal]
            Log.d(TAG, "Restoring mode to: $savedMode")
            when (savedMode) {
                RingerMode.SILENT -> actionManager.silencePhone()
                RingerMode.VIBRATE -> actionManager.vibratePhone()
                RingerMode.NORMAL -> actionManager.normalPhone()
            }
            // Clear the saved mode
            prefs.edit().remove(PREF_KEY_SAVED_MODE).apply()
        }
    }

    private fun saveCurrentMode(context: Context, mode: RingerMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(PREF_KEY_SAVED_MODE, mode.ordinal).apply()
    }

    companion object {
        private const val TAG = "CallReceiver"
        private const val PREFS_NAME = "call_receiver_prefs"
        private const val PREF_KEY_SAVED_MODE = "saved_ringer_mode"
    }
}
