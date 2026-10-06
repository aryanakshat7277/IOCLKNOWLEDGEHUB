package com.iocl.knowledgehub.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iocl.knowledgehub.ui.theme.*
import kotlin.math.*

// ============================================================
// GLASSMORPHISM CARD
// True frosted glass with blur, border gradient, reflection
// ============================================================
@Composable
fun GlassmorphismCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = 20.dp,
    borderWidth: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glass_border")
    val borderPhase by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "border_phase"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .drawBehind {
                // Animated gradient border
                val rad = borderPhase * PI.toFloat() / 180f
                val gradientBrush = Brush.sweepGradient(
                    0f to IOCLSaffron.copy(alpha = 0.6f),
                    0.25f to IOCLAmber.copy(alpha = 0.3f),
                    0.5f to IOCLBlueAccent.copy(alpha = 0.4f),
                    0.75f to IOCLSaffronLight.copy(alpha = 0.2f),
                    1f to IOCLSaffron.copy(alpha = 0.6f),
                    center = Offset(
                        size.width / 2 + cos(rad) * size.width * 0.1f,
                        size.height / 2 + sin(rad) * size.height * 0.1f
                    )
                )
                drawRoundRect(
                    brush = gradientBrush,
                    cornerRadius = CornerRadius(cornerRadius.toPx()),
                    style = Stroke(borderWidth.toPx())
                )
            }
            .background(
                color = GlassWhite,
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        // Reflection highlight at top
        Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                ),
                size = Size(size.width, 80.dp.toPx())
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

// ============================================================
// ANIMATED STAT RING
// Circular progress ring with animated fill and center value
// ============================================================
@Composable
fun AnimatedStatRing(
    value: Float,
    maxValue: Float,
    label: String,
    color: Color = IOCLSaffron,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    strokeWidth: Dp = 8.dp
) {
    val percentage = (value / maxValue.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(2000, easing = FastOutSlowInEasing),
        label = "ring"
    )
    val animatedValue = animateIntCounter(value.toInt())

    val infiniteTransition = rememberInfiniteTransition(label = "ring_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        0.3f, 0.7f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring_glow_alpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sweepAngle = animatedPercentage * 360f
                val arcStroke = strokeWidth.toPx()
                val arcSize = Size(this.size.width - arcStroke, this.size.height - arcStroke)
                val topLeft = Offset(arcStroke / 2, arcStroke / 2)

                // Background track
                drawArc(
                    color = color.copy(alpha = 0.1f),
                    startAngle = -90f, sweepAngle = 360f,
                    useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(arcStroke, cap = StrokeCap.Round)
                )

                // Glow behind progress
                drawArc(
                    color = color.copy(alpha = glowAlpha * 0.3f),
                    startAngle = -90f, sweepAngle = sweepAngle,
                    useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(arcStroke * 3f, cap = StrokeCap.Round)
                )

                // Progress arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(color, color.copy(alpha = 0.7f), color)
                    ),
                    startAngle = -90f, sweepAngle = sweepAngle,
                    useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(arcStroke, cap = StrokeCap.Round)
                )

                // End dot
                if (sweepAngle > 0) {
                    val endAngle = (-90f + sweepAngle) * PI.toFloat() / 180f
                    val cx = this.size.width / 2 + (this.size.width / 2 - arcStroke / 2) * cos(endAngle)
                    val cy = this.size.height / 2 + (this.size.height / 2 - arcStroke / 2) * sin(endAngle)
                    drawCircle(color = Color.White, radius = arcStroke * 0.6f, center = Offset(cx, cy))
                    drawCircle(color = color, radius = arcStroke * 0.4f, center = Offset(cx, cy))
                }
            }

            Text(
                text = animatedValue.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============================================================
// AURORA BACKGROUND
// Northern lights / aurora borealis effect
// ============================================================
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    baseColor: Color = IOCLNavyDark
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    val phase1 by infiniteTransition.animateFloat(
        0f, 2 * PI.toFloat(),
        infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "aurora1"
    )
    val phase2 by infiniteTransition.animateFloat(
        0f, 2 * PI.toFloat(),
        infiniteRepeatable(tween(15000, easing = LinearEasing)),
        label = "aurora2"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Base
        drawRect(baseColor)

        val w = size.width
        val h = size.height

        // Aurora bands
        for (i in 0..5) {
            val yBase = h * (0.15f + i * 0.12f)
            val path = Path().apply {
                moveTo(0f, yBase)
                for (x in 0..w.toInt() step 3) {
                    val xf = x.toFloat()
                    val wave1 = sin(xf * 0.003f + phase1 + i * 0.5f) * h * 0.04f
                    val wave2 = sin(xf * 0.007f + phase2 + i * 0.3f) * h * 0.02f
                    val y = yBase + wave1 + wave2
                    lineTo(xf, y)
                }
                lineTo(w, yBase + h * 0.15f)
                lineTo(0f, yBase + h * 0.15f)
                close()
            }

            val auroraColor = when (i % 3) {
                0 -> IOCLSaffron
                1 -> IOCLBlueAccent
                else -> IOCLAmber
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        auroraColor.copy(alpha = 0.04f),
                        auroraColor.copy(alpha = 0.02f),
                        Color.Transparent
                    ),
                    startY = yBase - h * 0.02f,
                    endY = yBase + h * 0.15f
                )
            )
        }
    }
}

// ============================================================
// GEOMETRIC PATTERN OVERLAY
// Kept for backward compatibility — renders as no-op
// (User requested removal of geometric/particle overlays)
// ============================================================
@Composable
fun GeometricPatternOverlay(
    modifier: Modifier = Modifier,
    color: Color = IOCLWhite.copy(alpha = 0.03f)
) {
    // No-op: geometric pattern removed per user request
    // Callers keep compiling without changes
    Spacer(modifier = modifier)
}

