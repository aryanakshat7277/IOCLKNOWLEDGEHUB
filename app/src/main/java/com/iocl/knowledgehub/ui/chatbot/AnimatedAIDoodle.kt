package com.iocl.knowledgehub.ui.chatbot

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iocl.knowledgehub.ui.theme.*
import kotlin.math.*

// ═══════════════════════════════════════════════════════════════
// ANIMATED AI DOODLE — Living, breathing AI avatar
// Concentric pulsing rings + orbiting particles + waveform center
// ═══════════════════════════════════════════════════════════════

@Composable
fun AnimatedAIDoodle(
    modifier: Modifier = Modifier,
    isSpeaking: Boolean = false,
    isThinking: Boolean = false,
    size: Dp = 120.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "doodle")

    // Core rotation
    val rotation by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "rot"
    )

    // Ring pulses
    val pulse1 by infiniteTransition.animateFloat(
        0.85f, 1.15f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        1.1f, 0.9f,
        infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p2"
    )
    val pulse3 by infiniteTransition.animateFloat(
        0.95f, 1.05f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p3"
    )

    // Voice waveform amplitude (amplified when speaking)
    val wavePhase by infiniteTransition.animateFloat(
        0f, 2f * PI.toFloat(),
        infiniteRepeatable(tween(if (isSpeaking) 600 else 2000, easing = LinearEasing)),
        label = "wave"
    )
    val waveAmplitude by animateFloatAsState(
        targetValue = if (isSpeaking) 12f else if (isThinking) 6f else 3f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "amp"
    )

    // Thinking dots orbit speed
    val thinkOrbit by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(if (isThinking) 1200 else 4000, easing = LinearEasing)),
        label = "think"
    )

    // Glow intensity
    val glowAlpha by infiniteTransition.animateFloat(
        0.15f, 0.4f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    // Neural network nodes flicker
    val nodeFlicker by infiniteTransition.animateFloat(
        0.3f, 1f,
        infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flicker"
    )

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2
        val cy = this.size.height / 2
        val baseRadius = this.size.minDimension / 2

        // ── Layer 1: Outer glow ──
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    IOCLSaffron.copy(alpha = glowAlpha),
                    IOCLAmber.copy(alpha = glowAlpha * 0.5f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = baseRadius * 1.2f
            )
        )

        // ── Layer 2: Outer ring (dashed, rotating) ──
        rotate(rotation, Offset(cx, cy)) {
            val dashCount = 24
            val ringRadius = baseRadius * 0.92f * pulse1
            for (i in 0 until dashCount) {
                val angle = (360f / dashCount) * i
                val startRad = angle * PI.toFloat() / 180f
                val endRad = (angle + 8f) * PI.toFloat() / 180f
                val alpha = if (i % 3 == 0) 0.6f else 0.25f
                drawLine(
                    color = IOCLSaffron.copy(alpha = alpha),
                    start = Offset(
                        cx + ringRadius * cos(startRad),
                        cy + ringRadius * sin(startRad)
                    ),
                    end = Offset(
                        cx + ringRadius * cos(endRad),
                        cy + ringRadius * sin(endRad)
                    ),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
            }
        }

        // ── Layer 3: Middle ring (counter-rotating, dotted) ──
        rotate(-rotation * 0.6f, Offset(cx, cy)) {
            val midRadius = baseRadius * 0.72f * pulse2
            drawCircle(
                color = IOCLBlueAccent.copy(alpha = 0.2f),
                radius = midRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(6f, 10f), thinkOrbit * 0.5f
                ))
            )
        }

        // ── Layer 4: Inner ring (solid, breathing) ──
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    IOCLSaffron.copy(alpha = 0.5f),
                    IOCLAmber.copy(alpha = 0.3f),
                    IOCLBlueAccent.copy(alpha = 0.2f),
                    IOCLSaffron.copy(alpha = 0.5f)
                ),
                center = Offset(cx, cy)
            ),
            radius = baseRadius * 0.55f * pulse3,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )

        // ── Layer 5: Core circle (filled, glassmorphic) ──
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    IOCLNavy.copy(alpha = 0.9f),
                    IOCLNavyDark.copy(alpha = 0.95f)
                ),
                center = Offset(cx, cy),
                radius = baseRadius * 0.42f
            ),
            radius = baseRadius * 0.42f,
            center = Offset(cx, cy)
        )
        // Core border glow
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    IOCLSaffron.copy(alpha = 0.6f),
                    IOCLAmber.copy(alpha = 0.3f),
                    IOCLBlueAccent.copy(alpha = 0.4f),
                    IOCLSaffron.copy(alpha = 0.6f)
                ),
                center = Offset(
                    cx + cos(rotation * PI.toFloat() / 180f) * 2f,
                    cy + sin(rotation * PI.toFloat() / 180f) * 2f
                )
            ),
            radius = baseRadius * 0.42f,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )

        // ── Layer 6: Voice waveform (inside core) ──
        val wavePoints = 32
        val waveRadius = baseRadius * 0.28f
        val path = Path()
        for (i in 0..wavePoints) {
            val angle = (360f / wavePoints) * i
            val rad = angle * PI.toFloat() / 180f
            val wave = sin(rad * 4f + wavePhase) * waveAmplitude
            val r = waveRadius + wave
            val x = cx + r * cos(rad)
            val y = cy + r * sin(rad)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    IOCLSaffron.copy(alpha = 0.7f),
                    IOCLAmber.copy(alpha = 0.5f),
                    IOCLSaffronLight.copy(alpha = 0.6f)
                )
            ),
            style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // ── Layer 7: Orbiting particles ──
        val particleCount = 6
        for (i in 0 until particleCount) {
            val angle = thinkOrbit + (360f / particleCount) * i
            val orbitRadius = baseRadius * 0.65f
            val rad = angle * PI.toFloat() / 180f
            val px = cx + orbitRadius * cos(rad)
            val py = cy + orbitRadius * sin(rad)

            val particleAlpha = (sin(rad * 2f + wavePhase) * 0.3f + 0.5f).coerceIn(0.2f, 0.8f)
            val particleSize = if (i % 2 == 0) 3.5f else 2.5f

            // Particle glow
            drawCircle(
                color = IOCLSaffron.copy(alpha = particleAlpha * 0.3f),
                radius = particleSize * 3f,
                center = Offset(px, py)
            )
            // Particle core
            drawCircle(
                color = if (i % 3 == 0) IOCLSaffron else if (i % 3 == 1) IOCLAmber else IOCLBlueAccent,
                radius = particleSize,
                center = Offset(px, py),
                alpha = particleAlpha
            )
        }

        // ── Layer 8: Neural network connection lines ──
        val nodeCount = 5
        for (i in 0 until nodeCount) {
            val angle1 = rotation * 0.3f + (360f / nodeCount) * i
            val angle2 = -rotation * 0.2f + (360f / nodeCount) * ((i + 2) % nodeCount)
            val r1 = baseRadius * 0.55f
            val r2 = baseRadius * 0.35f
            val rad1 = angle1 * PI.toFloat() / 180f
            val rad2 = angle2 * PI.toFloat() / 180f

            drawLine(
                color = IOCLSaffron.copy(alpha = nodeFlicker * 0.15f),
                start = Offset(cx + r1 * cos(rad1), cy + r1 * sin(rad1)),
                end = Offset(cx + r2 * cos(rad2), cy + r2 * sin(rad2)),
                strokeWidth = 0.8f
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// COMPACT AI DOODLE — For message bubble voice button
// ═══════════════════════════════════════════════════════════════

@Composable
fun CompactSpeakingDoodle(
    modifier: Modifier = Modifier,
    isSpeaking: Boolean = false,
    size: Dp = 32.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "compact_doodle")

    val wavePhase by infiniteTransition.animateFloat(
        0f, 2f * PI.toFloat(),
        infiniteRepeatable(tween(if (isSpeaking) 500 else 2000, easing = LinearEasing)),
        label = "c_wave"
    )
    val barAmplitude by animateFloatAsState(
        targetValue = if (isSpeaking) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "c_amp"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isSpeaking) 0.4f else 0.1f,
        animationSpec = tween(300),
        label = "c_glow"
    )

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2
        val cy = this.size.height / 2
        val radius = this.size.minDimension / 2

        // Background glow
        drawCircle(
            color = IOCLSaffron.copy(alpha = glowAlpha),
            radius = radius,
            center = Offset(cx, cy)
        )

        // Sound wave bars
        val barCount = 5
        val barWidth = this.size.width * 0.08f
        val maxBarHeight = this.size.height * 0.45f
        val totalWidth = barCount * barWidth + (barCount - 1) * barWidth * 0.6f
        val startX = (this.size.width - totalWidth) / 2

        for (i in 0 until barCount) {
            val phase = wavePhase + i * 0.8f
            val height = (sin(phase) * 0.5f + 0.5f) * maxBarHeight * barAmplitude +
                    maxBarHeight * 0.15f
            val x = startX + i * barWidth * 1.6f

            drawRoundRect(
                color = if (isSpeaking) IOCLSaffron else IOCLGrayMedium,
                topLeft = Offset(x, cy - height / 2),
                size = androidx.compose.ui.geometry.Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2)
            )
        }
    }
}
