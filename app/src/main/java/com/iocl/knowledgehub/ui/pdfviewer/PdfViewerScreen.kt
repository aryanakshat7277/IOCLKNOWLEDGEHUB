package com.iocl.knowledgehub.ui.pdfviewer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iocl.knowledgehub.ui.components.ErrorView
import com.iocl.knowledgehub.ui.components.IOCLLoadingIndicator
import com.iocl.knowledgehub.ui.theme.IOCLSaffron
import androidx.compose.ui.text.font.FontFamily
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import javax.inject.Inject

// =====================================================================
// UI State
// =====================================================================
data class PdfViewerUiState(
    val pdfUrl: String = "",
    val title: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val pageCount: Int = 0
)

// =====================================================================
// ViewModel
// =====================================================================
@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfViewerUiState())
    val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    init {
        val rawUrl = savedStateHandle.get<String>("pdfUrl") ?: ""
        val rawTitle = savedStateHandle.get<String>("title") ?: "Document"

        val decodedUrl = try { URLDecoder.decode(rawUrl, "UTF-8") } catch (e: Exception) { rawUrl }
        val decodedTitle = try { URLDecoder.decode(rawTitle, "UTF-8") } catch (e: Exception) { rawTitle }

        _uiState.update {
            it.copy(
                pdfUrl = decodedUrl,
                title = decodedTitle,
                isLoading = true,
                error = if (decodedUrl.isBlank()) "Invalid PDF URL" else null
            )
        }

        if (decodedUrl.isNotBlank()) {
            loadPdf(decodedUrl)
        }
    }

    private fun loadPdf(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // If it's an asset file
                val file = if (url.startsWith("file:///android_asset/")) {
                    val assetPath = url.removePrefix("file:///android_asset/")
                    val cacheFile = File(context.cacheDir, assetPath.substringAfterLast('/'))
                    if (!cacheFile.exists()) {
                        context.assets.open(assetPath).use { input ->
                            FileOutputStream(cacheFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    cacheFile
                } else if (url.startsWith("http")) {
                    val fileName = url.substringAfterLast('/').substringBefore('?')
                    val cacheFile = File(context.cacheDir, fileName)
                    if (!cacheFile.exists()) {
                        val client = okhttp3.OkHttpClient()
                        val request = okhttp3.Request.Builder().url(url).build()
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            val body = response.body
                            if (body != null) {
                                FileOutputStream(cacheFile).use { output ->
                                    body.byteStream().copyTo(output)
                                }
                            } else {
                                throw Exception("Empty response from server")
                            }
                        } else if (response.code == 401) {
                            throw Exception("Cloudinary security block: Old magazine upload format. Please upload a NEW magazine.")
                        } else {
                            throw Exception("Server returned HTTP ${response.code}")
                        }
                    }
                    cacheFile
                } else {
                    throw Exception("Unsupported PDF URL format: $url")
                }

                fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fileDescriptor!!)
                
                _uiState.update { 
                    it.copy(isLoading = false, pageCount = pdfRenderer?.pageCount ?: 0) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load PDF: ${e.message}") }
            }
        }
    }

    suspend fun getPageBitmap(pageIndex: Int): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex >= renderer.pageCount) return@withContext null

        var bitmap: Bitmap? = null
        try {
            val page = renderer.openPage(pageIndex)
            // Render at 2x resolution for better quality
            bitmap = Bitmap.createBitmap(
                page.width * 2,
                page.height * 2,
                Bitmap.Config.ARGB_8888
            )
            // Fill white background (PDFs are transparent by default)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext bitmap
    }

    fun downloadPdfToStorage(context: Context) {
        val url = uiState.value.pdfUrl
        if (!url.startsWith("file:///android_asset/")) {
            Toast.makeText(context, "Only offline assets can be downloaded directly.", Toast.LENGTH_SHORT).show()
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val assetPath = url.removePrefix("file:///android_asset/")
                val fileName = assetPath.substringAfterLast('/')
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destFile = File(downloadsDir, fileName)
                
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pdfRenderer?.close()
        fileDescriptor?.close()
    }
    
    fun retry() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadPdf(uiState.value.pdfUrl)
    }
}

// =====================================================================
// Screen
// =====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfUrl: String,
    title: String,
    onBackClick: () -> Unit,
    viewModel: PdfViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            uiState.title,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.downloadPdfToStorage(context) }) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = "Download",
                            tint = IOCLSaffron
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.LightGray)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        IOCLLoadingIndicator("Loading Offline PDF...")
                    }
                }
                uiState.error != null -> {
                    ErrorView(
                        message = uiState.error!!,
                        onRetry = viewModel::retry
                    )
                }
                else -> {
                    var scale by remember { mutableFloatStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }
                    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

                    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                        scale = (scale * zoomChange).coerceIn(1f, 5f)
                        if (scale == 1f) {
                            offset = Offset.Zero
                        } else {
                            val maxX = (viewportSize.width * (scale - 1)) / 2f
                            val maxY = (viewportSize.height * (scale - 1)) / 2f
                            offset = Offset(
                                x = (offset.x + offsetChange.x).coerceIn(-maxX, maxX),
                                y = (offset.y + offsetChange.y).coerceIn(-maxY, maxY)
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { viewportSize = it }
                            .pointerInput(scale) {
                                if (scale > 1f) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val maxX = (viewportSize.width * (scale - 1)) / 2f
                                        val maxY = (viewportSize.height * (scale - 1)) / 2f
                                        offset = Offset(
                                            x = (offset.x + dragAmount.x).coerceIn(-maxX, maxX),
                                            y = (offset.y + dragAmount.y).coerceIn(-maxY, maxY)
                                        )
                                    }
                                }
                            }
                            .transformable(state = state)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        userScrollEnabled = scale == 1f
                    ) {
                        items(uiState.pageCount) { index ->
                            var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
                            
                            LaunchedEffect(index) {
                                pageBitmap = viewModel.getPageBitmap(index)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                                    .aspectRatio(0.7f) // approximate A4 ratio before bitmap loads
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                if (pageBitmap != null) {
                                    Image(
                                        bitmap = pageBitmap!!.asImageBitmap(),
                                        contentDescription = "Page ${index + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    CircularProgressIndicator(color = IOCLSaffron)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
