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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.iocl.knowledgehub.domain.repository.AuthRepository
import com.iocl.knowledgehub.ui.components.*
import com.iocl.knowledgehub.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// =====================================================================
// UI State
// =====================================================================

data class AdminDashboardUiState(
    val totalMagazines: Int = 0,
    val totalNewspapers: Int = 0,
    val totalUsers: Int = 0,
    val totalViews: Int = 0,
    val recentMagazines: List<Magazine> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

// =====================================================================
// ViewModel
// =====================================================================

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val magazineRepository: MagazineRepository,
    private val newspaperRepository: NewspaperRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load magazines count
            magazineRepository.getMagazines().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val magazines = result.data
                        val totalViews = magazines.sumOf { it.viewCount }
                        _uiState.update {
                            it.copy(
                                totalMagazines = magazines.size,
                                totalViews = totalViews,
                                recentMagazines = magazines.sortedByDescending { m -> m.uploadedAt }
                                    .take(5),
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
                            totalViews = it.totalViews + result.data.sumOf { n -> n.viewCount }
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        loadDashboardData()
    }
}

// =====================================================================
// Screen
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onBackClick: () -> Unit,
    onUploadClick: () -> Unit,
    onManageUsersClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Admin Dashboard",
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
                    IOCLLoadingIndicator("Loading dashboard...")
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
                    // Stats Cards Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            icon = Icons.Filled.MenuBook,
                            label = "Magazines",
                            value = "${uiState.totalMagazines}",
                            gradient = listOf(IOCLSaffron, IOCLOrangeAccent),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            icon = Icons.Filled.Newspaper,
                            label = "Newspapers",
                            value = "${uiState.totalNewspapers}",
                            gradient = listOf(IOCLBlue, IOCLBlueSoft),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Stats Cards Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            icon = Icons.Filled.People,
                            label = "Users",
                            value = "${uiState.totalUsers}",
                            gradient = listOf(IOCLGreen, IOCLTeal),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            icon = Icons.Filled.Visibility,
                            label = "Total Views",
                            value = formatCount(uiState.totalViews),
                            gradient = listOf(IOCLNavy, IOCLNavyLight),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    // Quick Actions
                    Text(
                        "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    QuickActionCard(
                        icon = Icons.Filled.Upload,
                        title = "Upload Document",
                        subtitle = "Add new magazine or newspaper",
                        color = IOCLSaffron,
                        onClick = onUploadClick
                    )

                    Spacer(Modifier.height(10.dp))

                    QuickActionCard(
                        icon = Icons.Filled.People,
                        title = "Manage Users",
                        subtitle = "View and manage user accounts",
                        color = IOCLBlue,
                        onClick = onManageUsersClick
                    )

                    Spacer(Modifier.height(10.dp))

                    QuickActionCard(
                        icon = Icons.Filled.Analytics,
                        title = "View Analytics",
                        subtitle = "Usage statistics and insights",
                        color = IOCLGreen,
                        onClick = onAnalyticsClick
                    )

                    Spacer(Modifier.height(28.dp))

                    // Recent Uploads
                    if (uiState.recentMagazines.isNotEmpty()) {
                        Text(
                            "Recent Uploads",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(12.dp))

                        uiState.recentMagazines.forEach { magazine ->
                            RecentUploadItem(magazine)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// Stat Card
// =====================================================================

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient))
                .padding(16.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(IOCLWhite.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = IOCLWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = IOCLWhite
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = IOCLWhite.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// =====================================================================
// Quick Action Card
// =====================================================================

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = IOCLGrayMedium,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// =====================================================================
// Recent Upload Item
// =====================================================================

@Composable
private fun RecentUploadItem(magazine: Magazine) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IOCLSaffron.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Description,
                    contentDescription = null,
                    tint = IOCLSaffron,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    magazine.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    "${magazine.month.toMonthName()} ${magazine.year} • ${magazine.viewCount} views",
                    style = MaterialTheme.typography.bodySmall,
                    color = IOCLGrayMedium
                )
            }
        }
    }
}
