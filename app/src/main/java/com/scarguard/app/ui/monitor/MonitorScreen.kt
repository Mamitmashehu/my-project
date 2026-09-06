package com.scarguard.app.ui.monitor

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.scarguard.app.data.RiskClassifier
import com.scarguard.app.repository.MonitoringRepository
import com.scarguard.app.ui.capture.CameraCaptureView
import com.scarguard.app.ui.capture.RequiresCameraPermission
import com.scarguard.app.ui.components.LocalPhotoThumbnail
import com.scarguard.app.ui.components.RiskBadge
import com.scarguard.app.ui.components.SectionCard
import com.scarguard.app.ui.scarGuardViewModel
import com.scarguard.app.util.formatDelta
import java.io.File

private sealed interface MonitorStep {
    data object Capturing : MonitorStep
    data object Analyzing : MonitorStep
    data class Result(val outcome: MonitoringRepository.PhotoReadingOutcome) : MonitorStep
    data class Error(val message: String) : MonitorStep
}

@Composable
fun MonitorScreen(onDone: () -> Unit, onCancel: () -> Unit) {
    val viewModel = scarGuardViewModel { MonitorViewModel(it) }
    val profile by viewModel.activeProfile.collectAsState()
    var step by remember { mutableStateOf<MonitorStep>(MonitorStep.Capturing) }

    if (profile == null) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text("Set a baseline first", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("You need a baseline photo before you can take a follow-up comparison.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onCancel) { Text("Back") }
        }
        return
    }

    when (val current = step) {
        MonitorStep.Capturing -> {
            RequiresCameraPermission {
                CameraCaptureView(
                    instructions = "Line the wound up the same way as your baseline photo, then capture.",
                    photoFilePrefix = "followup",
                    onClose = onCancel,
                    onCaptured = { file, bitmap ->
                        step = MonitorStep.Analyzing
                        viewModel.analyzeAndSave(
                            file = file,
                            bitmap = bitmap,
                            onResult = { outcome -> step = MonitorStep.Result(outcome) },
                            onError = { t -> step = MonitorStep.Error(t.message ?: "Something went wrong") },
                        )
                    },
                )
            }
        }
        MonitorStep.Analyzing -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Comparing against your baseline…")
                }
            }
        }
        is MonitorStep.Error -> {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text("Couldn't complete the check", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(current.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { step = MonitorStep.Capturing }) { Text("Try again") }
            }
        }
        is MonitorStep.Result -> {
            val outcome = current.outcome
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text("Check complete", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))

                SectionCard {
                    RiskBadge(outcome.reading.riskLevel)
                    Spacer(Modifier.height(8.dp))
                    Text(RiskClassifier.adviceFor(outcome.reading.riskLevel), style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        BaselineThumbnail(path = profile?.baselinePhotoPath)
                        Spacer(Modifier.height(4.dp))
                        Text("Baseline", style = MaterialTheme.typography.labelMedium)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        HeatmapThumbnail(bitmap = outcome.heatmap)
                        Spacer(Modifier.height(4.dp))
                        Text("Redness heatmap", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile(
                        modifier = Modifier.weight(1f),
                        label = "Redness change",
                        value = "${outcome.reading.rednessDelta?.formatDelta() ?: "--"}%",
                    )
                    StatTile(
                        modifier = Modifier.weight(1f),
                        label = "Temperature change",
                        value = outcome.reading.temperatureDeltaC?.let { "${it.formatDelta()}°C" } ?: "no sensor",
                    )
                }

                Spacer(Modifier.height(24.dp))
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}

@Composable
private fun BaselineThumbnail(path: String?) {
    LocalPhotoThumbnail(path = path, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
}

@Composable
private fun HeatmapThumbnail(bitmap: Bitmap) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp)),
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}
