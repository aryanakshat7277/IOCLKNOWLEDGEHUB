package com.iocl.knowledgehub.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iocl.knowledgehub.domain.model.*
import com.iocl.knowledgehub.domain.usecase.magazine.UploadMagazineUseCase
import com.iocl.knowledgehub.domain.usecase.newspaper.UploadNewspaperUseCase
import com.iocl.knowledgehub.domain.repository.NotificationRepository
import com.iocl.knowledgehub.ui.components.*
import com.iocl.knowledgehub.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// =====================================================================
// UI State
// =====================================================================

data class UploadDocumentUiState(
    val documentType: String = "Magazine",
    val title: String = "",
    val description: String = "",
    val selectedCategory: String = "CORPORATE",
    val year: String = "2026",
    val month: String = "1",
    val edition: String = "",
    val pdfUri: Uri? = null,
    val pdfFileName: String = "",
    val coverUri: Uri? = null,
    val coverFileName: String = "",
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val uploadSuccess: Boolean = false,
    val error: String? = null
)

// =====================================================================
// ViewModel
// =====================================================================

@HiltViewModel
class UploadDocumentViewModel @Inject constructor(
    private val uploadMagazineUseCase: UploadMagazineUseCase,
    private val uploadNewspaperUseCase: UploadNewspaperUseCase,
    private val notificationRepository: NotificationRepository,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val initialDocType = savedStateHandle.get<String>("type") ?: "Magazine"
    private val _uiState = MutableStateFlow(UploadDocumentUiState(documentType = initialDocType))
    val uiState: StateFlow<UploadDocumentUiState> = _uiState.asStateFlow()

    val magazineCategories = MagazineCategory.entries.map { it.name }
    val newspaperCategories = NewspaperCategory.entries.map { it.name }

    fun onDocTypeChange(type: String) {
        _uiState.update { it.copy(documentType = type, selectedCategory = "") }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title, error = null) }
    }

    fun onDescriptionChange(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    fun onCategoryChange(cat: String) {
        _uiState.update { it.copy(selectedCategory = cat) }
    }

    fun onYearChange(year: String) {
        _uiState.update { it.copy(year = year) }
    }

    fun onMonthChange(month: String) {
        _uiState.update { it.copy(month = month) }
    }

    fun onEditionChange(edition: String) {
        _uiState.update { it.copy(edition = edition) }
    }

    fun onPdfSelected(uri: Uri?, fileName: String) {
        _uiState.update { it.copy(pdfUri = uri, pdfFileName = fileName, error = null) }
    }

    fun onCoverSelected(uri: Uri?, fileName: String) {
        _uiState.update { it.copy(coverUri = uri, coverFileName = fileName) }
    }

    fun upload(pdfBytes: ByteArray, coverBytes: ByteArray?) {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "Title is required") }
            return
        }
        if (pdfBytes.isEmpty()) {
            _uiState.update { it.copy(error = "Please select a PDF file") }
            return
        }

        _uiState.update { it.copy(isUploading = true, error = null) }

        viewModelScope.launch {
            val result = if (state.documentType == "Magazine") {
                val magazine = Magazine(
                    title = state.title,
                    description = state.description,
                    month = state.month.toIntOrNull() ?: 1,
                    year = state.year.toIntOrNull() ?: 2026,
                    category = try {
                        MagazineCategory.valueOf(state.selectedCategory)
                    } catch (e: Exception) {
                        MagazineCategory.CORPORATE
                    }
                )
                uploadMagazineUseCase(magazine, pdfBytes, coverBytes)
            } else {
                val newspaper = Newspaper(
                    title = state.title,
                    summary = state.description,
                    edition = state.edition,
                    category = try {
                        NewspaperCategory.valueOf(state.selectedCategory)
                    } catch (e: Exception) {
                        NewspaperCategory.DAILY
                    }
                )
                uploadNewspaperUseCase(newspaper, pdfBytes, coverBytes)
            }

            when (result) {
                is Resource.Success -> {
                    val docId = if (state.documentType == "Magazine") (result.data as Magazine).id else (result.data as Newspaper).id
                    val notification = Notification(
                        title = "New ${state.documentType} Uploaded",
                        message = "'${state.title}' has been uploaded by the admin.",
                        type = if (state.documentType == "Magazine") NotificationType.NEW_MAGAZINE else NotificationType.NEW_NEWSPAPER,
                        documentId = docId,
                        documentType = if (state.documentType == "Magazine") DocumentType.MAGAZINE else DocumentType.NEWSPAPER
                    )
                    notificationRepository.addNotification(notification)
                    
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadSuccess = true
                        )
                    }
                }

                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            error = result.message
                        )
                    }
                }

                is Resource.Loading -> {}
            }
        }
    }

    fun resetForm() {
        _uiState.value = UploadDocumentUiState()
    }
}

