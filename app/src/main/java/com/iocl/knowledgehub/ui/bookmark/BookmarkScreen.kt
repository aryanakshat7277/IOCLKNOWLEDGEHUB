package com.iocl.knowledgehub.ui.bookmark

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.iocl.knowledgehub.domain.model.*
import com.iocl.knowledgehub.domain.repository.BookmarkRepository
import com.iocl.knowledgehub.domain.usecase.bookmark.GetBookmarksUseCase
import com.iocl.knowledgehub.domain.usecase.bookmark.ToggleBookmarkUseCase
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

data class BookmarkUiState(
    val bookmarks: List<Bookmark> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

// =====================================================================
// ViewModel
// =====================================================================

@HiltViewModel
class BookmarkViewModel @Inject constructor(
    private val getBookmarksUseCase: GetBookmarksUseCase,
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookmarkUiState())
    val uiState: StateFlow<BookmarkUiState> = _uiState.asStateFlow()

    init {
        loadBookmarks()
    }

    private fun loadBookmarks() {
        viewModelScope.launch {
            getBookmarksUseCase().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                bookmarks = result.data,
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

    fun removeBookmark(documentId: String) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmark(documentId)
            // Refresh bookmarks list
            loadBookmarks()
        }
    }

    fun refresh() {
        loadBookmarks()
    }
}

// =====================================================================
// Screen
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(
    onBackClick: () -> Unit,
    onMagazineClick: (String) -> Unit,
    onNewspaperClick: (String) -> Unit,
    viewModel: BookmarkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Bookmarks",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                    IOCLLoadingIndicator("Loading bookmarks...")
                }
            }

            uiState.error != null -> {
                ErrorView(
                    message = uiState.error!!,
                    onRetry = viewModel::refresh,
                    modifier = Modifier.padding(padding)
                )
            }

            uiState.bookmarks.isEmpty() -> {
                EmptyStateView(
                    icon = Icons.Filled.BookmarkBorder,
                    title = "No Bookmarks Yet",
                    subtitle = "Save your favorite magazines and newspapers to read later",
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.bookmarks,
                        key = { it.id }
                    ) { bookmark ->
                        BookmarkItem(
                            bookmark = bookmark,
                            onClick = {
                                when (bookmark.documentType) {
                                    DocumentType.MAGAZINE -> onMagazineClick(bookmark.documentId)
                                    DocumentType.NEWSPAPER -> onNewspaperClick(bookmark.documentId)
                                }
                            },
                            onRemove = { viewModel.removeBookmark(bookmark.documentId) }
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================
// Bookmark Item Composable
// =====================================================================

@Composable
private fun BookmarkItem(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = bookmark.coverImageUrl,
                contentDescription = bookmark.title,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Document type badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (bookmark.documentType) {
                        DocumentType.MAGAZINE -> IOCLSaffron.copy(alpha = 0.15f)
                        DocumentType.NEWSPAPER -> IOCLBlue.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = bookmark.documentType.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (bookmark.documentType) {
                            DocumentType.MAGAZINE -> IOCLSaffron
                            DocumentType.NEWSPAPER -> IOCLBlue
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = bookmark.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Saved on ${sdf.format(Date(bookmark.addedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Remove button
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Remove bookmark",
                    tint = IOCLRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
