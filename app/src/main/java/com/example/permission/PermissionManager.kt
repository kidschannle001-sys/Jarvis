package com.example.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisNotificationService

enum class PermissionState {
    GRANTED,
    NOT_GRANTED,
    ENABLED,
    SETUP_REQUIRED,
    NOT_AVAILABLE
}

data class PermissionModel(
    val id: String,
    val name: String,
    val titleHindi: String,
    val descriptionHindi: String,
    val whyNeeded: String,
    val state: PermissionState,
    val manifestPermission: String? = null,
    val isSystemSetting: Boolean = false,
    val settingsAction: String? = null,
    val isOptional: Boolean = false
)

class PermissionManager(private val context: Context) {

    fun checkAllPermissions(isTtsReady: Boolean = true): List<PermissionModel> {
        val list = mutableListOf<PermissionModel>()

        // 1. Microphone
        val micGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionModel(
                id = "mic",
                name = "Microphone (Audio Recording)",
                titleHindi = "माइक की अनुमति (Voice Input)",
                descriptionHindi = "JARVIS ke saath bolkar baat karne aur voice commands dene ke liye aavashyak hai.",
                whyNeeded = "Aapki aawaz sunne aur speech-to-text karne ke liye Android runtime permission zaroori hai.",
                state = if (micGranted) PermissionState.GRANTED else PermissionState.NOT_GRANTED,
                manifestPermission = Manifest.permission.RECORD_AUDIO,
                isOptional = false
            )
        )

        // 2. Accessibility Service
        val a11yEnabled = JarvisAccessibilityService.isAccessibilityEnabled(context)
        list.add(
            PermissionModel(
                id = "accessibility",
                name = "Accessibility Service",
                titleHindi = "एक्सेसिबिलिटी सर्विस (Screen Control)",
                descriptionHindi = "Screen par kya likha hai padhne, buttons tap karne aur phone control karne ke liye.",
                whyNeeded = "Android OS bina Accessibility Service ke kisi bhi app ko screen interact karne ki ijazat nahi deta.",
                state = if (a11yEnabled) PermissionState.ENABLED else PermissionState.SETUP_REQUIRED,
                isSystemSetting = true,
                settingsAction = Settings.ACTION_ACCESSIBILITY_SETTINGS,
                isOptional = true
            )
        )

        // 3. Contacts
        val contactsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionModel(
                id = "contacts",
                name = "Contacts (Read Contacts)",
                titleHindi = "कॉन्टैक्ट्स अनुमति (Contact Lookup)",
                descriptionHindi = "\"Rahul ko call karo\" jaise commands mein contact dhoondhne ke liye.",
                whyNeeded = "Aapke phone contacts me se naam aur phone number match karne ke liye use hota hai.",
                state = if (contactsGranted) PermissionState.GRANTED else PermissionState.NOT_GRANTED,
                manifestPermission = Manifest.permission.READ_CONTACTS,
                isOptional = true
            )
        )

        // 4. Phone Calls
        val callGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionModel(
                id = "call",
                name = "Phone Calls (Direct Call)",
                titleHindi = "फोन कॉल्स अनुमति (Direct Calling)",
                descriptionHindi = "Voice command milne par seedhe contact ko call lagane ke liye.",
                whyNeeded = "Agar yeh permission nahi hogi, toh JARVIS direct call ke bajaye phone dialer screen kholega.",
                state = if (callGranted) PermissionState.GRANTED else PermissionState.NOT_GRANTED,
                manifestPermission = Manifest.permission.CALL_PHONE,
                isOptional = true
            )
        )

        // 5. Calendar
        val calendarGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionModel(
                id = "calendar",
                name = "Calendar (Upcoming Events)",
                titleHindi = "कैलेंडर अनुमति (Calendar Events)",
                descriptionHindi = "\"Mera calendar check karo\" bolne par aane wale schedule events padhkar batane ke liye.",
                whyNeeded = "Aapke Google ya device calendar me scheduled events ko read karne ke liye.",
                state = if (calendarGranted) PermissionState.GRANTED else PermissionState.NOT_GRANTED,
                manifestPermission = Manifest.permission.READ_CALENDAR,
                isOptional = true
            )
        )

        // 6. Notification Access
        val notifAccessGranted = JarvisNotificationService.isNotificationAccessGranted(context)
        list.add(
            PermissionModel(
                id = "notif_access",
                name = "Notification Listener Access",
                titleHindi = "नोटिफिकेशन एक्सेस (Notification Reader)",
                descriptionHindi = "\"Notification padhkar sunao\" bolne par active notifications ko inspect karne ke liye.",
                whyNeeded = "Android me incoming notifications ko padhne ke liye Notification Listener settings zaroori hoti hai.",
                state = if (notifAccessGranted) PermissionState.ENABLED else PermissionState.SETUP_REQUIRED,
                isSystemSetting = true,
                settingsAction = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS",
                isOptional = true
            )
        )

        // 7. Post Notifications
        val postNotifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        list.add(
            PermissionModel(
                id = "post_notif",
                name = "System Notifications",
                titleHindi = "सिस्टम नोटिफिकेशन (Alerts & Status)",
                descriptionHindi = "Background task alerts aur task status updates display karne ke liye.",
                whyNeeded = "Android 13+ me apps ko notification dikhane ke liye yeh permission required hoti hai.",
                state = if (postNotifGranted) PermissionState.GRANTED else PermissionState.NOT_GRANTED,
                manifestPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null,
                isOptional = true
            )
        )

        // 8. Text-To-Speech
        list.add(
            PermissionModel(
                id = "tts",
                name = "Text-to-Speech Engine",
                titleHindi = "टेक्स्ट-टू-स्पीच इंजन (JARVIS Voice)",
                descriptionHindi = "JARVIS ki aawaz sunne ke liye Google TTS engine available hona chahiye.",
                whyNeeded = "Jawaab ko aawaz mein bolne ke liye Android TTS synthesizer use hota hai.",
                state = if (isTtsReady) PermissionState.ENABLED else PermissionState.NOT_AVAILABLE,
                isSystemSetting = true,
                settingsAction = "com.android.settings.TTS_SETTINGS",
                isOptional = false
            )
        )

        // 9. Vibration & Haptics
        val hasVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.hasVibrator() ?: false
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            v?.hasVibrator() ?: false
        }
        list.add(
            PermissionModel(
                id = "vibration",
                name = "Haptic Vibration",
                titleHindi = "वाइब्रेशन फीडबैक (Haptics)",
                descriptionHindi = "Task start, completion aur stop hone par haptic feedback mehsoos hota hai.",
                whyNeeded = "Touch feedback aur task execution confirmation ke liye.",
                state = if (hasVibrator) PermissionState.ENABLED else PermissionState.NOT_AVAILABLE,
                isOptional = true
            )
        )

        // 10. Speech Recognizer Hardware Support
        val hasSpeechRecognizer = SpeechRecognizer.isRecognitionAvailable(context)
        list.add(
            PermissionModel(
                id = "speech_rec",
                name = "Speech Recognizer System",
                titleHindi = "स्पीच रिकग्निशन सर्विस (Android STT)",
                descriptionHindi = "Android ki official speech recognition service device par uplabdh hai.",
                whyNeeded = "Microphone se aane wale audio ko text mein badalne ke liye.",
                state = if (hasSpeechRecognizer) PermissionState.ENABLED else PermissionState.NOT_AVAILABLE,
                isOptional = false
            )
        )

        return list
    }
}