// =====================================================================
// Screen
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadDocumentScreen(
    onBackClick: () -> Unit,
    viewModel: UploadDocumentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // File pickers
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val name = uri.lastPathSegment ?: "document.pdf"
            viewModel.onPdfSelected(uri, name)
        }
    }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val name = uri.lastPathSegment ?: "cover.jpg"
            viewModel.onCoverSelected(uri, name)
        }
    }

    // Success snackbar
    LaunchedEffect(uiState.uploadSuccess) {
        if (uiState.uploadSuccess) {
            viewModel.resetForm()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Upload Document",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {


            // Title
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IOCLSaffron,
                    focusedLabelColor = IOCLSaffron,
                    cursorColor = IOCLSaffron
                ),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            // Description
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IOCLSaffron,
                    focusedLabelColor = IOCLSaffron,
                    cursorColor = IOCLSaffron
                ),
                maxLines = 5
            )

            Spacer(Modifier.height(12.dp))

            // Category Dropdown
            val categories = if (uiState.documentType == "Magazine")
                viewModel.magazineCategories else viewModel.newspaperCategories
            var categoryExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IOCLSaffron,
                        focusedLabelColor = IOCLSaffron
                    )
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                viewModel.onCategoryChange(cat)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Year and Month / Edition
            if (uiState.documentType == "Magazine") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.year,
                        onValueChange = viewModel::onYearChange,
                        label = { Text("Year") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IOCLSaffron,
                            focusedLabelColor = IOCLSaffron,
                            cursorColor = IOCLSaffron
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.month,
                        onValueChange = viewModel::onMonthChange,
                        label = { Text("Month (1-12)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IOCLSaffron,
                            focusedLabelColor = IOCLSaffron,
                            cursorColor = IOCLSaffron
                        ),
                        singleLine = true
                    )
                }
            } else {
                OutlinedTextField(
                    value = uiState.edition,
                    onValueChange = viewModel::onEditionChange,
                    label = { Text("Edition") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IOCLSaffron,
                        focusedLabelColor = IOCLSaffron,
                        cursorColor = IOCLSaffron
                    ),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(20.dp))

            // PDF File Picker
            Text(
                "PDF File *",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            FilePickerButton(
                label = uiState.pdfFileName.ifBlank { "Select PDF File" },
                isSelected = uiState.pdfUri != null,
                icon = Icons.Filled.PictureAsPdf,
                onClick = { pdfPicker.launch("application/pdf") }
            )

            Spacer(Modifier.height(12.dp))

            // Cover Image Picker
            Text(
                "Cover Image (optional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            FilePickerButton(
                label = uiState.coverFileName.ifBlank { "Select Cover Image" },
                isSelected = uiState.coverUri != null,
                icon = Icons.Filled.Image,
                onClick = { coverPicker.launch("image/*") }
            )

            // Error
            AnimatedVisibility(visible = uiState.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = IOCLRed.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = null,
                            tint = IOCLRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            uiState.error ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = IOCLRed
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Upload Button
            Button(
                onClick = {
                    val pdfBytes = uiState.pdfUri?.let { uri ->
                        context.contentResolver.openInputStream(uri)?.readBytes()
                    } ?: ByteArray(0)

                    val coverBytes = uiState.coverUri?.let { uri ->
                        context.contentResolver.openInputStream(uri)?.readBytes()
                    }

                    viewModel.upload(pdfBytes, coverBytes)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IOCLSaffron),
                enabled = !uiState.isUploading
            ) {
                if (uiState.isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = IOCLWhite,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Uploading...")
                } else {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Upload Document",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            // Success message
            AnimatedVisibility(visible = uiState.uploadSuccess) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = IOCLGreen.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = IOCLGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Document uploaded successfully!",
                            style = MaterialTheme.typography.bodySmall,
                            color = IOCLGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// =====================================================================
// File Picker Button
// =====================================================================

@Composable
private fun FilePickerButton(
    label: String,
    isSelected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) IOCLSaffron else IOCLGrayLight
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) IOCLSaffron.copy(alpha = 0.12f)
                        else IOCLGrayLight.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) IOCLSaffron else IOCLGrayMedium,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else IOCLGrayMedium,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = IOCLGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

