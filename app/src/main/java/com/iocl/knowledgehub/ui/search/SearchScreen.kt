package com.iocl.knowledgehub.ui.search

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iocl.knowledgehub.domain.model.*
import com.iocl.knowledgehub.domain.usecase.magazine.SearchMagazinesUseCase
import com.iocl.knowledgehub.domain.usecase.newspaper.SearchNewspapersUseCase
import com.iocl.knowledgehub.ui.components.*
import com.iocl.knowledgehub.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// =====================================================================
// Search Item sealed class
// =====================================================================

sealed class SearchItem {
    data class MagazineItem(val magazine: Magazine) : SearchItem()
    data class NewspaperItem(val newspaper: Newspaper) : SearchItem()
}

// =====================================================================
// UI State
// =====================================================================

data class SearchUiState(
    val query: String = "",
    val selectedTab: Int = 0,
    val magazines: List<Magazine> = emptyList(),
    val newspapers: List<Newspaper> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false
)

// =====================================================================
// ViewModel
// =====================================================================

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMagazinesUseCase: SearchMagazinesUseCase,
    private val searchNewspapersUseCase: SearchNewspapersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(400) // debounce
                search()
            }
        } else if (query.isEmpty()) {
            _uiState.update {
                it.copy(
                    magazines = emptyList(),
                    newspapers = emptyList(),
                    hasSearched = false
                )
            }
        }
    }

    fun onTabChange(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun search() {
        val query = _uiState.value.query
        if (query.isBlank()) return
        _uiState.update { it.copy(isLoading = true, hasSearched = true) }

        viewModelScope.launch {
            searchMagazinesUseCase(query).collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(magazines = result.data) }
                }
            }
        }
        viewModelScope.launch {
            searchNewspapersUseCase(query).collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(newspapers = result.data, isLoading = false) }
                }
                if (result is Resource.Error) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}

// =====================================================================
// Screen
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onMagazineClick: (String) -> Unit,
    onNewspaperClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tabs = listOf("All", "Magazines", "Newspapers")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Search",
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
            IOCLSearchBar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = viewModel::search,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(12.dp))

            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = IOCLSaffron
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.onTabChange(index) },
                        text = {
                            Text(
                                title,
                                fontWeight = if (uiState.selectedTab == index)
                                    FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        IOCLLoadingIndicator("Searching...")
                    }
                }

                !uiState.hasSearched -> {
                    EmptyStateView(
                        icon = Icons.Filled.Search,
                        title = "Search Documents",
                        subtitle = "Type at least 2 characters to search"
                    )
                }

                else -> {
                    val items = when (uiState.selectedTab) {
                        1 -> uiState.magazines.map { SearchItem.MagazineItem(it) }
                        2 -> uiState.newspapers.map { SearchItem.NewspaperItem(it) }
                        else -> uiState.magazines.map { SearchItem.MagazineItem(it) } +
                                uiState.newspapers.map { SearchItem.NewspaperItem(it) }
                    }

                    if (items.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Filled.SearchOff,
                            title = "No Results",
                            subtitle = "Try different keywords"
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items, key = {
                                when (it) {
                                    is SearchItem.MagazineItem -> "mag_${it.magazine.id}"
                                    is SearchItem.NewspaperItem -> "news_${it.newspaper.id}"
                                }
                            }) { item ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + slideInVertically()
                                ) {
                                    when (item) {
                                        is SearchItem.MagazineItem -> {
                                            DocumentCard(
                                                title = item.magazine.title,
                                                subtitle = "Magazine • ${item.magazine.year}",
                                                coverImageUrl = if (item.magazine.pdfUrl.startsWith("file:///android_asset/")) item.magazine.pdfUrl else item.magazine.coverImageUrl,
                                                category = "MAGAZINE",
                                                viewCount = item.magazine.viewCount,
                                                isBookmarked = item.magazine.isBookmarked,
                                                onCardClick = { onMagazineClick(item.magazine.id) },
                                                onBookmarkClick = { }
                                            )
                                        }

                                        is SearchItem.NewspaperItem -> {
                                            DocumentCard(
                                                title = item.newspaper.title,
                                                subtitle = "Newspaper • ${item.newspaper.edition}",
                                                coverImageUrl = item.newspaper.coverImageUrl,
                                                category = "NEWSPAPER",
                                                viewCount = item.newspaper.viewCount,
                                                isBookmarked = item.newspaper.isBookmarked,
                                                onCardClick = { onNewspaperClick(item.newspaper.id) },
                                                onBookmarkClick = { }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
