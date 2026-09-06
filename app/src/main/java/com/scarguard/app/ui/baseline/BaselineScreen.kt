package com.scarguard.app.ui.baseline

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.scarguard.app.ui.capture.CameraCaptureView
import com.scarguard.app.ui.capture.RequiresCameraPermission
import com.scarguard.app.ui.scarGuardViewModel
import java.io.File

private sealed interface BaselineStep {
    data object Capturing : BaselineStep
    data class Review(val file: File, val bitmap: Bitmap) : BaselineStep
}

@Composable
fun BaselineScreen(onDone: () -> Unit, onCancel: () -> Unit) {
    val viewModel = scarGuardViewModel { BaselineViewModel(it) }
    var step by remember { mutableStateOf<BaselineStep>(BaselineStep.Capturing) }
    val liveTemperature by viewModel.liveTemperature.collectAsState()

    when (val current = step) {
        is BaselineStep.Capturing -> {
            RequiresCameraPermission {
                CameraCaptureView(
                    instructions = "Center the incision or wound area inside the frame, then capture. This becomes your reference photo.",
                    photoFilePrefix = "baseline",
                    onClose = onCancel,
                    onCaptured = { file, bitmap -> step = BaselineStep.Review(file, bitmap) },
                )
            }
        }
        is BaselineStep.Review -> {
            var label by rememberSaveable { mutableStateOf("") }
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text("Confirm baseline", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "This photo and today's temperature become the reference every future check compares against.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))

                Image(
                    bitmap = current.bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp)),
                )

                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (optional, e.g. \"Left knee incision\")") },
                    modifier = Modifier.fillMaxWidth(),
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                Text(
                    liveTemperature?.let { "Current sensor reading: %.1f°C".format(it) }
                        ?: "No live temperature yet -- connect the sensor to record one alongside this baseline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            viewModel.saveBaseline(label, current.file, current.bitmap, liveTemperature, onDone)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save as baseline") }

                    androidx.compose.material3.OutlinedButton(
                        onClick = { step = BaselineStep.Capturing },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Retake") }
                }
            }
        }
    }
}
