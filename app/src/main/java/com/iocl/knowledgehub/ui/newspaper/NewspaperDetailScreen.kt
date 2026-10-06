package com.iocl.knowledgehub.ui.newspaper

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.iocl.knowledgehub.domain.model.*
import com.iocl.knowledgehub.domain.repository.NewspaperRepository
import com.iocl.knowledgehub.domain.usecase.bookmark.ToggleBookmarkUseCase
import com.iocl.knowledgehub.ui.components.*
import com.iocl.knowledgehub.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.app.DownloadManager
import android.content.Context
import android.os.Environment

// =====================================================================
// ViewModel
// =====================================================================

import com.iocl.knowledgehub.domain.repository.AuthRepository
import com.iocl.knowledgehub.domain.model.UserRole

data class NewspaperDetailUiState(
    val newspaper: Newspaper? = null,
    val isLoading: Boolean = true,
    val isBookmarked: Boolean = false,
    val error: String? = null,
    val isAdmin: Boolean = false,
    val isDeleted: Boolean = false
)

@HiltViewModel
class NewspaperDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newspaperRepository: NewspaperRepository,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val newspaperId: String = savedStateHandle.get<String>("newspaperId") ?: ""
    private val _uiState = MutableStateFlow(NewspaperDetailUiState())
    val uiState: StateFlow<NewspaperDetailUiState> = _uiState.asStateFlow()

    init {
        val role = authRepository.getSavedRole()
        val isAdmin = role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN
        _uiState.update { it.copy(isAdmin = isAdmin) }
        loadNewspaper()
    }

    private fun loadNewspaper() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = newspaperRepository.getNewspaperById(newspaperId)) {
                is Resource.Success -> {
                    newspaperRepository.incrementViewCount(newspaperId)
                    _uiState.update {
                        it.copy(
                            newspaper = result.data,
                            isBookmarked = result.data.isBookmarked,
                            isLoading = false
                        )
                    }
                }

                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }

                is Resource.Loading -> {}
            }
        }
    }

    fun toggleBookmark() {
        val paper = _uiState.value.newspaper ?: return
        viewModelScope.launch {
            val result = toggleBookmarkUseCase(
                Bookmark(
                    documentId = paper.id,
                    documentType = DocumentType.NEWSPAPER,
                    title = paper.title,
                    coverImageUrl = paper.coverImageUrl
                )
            )
            if (result is Resource.Success) {
                _uiState.update { it.copy(isBookmarked = result.data) }
            }
        }
    }

    fun deleteNewspaper() {
        val paper = _uiState.value.newspaper ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = newspaperRepository.deleteNewspaper(paper.id)
            if (result is Resource.Success) {
                _uiState.update { it.copy(isDeleted = true, isLoading = false) }
            } else if (result is Resource.Error) {
                _uiState.update { it.copy(error = result.message, isLoading = false) }
            }
        }
    }
}

// =====================================================================
// Screen
// =====================================================================

@Composable
fun NewspaperDetailScreen(
    newspaperId: String,
    onBackClick: () -> Unit,
    onOpenPdf: (String, String) -> Unit, // Keeping this for signature compatibility
    viewModel: NewspaperDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            Toast.makeText(context, "Notice deleted successfully", Toast.LENGTH_SHORT).show()
            onBackClick()
        }
    }
    
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                IOCLLoadingIndicator("Loading notice...")
            }
        }

        uiState.error != null -> {
            ErrorView(message = uiState.error!!, onRetry = {})
        }

        uiState.newspaper != null -> {
            val paper = uiState.newspaper!!
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero Image Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    AsyncImage(
                        model = paper.coverImageUrl,
                        contentDescription = paper.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(0.4f),
                                        Color.Transparent,
                                        Color.Black.copy(0.8f)
                                    )
                                )
                            )
                    )

                    // Top action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Back",
                                tint = IOCLWhite
                            )
                        }
                        
                        Row {
                            if (uiState.isAdmin) {
                                var showDeleteConfirm by remember { mutableStateOf(false) }
                                IconButton(onClick = { showDeleteConfirm = true }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                                
                                if (showDeleteConfirm) {
                                    AlertDialog(
                                        onDismissRequest = { showDeleteConfirm = false },
                                        title = { Text("Delete Notice") },
                                        text = { Text("Are you sure you want to delete this notice? This action cannot be undone.") },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    showDeleteConfirm = false
                                                    viewModel.deleteNewspaper()
                                                }
                                            ) {
                                                Text("Delete", color = Color.Red)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showDeleteConfirm = false }) {
                                                Text("Cancel")
                                            }
                                        }
                                    )
                                }
                            }
                            
                            IconButton(onClick = viewModel::toggleBookmark) {
                                Icon(
                                    if (uiState.isBookmarked) Icons.Filled.Bookmark
                                    else Icons.Outlined.BookmarkBorder,
                                    "Bookmark",
                                    tint = if (uiState.isBookmarked) IOCLSaffron else IOCLWhite
                                )
                            }
                        }
                    }

                    // Title overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = IOCLSaffron
                        ) {
                            Text(
                                paper.category.name,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = IOCLWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            paper.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = IOCLWhite
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Uploaded by ${paper.uploadedBy.ifBlank { "Admin" }} • ${sdf.format(Date(paper.publishDate))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = IOCLGrayMedium
                        )
                    }
                }

                // Content Section
                Column(modifier = Modifier.padding(20.dp)) {
                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        NewspaperDetailStat(
                            Icons.Filled.Download,
                            "${paper.downloadCount}",
                            "Downloads"
                        )
                        NewspaperDetailStat(Icons.Filled.Visibility, "${paper.viewCount}", "Views")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

                    Text(
                        "Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        paper.summary.ifBlank { "No summary available for this notice." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(24.dp))

                    // Action buttons
                    Button(
                        onClick = { onOpenPdf(paper.pdfUrl, paper.title) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IOCLSaffron)
                    ) {
                        Icon(Icons.Filled.MenuBook, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Read Notice",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { 
                            if (paper.pdfUrl.startsWith("file:///android_asset/")) {
                                try {
                                    val assetPath = paper.pdfUrl.removePrefix("file:///android_asset/")
                                    val fileName = assetPath.substringAfterLast('/')
                                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                                    val destFile = java.io.File(downloadsDir, fileName)
                                    
                                    context.assets.open(assetPath).use { input ->
                                        java.io.FileOutputStream(destFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    android.widget.Toast.makeText(context, "Saved to Downloads: $fileName", android.widget.Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Download failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IOCLSaffron)
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = null,
                            tint = IOCLSaffron
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Download PDF",
                            fontWeight = FontWeight.SemiBold,
                            color = IOCLSaffron
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewspaperDetailStat(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = IOCLSaffron, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
