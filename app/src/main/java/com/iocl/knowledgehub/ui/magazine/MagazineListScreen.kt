package com.iocl.knowledgehub.ui.magazine

import androidx.compose.animation.*
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
import com.iocl.knowledgehub.domain.model.toMonthName
import com.iocl.knowledgehub.ui.components.*
import com.iocl.knowledgehub.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagazineListScreen(
    onBackClick: () -> Unit,
    onMagazineClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onUploadMagazineClick: () -> Unit = {},
    viewModel: MagazineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val years = (2026 downTo 2020).map { it.toString() }
    val months = listOf("All") + (1..12).map { it.toMonthName() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Magazines",
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (uiState.isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = onUploadMagazineClick,
                    icon = { Icon(Icons.Filled.Upload, contentDescription = "Upload") },
                    text = { Text("Upload Magazine") },
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

                uiState.magazines.isEmpty() -> {
                    EmptyStateView(
                        icon = Icons.Filled.MenuBook,
                        title = "No Magazines Found",
                        subtitle = "No magazines available for the selected period"
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.magazines, key = { it.id }) { magazine ->
                            DocumentCard(
                                title = magazine.title,
                                subtitle = "${magazine.month.toMonthName()} ${magazine.year}",
                                coverImageUrl = if (magazine.pdfUrl.startsWith("file:///android_asset/")) magazine.pdfUrl else magazine.coverImageUrl,
                                category = magazine.category.name,
                                viewCount = magazine.viewCount,
                                isBookmarked = magazine.isBookmarked,
                                onCardClick = { onMagazineClick(magazine.id) },
                                onBookmarkClick = { viewModel.toggleBookmark(magazine) }
                            )
                        }
                    }
                }
            }
        }
    }
}
