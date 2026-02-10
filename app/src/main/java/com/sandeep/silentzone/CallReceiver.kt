package com.sandeep.silentzone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                
                // key logic: Is user in a meeting?
                val contextEngine = AgentContextEngine(context)
                if (contextEngine.isUserInMeeting()) {
                    val actionManager = AgentActionManager(context)
                    
                    // 1. Silence the phone immediately
                    actionManager.silencePhone()
                    
                    // 2. Send SMS if number is valid
                    if (!incomingNumber.isNullOrEmpty()) {
                        actionManager.sendMeetingSms(incomingNumber)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "CallReceiver"
    }
}
