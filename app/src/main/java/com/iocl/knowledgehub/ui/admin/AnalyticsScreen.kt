package com.iocl.knowledgehub.ui.admin

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iocl.knowledgehub.domain.model.*
import com.iocl.knowledgehub.domain.repository.MagazineRepository
import com.iocl.knowledgehub.domain.repository.NewspaperRepository
import com.iocl.knowledgehub.ui.components.*
import com.iocl.knowledgehub.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// =====================================================================
// Data Classes
// =====================================================================

data class BarChartData(
    val label: String,
    val value: Float,
    val color: Color
)

data class AnalyticsUiState(
    val totalViews: Int = 0,
    val totalDownloads: Int = 0,
    val totalMagazines: Int = 0,
    val totalNewspapers: Int = 0,
    val monthlyViews: List<BarChartData> = emptyList(),
    val categoryDistribution: List<BarChartData> = emptyList(),
    val topDocuments: List<Magazine> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

// =====================================================================
// ViewModel
// =====================================================================

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val magazineRepository: MagazineRepository,
    private val newspaperRepository: NewspaperRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val chartColors = listOf(
        IOCLSaffron, IOCLBlue, IOCLGreen, IOCLTeal,
        IOCLOrange, IOCLRed, IOCLAmber
    )

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            magazineRepository.getMagazines().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val magazines = result.data
                        val totalViews = magazines.sumOf { it.viewCount }
                        val totalDownloads = magazines.sumOf { it.downloadCount }

                        // Build monthly views chart data
                        val monthlyViews = (1..6).map { month ->
                            val monthMags = magazines.filter { it.month == month }
                            BarChartData(
                                label = month.toMonthName().take(3),
                                value = monthMags.sumOf { it.viewCount }.toFloat(),
                                color = chartColors[(month - 1) % chartColors.size]
                            )
                        }

                        // Build category distribution
                        val categoryDist = magazines
                            .groupBy { it.category.name }
                            .map { (cat, mags) ->
                                BarChartData(
                                    label = cat.take(8),
                                    value = mags.size.toFloat(),
                                    color = chartColors[
                                        MagazineCategory.entries.indexOfFirst { it.name == cat }
                                            .coerceIn(0, chartColors.lastIndex)
                                    ]
                                )
                            }

                        val topDocs = magazines
                            .sortedByDescending { it.viewCount }
                            .take(5)

                        _uiState.update {
                            it.copy(
                                totalViews = totalViews,
                                totalDownloads = totalDownloads,
                                totalMagazines = magazines.size,
                                monthlyViews = monthlyViews,
                                categoryDistribution = categoryDist,
                                topDocuments = topDocs,
                                isLoading = false,
                                error = null
                            )
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                    }

                    is Resource.Loading -> {}
                }
            }
        }

        viewModelScope.launch {
            newspaperRepository.getNewspapers().collect { result ->
                if (result is Resource.Success) {
                    _uiState.update {
                        it.copy(
                            totalNewspapers = result.data.size,
                            totalViews = it.totalViews + result.data.sumOf { n -> n.viewCount },
                            totalDownloads = it.totalDownloads + result.data.sumOf { n -> n.downloadCount }
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        loadAnalytics()
    }
}

// =====================================================================
// Screen
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBackClick: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Analytics",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    IOCLLoadingIndicator("Loading analytics...")
                }
            }

            uiState.error != null -> {
                ErrorView(
                    message = uiState.error!!,
                    onRetry = viewModel::refresh,
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Overview Stats
                    Text(
                        "Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnalyticsStatChip(
                            icon = Icons.Filled.Visibility,
                            label = "Views",
                            value = formatCount(uiState.totalViews),
                            color = IOCLSaffron,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsStatChip(
                            icon = Icons.Filled.Download,
                            label = "Downloads",
                            value = formatCount(uiState.totalDownloads),
                            color = IOCLBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnalyticsStatChip(
                            icon = Icons.Filled.MenuBook,
                            label = "Magazines",
                            value = "${uiState.totalMagazines}",
                            color = IOCLGreen,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsStatChip(
                            icon = Icons.Filled.Newspaper,
                            label = "Newspapers",
                            value = "${uiState.totalNewspapers}",
                            color = IOCLTeal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    // Monthly Views Bar Chart
                    if (uiState.monthlyViews.isNotEmpty()) {
                        Text(
                            "Monthly Views",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            BarChart(
                                data = uiState.monthlyViews,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .padding(20.dp)
                            )
                        }

                        Spacer(Modifier.height(28.dp))
                    }

                    // Category Distribution
                    if (uiState.categoryDistribution.isNotEmpty()) {
                        Text(
                            "Category Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                uiState.categoryDistribution.forEach { item ->
                                    CategoryBar(
                                        label = item.label,
                                        value = item.value.toInt(),
                                        maxValue = uiState.categoryDistribution.maxOf { it.value }
                                            .toInt(),
                                        color = item.color
                                    )
                                    Spacer(Modifier.height(10.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(28.dp))
                    }

                    // Top Documents
                    if (uiState.topDocuments.isNotEmpty()) {
                        Text(
                            "Top Documents",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                uiState.topDocuments.forEachIndexed { index, magazine ->
                                    TopDocumentRow(
                                        rank = index + 1,
                                        title = magazine.title,
                                        views = magazine.viewCount,
                                        downloads = magazine.downloadCount
                                    )
                                    if (index < uiState.topDocuments.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            color = IOCLGrayLight
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

// =====================================================================
// Bar Chart using Canvas
// =====================================================================

@Composable
private fun BarChart(
    data: List<BarChartData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val maxValue = data.maxOf { it.value }.coerceAtLeast(1f)

    // Animate bars
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier) {
        val barWidth = size.width / (data.size * 2f)
        val chartHeight = size.height - 30f
        val spacing = barWidth

        data.forEachIndexed { index, item ->
            val barHeight = (item.value / maxValue) * chartHeight * animationProgress.value
            val x = index * (barWidth + spacing) + spacing / 2

            // Bar with rounded top
            drawRoundRect(
                color = item.color,
                topLeft = Offset(x, chartHeight - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Bar shadow / glow
            drawRoundRect(
                color = item.color.copy(alpha = 0.2f),
                topLeft = Offset(x - 2, chartHeight - barHeight - 2),
                size = Size(barWidth + 4, barHeight + 4),
                cornerRadius = CornerRadius(10f, 10f)
            )

            // Label
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    item.label,
                    x + barWidth / 2,
                    size.height,
                    android.graphics.Paint().apply {
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 28f
                        color = android.graphics.Color.GRAY
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}

// =====================================================================
// Category Horizontal Bar
// =====================================================================

@Composable
private fun CategoryBar(
    label: String,
    value: Int,
    maxValue: Int,
    color: Color
) {
    val animatedWidth = remember { Animatable(0f) }
    LaunchedEffect(value) {
        animatedWidth.animateTo(
            targetValue = if (maxValue > 0) value.toFloat() / maxValue else 0f,
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        )
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                "$value",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedWidth.value)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(color, color.copy(alpha = 0.7f))
                        )
                    )
            )
        }
    }
}

// =====================================================================
// Analytics Stat Chip
// =====================================================================

@Composable
private fun AnalyticsStatChip(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = IOCLGrayMedium
                )
            }
        }
    }
}

// =====================================================================
// Top Document Row
// =====================================================================

@Composable
private fun TopDocumentRow(
    rank: Int,
    title: String,
    views: Int,
    downloads: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when (rank) {
                        1 -> IOCLSaffron
                        2 -> IOCLGrayMedium
                        3 -> IOCLOrange
                        else -> IOCLGrayLight
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$rank",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = IOCLWhite
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${formatCount(views)} views",
                    style = MaterialTheme.typography.labelSmall,
                    color = IOCLGrayMedium
                )
                Text(
                    "${formatCount(downloads)} downloads",
                    style = MaterialTheme.typography.labelSmall,
                    color = IOCLGrayMedium
                )
            }
        }
    }
}
