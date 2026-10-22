package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: JarvisAccessibilityService? = null
            private set

        fun isAccessibilityEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${JarvisAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = enabledServices.split(":")
            for (service in colonSplitter) {
                if (service.equals(expectedServiceName, ignoreCase = true) ||
                    service.contains(context.packageName)
                ) {
                    return true
                }
            }
            return false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events monitored for context
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    fun getVisibleScreenText(): String {
        val rootNode = rootInActiveWindow ?: return "Screen content could not be read. (Window is null or secure)."
        val stringBuilder = StringBuilder()
        val visitedNodes = HashSet<AccessibilityNodeInfo>()
        traverseNode(rootNode, stringBuilder, visitedNodes, 0)
        val result = stringBuilder.toString().trim()
        return if (result.isEmpty()) "Screen is currently blank or containing no readable text." else result
    }

    private fun traverseNode(
        node: AccessibilityNodeInfo?,
        builder: StringBuilder,
        visited: HashSet<AccessibilityNodeInfo>,
        depth: Int
    ) {
        if (node == null || depth > 12) return
        if (visited.contains(node)) return
        visited.add(node)

        // Never inspect passwords or secure fields
        if (node.isPassword) {
            builder.append("[Protected Password Field]\n")
            return
        }

        if (node.isVisibleToUser) {
            val text = node.text?.toString()?.trim()
            val contentDesc = node.contentDescription?.toString()?.trim()

            if (!text.isNullOrEmpty()) {
                if (node.isClickable) {
                    builder.append("[Button/Control: \"$text\"]\n")
                } else {
                    builder.append("$text\n")
                }
            } else if (!contentDesc.isNullOrEmpty()) {
                if (node.isClickable) {
                    builder.append("[Clickable: \"$contentDesc\"]\n")
                } else {
                    builder.append("$contentDesc\n")
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseNode(child, builder, visited, depth + 1)
        }
    }

    fun clickElementByText(targetText: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val matchedNodes = root.findAccessibilityNodeInfosByText(targetText)
        for (node in matchedNodes) {
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            // Check parent if child itself wasn't clickable
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
                parent = parent.parent
            }
        }
        return false
    }

    fun performBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performScroll(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return performScrollOnNode(root, action)
    }

    private fun performScrollOnNode(node: AccessibilityNodeInfo?, action: Int): Boolean {
        if (node == null) return false
        if (node.isScrollable) {
            if (node.performAction(action)) return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (performScrollOnNode(child, action)) return true
        }
        return false
    }
}
