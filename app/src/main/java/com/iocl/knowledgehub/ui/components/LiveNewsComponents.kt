package com.iocl.knowledgehub.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iocl.knowledgehub.domain.model.LiveNewsCategory
import com.iocl.knowledgehub.domain.model.LiveNewsItem
import com.iocl.knowledgehub.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CategoryFilterRow(
    selected: LiveNewsCategory?,
    onSelect: (LiveNewsCategory?) -> Unit
) {
    val categories = linkedMapOf(
        null to "All",
        LiveNewsCategory.CORPORATE to "Corporate",
        LiveNewsCategory.FINANCE to "Finance",
        LiveNewsCategory.OPERATIONS to "Operations",
        LiveNewsCategory.SUSTAINABILITY to "Green",
        LiveNewsCategory.PROJECTS to "Projects",
        LiveNewsCategory.TECHNOLOGY to "Tech",
        LiveNewsCategory.HR to "HR",
        LiveNewsCategory.SAFETY to "Safety",
        LiveNewsCategory.CSR to "CSR"
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories.entries.toList()) { (cat, label) ->
            FilterChip(
                selected = selected == cat,
                onClick = { onSelect(cat) },
                label = { Text(label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = IOCLSaffron,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
fun LastUpdatedBanner(timestamp: Long, count: Int) {
    val time = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(IOCLSaffron.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Schedule, null, tint = IOCLSaffron, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            "Updated $time  •  $count articles",
            style = MaterialTheme.typography.labelSmall,
            color = IOCLSaffron
        )
    }
}

@Composable
fun FeaturedNewsCard(article: LiveNewsItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column {
            // Image / gradient area
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                if (article.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = article.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Brush.linearGradient(listOf(IOCLNavy, IOCLBlue)))
                    ) {
                        Icon(
                            categoryIcon(article.category), null,
                            tint = Color.White.copy(0.12f),
                            modifier = Modifier.size(90.dp).align(Alignment.Center)
                        )
                    }
                }
                // Dark gradient overlay
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.80f))))
                )
                // Text content overlaid on image
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
                ) {
                    CategoryChip(article.category)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        article.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        fontSize = 16.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        formatRelativeTime(article.publishedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.65f)
                    )
                }
            }
            // ── Compact source footer (outside the Box, below the image) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.6f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Newspaper, null,
                    tint = IOCLBlue.copy(0.7f),
                    modifier = Modifier.size(10.dp)
                )
                Spacer(Modifier.width(5.dp))
                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(article.publishedAt))
                Text(
                    "Source: ${article.sourceName} • $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun NewsListCard(article: LiveNewsItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Main content row
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Category-colored thumbnail
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    categoryColor(article.category).copy(0.18f),
                                    categoryColor(article.category).copy(0.06f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (article.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = article.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Icon(
                            categoryIcon(article.category), null,
                            tint = categoryColor(article.category),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                // Text content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        article.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    if (article.summary.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            article.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    // Category chip + timestamp
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryChip(article.category, compact = true)
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Filled.Schedule, null,
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            formatRelativeTime(article.publishedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // ── Compact source footer strip ──────────────────────────────
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(0.6f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Newspaper, null,
                    tint = IOCLBlue.copy(0.55f),
                    modifier = Modifier.size(9.dp)
                )
                Spacer(Modifier.width(4.dp))
                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(article.publishedAt))
                Text(
                    "Source: ${article.sourceName} • $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun CategoryChip(category: LiveNewsCategory, compact: Boolean = false) {
    val color = categoryColor(category)
    val label = category.name.replace("_", " ")
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(0.12f))
            .padding(horizontal = if (compact) 4.dp else 6.dp, vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color,
            fontWeight = FontWeight.Medium, fontSize = if (compact) 9.sp else 10.sp)
    }
}

@Composable
fun ShimmerNewsList() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.12f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alpha"
    )
    val shimmerBg = MaterialTheme.colorScheme.onSurface.copy(alpha)

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp))
                .background(shimmerBg)
        )
        repeat(3) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)) {
                Row(Modifier.padding(12.dp)) {
                    Box(Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(shimmerBg))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth(0.35f).height(11.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBg))
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBg))
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.fillMaxWidth(0.8f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBg))
                    }
                }
            }
        }
    }
}

fun formatRelativeTime(epochMillis: Long): String {
    val diff = System.currentTimeMillis() - epochMillis
    return when {
        diff < 60_000L      -> "Just now"
        diff < 3_600_000L   -> "${diff / 60_000}m ago"
        diff < 86_400_000L  -> "${diff / 3_600_000}h ago"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(epochMillis))
    }
}

fun categoryColor(cat: LiveNewsCategory): Color = when (cat) {
    LiveNewsCategory.FINANCE      -> Color(0xFF1565C0)
    LiveNewsCategory.OPERATIONS   -> Color(0xFFE65100)
    LiveNewsCategory.SUSTAINABILITY -> Color(0xFF2E7D32)
    LiveNewsCategory.SAFETY       -> Color(0xFFC62828)
    LiveNewsCategory.HR           -> Color(0xFF6A1B9A)
    LiveNewsCategory.CSR          -> Color(0xFF00838F)
    LiveNewsCategory.PROJECTS     -> Color(0xFF4527A0)
    LiveNewsCategory.TECHNOLOGY   -> Color(0xFF00695C)
    LiveNewsCategory.EMERGENCY    -> Color(0xFFB71C1C)
    else                          -> IOCLSaffron
}

fun categoryIcon(cat: LiveNewsCategory): ImageVector = when (cat) {
    LiveNewsCategory.FINANCE      -> Icons.Filled.TrendingUp
    LiveNewsCategory.OPERATIONS   -> Icons.Filled.LocalGasStation
    LiveNewsCategory.SUSTAINABILITY -> Icons.Filled.EnergySavingsLeaf
    LiveNewsCategory.SAFETY       -> Icons.Filled.HealthAndSafety
    LiveNewsCategory.HR           -> Icons.Filled.People
    LiveNewsCategory.CSR          -> Icons.Filled.VolunteerActivism
    LiveNewsCategory.PROJECTS     -> Icons.Filled.Construction
    LiveNewsCategory.TECHNOLOGY   -> Icons.Filled.Memory
    LiveNewsCategory.EMERGENCY    -> Icons.Filled.Warning
    else                          -> Icons.Filled.Business
}
