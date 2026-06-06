package com.sandeep.silentzone.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object FeedbackUtils {
    fun rateApp(context: Context) {
        val packageName = context.packageName
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            )
        } catch (e: Exception) {
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    )
                )
            } catch (fallbackError: Exception) {
                Toast.makeText(context, "Unable to open Play Store", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareApp(context: Context) {
        val shareText = """
            Smart Sound Management with SilentZone 📱
            
            No more manual toggling between Silent, Vibrate, and Normal modes. SilentZone automates your device's profile using intelligent location and network triggers.
            Download it here: https://play.google.com/store/apps/details?id=${context.packageName}
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Share SilentZone via..."))
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share at this time", Toast.LENGTH_SHORT).show()
        }
    }
}
