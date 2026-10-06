package com.iocl.knowledgehub.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iocl.knowledgehub.ui.theme.*
import kotlin.math.*
import kotlin.random.Random

// ============================================================
// ELEGANT GRADIENT BACKGROUND
// Replaces particle effects with a smooth flowing mesh gradient
// Premium, sophisticated, no random dots
// ============================================================
@Composable
fun ElegantGradientBackground(
    modifier: Modifier = Modifier,
    baseColors: List<Color> = listOf(IOCLNavyDark, IOCLNavy, IOCLBlue),
    accentColor: Color = IOCLSaffron,
    secondAccent: Color = IOCLBlueAccent
) {
    val infiniteTransition = rememberInfiniteTransition(label = "elegant_bg")
    val phase1 by infiniteTransition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(8000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p1"
    )
    val phase2 by infiniteTransition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(12000, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "p2"
    )
    val phase3 by infiniteTransition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(15000, easing = FastOutLinearInEasing), RepeatMode.Reverse),
        label = "p3"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Base gradient
        drawRect(brush = Brush.verticalGradient(baseColors))

        // Warm saffron glow — upper area, slow drift
        val glow1 = Offset(
            w * (0.4f + phase1 * 0.2f),
            h * (0.15f + phase2 * 0.15f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentColor.copy(alpha = 0.06f), Color.Transparent),
                center = glow1, radius = w * 0.7f
            ),
            center = glow1, radius = w * 0.7f
        )

        // Cool blue glow — lower area
        val glow2 = Offset(
            w * (0.7f - phase2 * 0.3f),
            h * (0.65f + phase3 * 0.15f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondAccent.copy(alpha = 0.05f), Color.Transparent),
                center = glow2, radius = w * 0.6f
            ),
            center = glow2, radius = w * 0.6f
        )

        // Subtle amber glow — corner accent
        val glow3 = Offset(
            w * (0.2f + phase3 * 0.3f),
            h * (0.8f - phase1 * 0.2f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(IOCLAmber.copy(alpha = 0.04f), Color.Transparent),
                center = glow3, radius = w * 0.5f
            ),
            center = glow3, radius = w * 0.5f
        )
    }
}

// ============================================================
// ANIMATED MESH GRADIENT BACKGROUND
// Creates a fluid, organic gradient that shifts over time
// ============================================================
@Composable
fun AnimatedMeshGradient(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(IOCLNavyDark, IOCLNavy, IOCLBlue, IOCLNavyLight)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val phase1 by infiniteTransition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p1"
    )
    val phase2 by infiniteTransition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(8000, easing = LinearOutSlowInEasing), RepeatMode.Reverse), label = "p2"
    )
    val phase3 by infiniteTransition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(10000, easing = FastOutLinearInEasing), RepeatMode.Reverse), label = "p3"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Layer 1: Base gradient
        drawRect(brush = Brush.verticalGradient(colors))

        // Layer 2: Moving radial gradients (organic blobs)
        val blob1Center = Offset(w * (0.3f + phase1 * 0.4f), h * (0.2f + phase2 * 0.3f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(IOCLSaffron.copy(alpha = 0.08f), Color.Transparent),
                center = blob1Center, radius = w * 0.6f
            ),
            center = blob1Center, radius = w * 0.6f
        )

        val blob2Center = Offset(w * (0.7f - phase2 * 0.3f), h * (0.6f + phase3 * 0.2f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(IOCLBlueAccent.copy(alpha = 0.06f), Color.Transparent),
                center = blob2Center, radius = w * 0.5f
            ),
            center = blob2Center, radius = w * 0.5f
        )

        val blob3Center = Offset(w * (0.5f + phase3 * 0.2f), h * (0.8f - phase1 * 0.3f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(IOCLAmber.copy(alpha = 0.05f), Color.Transparent),
                center = blob3Center, radius = w * 0.4f
            ),
            center = blob3Center, radius = w * 0.4f
        )
    }
}

