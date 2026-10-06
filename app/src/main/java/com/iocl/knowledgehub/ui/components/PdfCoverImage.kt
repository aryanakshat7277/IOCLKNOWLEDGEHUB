package com.iocl.knowledgehub.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.iocl.knowledgehub.ui.theme.IOCLSaffron
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun PdfCoverImage(
    pdfUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(pdfUrl) {
        if (!pdfUrl.startsWith("file:///android_asset/")) {
            hasError = true
            return@LaunchedEffect
        }
        
        withContext(Dispatchers.IO) {
            try {
                val assetPath = pdfUrl.removePrefix("file:///android_asset/")
                val thumbFile = File(context.cacheDir, "thumb_${assetPath.substringAfterLast('/')}.jpg")
                
                if (thumbFile.exists()) {
                    coverBitmap = android.graphics.BitmapFactory.decodeFile(thumbFile.absolutePath)
                } else {
                    val cachePdfFile = File(context.cacheDir, "cover_${assetPath.substringAfterLast('/')}")
                    
                    if (!cachePdfFile.exists()) {
                        context.assets.open(assetPath).use { input ->
                            FileOutputStream(cachePdfFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    
                    val fileDescriptor = ParcelFileDescriptor.open(cachePdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val pdfRenderer = PdfRenderer(fileDescriptor)
                    
                    if (pdfRenderer.pageCount > 0) {
                        val page = pdfRenderer.openPage(0)
                        // Scale down resolution for faster rendering and smaller memory footprint
                        val bitmap = Bitmap.createBitmap(
                            page.width / 2,
                            page.height / 2,
                            Bitmap.Config.ARGB_8888
                        )
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        
                        // Save the thumbnail to disk so next time it loads instantly
                        FileOutputStream(thumbFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        }
                        
                        coverBitmap = bitmap
                    }
                    pdfRenderer.close()
                    fileDescriptor.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                hasError = true
            }
        }
    }

    Box(
        modifier = modifier.background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        if (coverBitmap != null) {
            Image(
                bitmap = coverBitmap!!.asImageBitmap(),
                contentDescription = "PDF Cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (hasError) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Gray))
        } else {
            CircularProgressIndicator(color = IOCLSaffron)
        }
    }
}
