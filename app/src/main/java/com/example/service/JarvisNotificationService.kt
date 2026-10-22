package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.CopyOnWriteArrayList

data class CachedNotification(
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

class JarvisNotificationService : NotificationListenerService() {

    companion object {
        private val recentNotifications = CopyOnWriteArrayList<CachedNotification>()

        fun isNotificationAccessGranted(context: Context): Boolean {
            val componentName = ComponentName(context, JarvisNotificationService::class.java)
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return flat != null && flat.contains(componentName.flattenToString())
        }

        fun getRecentNotificationsSummary(): String {
            if (recentNotifications.isEmpty()) {
                return "Koi naye notifications nahi hain. (No recent notifications found)."
            }
            val builder = StringBuilder("Haal hi ke notifications:\n")
            val latest = recentNotifications.takeLast(5).reversed()
            for ((index, item) in latest.withIndex()) {
                builder.append("${index + 1}. [${cleanPackageName(item.packageName)}] ${item.title}: ${item.text}\n")
            }
            return builder.toString().trim()
        }

        private fun cleanPackageName(pkg: String): String {
            return when {
                pkg.contains("whatsapp") -> "WhatsApp"
                pkg.contains("youtube") -> "YouTube"
                pkg.contains("android.gm") -> "Gmail"
                pkg.contains("messaging") -> "Messages"
                pkg.contains("dialer") -> "Phone"
                pkg.contains("calendar") -> "Calendar"
                else -> pkg.substringAfterLast('.')
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val extras = sbn.notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE).orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        if (title.isNotBlank() || text.isNotBlank()) {
            val item = CachedNotification(
                packageName = sbn.packageName,
                title = title,
                text = text,
                timestamp = sbn.postTime
            )
            recentNotifications.add(item)
            if (recentNotifications.size > 20) {
                recentNotifications.removeAt(0)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Can clean up if needed
    }
}
