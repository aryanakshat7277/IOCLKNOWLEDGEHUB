package com.iocl.knowledgehub.ui.newspaper

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iocl.knowledgehub.ui.components.*
import com.iocl.knowledgehub.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewspaperListScreen(
    onBackClick: () -> Unit,
    onNewspaperClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onUploadClick: () -> Unit = {},
    viewModel: NewspaperViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filters = listOf("All", "Today", "This Week", "This Month")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Admin Notices",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = IOCLSaffron
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = onUploadClick,
                    icon = { Icon(Icons.Filled.Upload, contentDescription = "Upload Notice") },
                    text = { Text("Upload Notice") },
                    containerColor = IOCLSaffron,
                    contentColor = IOCLWhite
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FilterChipRow(
                options = filters,
                selectedOption = uiState.selectedFilter,
                onOptionSelected = viewModel::onFilterSelected,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when {
                uiState.isLoading -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(6) { DocumentCardShimmer() }
                    }
                }

                uiState.error != null -> {
                    ErrorView(
                        message = uiState.error!!,
                        onRetry = viewModel::refresh
                    )
                }

                uiState.newspapers.isEmpty() -> {
                    EmptyStateView(
                        icon = Icons.Filled.Newspaper,
                        title = "No Newspapers Found",
                        subtitle = "No newspapers available for the selected period"
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.newspapers, key = { it.id }) { newspaper ->
                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            DocumentCard(
                                title = newspaper.title,
                                subtitle = sdf.format(Date(newspaper.publishDate)),
                                coverImageUrl = newspaper.coverImageUrl,
                                category = newspaper.category.name,
                                viewCount = newspaper.viewCount,
                                isBookmarked = newspaper.isBookmarked,
                                onCardClick = { onNewspaperClick(newspaper.id) },
                                onBookmarkClick = { viewModel.toggleBookmark(newspaper) }
                            )
                        }
                    }
                }
            }
        }
    }
}
