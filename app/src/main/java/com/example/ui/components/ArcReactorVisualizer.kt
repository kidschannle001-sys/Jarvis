package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.JarvisTaskStatus
import com.example.ui.theme.JarvisAccentGlow
import com.example.ui.theme.JarvisBlue
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanDim
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisRed
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorVisualizer(
    status: JarvisTaskStatus,
    isSpeaking: Boolean,
    rmsLevel: Float,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor")

    // Continuous rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    JarvisTaskStatus.THINKING -> 2000
                    JarvisTaskStatus.LISTENING -> 4000
                    JarvisTaskStatus.EXECUTING -> 3000
                    else -> 10000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Counter-rotation
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    JarvisTaskStatus.THINKING -> 3000
                    JarvisTaskStatus.LISTENING -> 5000
                    else -> 12000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rotation"
    )

    // Breathing pulse
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    isSpeaking -> 600
                    status == JarvisTaskStatus.LISTENING -> 700
                    status == JarvisTaskStatus.THINKING -> 500
                    else -> 1800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryColor = when {
        status == JarvisTaskStatus.STOPPED || status == JarvisTaskStatus.FAILED -> JarvisRed
        status == JarvisTaskStatus.WAITING_FOR_CONFIRMATION -> JarvisGold
        status == JarvisTaskStatus.THINKING -> JarvisCyanBright
        isSpeaking -> JarvisCyan
        status == JarvisTaskStatus.LISTENING -> JarvisAccentGlow
        else -> JarvisCyanDim
    }

    val secondaryColor = when {
        status == JarvisTaskStatus.STOPPED -> Color(0xFF6B1717)
        status == JarvisTaskStatus.THINKING -> JarvisBlue
        else -> JarvisBlue
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2.2f

            // Audio reaction expansion
            val rmsBoost = if (status == JarvisTaskStatus.LISTENING) {
                ((rmsLevel.coerceIn(0f, 10f) / 10f) * 15f)
            } else 0f

            val currentRadius = (baseRadius * pulse) + rmsBoost

            // Outer glow ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.25f),
                        primaryColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 1.3f
                ),
                radius = currentRadius * 1.3f,
                center = center
            )

            // Outer dashed / segmented ring
            val outerStrokeWidth = 3f
            drawCircle(
                color = secondaryColor.copy(alpha = 0.4f),
                radius = currentRadius,
                center = center,
                style = Stroke(width = outerStrokeWidth)
            )

            // Rotating arcs
            rotate(rotation, pivot = center) {
                // Segment 1
                drawArc(
                    color = primaryColor,
                    startAngle = 0f,
                    sweepAngle = 70f,
                    useCenter = false,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = primaryColor,
                    startAngle = 120f,
                    sweepAngle = 70f,
                    useCenter = false,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = primaryColor,
                    startAngle = 240f,
                    sweepAngle = 70f,
                    useCenter = false,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
            }

            // Counter-rotating inner ring
            rotate(counterRotation, pivot = center) {
                val innerRadius = currentRadius * 0.72f
                drawCircle(
                    color = primaryColor.copy(alpha = 0.3f),
                    radius = innerRadius,
                    center = center,
                    style = Stroke(width = 2f)
                )

                // Inner arc brackets
                drawArc(
                    color = JarvisCyanBright,
                    startAngle = 30f,
                    sweepAngle = 40f,
                    useCenter = false,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = JarvisCyanBright,
                    startAngle = 150f,
                    sweepAngle = 40f,
                    useCenter = false,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = JarvisCyanBright,
                    startAngle = 270f,
                    sweepAngle = 40f,
                    useCenter = false,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )

                // Tech Tick marks
                val tickCount = 12
                for (i in 0 until tickCount) {
                    val angle = (i * (360f / tickCount)) * (Math.PI / 180f)
                    val r1 = innerRadius * 0.85f
                    val r2 = innerRadius * 0.95f
                    val startOffset = Offset(
                        center.x + (r1 * cos(angle)).toFloat(),
                        center.y + (r1 * sin(angle)).toFloat()
                    )
                    val endOffset = Offset(
                        center.x + (r2 * cos(angle)).toFloat(),
                        center.y + (r2 * sin(angle)).toFloat()
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.6f),
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Core Energy Center
            val coreRadius = currentRadius * 0.40f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryColor,
                        secondaryColor,
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // Central core hexagon/circle
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = coreRadius * 0.35f,
                center = center
            )
        }
    }
}
