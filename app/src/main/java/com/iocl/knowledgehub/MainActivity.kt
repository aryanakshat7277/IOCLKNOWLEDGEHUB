package com.iocl.knowledgehub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.iocl.knowledgehub.navigation.NavGraph
import com.iocl.knowledgehub.ui.components.FloatingParticles
import com.iocl.knowledgehub.ui.components.GeometricPatternOverlay
import com.iocl.knowledgehub.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlin.math.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            // Permissions handled
        }
        
        requestPermissionLauncher.launch(permissions.toTypedArray())

        enableEdgeToEdge()
        setContent {
            IOCLKnowledgeHubTheme {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    GodModeSplashScreen(onSplashFinished = { showSplash = false })
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        NavGraph(navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
fun GodModeSplashScreen(onSplashFinished: () -> Unit) {
    // ========== ORCHESTRATED ANIMATION TIMELINE ==========
    val logoScale = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoRotation = remember { Animatable(-15f) }
    val ringScale = remember { Animatable(0f) }
    val ringAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(40f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val lineWidth = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val overallAlpha = remember { Animatable(1f) }

    // Continuous animations
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val ringRotation by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "ring_rotate"
    )
    val glowPulse by infiniteTransition.animateFloat(
        0.2f, 0.6f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_pulse"
    )
    val secondRingRotation by infiniteTransition.animateFloat(
        360f, 0f,
        infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "ring2_rotate"
    )

    LaunchedEffect(Unit) {
        // Phase 1: Logo entrance with overshoot bounce
        logoAlpha.animateTo(1f, tween(400))
        logoScale.animateTo(1.3f, tween(500, easing = FastOutSlowInEasing))
        logoRotation.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
        logoScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 200f))

        // Phase 2: Decorative rings expand
        ringAlpha.animateTo(1f, tween(300))
        ringScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 150f))

        // Phase 3: Accent line sweep
        lineWidth.animateTo(1f, tween(800, easing = FastOutSlowInEasing))

        // Phase 4: Title slides up with spring
        titleAlpha.animateTo(1f, tween(500))
        titleOffset.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 200f))

        // Phase 5: Subtitle & tagline
        delay(150)
        subtitleAlpha.animateTo(1f, tween(400))
        delay(200)
        taglineAlpha.animateTo(1f, tween(400))

        // Hold
        delay(1200)

        // Phase 6: Exit — everything scales up and fades
        overallAlpha.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(overallAlpha.value)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Layer 1: Animated mesh gradient background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            // Deep dark gradient
            drawRect(brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF020510), IOCLNavyDark, Color(0xFF0A1535))
            ))

            // Organic light blobs
            val t = ringRotation * PI.toFloat() / 180f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(IOCLSaffron.copy(alpha = glowPulse * 0.12f), Color.Transparent),
                    center = Offset(w * 0.5f + cos(t) * w * 0.1f, h * 0.4f + sin(t) * h * 0.05f),
                    radius = w * 0.6f
                ),
                center = Offset(w * 0.5f, h * 0.4f), radius = w * 0.6f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(IOCLBlueAccent.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.3f, h * 0.7f), radius = w * 0.5f
                ),
                center = Offset(w * 0.3f, h * 0.7f), radius = w * 0.5f
            )
        }

        // Layer 2: Geometric pattern
        GeometricPatternOverlay(
            modifier = Modifier.fillMaxSize().alpha(0.4f),
            color = IOCLWhite.copy(alpha = 0.015f)
        )

        // Layer 3: Floating particles
        FloatingParticles(
            modifier = Modifier.fillMaxSize(),
            particleCount = 40,
            colors = listOf(
                IOCLSaffron.copy(0.4f), IOCLAmber.copy(0.3f),
                IOCLBlueAccent.copy(0.2f), IOCLWhite.copy(0.15f)
            )
        )

        // Layer 4: Decorative rotating rings
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(ringScale.value)
                .alpha(ringAlpha.value)
        ) {
            // Ring 1 — Dashed outer ring
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(ringRotation)
            ) {
                val dashLength = 15f
                val gapLength = 25f
                drawCircle(
                    color = IOCLSaffron.copy(alpha = 0.25f),
                    style = Stroke(
                        width = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, gapLength))
                    )
                )
            }

            // Ring 2 — Inner dotted ring (counter-rotating)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .rotate(secondRingRotation)
            ) {
                drawCircle(
                    color = IOCLAmber.copy(alpha = 0.15f),
                    style = Stroke(
                        width = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 12f))
                    )
                )
            }

            // Ring 3 — Solid thin ring
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp)
            ) {
                drawCircle(
                    color = IOCLSaffron.copy(alpha = 0.1f),
                    style = Stroke(width = 0.5f)
                )
            }

            // Orbital dots on ring 1
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(ringRotation)
            ) {
                val radius = size.width / 2
                for (angle in listOf(0f, 90f, 180f, 270f)) {
                    val rad = angle * PI.toFloat() / 180f
                    val x = center.x + radius * cos(rad)
                    val y = center.y + radius * sin(rad)
                    drawCircle(IOCLSaffron.copy(alpha = 0.6f), 3f, Offset(x, y))
                    drawCircle(IOCLSaffron.copy(alpha = 0.2f), 8f, Offset(x, y))
                }
            }
        }

        // Layer 5: Main content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // IOCL Logo with gradient text
            Box(
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .rotate(logoRotation.value)
            ) {
                // Glow behind text
                Text(
                    text = "IOCL",
                    fontSize = 76.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif,
                    color = IOCLSaffron.copy(alpha = glowPulse * 0.4f),
                    modifier = Modifier.blur(20.dp)
                )
                // Main text
                Text(
                    text = "IOCL",
                    fontSize = 76.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif,
                    style = MaterialTheme.typography.displayLarge.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(IOCLWhite, IOCLSaffronLight, IOCLAmber, IOCLSaffron)
                        ),
                        shadow = Shadow(
                            color = IOCLSaffron.copy(alpha = 0.4f),
                            offset = Offset(0f, 4f),
                            blurRadius = 20f
                        )
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Animated accent line with gradient
            Box(
                modifier = Modifier
                    .width(140.dp * lineWidth.value)
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                IOCLSaffron.copy(alpha = 0.4f),
                                IOCLAmber,
                                IOCLSaffron,
                                IOCLAmber,
                                IOCLSaffron.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(1.dp)
                    )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = "Knowledge Hub",
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Serif,
                color = IOCLWhite.copy(alpha = titleAlpha.value),
                modifier = Modifier.offset(y = titleOffset.value.dp),
                style = MaterialTheme.typography.headlineMedium.copy(
                    shadow = Shadow(IOCLNavy, Offset(0f, 2f), 10f)
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "INDIAN OIL CORPORATION LIMITED",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                color = IOCLGrayMedium.copy(alpha = subtitleAlpha.value),
                letterSpacing = 5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Premium loading bar
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(2.dp)
                    .alpha(taglineAlpha.value)
                    .clip(RoundedCornerShape(1.dp))
                    .background(IOCLWhite.copy(alpha = 0.05f))
            ) {
                val loadingTransition = rememberInfiniteTransition(label = "loading")
                val loadingOffset by loadingTransition.animateFloat(
                    -1f, 2f,
                    infiniteRepeatable(tween(1500, easing = LinearEasing)),
                    label = "loading_offset"
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.3f)
                        .offset(x = (loadingOffset * 180).dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, IOCLSaffron, Color.Transparent)
                            ),
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }

        // Bottom tagline
        Text(
            text = "Energizing Lives Since 1959",
            fontSize = 11.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Serif,
            color = IOCLGray.copy(alpha = taglineAlpha.value * 0.7f),
            textAlign = TextAlign.Center,
            letterSpacing = 3.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        )

        // Bottom decorative line
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .width(40.dp)
                .height(1.dp)
                .alpha(taglineAlpha.value)
                .background(IOCLSaffron.copy(alpha = 0.3f))
        )
    }
}
