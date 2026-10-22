package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.JarvisAccessibilityService
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun AccessibilitySetupScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isEnabled by remember {
        mutableStateOf(JarvisAccessibilityService.isAccessibilityEnabled(context))
    }

    LaunchedEffect(Unit) {
        isEnabled = JarvisAccessibilityService.isAccessibilityEnabled(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccessibilityNew,
                contentDescription = "Accessibility",
                tint = JarvisCyan,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "ACCESSIBILITY SETUP",
                    color = JarvisCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Screen Inspection & Phone Navigation",
                    color = JarvisTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Real-Time Verified Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isEnabled) JarvisGreen.copy(alpha = 0.12f) else JarvisSurfaceVariant
            ),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                    listOf(if (isEnabled) JarvisGreen else JarvisGold, Color.Transparent)
                )
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isEnabled) JarvisGreen else JarvisGold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnabled) "ACCESSIBILITY SERVICE: ENABLED" else "ACCESSIBILITY SERVICE: DISABLED",
                            color = if (isEnabled) JarvisGreen else JarvisGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            isEnabled = JarvisAccessibilityService.isAccessibilityEnabled(context)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCyan),
                        modifier = Modifier.testTag("recheck_accessibility_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Recheck", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isEnabled) {
                        "Verified Active! JARVIS ab aapki voice commands par phone screen padh sakta hai, back navigate kar sakta hai aur buttons tap kar sakta hai."
                    } else {
                        "Service band hai. Android rules ke anusaar Accessibility service ko user dwara Settings mein jaakar manually enable karna hota hai."
                    },
                    color = JarvisTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons: Enable & Manage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEnabled) JarvisSurfaceVariant else JarvisCyan,
                    contentColor = if (isEnabled) JarvisTextPrimary else Color(0xFF00363A)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("enable_accessibility_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Enable", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEnabled) "Manage in Settings" else "Enable Accessibility",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detailed Explanation Cards in Hindi/Hinglish
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisCardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ACCESSIBILITY KAISE KAAM KARTA HAI? (FAQ)",
                    color = JarvisCyan,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Point 1: Why required
                ExplanationSection(
                    icon = Icons.Default.Info,
                    title = "1. Accessibility Service kyun zaroori hai?",
                    body = "Jab aap bolte hain \"Jarvis, screen par kya likha hai?\" ya \"Neeche scroll karo\", toh Android OS keval authorized AccessibilityService ko hi screen element inspect aur interact karne ki permission deta hai."
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Point 2: Privacy and secure inspection
                ExplanationSection(
                    icon = Icons.Default.Security,
                    title = "2. JARVIS kya inspect karta hai aur kya nahi?",
                    body = "JARVIS keval screen par dikhne wale aam text aur buttons ko padhta hai. Android Keystore aur password fields strictly protected hote hain—JARVIS passwords ya hidden sensitive content ko kabhi nahi dekh sakta."
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Point 3: Supported Controls
                ExplanationSection(
                    icon = Icons.Default.TouchApp,
                    title = "3. Kaun-kaun se controls activate ho sakte hain?",
                    body = "• Dikhne wale buttons par click/tap karna\n• Screen par upar ya neeche scroll karna\n• Back button press karna (Global Action Back)\n• Visible screen content padhkar sunana"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Point 4: Step by step enable guide
                ExplanationSection(
                    icon = Icons.Default.Visibility,
                    title = "4. Kaise enable karein (Step-by-Step)?",
                    body = "1. Upar 'Enable Accessibility' button dabayein.\n2. Settings mein 'Downloaded apps' ya 'Installed services' par click karein.\n3. 'JARVIS AI' ko select karein.\n4. Toggle ko ON karein aur confirmation allow karein.\n5. Wapas JARVIS app me aane par 'Recheck' dabayein."
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Point 5: How to disable
                ExplanationSection(
                    icon = Icons.Default.Error,
                    title = "5. Kaise disable karein?",
                    body = "Aap kabhi bhi wapas Android Settings > Accessibility > JARVIS AI me jakar toggle ko OFF kar sakte hain."
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ExplanationSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(JarvisSurfaceVariant, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = title, color = JarvisCyanBright, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = body, color = JarvisTextPrimary, fontSize = 12.sp, lineHeight = 17.sp)
    }
}
