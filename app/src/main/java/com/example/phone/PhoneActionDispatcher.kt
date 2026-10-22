package com.example.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.data.model.ActionType
import com.example.data.model.JarvisAction
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisNotificationService
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ActionResult(
    val success: Boolean,
    val feedbackMessage: String,
    val details: String? = null
)

data class ContactItem(
    val name: String,
    val number: String
)

class PhoneActionDispatcher(private val context: Context) {

    fun executeAction(action: JarvisAction): ActionResult {
        return when (action.type) {
            ActionType.OPEN_APP -> openApp(action.target)
            ActionType.OPEN_SETTINGS -> openSettings(action.target)
            ActionType.READ_VISIBLE_SCREEN -> readScreen()
            ActionType.TAP_ACCESSIBLE_CONTROL -> tapAccessibleControl(action.target)
            ActionType.SCROLL_SCREEN -> scrollScreen(action.extra.lowercase() != "up")
            ActionType.GO_BACK -> goBack()
            ActionType.PREPARE_MESSAGE -> prepareWhatsAppMessage(action.target, action.extra)
            ActionType.START_CALL -> initiateCall(action.target)
            ActionType.READ_SUPPORTED_NOTIFICATION -> readNotifications()
            ActionType.READ_CALENDAR_EVENT -> readCalendarEvents()
            ActionType.STOP_TASK -> ActionResult(true, "Current action stopped by user.")
            ActionType.NONE -> ActionResult(true, "No action required.")
        }
    }

    fun openApp(appName: String): ActionResult {
        val query = appName.trim().lowercase()
        val pm = context.packageManager

        // Known alias map
        val aliasMap = mapOf(
            "youtube" to "com.google.android.youtube",
            "yt" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "google chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "whatsapp" to "com.whatsapp",
            "settings" to "com.android.settings",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "calculator" to "com.google.android.calculator",
            "calendar" to "com.google.android.calendar",
            "gmail" to "com.google.android.gm",
            "mail" to "com.google.android.gm",
            "photos" to "com.google.android.apps.photos",
            "gallery" to "com.google.android.apps.photos"
        )

        val targetPkg = aliasMap[query]
        if (targetPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                vibrate(HapticType.SUCCESS)
                return ActionResult(true, "$appName khol diya gaya hai.")
            }
        }

