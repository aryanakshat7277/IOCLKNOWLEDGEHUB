package com.iocl.knowledgehub.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iocl.knowledgehub.ui.theme.*

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(20f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(500))
        offsetY.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 200f))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "error")
    val iconPulse by infiniteTransition.animateFloat(
        0.9f, 1.1f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp)
            .alpha(alpha.value)
            .offset(y = offsetY.value.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Error icon with glow
        Box(
            modifier = Modifier.size(80.dp).scale(iconPulse),
            contentAlignment = Alignment.Center
        ) {
            PulseGlow(color = IOCLRedLight, size = 80.dp)
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(IOCLRedLight.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("!", fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Serif,
                    color = IOCLRedLight, style = MaterialTheme.typography.headlineMedium)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Oops! Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(28.dp))

        // Retry button with gradient
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                Modifier
                    .background(
                        Brush.horizontalGradient(listOf(IOCLSaffron, IOCLAmber)),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp), tint = IOCLNavyDark)
                    Spacer(Modifier.width(8.dp))
                    Text("Try Again", fontWeight = FontWeight.Bold, color = IOCLNavyDark, fontFamily = FontFamily.Serif)
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.8f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(600))
        scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 200f))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "empty")
    val floatY by infiniteTransition.animateFloat(
        -5f, 5f,
        infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp)
            .alpha(alpha.value)
            .scale(scale.value),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Floating icon with glow
        Box(
            modifier = Modifier.size(100.dp).offset(y = floatY.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(IOCLSaffron.copy(0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    Modifier.size(40.dp),
                    tint = IOCLGrayMedium.copy(0.6f)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

