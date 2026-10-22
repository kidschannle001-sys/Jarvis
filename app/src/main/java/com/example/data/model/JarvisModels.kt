package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ActionType {
    OPEN_APP,
    OPEN_SETTINGS,
    READ_VISIBLE_SCREEN,
    TAP_ACCESSIBLE_CONTROL,
    SCROLL_SCREEN,
    GO_BACK,
    PREPARE_MESSAGE,
    START_CALL,
    READ_SUPPORTED_NOTIFICATION,
    READ_CALENDAR_EVENT,
    STOP_TASK,
    NONE
}

enum class JarvisTaskStatus {
    IDLE,
    LISTENING,
    THINKING,
    WAITING_FOR_CONFIRMATION,
    EXECUTING,
    SUCCESS,
    FAILED,
    STOPPED
}

data class JarvisAction(
    val type: ActionType,
    val target: String = "",
    val extra: String = "",
    val requiresConfirmation: Boolean = false,
    val description: String = ""
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null,
    val actionPayload: String? = null,
    val actionStatus: String? = null
)
