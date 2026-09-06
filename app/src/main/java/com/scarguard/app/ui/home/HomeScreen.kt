package com.scarguard.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.scarguard.app.ble.ConnectionState
import com.scarguard.app.data.Reading
import com.scarguard.app.data.ReadingSource
import com.scarguard.app.data.RiskLevel
import com.scarguard.app.data.RiskClassifier
import com.scarguard.app.data.ScarProfile
import com.scarguard.app.settings.AlertThresholds
import com.scarguard.app.ui.components.ConnectionPill
import com.scarguard.app.ui.components.LocalPhotoThumbnail
import com.scarguard.app.ui.components.RiskBadge
import com.scarguard.app.ui.components.SectionCard
import com.scarguard.app.ui.components.TemperatureChart
import com.scarguard.app.ui.components.style
import com.scarguard.app.ui.scarGuardViewModel
import com.scarguard.app.ui.theme.ScarGuardTheme
import com.scarguard.app.util.formatDelta
import com.scarguard.app.util.formatTemp
import com.scarguard.app.util.toReadableDateTime

@Composable
fun HomeScreen(
    onGoToConnect: () -> Unit,
    onSetBaseline: () -> Unit,
    onTakeFollowUp: () -> Unit,
) {
    val viewModel = scarGuardViewModel { HomeViewModel(it) }
    val state by viewModel.uiState.collectAsState()

    HomeScreenContent(
        state = state,
        onGoToConnect = onGoToConnect,
        onSetBaseline = onSetBaseline,
        onTakeFollowUp = onTakeFollowUp,
    )
}

/**
 * The actual UI, split out from [HomeScreen] so it can be rendered in Android Studio's
 * @Preview pane with fake data -- no device, emulator, sensor, or ViewModel required.
 */
@Composable
private fun HomeScreenContent(
    state: HomeUiState,
    onGoToConnect: () -> Unit,
    onSetBaseline: () -> Unit,
    onTakeFollowUp: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("ScarGuard", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        state.profile?.label ?: "No baseline set yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ConnectionPill(state = state.connectionState, modifier = Modifier.clickableGoTo(onGoToConnect))
            }
        }

        if (state.profile == null) {
            item {
                SectionCard {
                    Text("Set a baseline photo", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Before anything else, take one reference photo of the area (ideally right before or right after the operation). Every future check compares against this.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onSetBaseline, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Capture baseline photo")
                    }
                }
            }
        } else {
            item { RiskBanner(level = state.liveRisk) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Temperature",
                        value = state.liveTemperature?.formatTemp() ?: "--",
                        subtitle = state.liveTempDelta?.let { "${it.formatDelta()}°C vs baseline" }
                            ?: "Connect sensor",
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Redness",
                        value = state.latestReading?.rednessDelta?.let { "${it.formatDelta()}%" } ?: "--",
                        subtitle = state.latestReading?.let { it.timestamp.toReadableDateTime() }
                            ?: "No follow-up photo yet",
                    )
                }
            }

            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Temperature trend", style = MaterialTheme.typography.titleMedium)
                        Icon(Icons.Filled.Thermostat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(12.dp))
                    TemperatureChart(
                        points = state.chartPoints,
                        baselineTemp = state.profile?.baselineTemperatureC,
                        alertTemp = state.profile?.baselineTemperatureC?.plus(state.thresholds.alertTempDeltaC),
                    )
                }
            }

            item {
                SectionCard {
                    Text("Baseline vs latest", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            LocalPhotoThumbnail(
                                path = state.profile?.baselinePhotoPath,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("Baseline", style = MaterialTheme.typography.labelMedium)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            LocalPhotoThumbnail(
                                path = state.latestReading?.photoPath,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("Latest", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onTakeFollowUp, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Take follow-up photo")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onSetBaseline, modifier = Modifier.fillMaxWidth()) {
                        Text("Replace baseline photo")
                    }
                }
            }
        }
    }
}

@Composable
private fun RiskBanner(level: RiskLevel) {
    val style = level.style()
    SectionCard(
        modifier = Modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                RiskBadge(level)
                Spacer(Modifier.height(8.dp))
                Text(
                    RiskClassifier.adviceFor(level),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        )
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Modifier.clickableGoTo(action: () -> Unit): Modifier =
    this.clickable(onClick = action)

// ---- Previews: render with Android Studio's "Split" or "Design" view, no device needed ----

private val previewProfile = ScarProfile(
    id = 1,
    label = "Left knee incision",
    createdAt = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000,
    baselinePhotoPath = "",
    baselineRednessScore = 12f,
    baselineAvgRed = 180f,
    baselineAvgGreen = 140f,
    baselineAvgBlue = 130f,
    baselineTemperatureC = 33.5f,
)

private val previewReading = Reading(
    id = 1,
    profileId = 1,
    timestamp = System.currentTimeMillis(),
    source = ReadingSource.BOTH,
    temperatureC = 34.6f,
    temperatureDeltaC = 1.1f,
    photoPath = null,
    rednessScore = 19f,
    rednessDelta = 7f,
    colorShiftScore = 10f,
    riskLevel = RiskLevel.WATCH,
)

@Preview(showBackground = true, name = "Home - monitoring")
@Composable
private fun HomeScreenMonitoringPreview() {
    ScarGuardTheme {
        HomeScreenContent(
            state = HomeUiState(
                connectionState = ConnectionState.Connected("ScarGuard Sensor", "AA:BB:CC:DD:EE:FF"),
                liveTemperature = 34.6f,
                profile = previewProfile,
                latestReading = previewReading,
                chartPoints = listOf(
                    System.currentTimeMillis() - 4 * 3_600_000L to 33.4f,
                    System.currentTimeMillis() - 3 * 3_600_000L to 33.6f,
                    System.currentTimeMillis() - 2 * 3_600_000L to 34.1f,
                    System.currentTimeMillis() - 1 * 3_600_000L to 34.6f,
                ),
                liveTempDelta = 1.1f,
                liveRisk = RiskLevel.WATCH,
                thresholds = AlertThresholds(),
            ),
            onGoToConnect = {},
            onSetBaseline = {},
            onTakeFollowUp = {},
        )
    }
}

@Preview(showBackground = true, name = "Home - no baseline yet")
@Composable
private fun HomeScreenEmptyPreview() {
    ScarGuardTheme {
        HomeScreenContent(
            state = HomeUiState(),
            onGoToConnect = {},
            onSetBaseline = {},
            onTakeFollowUp = {},
        )
    }
}
