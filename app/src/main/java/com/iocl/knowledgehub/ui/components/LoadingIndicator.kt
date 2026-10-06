package com.iocl.knowledgehub.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iocl.knowledgehub.ui.theme.*
import kotlin.math.*

@Composable
fun IOCLLoadingIndicator(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "premium_loading")

    // Outer ring rotation
    val outerRotation by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "outer_rot"
    )
    // Inner ring counter-rotation
    val innerRotation by infiniteTransition.animateFloat(
        360f, 0f,
        infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "inner_rot"
    )
    // Pulse
    val pulseScale by infiniteTransition.animateFloat(
        0.85f, 1.15f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    // Glow
    val glowAlpha by infiniteTransition.animateFloat(
        0.2f, 0.6f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    // Dots
    val dotsPhase by infiniteTransition.animateFloat(
        0f, 2 * PI.toFloat(),
        infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "dots"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).scale(pulseScale),
            contentAlignment = Alignment.Center
        ) {
            // Glow background
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(IOCLSaffron.copy(alpha = glowAlpha * 0.3f), Color.Transparent)
                    ),
                    radius = size.width * 0.7f
                )
            }

            // Outer arc
            Canvas(modifier = Modifier.fillMaxSize()) {
                val outerRad = outerRotation * PI.toFloat() / 180f
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(IOCLSaffron, IOCLAmber.copy(alpha = 0.1f), Color.Transparent, IOCLSaffron)
                    ),
                    startAngle = outerRotation,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Inner arc
            Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(IOCLBlueAccent, IOCLCyan.copy(alpha = 0.1f), Color.Transparent, IOCLBlueAccent)
                    ),
                    startAngle = innerRotation,
                    sweepAngle = 200f,
                    useCenter = false,
                    style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Center dot ring
            Canvas(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                val radius = size.width / 2
                for (i in 0 until 6) {
                    val angle = dotsPhase + i * (2 * PI.toFloat() / 6)
                    val dotSize = 3f + sin(dotsPhase + i * 0.5f) * 1.5f
                    val dotAlpha = 0.5f + sin(dotsPhase + i * 0.8f) * 0.3f
                    val x = center.x + radius * 0.6f * cos(angle)
                    val y = center.y + radius * 0.6f * sin(angle)
                    drawCircle(
                        color = IOCLSaffron.copy(alpha = dotAlpha),
                        radius = dotSize,
                        center = Offset(x, y)
                    )
                }
            }

            // Center IOCL dot
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(IOCLSaffron, IOCLAmber))
                    )
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Serif
        )
    }
}
