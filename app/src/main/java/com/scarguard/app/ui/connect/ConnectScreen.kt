package com.scarguard.app.ui.connect

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scarguard.app.ble.ConnectionState
import com.scarguard.app.ble.ScannedDevice
import com.scarguard.app.ui.components.SectionCard
import com.scarguard.app.ui.scarGuardViewModel
import com.scarguard.app.util.Permissions

@Composable
fun ConnectScreen() {
    val viewModel = scarGuardViewModel { ConnectViewModel(it) }
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasPermissions by remember {
        mutableStateOf(Permissions.BLUETOOTH.all {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> hasPermissions = results.values.all { it } }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Connect your sensor", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Pair the ESP32 that carries the temperature sensor near the incision.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        if (!hasPermissions) {
            SectionCard {
                Text("Bluetooth permission needed", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "ScarGuard needs Bluetooth permission to find and connect to the sensor.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { permissionLauncher.launch(Permissions.BLUETOOTH) }) {
                    Text("Grant permission")
                }
            }
            return@Column
        }

        if (!state.bluetoothSupported) {
            SectionCard { Text("This device doesn't support Bluetooth Low Energy.") }
            return@Column
        }

        if (!state.bluetoothEnabled) {
            SectionCard { Text("Turn on Bluetooth to continue.") }
            return@Column
        }

        (state.connectionState as? ConnectionState.Failed)?.let { failed ->
            SectionCard(contentPadding = PaddingValues(16.dp)) {
                Text(failed.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))
        }

        when (val connectionState = state.connectionState) {
            is ConnectionState.Connected -> {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(Color(0xFF2E9E5B))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(connectionState.name, style = MaterialTheme.typography.titleMedium)
                            Text("Connected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = { viewModel.disconnect() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Disconnect")
                    }
                }
            }
            is ConnectionState.Connecting -> {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Connecting…")
                    }
                }
            }
            else -> {
                Button(
                    onClick = { viewModel.startScan() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = connectionState != ConnectionState.Scanning,
                ) {
                    Icon(Icons.Filled.BluetoothSearching, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (connectionState == ConnectionState.Scanning) "Scanning…" else "Scan for devices")
                }
                Spacer(Modifier.height(16.dp))

                if (state.scanResults.isEmpty() && connectionState == ConnectionState.Scanning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Looking for nearby devices…", style = MaterialTheme.typography.bodySmall)
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.scanResults, key = { it.address }) { device ->
                        DeviceRow(device = device, onClick = { viewModel.connect(device.address) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: ScannedDevice, onClick: () -> Unit) {
    SectionCard(
        modifier = Modifier.clickable(onClick = onClick),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleSmall)
                Text(device.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${device.rssi} dBm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        Modifier
            .size(10.dp)
            .background(color, CircleShape)
    )
}