// ============================================================
// WAVE BACKGROUND
// Animated sine wave decoration at bottom of sections
// ============================================================
@Composable
fun AnimatedWaveBackground(
    modifier: Modifier = Modifier,
    waveColor: Color = IOCLSaffron.copy(alpha = 0.1f),
    waveColor2: Color = IOCLBlueAccent.copy(alpha = 0.05f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        0f, 2 * PI.toFloat(),
        infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "wave_phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val waveHeight = h * 0.08f

        // Wave 1
        val path1 = Path().apply {
            moveTo(0f, h)
            for (x in 0..w.toInt() step 4) {
                val xf = x.toFloat()
                val y = h - waveHeight - sin(xf * 0.01f + phase) * waveHeight * 0.5f -
                        sin(xf * 0.005f + phase * 0.7f) * waveHeight * 0.3f
                lineTo(xf, y)
            }
            lineTo(w, h)
            close()
        }
        drawPath(path1, waveColor)

        // Wave 2 (offset)
        val path2 = Path().apply {
            moveTo(0f, h)
            for (x in 0..w.toInt() step 4) {
                val xf = x.toFloat()
                val y = h - waveHeight * 0.6f - sin(xf * 0.008f + phase + 1f) * waveHeight * 0.4f -
                        cos(xf * 0.012f + phase * 1.3f) * waveHeight * 0.2f
                lineTo(xf, y)
            }
            lineTo(w, h)
            close()
        }
        drawPath(path2, waveColor2)
    }
}

// ============================================================
// PULSE GLOW EFFECT
// Creates a pulsating glow ring around content
// ============================================================
@Composable
fun PulseGlow(
    modifier: Modifier = Modifier,
    color: Color = IOCLSaffron,
    size: Dp = 120.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        0.8f, 1.2f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        0.4f, 0.1f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent)
                )
            )
    )
}

// ============================================================
// SUBTLE BREATHING GLOW
// Gentle pulsing border glow for premium cards
// ============================================================
@Composable
fun SubtleBreathingGlow(
    modifier: Modifier = Modifier,
    color: Color = IOCLSaffron,
    size: Dp = 200.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val alpha by infiniteTransition.animateFloat(
        0.08f, 0.2f,
        infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathing_alpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent)
                )
            )
    )
}

// ============================================================
// ANIMATED GRADIENT BORDER
// Creates a rotating gradient border around content
// ============================================================
@Composable
fun AnimatedGradientBorderColors(): List<Color> {
    val infiniteTransition = rememberInfiniteTransition(label = "border")
    val hueShift by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(5000, easing = LinearEasing)),
        label = "hue_shift"
    )
    return listOf(
        IOCLSaffron,
        IOCLAmber,
        IOCLOrangeAccent,
        IOCLSaffronLight,
        IOCLSaffron
    )
}

// ============================================================
// SHIMMER EFFECT (Enhanced)
// More realistic shimmer with wave pattern
// ============================================================
@Composable
fun EnhancedShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "shimmer_offset"
    )
    return Brush.linearGradient(
        colors = listOf(
            ShimmerLight.copy(alpha = 0.6f),
            ShimmerHighlight.copy(alpha = 0.9f),
            ShimmerLight.copy(alpha = 0.6f)
        ),
        start = Offset(offset, offset * 0.7f),
        end = Offset(offset + 400f, offset * 0.7f + 400f)
    )
}

// ============================================================
// SPRING ANIMATION HELPERS
// Bouncy, physics-based animation specs
// ============================================================
fun <T> bouncySpring(
    dampingRatio: Float = 0.5f,
    stiffness: Float = 300f
): SpringSpec<T> = spring(dampingRatio = dampingRatio, stiffness = stiffness)

fun <T> snappySpring(): SpringSpec<T> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

// ============================================================
// NUMBER COUNTER ANIMATION
// Smooth counting animation for stat displays
// ============================================================
@Composable
fun animateIntCounter(
    targetValue: Int,
    durationMs: Int = 2000
): Int {
    val animated by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        label = "counter"
    )
    return animated
}

// ============================================================
// STAGGER ANIMATION HELPER
// Creates staggered entrance animations for lists
// ============================================================
@Composable
fun staggeredAlpha(index: Int, baseDelay: Int = 80): Float {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * baseDelay.toLong())
        alpha.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
    }
    return alpha.value
}

@Composable
fun staggeredOffset(index: Int, baseDelay: Int = 80): Float {
    val offset = remember { Animatable(30f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * baseDelay.toLong())
        offset.animateTo(0f, animationSpec = tween(600, easing = FastOutSlowInEasing))
    }
    return offset.value
}

// ============================================================
// BACKWARD COMPATIBILITY — Keep old names as no-op or redirect
// FloatingParticles is kept as a thin wrapper that renders
// the ElegantGradientBackground instead, so callers don't break
// ============================================================
@Composable
fun FloatingParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 30,
    colors: List<Color> = listOf(
        IOCLSaffron.copy(alpha = 0.3f),
        IOCLAmber.copy(alpha = 0.2f),
        IOCLBlueAccent.copy(alpha = 0.15f),
        IOCLWhite.copy(alpha = 0.1f)
    )
) {
    // Replaced with elegant gradient — no particle dots
    ElegantGradientBackground(modifier = modifier)
}
