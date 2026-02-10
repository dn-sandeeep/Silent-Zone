package com.sandeep.silentzone

import android.content.Context
import android.media.AudioManager
import android.telephony.SmsManager
import android.util.Log

class AgentActionManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun setRingerMode(mode: Int) {
        try {
            audioManager.ringerMode = mode
        } catch (e: Exception) {
            Log.e(TAG, "Error setting ringer mode: ${e.message}")
        }
    }

    fun silencePhone() {
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
    }

    fun vibratePhone() {
        setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
    }

    fun normalPhone() {
        setRingerMode(AudioManager.RINGER_MODE_NORMAL)
    }

    fun sendMeetingSms(phoneNumber: String) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            val message = "I'm in a meeting. Will call you back."
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "AgentActionManager"
    }
}
