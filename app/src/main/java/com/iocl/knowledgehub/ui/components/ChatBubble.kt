package com.iocl.knowledgehub.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iocl.knowledgehub.domain.model.ChatMessage
import com.iocl.knowledgehub.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

@Composable
fun ChatBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val isUser = message.isFromUser

    // Entrance animation
    val alpha = remember { Animatable(0f) }
    val offsetX = remember { Animatable(if (isUser) 30f else -30f) }
    val scale = remember { Animatable(0.9f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(300))
        offsetX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f))
        scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f))
    }

    val shape = RoundedCornerShape(
        topStart = 20.dp, topEnd = 20.dp,
        bottomStart = if (isUser) 20.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 20.dp
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha.value)
            .offset(x = offsetX.value.dp)
            .scale(scale.value),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = shape,
            color = Color.Transparent,
            modifier = Modifier.widthIn(max = 300.dp),
            shadowElevation = if (isUser) 4.dp else 2.dp
        ) {
            Box(
                modifier = if (isUser) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(IOCLNavy, IOCLBlue, IOCLNavyLight)
                        ),
                        shape
                    )
                } else {
                    Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape
                        )
                        .drawBehind {
                            // Subtle animated border for AI messages
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    listOf(
                                        IOCLSaffron.copy(alpha = 0.15f),
                                        IOCLAmber.copy(alpha = 0.05f),
                                        IOCLBlueAccent.copy(alpha = 0.1f)
                                    )
                                ),
                                cornerRadius = CornerRadius(20.dp.toPx()),
                                style = Stroke(1f)
                            )
                        }
                }
            ) {
                Column(Modifier.padding(14.dp)) {
                    // AI label for bot messages
                    if (!isUser) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(IOCLSaffron.copy(0.3f), IOCLAmber.copy(0.1f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(IOCLSaffron)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "IOCL AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = IOCLSaffron,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Text(
                        message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) IOCLWhite else MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Timestamp
        Text(
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
            modifier = Modifier.padding(top = 3.dp, start = 10.dp, end = 10.dp)
        )
    }
}

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .drawBehind {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(IOCLSaffron.copy(0.1f), IOCLAmber.copy(0.05f))
                    ),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(1f)
                )
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // AI label
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(IOCLSaffron.copy(0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(IOCLSaffron))
        }

        Spacer(Modifier.width(4.dp))

        // Three orbiting dots
        repeat(3) { i ->
            val scale by infiniteTransition.animateFloat(
                0.5f, 1.2f,
                infiniteRepeatable(
                    tween(500, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse,
                    StartOffset(i * 180)
                ),
                label = "dot_scale_$i"
            )
            val alpha by infiniteTransition.animateFloat(
                0.3f, 1f,
                infiniteRepeatable(
                    tween(500, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse,
                    StartOffset(i * 180)
                ),
                label = "dot_alpha_$i"
            )

            Box(
                Modifier
                    .size(8.dp)
                    .scale(scale)
                    .alpha(alpha)
                    .background(
                        Brush.radialGradient(listOf(IOCLSaffron, IOCLAmber)),
                        CircleShape
                    )
            )
        }
    }
}
