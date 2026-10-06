package com.iocl.knowledgehub.ui.livenews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iocl.knowledgehub.ui.components.*
import com.iocl.knowledgehub.ui.theme.IOCLBlueAccent
import androidx.compose.ui.text.font.FontFamily
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveNewsScreen(
    onBackClick: () -> Unit,
    onNewsClick: (String) -> Unit,
    viewModel: LiveNewsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDateInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.selectDate(it)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = IOCLBlueAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Live News",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.isLatestNewsMode,
                    onClick = { viewModel.setLatestNewsMode(true) },
                    label = { Text("Latest 7 Days", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IOCLBlueAccent,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !uiState.isLatestNewsMode,
                    onClick = { viewModel.setLatestNewsMode(false) },
                    label = { Text("Specific Date", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IOCLBlueAccent,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Premium Selected Date display card
            if (!uiState.isLatestNewsMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = "Select Date",
                                tint = IOCLBlueAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Selected Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date(uiState.selectedDateInMillis)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = IOCLBlueAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            CategoryFilterRow(
                selected = uiState.selectedCategory,
                onSelect = viewModel::selectCategory
            )

            if (uiState.isLoading) {
                ShimmerNewsList()
            } else if (uiState.error != null) {
                ErrorView(
                    message = uiState.error!!,
                    onRetry = viewModel::loadNews
                )
            } else if (uiState.newsItems.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Filled.Newspaper,
                    title = "No Live News",
                    subtitle = "Check back later for latest updates",
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                if (uiState.lastUpdated > 0L) {
                    LastUpdatedBanner(uiState.lastUpdated, uiState.filteredNewsItems.size)
                    Spacer(Modifier.height(8.dp))
                }
                if (uiState.filteredNewsItems.isNotEmpty()) {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            FeaturedNewsCard(
                                article = uiState.filteredNewsItems.first(),
                                onClick = { onNewsClick(uiState.filteredNewsItems.first().id) }
                            )
                        }
                        items(uiState.filteredNewsItems.drop(1)) { article ->
                            NewsListCard(
                                article = article,
                                onClick = { onNewsClick(article.id) }
                            )
                        }
                    }
                } else {
                    EmptyStateView(
                        icon = Icons.Filled.Newspaper,
                        title = "No Articles Found",
                        subtitle = "No news matching selected filters for this date",
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        }
    }
}
