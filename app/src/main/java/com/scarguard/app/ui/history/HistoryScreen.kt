package com.scarguard.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.scarguard.app.data.Reading
import com.scarguard.app.data.ReadingSource
import com.scarguard.app.ui.components.LocalPhotoThumbnail
import com.scarguard.app.ui.components.RiskBadge
import com.scarguard.app.ui.components.SectionCard
import com.scarguard.app.ui.components.TemperatureChart
import com.scarguard.app.ui.scarGuardViewModel
import com.scarguard.app.util.formatDelta
import com.scarguard.app.util.formatTemp
import com.scarguard.app.util.toReadableDateTime

@Composable
fun HistoryScreen() {
    val viewModel = scarGuardViewModel { HistoryViewModel(it) }
    val state by viewModel.uiState.collectAsState()

    if (state.profile == null) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text("No baseline set yet", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Once you set a baseline and start monitoring, your temperature and photo history will show up here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("History", style = MaterialTheme.typography.headlineMedium) }

        item {
            SectionCard {
                Text("Temperature trend", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                val chartPoints = state.readings
                    .filter { it.temperatureC != null }
                    .sortedBy { it.timestamp }
                    .map { it.timestamp to (it.temperatureC ?: 0f) }
                TemperatureChart(points = chartPoints, baselineTemp = state.profile?.baselineTemperatureC, alertTemp = null)
            }
        }

        if (state.readings.isEmpty()) {
            item {
                SectionCard {
                    Text(
                        "No readings yet. Connect the sensor and take a follow-up photo to start building your timeline.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(state.readings, key = { it.id }) { reading -> ReadingRow(reading) }
        }
    }
}

@Composable
private fun ReadingRow(reading: Reading) {
    SectionCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (reading.photoPath != null) {
                LocalPhotoThumbnail(
                    path = reading.photoPath,
                    modifier = Modifier.size(56.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(reading.timestamp.toReadableDateTime(), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(summaryLine(reading), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RiskBadge(reading.riskLevel)
        }
    }
}

private fun summaryLine(reading: Reading): String {
    val parts = mutableListOf<String>()
    reading.temperatureC?.let { parts.add(it.formatTemp()) }
    reading.rednessDelta?.let { parts.add("redness ${it.formatDelta()}%") }
    if (reading.source == ReadingSource.TEMPERATURE) parts.add("sensor only")
    return if (parts.isEmpty()) "No data" else parts.joinToString(" · ")
}
