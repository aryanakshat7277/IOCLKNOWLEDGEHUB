package com.iocl.knowledgehub.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iocl.knowledgehub.ui.theme.*
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    val target = value.filter { it.isDigit() }.toIntOrNull() ?: 0
    val animatedValue = animateIntCounter(target)

    val infiniteTransition = rememberInfiniteTransition(label = "stat_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        0.05f, 0.15f,
        infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "stat_glow_alpha"
    )
    val wavePhase by infiniteTransition.animateFloat(
        0f, 2 * PI.toFloat(),
        infiniteRepeatable(tween(5000, easing = LinearEasing)),
        label = "wave_phase"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .drawBehind {
                    // Animated wave at bottom
                    val path = Path().apply {
                        moveTo(0f, size.height)
                        for (x in 0..size.width.toInt() step 3) {
                            val xf = x.toFloat()
                            val y = size.height - size.height * 0.15f +
                                    sin(xf * 0.015f + wavePhase) * size.height * 0.04f +
                                    sin(xf * 0.008f + wavePhase * 0.7f) * size.height * 0.02f
                            lineTo(xf, y)
                        }
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(path, Color.White.copy(alpha = 0.06f))

                    // Second wave
                    val path2 = Path().apply {
                        moveTo(0f, size.height)
                        for (x in 0..size.width.toInt() step 3) {
                            val xf = x.toFloat()
                            val y = size.height - size.height * 0.1f +
                                    sin(xf * 0.012f + wavePhase + 1f) * size.height * 0.03f
                            lineTo(xf, y)
                        }
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(path2, Color.White.copy(alpha = 0.04f))
                }
                .padding(20.dp)
        ) {
            // Large background icon with glow
            Icon(
                icon, null,
                Modifier
                    .size(72.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp),
                tint = IOCLWhite.copy(alpha = glowAlpha)
            )

            Column {
                // Icon container with glass effect
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(IOCLWhite.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, Modifier.size(22.dp), tint = IOCLWhite)
                }

                Spacer(Modifier.height(16.dp))

                // Animated counter with premium styling
                Text(
                    text = if (target > 0) animatedValue.toString() else value,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif,
                    color = IOCLWhite,
                    letterSpacing = (-0.5).sp
                )

                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = IOCLWhite.copy(0.85f),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}