        // Special handling for Settings or Dialer
        if (query.contains("setting")) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            vibrate(HapticType.SUCCESS)
            return ActionResult(true, "Android Settings khol di gayi hain.")
        }

        if (query.contains("dialer") || query.contains("phone") || query.contains("call")) {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            vibrate(HapticType.SUCCESS)
            return ActionResult(true, "Phone Dialer open kar diya gaya hai.")
        }

        // Dynamic search across all installed launchable apps
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString().lowercase()
            if (label.contains(query) || query.contains(label)) {
                val launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    vibrate(HapticType.SUCCESS)
                    return ActionResult(true, "${info.loadLabel(pm)} open kar diya gaya hai.")
                }
            }
        }

        return ActionResult(
            false,
            "Application '$appName' device par nahi mili ya launch nahi ki jaa saki."
        )
    }

    fun openSettings(settingType: String): ActionResult {
        val action = when (settingType.lowercase()) {
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "notifications", "notification" -> "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
            "wireless", "wifi" -> Settings.ACTION_WIRELESS_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            vibrate(HapticType.SUCCESS)
            ActionResult(true, "$settingType Settings open kar di gayi hain.")
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            ActionResult(true, "Settings open ho gayi hain.")
        }
    }

    fun readScreen(): ActionResult {
        val service = JarvisAccessibilityService.instance
        if (service == null) {
            return ActionResult(
                false,
                "Accessibility Service enable nahi hai. Kripya Permission Manager ya Accessibility Setup se enable karein."
            )
        }
        val text = service.getVisibleScreenText()
        return ActionResult(true, "Screen padh li gayi hai.", details = text)
    }

    fun tapAccessibleControl(targetLabel: String): ActionResult {
        val service = JarvisAccessibilityService.instance
        if (service == null) {
            return ActionResult(false, "Accessibility Service band hai.")
        }
        val clicked = service.clickElementByText(targetLabel)
        return if (clicked) {
            vibrate(HapticType.SUCCESS)
            ActionResult(true, "'$targetLabel' par tap kar diya gaya hai.")
        } else {
            ActionResult(false, "'$targetLabel' control screen par click karne ke liye nahi mila.")
        }
    }

    fun scrollScreen(down: Boolean): ActionResult {
        val service = JarvisAccessibilityService.instance
        if (service == null) {
            return ActionResult(false, "Accessibility Service inactive hai.")
        }
        val scrolled = service.performScroll(!down) // forward is down
        return if (scrolled) {
            ActionResult(true, if (down) "Neeche scroll kiya gaya." else "Upar scroll kiya gaya.")
        } else {
            ActionResult(false, "Scrollable content nahi mila.")
        }
    }

    fun goBack(): ActionResult {
        val service = JarvisAccessibilityService.instance
        if (service == null) {
            return ActionResult(false, "Accessibility Service inactive hai.")
        }
        val back = service.performBack()
        return if (back) {
            ActionResult(true, "Back navigate kar diya gaya.")
        } else {
            ActionResult(false, "Back action execute nahi ho saka.")
        }
    }

    fun searchContacts(query: String): List<ContactItem> {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return emptyList()

        val results = mutableListOf<ContactItem>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.let {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val name = it.getString(nameIndex)
                    val number = it.getString(numberIndex)
                    results.add(ContactItem(name, number))
                    if (results.size >= 5) break
                }
            }
        } catch (_: Exception) {
            // Ignore query error
        } finally {
            cursor?.close()
        }
        return results
    }

    fun initiateCall(target: String): ActionResult {
        val isAllDigits = target.replace("+", "").replace("-", "").replace(" ", "").all { it.isDigit() }
        var phoneNumber = target

        if (!isAllDigits) {
            val contacts = searchContacts(target)
            if (contacts.isEmpty()) {
                // If not found in contacts, open dialer with name or fallback
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                return ActionResult(false, "Contact '$target' nahi mila. Dialer khol diya gaya hai.")
            }
            if (contacts.size > 1) {
                val options = contacts.joinToString { "${it.name} (${it.number})" }
                return ActionResult(false, "Ek se zyada contact mile: $options. Kripya pura naam batayein.")
            }
            phoneNumber = contacts.first().number
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            // Safe fallback: open dialer with phone number pre-filled
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return try {
            context.startActivity(intent)
            vibrate(HapticType.START)
            ActionResult(true, "$target ($phoneNumber) ko call lagai jaa rahi hai.")
        } catch (e: Exception) {
            ActionResult(false, "Call initiate karne mein samasya aayi: ${e.message}")
        }
    }

    fun prepareWhatsAppMessage(recipient: String, messageText: String): ActionResult {
        var phoneNumber = recipient.filter { it.isDigit() || it == '+' }
        if (phoneNumber.isEmpty()) {
            val contacts = searchContacts(recipient)
            if (contacts.isNotEmpty()) {
                phoneNumber = contacts.first().number.filter { it.isDigit() || it == '+' }
            }
        }

        // Clean number for international format if needed
        if (phoneNumber.startsWith("0")) {
            phoneNumber = "91" + phoneNumber.substring(1)
        } else if (!phoneNumber.startsWith("+") && phoneNumber.length == 10) {
            phoneNumber = "91$phoneNumber"
        }
        phoneNumber = phoneNumber.replace("+", "")

        val encodedText = try {
            URLEncoder.encode(messageText, "UTF-8")
        } catch (_: Exception) {
            messageText
        }

        val intent = if (phoneNumber.isNotEmpty()) {
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=$encodedText")
            Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, messageText)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return try {
            context.startActivity(intent)
            vibrate(HapticType.SUCCESS)
            ActionResult(true, "WhatsApp par message taiyar kar diya gaya hai. Send karne se pehle review karein.")
        } catch (_: Exception) {
            // Fallback generic send
            val genericIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(Intent.createChooser(genericIntent, "Send message via").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                ActionResult(true, "Message draft taiyar kar diya gaya hai.")
            } catch (_: Exception) {
                ActionResult(false, "WhatsApp application device par installed nahi hai.")
            }
        }
    }

    fun readCalendarEvents(): ActionResult {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return ActionResult(false, "Calendar permission granted nahi hai. Kripya Permission Manager se enable karein.")
        }

        val now = System.currentTimeMillis()
        val end = now + 48 * 60 * 60 * 1000L // Next 48 hours

        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.EVENT_LOCATION
        )
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        val selectionArgs = arrayOf(now.toString(), end.toString())

        val eventsList = mutableListOf<String>()
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )
            val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            cursor?.let {
                val titleIdx = it.getColumnIndex(CalendarContract.Events.TITLE)
                val startIdx = it.getColumnIndex(CalendarContract.Events.DTSTART)
                while (it.moveToNext()) {
                    val title = it.getString(titleIdx) ?: "Untitled Event"
                    val startTime = it.getLong(startIdx)
                    eventsList.add("• $title ($dateText: ${dateFormat.format(Date(startTime))})")
                    if (eventsList.size >= 5) break
                }
            }
        } catch (e: Exception) {
            return ActionResult(false, "Calendar check karne mein samasya aayi: ${e.message}")
        } finally {
            cursor?.close()
        }

        return if (eventsList.isEmpty()) {
            ActionResult(true, "Agale 48 ghante mein koi calendar events scheduled nahi hain.")
        } else {
            val text = "Aane wale events:\n" + eventsList.joinToString("\n")
            ActionResult(true, text, details = text)
        }
    }

    private val dateText = "Samay"

    fun readNotifications(): ActionResult {
        val granted = JarvisNotificationService.isNotificationAccessGranted(context)
        if (!granted) {
            return ActionResult(
                false,
                "Notification Access enabled nahi hai. Permission Manager se Notification Access enable karein."
            )
        }
        val summary = JarvisNotificationService.getRecentNotificationsSummary()
        return ActionResult(true, summary, details = summary)
    }

    fun vibrate(type: HapticType) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                when (type) {
                    HapticType.START -> vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                    HapticType.SUCCESS -> vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                    HapticType.ERROR -> vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 80), -1))
                }
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                when (type) {
                    HapticType.START -> vibrator?.vibrate(50)
                    HapticType.SUCCESS -> vibrator?.vibrate(90)
                    HapticType.ERROR -> vibrator?.vibrate(longArrayOf(0, 80, 50, 80), -1)
                }
            }
        } catch (_: Exception) {
            // Ignore haptic failures
        }
    }
}

enum class HapticType {
    START,
    SUCCESS,
    ERROR
}
