package com.aalaya.pg.feature.receipts

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.HamsaTopBar
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.theme.HamsaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PdfPreviewScreen(uri: Uri, onBack: () -> Unit) {
    val context = LocalContext.current
    var page by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(uri) { mutableStateOf(false) }
    var scale by remember(uri) { mutableStateOf(1f) }
    var offsetX by remember(uri) { mutableStateOf(0f) }
    var offsetY by remember(uri) { mutableStateOf(0f) }
    val transformState = rememberTransformableState { zoom, pan, _ ->
        val nextScale = (scale * zoom).coerceIn(1f, 4f)
        scale = nextScale
        if (nextScale == 1f) {
            // At fit-to-page scale there is no useful panning; keeping the
            // image centred also makes pinch-out return to a clean baseline.
            offsetX = 0f
            offsetY = 0f
        } else {
            offsetX += pan.x
            offsetY += pan.y
        }
    }
    LaunchedEffect(uri) {
        val rendered = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        if (renderer.pageCount == 0) null else renderer.openPage(0).use { pdfPage ->
                            Bitmap.createBitmap(pdfPage.width, pdfPage.height, Bitmap.Config.ARGB_8888).also {
                                pdfPage.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            }
                        }
                    }
                }
            }.getOrNull()
        }
        page = rendered
        failed = rendered == null
    }
    Column(modifier = Modifier.fillMaxSize().background(HamsaTheme.colors.canvas)) {
        HamsaTopBar(title = "Receipt preview", onBack = onBack)
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                page != null -> Box(
                    modifier = Modifier.fillMaxSize().transformable(transformState),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = page!!.asImageBitmap(),
                        contentDescription = "Receipt preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        },
                    )
                }
                failed -> ErrorState("Unable to preview this receipt.", onRetry = onBack)
                else -> LoadingState(spinner = true)
            }
        }
    }
}
