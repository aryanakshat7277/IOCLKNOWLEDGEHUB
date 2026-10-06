package com.iocl.knowledgehub.ui.notification

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iocl.knowledgehub.domain.model.*
import com.iocl.knowledgehub.domain.repository.NotificationRepository
import com.iocl.knowledgehub.ui.components.*
import com.iocl.knowledgehub.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// =====================================================================
// UI State
// =====================================================================

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val unreadCount: Int = 0
)

// =====================================================================
// ViewModel
// =====================================================================

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
        observeUnreadCount()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            notificationRepository.getNotifications().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                notifications = result.data,
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

                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun observeUnreadCount() {
        viewModelScope.launch {
            notificationRepository.getUnreadCount().collect { count ->
                _uiState.update { it.copy(unreadCount = count) }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
            loadNotifications()
        }
    }

    fun refresh() {
        loadNotifications()
    }
}

// =====================================================================
// Screen
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBackClick: () -> Unit,
    onNotificationClick: (Notification) -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Notifications",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                        if (uiState.unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Badge(
                                containerColor = IOCLSaffron,
                                contentColor = IOCLWhite
                            ) {
                                Text("${uiState.unreadCount}")
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.unreadCount > 0) {
                        TextButton(onClick = viewModel::markAllAsRead) {
                            Text(
                                "Mark all read",
                                color = IOCLSaffron,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
                    IOCLLoadingIndicator("Loading notifications...")
                }
            }

            uiState.error != null -> {
                ErrorView(
                    message = uiState.error!!,
                    onRetry = viewModel::refresh,
                    modifier = Modifier.padding(padding)
                )
            }

            uiState.notifications.isEmpty() -> {
                EmptyStateView(
                    icon = Icons.Filled.Notifications,
                    title = "No Notifications",
                    subtitle = "You're all caught up! New notifications will appear here.",
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.notifications,
                        key = { it.id }
                    ) { notification ->
                        NotificationItem(
                            notification = notification,
                            onClick = {
                                viewModel.markAsRead(notification.id)
                                onNotificationClick(notification)
                            },
                            onDelete = { viewModel.deleteNotification(notification.id) }
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================
// Notification Item
// =====================================================================

@Composable
private fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val typeInfo = getNotificationTypeInfo(notification.type)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.isRead) 0.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                MaterialTheme.colorScheme.surface
            else
                IOCLSaffron.copy(alpha = 0.05f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Type icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(typeInfo.second.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    typeInfo.first,
                    contentDescription = null,
                    tint = typeInfo.second,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.isRead) FontWeight.Normal
                        else FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(IOCLSaffron)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sdf.format(Date(notification.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = IOCLGrayMedium
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Delete",
                            tint = IOCLGrayMedium,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getNotificationTypeInfo(type: NotificationType): Pair<ImageVector, androidx.compose.ui.graphics.Color> {
    return when (type) {
        NotificationType.NEW_MAGAZINE -> Icons.Filled.MenuBook to IOCLSaffron
        NotificationType.NEW_NEWSPAPER -> Icons.Filled.Newspaper to IOCLBlue
        NotificationType.GENERAL -> Icons.Filled.Info to IOCLGreen
        NotificationType.SYSTEM -> Icons.Filled.Settings to IOCLGrayDark
        NotificationType.UPDATE -> Icons.Filled.SystemUpdate to IOCLTeal
    }
}
