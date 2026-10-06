package com.iocl.knowledgehub.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iocl.knowledgehub.ui.theme.*
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun DocumentCard(
    title: String,
    subtitle: String,
    coverImageUrl: String,
    category: String,
    viewCount: Int,
    isBookmarked: Boolean,
    onCardClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.96f else 1f,
        spring(dampingRatio = 0.5f, stiffness = 400f), label = "card_scale"
    )

    val bmScale = remember { Animatable(1f) }
    LaunchedEffect(isBookmarked) {
        bmScale.animateTo(1.3f, tween(100))
        bmScale.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 500f))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource, null, onClick = onCardClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 12.dp,
            hoveredElevation = 10.dp
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                // Cover image
                if (coverImageUrl.startsWith("file:///android_asset/")) {
                    PdfCoverImage(
                        pdfUrl = coverImageUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = coverImageUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Premium gradient overlay (3-stop)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.4f to Color.Transparent,
                                0.7f to Color.Black.copy(0.3f),
                                1f to Color.Black.copy(0.8f)
                            )
                        )
                )

                // Top decorative wave pattern
                Canvas(modifier = Modifier.fillMaxWidth().height(40.dp).align(Alignment.TopCenter)) {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, size.height * 0.6f)
                        for (x in size.width.toInt() downTo 0 step 3) {
                            val xf = x.toFloat()
                            val y = size.height * 0.6f + sin(xf * 0.02f) * 6f
                            lineTo(xf, y)
                        }
                        close()
                    }
                    drawPath(path, IOCLNavy.copy(alpha = 0.4f))
                }

                // Category badge with glow
                Surface(
                    modifier = Modifier.padding(14.dp).align(Alignment.TopStart),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Transparent,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(listOf(IOCLSaffron, IOCLAmber)),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = IOCLWhite,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Bookmark button with bounce animation
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .scale(bmScale.value)
                ) {
                    // Glow behind bookmark
                    if (isBookmarked) {
                        Icon(
                            Icons.Filled.Bookmark, null,
                            Modifier.size(28.dp),
                            tint = IOCLSaffron.copy(alpha = 0.4f)
                        )
                    }
                    Icon(
                        if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        "Bookmark",
                        Modifier.size(24.dp),
                        tint = if (isBookmarked) IOCLSaffron else IOCLWhite
                    )
                }


            }

            // Content section with premium typography
            Column(Modifier.padding(18.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(IOCLSaffron)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentCardShimmer(modifier: Modifier = Modifier) {
    val shimmerBrush = EnhancedShimmerBrush()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(shimmerBrush)
            )
            Column(Modifier.padding(18.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.75f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmerBrush)
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.5f)
                        .height(13.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
        }
    }
}

