package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JarvisTaskStatus
import com.example.service.JarvisAccessibilityService
import com.example.ui.JarvisViewModel
import com.example.ui.components.ArcReactorVisualizer
import com.example.ui.theme.JarvisAccentGlow
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBlue
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanDim
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun DashboardScreen(
    viewModel: JarvisViewModel,
    onNavigateToApiSetup: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAccessibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    val taskStatus by viewModel.taskStatus.collectAsState()
    val transcript by viewModel.currentTranscript.collectAsState()
    val rmsLevel by viewModel.rmsLevel.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val hasApiKey by viewModel.hasApiKey.collectAsState()
    val pendingAction by viewModel.pendingAction.collectAsState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermissions()
        if (granted) {
            viewModel.startListening()
        }
    }

    val sampleVoiceCommands = listOf(
        "YouTube kholo",
        "Screen par kya likha hai?",
        "Settings kholo",
        "WhatsApp par message likho",
        "Rahul ko call lagao",
        "Mera calendar check karo",
        "Notification padhkar sunao",
        "Jarvis, stop"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top HUD Header with Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "JARVIS AI // ONLINE",
                    color = JarvisCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "PERSONAL ASSISTANT HUD",
                    color = JarvisTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            // Quick Status Chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // API Key Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasApiKey) JarvisCyan.copy(alpha = 0.15f) else JarvisRed.copy(alpha = 0.2f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(if (hasApiKey) JarvisCyan else JarvisRed, Color.Transparent)
                        )
                    ),
                    modifier = Modifier.clickable { onNavigateToApiSetup() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "API Status",
                            tint = if (hasApiKey) JarvisCyan else JarvisRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (hasApiKey) "API OK" else "NO KEY",
                            color = if (hasApiKey) JarvisCyan else JarvisRed,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Accessibility Chip
                val a11yActive = JarvisAccessibilityService.instance != null
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (a11yActive) JarvisGreen.copy(alpha = 0.15f) else JarvisSurfaceVariant,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(if (a11yActive) JarvisGreen else JarvisTextSecondary, Color.Transparent)
                        )
                    ),
                    modifier = Modifier.clickable { onNavigateToAccessibility() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessibilityNew,
                            contentDescription = "Accessibility",
                            tint = if (a11yActive) JarvisGreen else JarvisTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (a11yActive) "A11Y ON" else "A11Y OFF",
                            color = if (a11yActive) JarvisGreen else JarvisTextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Center Arc Reactor Visualizer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            ArcReactorVisualizer(
                status = taskStatus,
                isSpeaking = isSpeaking,
                rmsLevel = rmsLevel,
                size = 230.dp
            )
        }

        // Status Badge Pill
        val statusBg = when (taskStatus) {
            JarvisTaskStatus.LISTENING -> JarvisCyan.copy(alpha = 0.2f)
            JarvisTaskStatus.THINKING -> JarvisBlue.copy(alpha = 0.25f)
            JarvisTaskStatus.EXECUTING -> JarvisGold.copy(alpha = 0.2f)
            JarvisTaskStatus.WAITING_FOR_CONFIRMATION -> JarvisGold.copy(alpha = 0.3f)
            JarvisTaskStatus.SUCCESS -> JarvisGreen.copy(alpha = 0.2f)
            JarvisTaskStatus.FAILED -> JarvisRed.copy(alpha = 0.2f)
            JarvisTaskStatus.STOPPED -> JarvisRed.copy(alpha = 0.3f)
            JarvisTaskStatus.IDLE -> JarvisSurfaceVariant
        }
        val statusTextColor = when (taskStatus) {
            JarvisTaskStatus.LISTENING -> JarvisCyanBright
            JarvisTaskStatus.THINKING -> JarvisCyan
            JarvisTaskStatus.EXECUTING -> JarvisGold
            JarvisTaskStatus.WAITING_FOR_CONFIRMATION -> JarvisGold
            JarvisTaskStatus.SUCCESS -> JarvisGreen
            JarvisTaskStatus.FAILED -> JarvisRed
            JarvisTaskStatus.STOPPED -> JarvisRed
            JarvisTaskStatus.IDLE -> JarvisTextSecondary
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = statusBg,
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(statusTextColor, Color.Transparent))),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusTextColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STATUS: ${taskStatus.name}",
                    color = statusTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // System Message / Transcript Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (taskStatus == JarvisTaskStatus.LISTENING) JarvisCyan else JarvisCardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (transcript.isNotBlank()) "SPEECH RECOGNITION" else "SYSTEM TELEMETRY",
                        color = JarvisCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isSpeaking) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speaking",
                                tint = JarvisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SPEAKING",
                                color = JarvisCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (transcript.isNotBlank()) "\"$transcript\"" else statusMessage,
                    color = if (transcript.isNotBlank()) JarvisCyanBright else JarvisTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    minLines = 2
                )
            }
        }

        // Confirmation Action Card
        AnimatedVisibility(visible = pendingAction != null) {
            pendingAction?.let { action ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .border(1.dp, JarvisGold, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Confirmation",
                                tint = JarvisGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACTION CONFIRMATION REQUIRED",
                                color = JarvisGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = action.description.ifEmpty { "Execute ${action.type.name} on ${action.target}?" },
                            color = JarvisTextPrimary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.cancelPendingAction() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisRed),
                                modifier = Modifier.testTag("cancel_action_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.confirmPendingAction() },
                                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color(0xFF00363A)),
                                modifier = Modifier.testTag("confirm_action_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Confirm", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Confirm & Run")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Suggestion Chips
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "VOICE COMMAND SUGGESTIONS (HINDI / ENGLISH)",
                color = JarvisTextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(sampleVoiceCommands) { cmd ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = JarvisSurfaceVariant,
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(JarvisCyanDim, Color.Transparent))),
                        modifier = Modifier.clickable {
                            if (cmd.contains("stop")) {
                                viewModel.stopAll()
                            } else {
                                viewModel.sendTextMessage(cmd)
                            }
                        }
                    ) {
                        Text(
                            text = cmd,
                            color = JarvisTextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bottom Controls: Glowing Mic Button + Global STOP Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // STOP BUTTON
            OutlinedButton(
                onClick = { viewModel.stopAll() },
                modifier = Modifier
                    .size(64.dp)
                    .testTag("stop_task_button"),
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = JarvisRed.copy(alpha = 0.15f),
                    contentColor = JarvisRed
                ),
                border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(JarvisRed, Color(0xFF6B1717))))
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Jarvis Stop",
                    tint = JarvisRed,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(28.dp))

            // GLOWING MIC BUTTON
            val isListening = taskStatus == JarvisTaskStatus.LISTENING
            val micBgColor by animateColorAsState(
                targetValue = if (isListening) JarvisCyanBright else JarvisCyan,
                label = "mic_color"
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(
                        elevation = if (isListening) 24.dp else 12.dp,
                        shape = CircleShape,
                        spotColor = JarvisCyanBright,
                        ambientColor = JarvisCyan
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(micBgColor, JarvisBlue)
                        )
                    )
                    .clickable {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    .testTag("voice_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.Mic,
                    contentDescription = "Toggle Voice Listening",
                    tint = Color(0xFF00363A),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (taskStatus == JarvisTaskStatus.LISTENING) "Listening... Tap to stop or say 'Jarvis stop'" else "Tap microphone to speak with JARVIS",
            color = JarvisTextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
