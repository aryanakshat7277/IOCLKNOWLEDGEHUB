package com.iocl.knowledgehub.ui.profile

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.iocl.knowledgehub.domain.model.*
import com.iocl.knowledgehub.domain.usecase.auth.GetCurrentUserUseCase
import com.iocl.knowledgehub.domain.usecase.auth.LogoutUseCase
import com.iocl.knowledgehub.ui.components.*
import com.iocl.knowledgehub.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// =====================================================================
// UI State
// =====================================================================

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val downloadOverWifiOnly: Boolean = true,
    val isLoggingOut: Boolean = false,
    val logoutSuccess: Boolean = false,
    val error: String? = null
)

// =====================================================================
// ViewModel
// =====================================================================

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                _uiState.update {
                    it.copy(user = user, isLoading = false)
                }
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(isDarkMode = enabled) }
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun toggleWifiDownload(enabled: Boolean) {
        _uiState.update { it.copy(downloadOverWifiOnly = enabled) }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            when (logoutUseCase()) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isLoggingOut = false, logoutSuccess = true)
                    }
                }

                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoggingOut = false, error = "Failed to logout")
                    }
                }

                is Resource.Loading -> {}
            }
        }
    }
}

// =====================================================================
// Screen
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.logoutSuccess) {
        if (uiState.logoutSuccess) onLogout()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
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
                    IOCLLoadingIndicator("Loading profile...")
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // User Card
                    ProfileHeaderCard(user = uiState.user)

                    Spacer(Modifier.height(24.dp))

                    // Settings Section
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            SettingsToggle(
                                icon = Icons.Filled.Notifications,
                                title = "Notifications",
                                subtitle = "Receive push notifications",
                                isChecked = uiState.notificationsEnabled,
                                onCheckedChange = viewModel::toggleNotifications
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // About Section
                    Text(
                        "About",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            AboutItem(
                                icon = Icons.Filled.Info,
                                title = "App Version",
                                value = "1.0.0 (Build 1)"
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Logout Button
                    Button(
                        onClick = viewModel::logout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IOCLRed.copy(alpha = 0.1f),
                            contentColor = IOCLRed
                        ),
                        enabled = !uiState.isLoggingOut
                    ) {
                        if (uiState.isLoggingOut) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = IOCLRed,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Logout", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Footer
                    Text(
                        text = "Indian Oil Corporation Limited\n© 2024 All Rights Reserved",
                        style = MaterialTheme.typography.bodySmall,
                        color = IOCLGrayMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    )
                }
            }
        }
    }
}

// =====================================================================
// Profile Header Card
// =====================================================================

@Composable
private fun ProfileHeaderCard(user: User?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(IOCLNavy, IOCLBlue)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(3.dp, IOCLSaffron, CircleShape)
                        .background(IOCLNavyLight),
                    contentAlignment = Alignment.Center
                ) {
                    if (user?.profileImageUrl?.isNotBlank() == true) {
                        AsyncImage(
                            model = user.profileImageUrl,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = user?.name?.take(2)?.uppercase() ?: "U",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = IOCLWhite,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = user?.name ?: "IOCL Employee",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = IOCLWhite
                )

                Text(
                    text = user?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IOCLGrayMedium
                )

                Spacer(Modifier.height(12.dp))

                // Info chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (user?.department?.isNotBlank() == true) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = IOCLSaffron.copy(alpha = 0.2f)
                        ) {
                            Text(
                                user.department,
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 4.dp
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = IOCLSaffronLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = IOCLGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            user?.role?.name ?: "EMPLOYEE",
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 4.dp
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = IOCLGreenLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (user?.employeeId?.isNotBlank() == true) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Employee ID: ${user.employeeId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = IOCLGrayMedium
                    )
                }
            }
        }
    }
}

// =====================================================================
// Settings Toggle
// =====================================================================

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = IOCLSaffron,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = IOCLWhite,
                checkedTrackColor = IOCLSaffron,
                uncheckedThumbColor = IOCLGrayMedium,
                uncheckedTrackColor = IOCLGrayLight
            )
        )
    }
}

// =====================================================================
// About Item
// =====================================================================

@Composable
private fun AboutItem(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = IOCLBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotBlank()) {
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = IOCLGrayMedium
            )
        } else {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = IOCLGrayMedium,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
