package com.scarguard.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scarguard.app.ui.components.SectionCard
import com.scarguard.app.ui.scarGuardViewModel

@Composable
fun SettingsScreen() {
    val viewModel = scarGuardViewModel { SettingsViewModel(it) }
    val thresholds by viewModel.thresholds.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showForgetConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium) }

        item {
            SectionCard {
                Text("Alert sensitivity", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "These control when a check is flagged Watch or Alert. Lower values are more cautious.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))
                ThresholdRow(
                    label = "Temperature - Watch above baseline",
                    value = thresholds.watchTempDeltaC,
                    valueLabel = "+%.1f°C".format(thresholds.watchTempDeltaC),
                    range = 0.2f..3f,
                    onChange = { viewModel.setTempThresholds(it, maxOf(it + 0.2f, thresholds.alertTempDeltaC)) },
                )
                ThresholdRow(
                    label = "Temperature - Alert above baseline",
                    value = thresholds.alertTempDeltaC,
                    valueLabel = "+%.1f°C".format(thresholds.alertTempDeltaC),
                    range = 0.5f..4f,
                    onChange = { viewModel.setTempThresholds(minOf(thresholds.watchTempDeltaC, it - 0.2f), it) },
                )
                ThresholdRow(
                    label = "Redness - Watch change",
                    value = thresholds.watchRednessDelta,
                    valueLabel = "+%.0f%%".format(thresholds.watchRednessDelta),
                    range = 2f..40f,
                    onChange = { viewModel.setRednessThresholds(it, maxOf(it + 5f, thresholds.alertRednessDelta)) },
                )
                ThresholdRow(
                    label = "Redness - Alert change",
                    value = thresholds.alertRednessDelta,
                    valueLabel = "+%.0f%%".format(thresholds.alertRednessDelta),
                    range = 5f..60f,
                    onChange = { viewModel.setRednessThresholds(minOf(thresholds.watchRednessDelta, it - 5f), it) },
                )
            }
        }

        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Risk notifications", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Get notified when a check moves to Watch or Alert.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = thresholds.notificationsEnabled, onCheckedChange = { viewModel.setNotificationsEnabled(it) })
                }
            }
        }

        item {
            SectionCard {
                Text("Sensor", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    thresholds.lastConnectedDeviceName?.let { "Last connected: $it" } ?: "No device connected yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { showForgetConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Forget paired device")
                }
            }
        }

        item {
            SectionCard {
                Text("Data", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Deletes your baseline photo, history, and readings from this device. This can't be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete all data")
                }
            }
        }

        item {
            Text(
                "ScarGuard tracks visible skin-tone change and temperature trend as a home aid only. It is not a diagnostic device and does not replace medical advice -- if you're worried about infection, contact your care provider.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete all data?") },
            text = { Text("This removes your baseline photo and every recorded reading. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteAllData {}
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }

    if (showForgetConfirm) {
        AlertDialog(
            onDismissRequest = { showForgetConfirm = false },
            title = { Text("Forget this device?") },
            text = { Text("You'll need to scan and reconnect to keep getting temperature readings.") },
            confirmButton = {
                TextButton(onClick = {
                    showForgetConfirm = false
                    viewModel.forgetDevice()
                }) { Text("Forget") }
            },
            dismissButton = { TextButton(onClick = { showForgetConfirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ThresholdRow(
    label: String,
    value: Float,
    valueLabel: String,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
