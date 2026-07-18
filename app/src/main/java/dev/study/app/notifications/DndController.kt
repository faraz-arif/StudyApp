package dev.study.app.notifications

import android.app.NotificationManager
import android.content.Context
import android.os.Build

class DndController(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun hasNotificationPolicyAccess(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    /**
     * Enables Do Not Disturb mode.
     * filterMode: "PRIORITY" or "SILENCE"
     */
    fun enableDnd(filterMode: String) {
        if (!hasNotificationPolicyAccess()) return
        val filter = when (filterMode) {
            "SILENCE" -> NotificationManager.INTERRUPTION_FILTER_NONE
            else -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
        }
        try {
            notificationManager.setInterruptionFilter(filter)
        } catch (e: SecurityException) {
            // Log/ignore if access was revoked dynamically
        }
    }

    fun disableDnd() {
        if (!hasNotificationPolicyAccess()) return
        try {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        } catch (e: SecurityException) {
            // Log/ignore
        }
    }
}
