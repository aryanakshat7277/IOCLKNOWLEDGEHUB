package com.iocl.knowledgehub.ui.magazine

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
import com.iocl.knowledgehub.domain.repository.MagazineRepository
import com.iocl.knowledgehub.domain.repository.AuthRepository
import com.iocl.knowledgehub.domain.usecase.bookmark.ToggleBookmarkUseCase
import com.iocl.knowledgehub.ui.components.*
import com.iocl.knowledgehub.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.widget.Toast

// =====================================================================
// ViewModel
// =====================================================================

data class MagazineDetailUiState(
    val magazine: Magazine? = null,
    val isLoading: Boolean = true,
    val isBookmarked: Boolean = false,
    val error: String? = null,
    val isAdmin: Boolean = false,
    val isDeleted: Boolean = false
)

@HiltViewModel
class MagazineDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val magazineRepository: MagazineRepository,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val magazineId: String = savedStateHandle.get<String>("magazineId") ?: ""
    private val _uiState = MutableStateFlow(MagazineDetailUiState())
    val uiState: StateFlow<MagazineDetailUiState> = _uiState.asStateFlow()

    init {
        val role = authRepository.getSavedRole()
        val isAdmin = role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN
        _uiState.update { it.copy(isAdmin = isAdmin) }
        loadMagazine()
    }

    private fun loadMagazine() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = magazineRepository.getMagazineById(magazineId)) {
                is Resource.Success -> {
                    magazineRepository.incrementViewCount(magazineId)
                    _uiState.update {
                        it.copy(
                            magazine = result.data,
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
        val mag = _uiState.value.magazine ?: return
        viewModelScope.launch {
            val result = toggleBookmarkUseCase(
                Bookmark(
                    documentId = mag.id,
                    documentType = DocumentType.MAGAZINE,
                    title = mag.title,
                    coverImageUrl = mag.coverImageUrl
                )
            )
            if (result is Resource.Success) {
                _uiState.update { it.copy(isBookmarked = result.data) }
            }
        }
    }

    fun deleteMagazine() {
        val mag = _uiState.value.magazine ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = magazineRepository.deleteMagazine(mag.id)
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
fun MagazineDetailScreen(
    magazineId: String,
    onBackClick: () -> Unit,
    onOpenPdf: (String, String) -> Unit, // Keeping for signature compatibility
    viewModel: MagazineDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            Toast.makeText(context, "Magazine deleted successfully", Toast.LENGTH_SHORT).show()
            onBackClick()
        }
    }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                IOCLLoadingIndicator("Loading magazine...")
            }
        }

        uiState.error != null -> {
            ErrorView(message = uiState.error!!, onRetry = {})
        }

        uiState.magazine != null -> {
            val magazine = uiState.magazine!!
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
                    if (magazine.pdfUrl.startsWith("file:///android_asset/")) {
                        PdfCoverImage(
                            pdfUrl = magazine.pdfUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = magazine.coverImageUrl,
                            contentDescription = magazine.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
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
                                        title = { Text("Delete Magazine") },
                                        text = { Text("Are you sure you want to delete this magazine? This action cannot be undone.") },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    showDeleteConfirm = false
                                                    viewModel.deleteMagazine()
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
                                magazine.category.name,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = IOCLWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            magazine.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = IOCLWhite
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

                        DetailStat(
                            Icons.Filled.Download,
                            "${magazine.downloadCount}",
                            "Downloads"
                        )
                        DetailStat(Icons.Filled.Description, "${magazine.pageCount}", "Pages")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

                    Text(
                        "Description",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        magazine.description.ifBlank { "No description available for this magazine." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    // Edition info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Edition",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${magazine.month.toMonthName()} ${magazine.year}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Action buttons
                    Button(
                        onClick = { onOpenPdf(magazine.pdfUrl, magazine.title) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IOCLSaffron)
                    ) {
                        Icon(Icons.Filled.MenuBook, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Read Magazine",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { 
                            if (magazine.pdfUrl.startsWith("file:///android_asset/")) {
                                try {
                                    val assetPath = magazine.pdfUrl.removePrefix("file:///android_asset/")
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

// =====================================================================
// Detail Stat composable
// =====================================================================

@Composable
private fun DetailStat(icon: ImageVector, value: String, label: String) {
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
