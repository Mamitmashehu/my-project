package com.scarguard.app.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wraps the CameraX setup needed for the baseline / follow-up capture screens: a live preview
 * plus a still-image capture use case, writing JPEGs into app-private storage.
 */
class PhotoCaptureController(private val context: Context) {

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    fun bindToLifecycle(previewView: PreviewView, lifecycleOwner: LifecycleOwner, onError: (Throwable) -> Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()
                imageCapture = capture

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera", e)
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun unbind() {
        cameraProvider?.unbindAll()
    }

    suspend fun capturePhoto(destination: File): File = suspendCancellableCoroutine { cont ->
        val capture = imageCapture ?: run {
            cont.resumeWithException(IllegalStateException("Camera is not ready yet"))
            return@suspendCancellableCoroutine
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(destination).build()
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    cont.resume(destination)
                }

                override fun onError(exception: ImageCaptureException) {
                    cont.resumeWithException(exception)
                }
            }
        )
    }

    companion object {
        private const val TAG = "PhotoCaptureController"

        fun newPhotoFile(context: Context, prefix: String): File {
            val dir = File(context.filesDir, "photos").apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(java.util.Date())
            return File(dir, "${prefix}_$timestamp.jpg")
        }
    }
}
