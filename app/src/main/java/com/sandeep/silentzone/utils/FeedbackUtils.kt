package com.sandeep.silentzone.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object FeedbackUtils {
    private const val DEVELOPER_EMAIL = "support.silentzone@gmail.com"

    fun sendFeedback(
        context: Context,
        rating: Int,
        category: String,
        message: String
    ) {
        val ratingStars = "⭐".repeat(rating)
        val emailBody = """
            Feedback from SilentZone User
            -----------------------------
            Rating: $ratingStars ($rating/5)
            Category: $category
            
            Message:
            $message
            
            -----------------------------
            Device Info:
            OS: Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})
            Model: ${android.os.Build.MODEL}
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(DEVELOPER_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, "SilentZone App Feedback: $category")
            putExtra(Intent.EXTRA_TEXT, emailBody)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Send Feedback via..."))
        } catch (e: Exception) {
            Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
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
