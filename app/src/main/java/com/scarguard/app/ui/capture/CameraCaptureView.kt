package com.scarguard.app.ui.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.scarguard.app.camera.PhotoCaptureController
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-bleed camera preview with a square alignment guide (so the baseline photo and every
 * follow-up photo frame the wound the same way) and a shutter button. Reusable by the baseline
 * and follow-up capture screens.
 */
@Composable
fun CameraCaptureView(
    instructions: String,
    photoFilePrefix: String,
    onClose: () -> Unit,
    onCaptured: (File, Bitmap) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val controller = remember { PhotoCaptureController(context) }

    var isCapturing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    controller.bindToLifecycle(previewView, lifecycleOwner) { throwable ->
                        error = throwable.message ?: "Could not start the camera"
                    }
                }
            },
        )

        // Alignment guide: a centered square outline the user lines the wound up inside.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val guideSize = size.minDimension * 0.68f
            val left = (size.width - guideSize) / 2f
            val top = (size.height - guideSize) / 2f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = Size(guideSize, guideSize),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 3.dp.toPx()),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            Text(instructions, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodySmall)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
        ) {
            if (isCapturing) {
                CircularProgressIndicator(color = Color.White)
            } else {
                IconButton(
                    onClick = {
                        isCapturing = true
                        error = null
                        val destination = PhotoCaptureController.newPhotoFile(context, photoFilePrefix)
                        scope.launch {
                            try {
                                val file = controller.capturePhoto(destination)
                                val bitmap = withContext(Dispatchers.IO) {
                                    BitmapFactory.decodeFile(file.absolutePath)
                                }
                                isCapturing = false
                                if (bitmap != null) onCaptured(file, bitmap)
                                else error = "Couldn't read the photo back, try again"
                            } catch (t: Throwable) {
                                isCapturing = false
                                error = t.message ?: "Couldn't capture the photo"
                            }
                        }
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White, CircleShape),
                ) {
                    Icon(Icons.Filled.Lens, contentDescription = "Capture", tint = Color.Black, modifier = Modifier.size(56.dp))
                }
            }
        }
    }
}
