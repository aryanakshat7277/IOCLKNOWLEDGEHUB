package com.iocl.knowledgehub.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iocl.knowledgehub.data.repository.DashboardContentProvider
import com.iocl.knowledgehub.domain.model.*
import com.iocl.knowledgehub.domain.repository.NotificationRepository
import com.iocl.knowledgehub.domain.usecase.auth.GetCurrentUserUseCase
import com.iocl.knowledgehub.domain.usecase.bookmark.ToggleBookmarkUseCase
import com.iocl.knowledgehub.domain.usecase.magazine.GetMagazinesUseCase
import com.iocl.knowledgehub.domain.usecase.newspaper.GetNewspapersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val userName: String = "",
    val isAdmin: Boolean = false,
    val isGuest: Boolean = false,
    val totalMagazines: Int = 0,
    val totalNewspapers: Int = 0,
    val totalViews: Int = 0,
    val totalDownloads: Int = 0,
    val unreadNotifications: Int = 0,
    val recentMagazines: List<Magazine> = emptyList(),
    val recentNewspapers: List<Newspaper> = emptyList(),
    val trendingTags: List<String> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val dataSource: String = "loading" // "live", "groq", "deepseek", "openai", "openrouter", "offline"
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getMagazinesUseCase: GetMagazinesUseCase,
    private val getNewspapersUseCase: GetNewspapersUseCase,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
    private val notificationRepository: NotificationRepository,
    private val contentProvider: DashboardContentProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        loadUserInfo()
        loadMagazines()
        loadNewspapers()
        loadNotificationCount()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            try {
                getCurrentUserUseCase().collect { user ->
                    if (user != null) {
                        _uiState.update {
                            it.copy(
                                userName = user.name.ifEmpty { user.email.substringBefore("@").ifEmpty { "User" } },
                                isAdmin = user.role == UserRole.ADMIN || user.role == UserRole.SUPER_ADMIN,
                                isGuest = user.role == UserRole.GUEST
                            )
                        }
                    } else {
                        _uiState.update { it.copy(userName = "User") }
                    }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(userName = "User") }
            }
        }
    }

    private fun loadMagazines() {
        viewModelScope.launch {
            try {
                getMagazinesUseCase().collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            android.util.Log.d("IOCLDashboardData", "Magazines Loading...")
                            _uiState.update { it.copy(isLoading = true) }
                        }
                        is Resource.Success -> {
                            android.util.Log.d("IOCLDashboardData", "Magazines Success, count: ${resource.data.size}")
                            val magazines = resource.data
                            if (magazines.isNotEmpty()) {
                                updateMagazineState(magazines, "live")
                            } else {
                                fetchMagazinesFromAI()
                            }
                        }
                        is Resource.Error -> {
                            android.util.Log.e("IOCLDashboardData", "Magazines Error: ${resource.message}")
                            launch { fetchMagazinesFromAI() }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("IOCLDashboardData", "loadMagazines Exception: ${e.message}")
                launch { fetchMagazinesFromAI() }
            }
        }
    }

    private fun loadNewspapers() {
        viewModelScope.launch {
            try {
                getNewspapersUseCase().collect { resource ->
                    when (resource) {
                        is Resource.Loading -> { 
                            android.util.Log.d("IOCLDashboardData", "Newspapers Loading...")
                        }
                        is Resource.Success -> {
                            android.util.Log.d("IOCLDashboardData", "Newspapers Success, count: ${resource.data.size}")
                            val newspapers = resource.data
                            if (newspapers.isNotEmpty()) {
                                updateNewspaperState(newspapers, "live")
                            } else {
                                fetchNewspapersFromAI()
                            }
                        }
                        is Resource.Error -> {
                            android.util.Log.e("IOCLDashboardData", "Newspapers Error: ${resource.message}")
                            launch { fetchNewspapersFromAI() }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("IOCLDashboardData", "loadNewspapers Exception: ${e.message}")
                launch { fetchNewspapersFromAI() }
            }
        }
    }

    private suspend fun fetchMagazinesFromAI() {
        android.util.Log.e("IOCLDashboardData", "fetchMagazinesFromAI() started")
        try {
            val magazines = contentProvider.fetchMagazines()
            android.util.Log.e("IOCLDashboardData", "fetchMagazinesFromAI() success: ${magazines.size}")
            updateMagazineState(magazines, contentProvider.lastDataSource)
        } catch (e: Exception) {
            android.util.Log.e("IOCLDashboardData", "fetchMagazinesFromAI() error: ${e.message}")
            _uiState.update { it.copy(isLoading = false, error = null, dataSource = "offline") }
        }
    }

    private suspend fun fetchNewspapersFromAI() {
        android.util.Log.e("IOCLDashboardData", "fetchNewspapersFromAI() started")
        try {
            val newspapers = contentProvider.fetchNewspapers()
            android.util.Log.e("IOCLDashboardData", "fetchNewspapersFromAI() success: ${newspapers.size}")
            updateNewspaperState(newspapers, contentProvider.lastDataSource)
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = null, dataSource = "offline") }
        }
    }

    private fun updateMagazineState(magazines: List<Magazine>, source: String) {
        val totalViews = magazines.sumOf { it.viewCount }
        val totalDownloads = magazines.sumOf { it.downloadCount }
        val recent = magazines.sortedByDescending { it.uploadedAt }.take(10)

        // Extract trending tags from magazines
        val magTags = magazines.flatMap { it.tags }.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.take(8).map { it.key }

        _uiState.update {
            it.copy(
                totalMagazines = magazines.size,
                totalViews = totalViews,
                totalDownloads = totalDownloads,
                recentMagazines = recent,
                trendingTags = magTags,
                isLoading = false,
                error = null,
                dataSource = source
            )
        }
    }

    private fun updateNewspaperState(newspapers: List<Newspaper>, source: String) {
        val totalViews = newspapers.sumOf { it.viewCount }
        val totalDownloads = newspapers.sumOf { it.downloadCount }
        val recent = newspapers.sortedByDescending { it.uploadedAt }.take(10)

        // Merge newspaper tags into trending
        val newsTags = newspapers.flatMap { it.tags }.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.take(5).map { it.key }

        _uiState.update {
            it.copy(
                totalNewspapers = newspapers.size,
                totalViews = it.totalViews + totalViews,
                totalDownloads = it.totalDownloads + totalDownloads,
                recentNewspapers = recent,
                trendingTags = (it.trendingTags + newsTags).distinct().take(12),
                isLoading = false,
                error = null,
                dataSource = source
            )
        }
    }

    private fun loadNotificationCount() {
        viewModelScope.launch {
            try {
                notificationRepository.getUnreadCount().collect { count ->
                    _uiState.update { it.copy(unreadNotifications = count) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(unreadNotifications = 0) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleMagazineBookmark(magazine: Magazine) {
        viewModelScope.launch {
            val bookmark = Bookmark(
                documentId = magazine.id,
                documentType = DocumentType.MAGAZINE,
                title = magazine.title,
                coverImageUrl = magazine.coverImageUrl
            )
            val result = toggleBookmarkUseCase(bookmark)
            if (result is Resource.Success) {
                _uiState.update { state ->
                    state.copy(
                        recentMagazines = state.recentMagazines.map {
                            if (it.id == magazine.id) it.copy(isBookmarked = !it.isBookmarked) else it
                        }
                    )
                }
            }
        }
    }

    fun toggleNewspaperBookmark(newspaper: Newspaper) {
        viewModelScope.launch {
            val bookmark = Bookmark(
                documentId = newspaper.id,
                documentType = DocumentType.NEWSPAPER,
                title = newspaper.title,
                coverImageUrl = newspaper.coverImageUrl
            )
            val result = toggleBookmarkUseCase(bookmark)
            if (result is Resource.Success) {
                _uiState.update { state ->
                    state.copy(
                        recentNewspapers = state.recentNewspapers.map {
                            if (it.id == newspaper.id) it.copy(isBookmarked = !it.isBookmarked) else it
                        }
                    )
                }
            }
        }
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                totalViews = 0,
                totalDownloads = 0,
                dataSource = "loading"
            )
        }
        loadDashboardData()
    }
}
