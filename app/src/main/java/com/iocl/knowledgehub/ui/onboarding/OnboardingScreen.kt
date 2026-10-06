package com.iocl.knowledgehub.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iocl.knowledgehub.ui.components.*
import com.iocl.knowledgehub.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val gradientColors: List<Color>,
    val accentColor: Color
)

val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Filled.MenuBook,
        title = "Digital Library",
        subtitle = "Access thousands of IOCL magazines, newspapers, and publications from one centralized platform",
        gradientColors = listOf(Color(0xFF020510), IOCLNavyDark, IOCLNavy),
        accentColor = IOCLSaffron
    ),
    OnboardingPage(
        icon = Icons.Filled.Search,
        title = "Smart Search",
        subtitle = "Find any document instantly with our AI-powered search engine across all publications",
        gradientColors = listOf(IOCLNavyDark, IOCLBlue, IOCLNavy),
        accentColor = IOCLBlueAccent
    ),
    OnboardingPage(
        icon = Icons.Filled.SmartToy,
        title = "AI Assistant",
        subtitle = "Chat with our Gemini-powered AI to get answers, summaries, and insights from your documents",
        gradientColors = listOf(Color(0xFF0A1535), IOCLNavyDark, Color(0xFF0D1B4A)),
        accentColor = IOCLAmber
    ),
    OnboardingPage(
        icon = Icons.Filled.AdminPanelSettings,
        title = "Complete Control",
        subtitle = "Admins can upload, manage, and analyze document engagement with a powerful dashboard",
        gradientColors = listOf(IOCLNavyDark, IOCLNavy, IOCLNavyLight),
        accentColor = IOCLGreen
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.size - 1

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageData = onboardingPages[page]
            val pageOffset = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction).absoluteValue

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Parallax effect on background
                        translationX = size.width * pageOffset * -0.3f
                        alpha = 1f - pageOffset.coerceIn(0f, 1f) * 0.5f
                    }
            ) {
                // Background gradient
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(pageData.gradientColors))
                )


                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .graphicsLayer {
                            // Content slides differently from background (parallax)
                            translationX = size.width * pageOffset * 0.2f
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(Modifier.weight(0.3f))

                    // Icon with animated rings
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(180.dp)
                    ) {
                        // Pulsing glow
                        PulseGlow(
                            color = pageData.accentColor,
                            size = 180.dp
                        )

                        // Rotating ring
                        val infiniteTransition = rememberInfiniteTransition(label = "ob_ring_$page")
                        val rotation by infiniteTransition.animateFloat(
                            0f, 360f,
                            infiniteRepeatable(tween(10000, easing = LinearEasing)),
                            label = "ring_rot_$page"
                        )
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .rotate(rotation)
                                .border(
                                    1.dp,
                                    Brush.sweepGradient(
                                        listOf(
                                            pageData.accentColor.copy(alpha = 0.4f),
                                            Color.Transparent,
                                            pageData.accentColor.copy(alpha = 0.2f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )

                        // Icon container
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            pageData.accentColor.copy(alpha = 0.2f),
                                            pageData.accentColor.copy(alpha = 0.05f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                pageData.icon, null,
                                Modifier.size(44.dp),
                                tint = pageData.accentColor
                            )
                        }
                    }

                    Spacer(Modifier.height(48.dp))

                    // Title
                    Text(
                        pageData.title,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Serif,
                        color = IOCLWhite,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(Modifier.height(16.dp))

                    // Accent line
                    Box(
                        Modifier
                            .width(48.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, pageData.accentColor, Color.Transparent)
                                )
                            )
                    )

                    Spacer(Modifier.height(20.dp))

                    // Subtitle
                    Text(
                        pageData.subtitle,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Serif,
                        color = IOCLGrayMedium,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    Spacer(Modifier.weight(0.5f))
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Custom page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                onboardingPages.forEachIndexed { index, page ->
                    val isActive = pagerState.currentPage == index
                    val width by animateDpAsState(
                        if (isActive) 28.dp else 8.dp,
                        spring(dampingRatio = 0.6f), label = "ind_w"
                    )
                    val color by animateColorAsState(
                        if (isActive) page.accentColor else IOCLGrayMedium.copy(0.3f),
                        tween(300), label = "ind_c"
                    )
                    Box(
                        modifier = Modifier
                            .width(width)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Action buttons
            AnimatedContent(
                targetState = isLastPage,
                transitionSpec = {
                    fadeIn(tween(300)) + scaleIn(tween(300)) togetherWith
                            fadeOut(tween(200)) + scaleOut(tween(200))
                },
                label = "button"
            ) { lastPage ->
                if (lastPage) {
                    // Get Started button
                    Button(
                        onClick = onGetStarted,
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(listOf(IOCLSaffron, IOCLAmber)),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Get Started",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif,
                                    color = IOCLNavyDark
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = IOCLNavyDark)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onGetStarted) {
                            Text("Skip", color = IOCLGrayMedium, fontFamily = FontFamily.Serif)
                        }
                        Button(
                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .background(
                                        Brush.linearGradient(listOf(IOCLSaffron, IOCLAmber)),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = IOCLNavyDark, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
