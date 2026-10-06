package com.iocl.knowledgehub.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.iocl.knowledgehub.domain.model.Magazine
import com.iocl.knowledgehub.domain.model.Newspaper
import com.iocl.knowledgehub.domain.model.toMonthName
import com.iocl.knowledgehub.ui.components.*
import com.iocl.knowledgehub.R
import com.iocl.knowledgehub.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlinx.coroutines.delay
import kotlin.math.*

// ════════════════════════════════════════════════════════════════
// GOD-LEVEL DASHBOARD SCREEN
// Ultra-premium, animation-rich IOCL Knowledge Hub experience
// ════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToMagazines: () -> Unit,
    onNavigateToAdminNotices: () -> Unit,
    onNavigateToLiveNews: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToChatbot: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToMagazineDetail: (String) -> Unit,
    onNavigateToNewspaperDetail: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val pullToRefreshState = rememberPullToRefreshState()

    // ── Master entrance animation ──
    var screenVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { screenVisible = true }

    Scaffold(
        bottomBar = {
            IOCLBottomBar(
                currentRoute = "dashboard",
                onItemClick = { route ->
                    when (route) {
                        "magazines" -> onNavigateToMagazines()
                        "admin_notices" -> onNavigateToAdminNotices()
                        "live_news" -> onNavigateToLiveNews()
                        "chatbot" -> onNavigateToChatbot()
                        "profile" -> onNavigateToProfile()
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (uiState.error != null && uiState.recentMagazines.isEmpty() && uiState.recentNewspapers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                ErrorView(
                    message = uiState.error ?: "Unknown error occurred",
                    onRetry = { viewModel.refresh() }
                )
            }
        } else {
            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                state = pullToRefreshState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = uiState.isLoading,
                        modifier = Modifier.align(Alignment.TopCenter),
                        color = IOCLSaffron,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // ═══════════════════════════════════════
                    // 1. PARALLAX COLLAPSING HEADER
                    // ═══════════════════════════════════════
                    PremiumDashboardHeader(
                        userName = uiState.userName,
                        isAdmin = uiState.isAdmin,
                        unreadNotifications = uiState.unreadNotifications,
                        dataSource = uiState.dataSource,
                        scrollOffset = scrollState.value,
                        onNotificationsClick = onNavigateToNotifications,
                        onAdminClick = onNavigateToAdmin
                    )

                    // ── Floating Search Bar (overlaps header) ──
                    IOCLSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChanged,
                        onSearch = onNavigateToSearch,
                        placeholder = "Search magazines, papers...",
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .offset(y = (-24).dp)
                    )

                    Spacer(Modifier.height(4.dp))





                    // ═══════════════════════════════════════
                    // 3. QUICK ACTIONS GRID (2×2)
                    // ═══════════════════════════════════════
                    PremiumQuickActionsGrid(
                        onMagazinesClick = onNavigateToMagazines,
                        onAdminNoticesClick = onNavigateToAdminNotices,
                        onLiveNewsClick = onNavigateToLiveNews,
                        onBookmarksClick = onNavigateToBookmarks,
                        onChatbotClick = onNavigateToChatbot,
                        isGuest = uiState.isGuest
                    )

                    Spacer(Modifier.height(28.dp))

                    // ═══════════════════════════════════════
                    // 3.5. TRENDING TAGS
                    // ═══════════════════════════════════════
                    if (uiState.trendingTags.isNotEmpty()) {
                        TrendingTagsSection(tags = uiState.trendingTags)
                        Spacer(Modifier.height(16.dp))
                    }

                    // ═══════════════════════════════════════
                    // 4. ANIMATED WAVE DIVIDER
                    // ═══════════════════════════════════════
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        AnimatedWaveBackground(
                            waveColor = IOCLSaffron.copy(alpha = 0.08f),
                            waveColor2 = IOCLBlueAccent.copy(alpha = 0.04f)
                        )
                    }

                    // ═══════════════════════════════════════
                    // 5. RECENT MAGAZINES SECTION
                    // ═══════════════════════════════════════
                    PremiumSectionHeader(
                        title = "Recent Magazines",
                        accentColor = IOCLSaffron,
                        onSeeAllClick = onNavigateToMagazines
                    )
                    Spacer(Modifier.height(14.dp))

                    if (uiState.isLoading) {
                        PremiumShimmerRow()
                    } else if (uiState.recentMagazines.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Outlined.MenuBook,
                            title = "No Magazines Yet",
                            subtitle = "Check back later for new publications",
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(
                                items = uiState.recentMagazines,
                                key = { index, m -> "mag_${index}_${m.id}" }
                            ) { index, magazine ->
                                val alpha = staggeredAlpha(index, baseDelay = 80)
                                val offsetY = staggeredOffset(index, baseDelay = 80)

                                PremiumDocumentCard(
                                    title = magazine.title,
                                    subtitle = "${magazine.month.toMonthName()} ${magazine.year}",
                                    description = magazine.description,
                                    tags = magazine.tags,
                                    coverImageUrl = if (magazine.pdfUrl.startsWith("file:///android_asset/")) magazine.pdfUrl else magazine.coverImageUrl,
                                    category = magazine.category.name.lowercase()
                                        .replaceFirstChar { it.uppercase() }
                                        .replace("_", " "),
                                    viewCount = magazine.viewCount,
                                    downloadCount = magazine.downloadCount,
                                    isBookmarked = magazine.isBookmarked,
                                    onCardClick = { onNavigateToMagazineDetail(magazine.id) },
                                    onBookmarkClick = { viewModel.toggleMagazineBookmark(magazine) },
                                    accentColor = IOCLBlueAccent,
                                    modifier = Modifier
                                        .width(280.dp)
                                        .alpha(alpha)
                                        .offset(y = offsetY.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // ═══════════════════════════════════════
                    // 6. WAVE DIVIDER 2
                    // ═══════════════════════════════════════
                    if (!uiState.isGuest) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                        ) {
                            AnimatedWaveBackground(
                                waveColor = IOCLBlueAccent.copy(alpha = 0.06f),
                                waveColor2 = IOCLSaffron.copy(alpha = 0.04f)
                            )
                        }

                        // ═══════════════════════════════════════
                        // 7. RECENT NEWSPAPERS SECTION
                        // ═══════════════════════════════════════
                        PremiumSectionHeader(
                            title = "Recent Notices",
                            accentColor = IOCLBlueAccent,
                            onSeeAllClick = onNavigateToAdminNotices
                        )
                        Spacer(Modifier.height(14.dp))

                        if (uiState.isLoading) {
                            PremiumShimmerRow()
                        } else if (uiState.recentNewspapers.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Outlined.Newspaper,
                                title = "No Newspapers Yet",
                                subtitle = "Check back later for latest editions",
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(
                                    items = uiState.recentNewspapers,
                                    key = { index, n -> "news_${index}_${n.id}" }
                                ) { index, newspaper ->
                                    val alpha = staggeredAlpha(index, baseDelay = 80)
                                    val offsetY = staggeredOffset(index, baseDelay = 80)

                                    PremiumDocumentCard(
                                        title = newspaper.title,
                                        subtitle = newspaper.headline,
                                        description = newspaper.summary,
                                        tags = newspaper.tags,
                                        coverImageUrl = newspaper.coverImageUrl,
                                        category = newspaper.category.name.lowercase()
                                            .replaceFirstChar { it.uppercase() }
                                            .replace("_", " "),
                                        viewCount = newspaper.viewCount,
                                        downloadCount = newspaper.downloadCount,
                                        isBookmarked = newspaper.isBookmarked,
                                        onCardClick = { onNavigateToAdminNotices() },
                                        onBookmarkClick = { viewModel.toggleNewspaperBookmark(newspaper) },
                                        accentColor = IOCLSaffron,
                                        modifier = Modifier
                                            .width(280.dp)
                                            .alpha(alpha)
                                            .offset(y = offsetY.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                    } else {
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// ▌ PREMIUM DASHBOARD HEADER ▐
// ─ Parallax mesh gradient, floating particles, typewriter name,
//   pulsing notification badge, gold admin shield
// ════════════════════════════════════════════════════════════════════

@Composable
private fun PremiumDashboardHeader(
    userName: String,
    isAdmin: Boolean,
    unreadNotifications: Int,
    dataSource: String,
    scrollOffset: Int,
    onNotificationsClick: () -> Unit,
    onAdminClick: () -> Unit
) {
    // Parallax collapse: header shrinks as you scroll
    val maxHeaderHeight = 220f
    val minHeaderHeight = 130f
    val collapseProgress = (scrollOffset / 400f).coerceIn(0f, 1f)
    val headerHeight = (maxHeaderHeight - (maxHeaderHeight - minHeaderHeight) * collapseProgress).dp
    val parallaxOffset = scrollOffset * 0.4f

    // Typewriter effect for user name
    val displayName = if (isAdmin) "Admin" else userName.ifEmpty { "User" }
    var typewriterText by remember { mutableStateOf("") }
    LaunchedEffect(displayName) {
        typewriterText = ""
        displayName.forEachIndexed { index, _ ->
            delay(60)
            typewriterText = displayName.substring(0, index + 1)
        }
    }

    // Subtitle fade entrance
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (typewriterText == displayName) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "subtitle_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
    ) {
        // ── Layer 1: Animated Mesh Gradient ──
        AnimatedMeshGradient(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-parallaxOffset * 0.3f).dp),
            colors = listOf(IOCLNavyDark, IOCLNavy, IOCLBlue, IOCLNavyDark)
        )

        // ── Layer 2: Geometric Pattern Overlay ──
        GeometricPatternOverlay(
            modifier = Modifier.fillMaxSize(),
            color = IOCLWhite.copy(alpha = 0.02f)
        )

        // ── Layer 3: Floating Particles ──
        FloatingParticles(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-parallaxOffset * 0.15f).dp),
            particleCount = 25,
            colors = listOf(
                IOCLSaffron.copy(alpha = 0.35f),
                IOCLAmber.copy(alpha = 0.25f),
                IOCLBlueAccent.copy(alpha = 0.2f),
                IOCLWhite.copy(alpha = 0.12f)
            )
        )

        // ── Layer 4: Bottom Gradient Fade ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // ── Content ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp)
        ) {
            // Top Row: Greeting + Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Greeting with animated fade
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_iocl_logo),
                            contentDescription = "IOCL Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = getGreeting(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = IOCLWhite.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.Serif
                        )
                        Spacer(Modifier.width(8.dp))
                        // Data Source Indicator
                        if (dataSource != "loading") {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (dataSource == "offline") IOCLOrange.copy(alpha = 0.2f) else IOCLGreen.copy(alpha = 0.2f),
                                border = BorderStroke(0.5.dp, if (dataSource == "offline") IOCLOrange.copy(alpha = 0.5f) else IOCLGreen.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (dataSource == "offline") IOCLOrange else IOCLGreen)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = dataSource.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        color = IOCLWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    // Typewriter Name
                    Text(
                        text = typewriterText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = IOCLWhite,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ── Admin Shield with Gold Gradient ──
                    AnimatedVisibility(
                        visible = isAdmin,
                        enter = fadeIn(tween(400)) + scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ),
                        exit = fadeOut() + scaleOut()
                    ) {
                        GoldAdminBadge(onClick = {})
                    }

                    // ── Notification Bell with Pulse Glow ──
                    PulsingNotificationBell(
                        unreadCount = unreadNotifications,
                        onClick = onNotificationsClick
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Bottom subtitle
            Text(
                text = "IOCL Knowledge Hub",
                style = MaterialTheme.typography.bodySmall,
                color = IOCLSaffronLight.copy(alpha = 0.8f * subtitleAlpha),
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.Serif
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// ▌ GOLD ADMIN BADGE ▐
// ═ Shield icon with animated gold gradient sweep ═
// ════════════════════════════════════════════════════════════════

@Composable
private fun GoldAdminBadge(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "admin_gold")
    val shimmerPhase by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "gold_shimmer"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .drawBehind {
                val rad = shimmerPhase * PI.toFloat() / 180f
                val brush = Brush.sweepGradient(
                    0f to GradientGoldStart,
                    0.3f to GradientGoldEnd,
                    0.5f to GradientGoldStart.copy(alpha = 0.8f),
                    0.7f to GradientGoldEnd,
                    1f to GradientGoldStart,
                    center = Offset(
                        size.width / 2 + cos(rad) * 4f,
                        size.height / 2 + sin(rad) * 4f
                    )
                )
                drawCircle(brush = brush, radius = size.minDimension / 2)
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner dark circle for contrast
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(IOCLNavyDark.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = "Admin Panel",
                tint = GradientGoldEnd,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// ▌ PULSING NOTIFICATION BELL ▐
// ═ Bell icon with animated pulse glow on unread notifications ═
// ════════════════════════════════════════════════════════════════

@Composable
private fun PulsingNotificationBell(
    unreadCount: Int,
    onClick: () -> Unit
) {
    val hasUnread = unreadCount > 0

    val infiniteTransition = rememberInfiniteTransition(label = "bell_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (hasUnread) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "bell_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (hasUnread) 0.4f else 0f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "bell_glow"
    )

    Box(contentAlignment = Alignment.Center) {
        // Glow ring behind bell
        if (hasUnread) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                IOCLSaffron.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        BadgedBox(
            badge = {
                if (hasUnread) {
                    Badge(
                        containerColor = IOCLSaffron,
                        contentColor = IOCLWhite
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else "$unreadCount",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = if (hasUnread) Icons.Filled.Notifications
                    else Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = IOCLWhite,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// ▌ PREMIUM STAT RINGS ROW ▐
// ═ Horizontal scroll of AnimatedStatRing components with
//   GlassmorphismCard backgrounds ═
// ════════════════════════════════════════════════════════════════

@Composable
private fun PremiumStatRingsRow(
    totalMagazines: Int,
    totalNewspapers: Int,
    totalViews: Int,
    totalDownloads: Int
) {
    val statItems = listOf(
        StatItem("Magazines", totalMagazines.toFloat(), 200f, IOCLBlueAccent, Icons.Filled.MenuBook),
        StatItem("Newspapers", totalNewspapers.toFloat(), 200f, IOCLSaffron, Icons.Filled.Newspaper),
        StatItem("Views", totalViews.toFloat(), 10000f, IOCLTeal, Icons.Filled.Visibility),
        StatItem("Downloads", totalDownloads.toFloat(), 5000f, IOCLGreen, Icons.Filled.Download)
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(statItems) { index, item ->
            val alpha = staggeredAlpha(index, baseDelay = 100)
            val offsetY = staggeredOffset(index, baseDelay = 100)

            GlassmorphismCard(
                modifier = Modifier
                    .width(140.dp)
                    .alpha(alpha)
                    .offset(y = offsetY.dp),
                cornerRadius = 20.dp,
                blurRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedStatRing(
                        value = item.value,
                        maxValue = item.maxValue,
                        label = item.label,
                        color = item.color,
                        size = 90.dp,
                        strokeWidth = 7.dp
                    )
                }
            }
        }
    }
}

private data class StatItem(
    val label: String,
    val value: Float,
    val maxValue: Float,
    val color: Color,
    val icon: ImageVector
)

@Composable
private fun StatRingsShimmer() {
    val shimmerBrush = EnhancedShimmerBrush()
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(4) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(140.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// ▌ PREMIUM QUICK ACTIONS GRID (2×2) ▐
// ═ Gradient cards with pulse glow icons, spring bounce on tap,
//   staggered entrance with 80ms delay ═
// ════════════════════════════════════════════════════════════════

@Composable
private fun PremiumQuickActionsGrid(
    onMagazinesClick: () -> Unit,
    onAdminNoticesClick: () -> Unit,
    onLiveNewsClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onChatbotClick: () -> Unit,
    isGuest: Boolean = false
) {
    val actions = mutableListOf(
        QuickAction("Magazines", Icons.Outlined.MenuBook, listOf(GradientBlueStart, IOCLBlueSoft), IOCLBluePale, onMagazinesClick),
        QuickAction("AI Chat", Icons.Outlined.SmartToy, listOf(IOCLTeal, IOCLCyan), IOCLBluePale, onChatbotClick)
    )
    if (!isGuest) {
        actions.add(1, QuickAction("Live News", Icons.Outlined.Newspaper, listOf(GradientSaffronStart, IOCLSaffronLight), IOCLOrangePale, onLiveNewsClick))
        actions.add(2, QuickAction("Admin Notices", Icons.Outlined.Campaign, listOf(Color(0xFFE53935), Color(0xFFEF5350)), Color(0xFFFFEBEE), onAdminNoticesClick))
        actions.add(3, QuickAction("Saved", Icons.Outlined.BookmarkBorder, listOf(IOCLGreen, IOCLGreenLight), IOCLGreenPale, onBookmarksClick))
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // Section title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(IOCLBlueAccent)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Serif
            )
        }
        Spacer(Modifier.height(16.dp))

        // Grid
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            actions.chunked(2).forEachIndexed { rowIndex, rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowActions.forEachIndexed { colIndex, action ->
                        PremiumQuickActionCard(
                            action = action,
                            staggerIndex = rowIndex * 2 + colIndex,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowActions.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val lightBg: Color,
    val onClick: () -> Unit
)

@Composable
private fun PremiumQuickActionCard(
    action: QuickAction,
    staggerIndex: Int,
    modifier: Modifier = Modifier
) {
    // Staggered entrance
    val alpha = staggeredAlpha(staggerIndex, baseDelay = 80)
    val offsetY = staggeredOffset(staggerIndex, baseDelay = 80)

    // Spring bounce on press
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounce_scale"
    )

    // Pulsing icon glow
    val infiniteTransition = rememberInfiniteTransition(label = "action_glow_$staggerIndex")
    val iconGlow by infiniteTransition.animateFloat(
        0.15f, 0.4f,
        infiniteRepeatable(
            tween(2500 + staggerIndex * 200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "icon_glow_$staggerIndex"
    )

    Card(
        modifier = modifier
            .height(100.dp)
            .alpha(alpha)
            .offset(y = offsetY.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        action.onClick()
                    }
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = action.gradientColors,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(16.dp)
        ) {
            // Decorative large icon in background
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-6).dp),
                tint = IOCLWhite.copy(alpha = 0.08f)
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon with glow
                Box(contentAlignment = Alignment.Center) {
                    // Glow behind icon
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(IOCLWhite.copy(alpha = iconGlow))
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(IOCLWhite.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.label,
                            modifier = Modifier.size(20.dp),
                            tint = IOCLWhite
                        )
                    }
                }

                Text(
                    text = action.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = IOCLWhite,
                    fontFamily = FontFamily.Serif
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// ▌ PREMIUM SECTION HEADER ▐
// ═ Accent dot + title + animated "See All" arrow ═
// ════════════════════════════════════════════════════════════════

@Composable
private fun PremiumSectionHeader(
    title: String,
    accentColor: Color,
    onSeeAllClick: () -> Unit
) {
    val alpha = staggeredAlpha(0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .alpha(alpha),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Accent dot with glow
            Box(contentAlignment = Alignment.Center) {
                PulseGlow(
                    color = accentColor,
                    size = 20.dp
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Serif
            )
        }

        // Animated "See All" button
        TextButton(onClick = onSeeAllClick) {
            Text(
                text = "See All",
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.width(4.dp))

            val infiniteTransition = rememberInfiniteTransition(label = "arrow_nudge")
            val arrowOffset by infiniteTransition.animateFloat(
                0f, 4f,
                infiniteRepeatable(
                    tween(1000, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "arrow_x"
            )
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .offset(x = arrowOffset.dp),
                tint = accentColor
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// ▌ PREMIUM DOCUMENT CARD ▐
// ═ Elevated card with gradient overlay, category badge, bookmark
//   animation, view counter, and parallax-ready cover image ═
// ════════════════════════════════════════════════════════════════

@Composable
private fun PremiumDocumentCard(
    title: String,
    subtitle: String,
    description: String,
    tags: List<String>,
    coverImageUrl: String,
    category: String,
    viewCount: Int,
    downloadCount: Int,
    isBookmarked: Boolean,
    onCardClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    // Tap bounce
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_bounce"
    )

    // Bookmark animation
    val bookmarkScale by animateFloatAsState(
        targetValue = if (isBookmarked) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bookmark_pop"
    )
    val bookmarkColor by animateColorAsState(
        targetValue = if (isBookmarked) IOCLSaffron else IOCLWhite,
        animationSpec = tween(400),
        label = "bookmark_color"
    )

    Card(
        modifier = modifier
            .scale(cardScale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        onCardClick()
                    }
                )
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 2.dp
        )
    ) {
        Column {
            // Cover Image Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            ) {
                // Dynamic gradient background if no real image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(accentColor.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.8f))
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Article,
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.Center)
                            .alpha(0.15f),
                        tint = IOCLWhite
                    )
                }

                // Image (will draw over gradient if valid URL)
                if (coverImageUrl.startsWith("file:///android_asset/")) {
                    PdfCoverImage(
                        pdfUrl = coverImageUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (coverImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = coverImageUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )

                // Accent line at top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accentColor,
                                    accentColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                        .align(Alignment.TopCenter)
                )

                // Category badge with glass effect
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = category,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = IOCLWhite,
                        fontSize = 10.sp
                    )
                }

                // Bookmark button with spring animation
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .scale(bookmarkScale)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark
                        else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = bookmarkColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Stats (Views & Downloads)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = IOCLWhite.copy(alpha = 0.9f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatCount(downloadCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = IOCLWhite.copy(alpha = 0.9f),
                        fontSize = 10.sp
                    )
                }
            }

            // Content
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                // Tags
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(tags.take(3)) { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "#$tag",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// ▌ PREMIUM SHIMMER ROW ▐
// ═ Loading placeholder with EnhancedShimmerBrush ═
// ════════════════════════════════════════════════════════════════

@Composable
private fun PremiumShimmerRow() {
    val shimmerBrush = EnhancedShimmerBrush()
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(3) { index ->
            val alpha = staggeredAlpha(index, baseDelay = 120)
            Card(
                modifier = Modifier
                    .width(280.dp)
                    .alpha(alpha),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                            .background(shimmerBrush)
                    )
                    Column(Modifier.padding(16.dp)) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.85f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(shimmerBrush)
                        )
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier
                                .fillMaxWidth(0.55f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// ▌ GREETING HELPER ▐
// ════════════════════════════════════════════════════════════════

private fun getGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning ☀\uFE0F"
        hour < 17 -> "Good Afternoon 🌤\uFE0F"
        hour < 21 -> "Good Evening 🌇"
        else -> "Good Night 🌙"
    }
}

// ════════════════════════════════════════════════════════════════
// ▌ LIVE NEWS TICKER ▐
// ════════════════════════════════════════════════════════════════
@Composable
private fun LiveNewsTicker(headlines: List<String>) {
    val scrollState = rememberScrollState()
    
    // Auto-scroll logic
    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            if (scrollState.canScrollForward) {
                scrollState.animateScrollTo(scrollState.value + 2)
            } else {
                scrollState.scrollTo(0)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(12.dp),
        color = IOCLSaffron.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, IOCLSaffron.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Campaign,
                contentDescription = "News",
                tint = IOCLSaffron,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState, enabled = false)
            ) {
                headlines.forEach { headline ->
                    Text(
                        text = "• $headline",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(24.dp))
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// ▌ TRENDING TAGS SECTION ▐
// ════════════════════════════════════════════════════════════════
@Composable
private fun TrendingTagsSection(tags: List<String>) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.TrendingUp,
                contentDescription = null,
                tint = IOCLOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Trending Topics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tags) { tag ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "#$tag",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}
