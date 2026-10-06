package com.iocl.knowledgehub.ui.admin

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.iocl.knowledgehub.domain.model.*
import com.iocl.knowledgehub.domain.repository.AuthRepository
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

data class ManageUsersUiState(
    val users: List<User> = emptyList(),
    val filteredUsers: List<User> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

// =====================================================================
// ViewModel
// =====================================================================

@HiltViewModel
class ManageUsersViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageUsersUiState())
    val uiState: StateFlow<ManageUsersUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Since AuthRepository doesn't have getAllUsers, we use currentUser flow as placeholder
            // In production, this would call a UserRepository with admin access
            authRepository.currentUser.collect { currentUser ->
                val users = if (currentUser != null) {
                    // Simulated user list based on current session
                    listOf(
                        currentUser,
                        User(
                            uid = "user_2",
                            name = "Rajesh Kumar",
                            email = "rajesh.kumar@iocl.com",
                            department = "Engineering",
                            designation = "Senior Engineer",
                            role = UserRole.EMPLOYEE,
                            isActive = true,
                            employeeId = "EMP002"
                        ),
                        User(
                            uid = "user_3",
                            name = "Priya Sharma",
                            email = "priya.sharma@iocl.com",
                            department = "HR",
                            designation = "HR Manager",
                            role = UserRole.ADMIN,
                            isActive = true,
                            employeeId = "EMP003"
                        ),
                        User(
                            uid = "user_4",
                            name = "Amit Patel",
                            email = "amit.patel@iocl.com",
                            department = "Safety",
                            designation = "Safety Officer",
                            role = UserRole.EMPLOYEE,
                            isActive = false,
                            employeeId = "EMP004"
                        ),
                        User(
                            uid = "user_5",
                            name = "Sneha Reddy",
                            email = "sneha.reddy@iocl.com",
                            department = "Finance",
                            designation = "Finance Analyst",
                            role = UserRole.EMPLOYEE,
                            isActive = true,
                            employeeId = "EMP005"
                        )
                    )
                } else {
                    emptyList()
                }
                _uiState.update {
                    it.copy(
                        users = users,
                        filteredUsers = users,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) {
                state.users
            } else {
                state.users.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.email.contains(query, ignoreCase = true) ||
                            it.employeeId.contains(query, ignoreCase = true)
                }
            }
            state.copy(searchQuery = query, filteredUsers = filtered)
        }
    }

    fun toggleUserActive(user: User) {
        viewModelScope.launch {
            val updatedUser = user.copy(isActive = !user.isActive)
            authRepository.updateProfile(updatedUser)

            _uiState.update { state ->
                val updatedUsers = state.users.map {
                    if (it.uid == user.uid) updatedUser else it
                }
                val filtered = if (state.searchQuery.isBlank()) {
                    updatedUsers
                } else {
                    updatedUsers.filter {
                        it.name.contains(state.searchQuery, ignoreCase = true) ||
                                it.email.contains(state.searchQuery, ignoreCase = true)
                    }
                }
                state.copy(users = updatedUsers, filteredUsers = filtered)
            }
        }
    }

    fun refresh() {
        loadUsers()
    }
}

// =====================================================================
// Screen
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUsersScreen(
    onBackClick: () -> Unit,
    viewModel: ManageUsersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Manage Users",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            IOCLSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onSearch = {},
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = "Search by name, email, or ID..."
            )

            // User count
            Text(
                text = "${uiState.filteredUsers.size} users",
                style = MaterialTheme.typography.labelMedium,
                color = IOCLGrayMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            when {
                uiState.isLoading -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        IOCLLoadingIndicator("Loading users...")
                    }
                }

                uiState.error != null -> {
                    ErrorView(
                        message = uiState.error!!,
                        onRetry = viewModel::refresh
                    )
                }

                uiState.filteredUsers.isEmpty() -> {
                    EmptyStateView(
                        icon = Icons.Filled.PersonSearch,
                        title = "No Users Found",
                        subtitle = "No users match your search criteria"
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = uiState.filteredUsers,
                            key = { it.uid }
                        ) { user ->
                            UserCard(
                                user = user,
                                onToggleActive = { viewModel.toggleUserActive(user) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// User Card
// =====================================================================

@Composable
private fun UserCard(
    user: User,
    onToggleActive: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (user.isActive)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (user.role) {
                            UserRole.SUPER_ADMIN -> IOCLSaffron.copy(alpha = 0.15f)
                            UserRole.ADMIN -> IOCLBlue.copy(alpha = 0.15f)
                            UserRole.EMPLOYEE -> IOCLGreen.copy(alpha = 0.15f)
                            UserRole.GUEST -> IOCLGrayMedium.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (user.profileImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = user.profileImageUrl,
                        contentDescription = user.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = user.name.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = when (user.role) {
                            UserRole.SUPER_ADMIN -> IOCLSaffron
                            UserRole.ADMIN -> IOCLBlue
                            UserRole.EMPLOYEE -> IOCLGreen
                            UserRole.GUEST -> IOCLGrayMedium
                        }
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        user.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Role badge
                    RoleBadge(role = user.role)
                }

                Text(
                    user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (user.department.isNotBlank()) {
                        Text(
                            user.department,
                            style = MaterialTheme.typography.labelSmall,
                            color = IOCLGrayMedium
                        )
                    }
                    if (user.employeeId.isNotBlank()) {
                        Text(
                            "• ${user.employeeId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = IOCLGrayMedium
                        )
                    }
                }
            }

            // Active toggle
            Switch(
                checked = user.isActive,
                onCheckedChange = { onToggleActive() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = IOCLWhite,
                    checkedTrackColor = IOCLGreen,
                    uncheckedThumbColor = IOCLGrayMedium,
                    uncheckedTrackColor = IOCLGrayLight
                )
            )
        }
    }
}

// =====================================================================
// Role Badge
// =====================================================================

@Composable
private fun RoleBadge(role: UserRole) {
    val (color, label) = when (role) {
        UserRole.SUPER_ADMIN -> IOCLSaffron to "Super Admin"
        UserRole.ADMIN -> IOCLBlue to "Admin"
        UserRole.EMPLOYEE -> IOCLGreen to "Employee"
        UserRole.GUEST -> IOCLGrayMedium to "Guest"
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 9.sp
        )
    }
}
