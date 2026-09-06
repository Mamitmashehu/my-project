package com.scarguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.scarguard.app.ble.ConnectionState
import com.scarguard.app.ui.theme.RiskNormal
import com.scarguard.app.ui.theme.ScarGuardTheme
import com.scarguard.app.ui.theme.Slate

@Composable
fun ConnectionPill(state: ConnectionState, modifier: Modifier = Modifier) {
    val (label, color) = when (state) {
        is ConnectionState.Connected -> state.name to RiskNormal
        is ConnectionState.Connecting -> "Connecting…" to Slate
        ConnectionState.Scanning -> "Scanning…" to Slate
        ConnectionState.Disconnected, ConnectionState.Idle -> "Not connected" to Slate
        is ConnectionState.Failed -> "Connection issue" to MaterialTheme.colorScheme.error
    }
    val connected = state is ConnectionState.Connected

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = if (connected) Icons.Filled.Bluetooth else Icons.Filled.BluetoothDisabled,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.size(6.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview(showBackground = true, name = "Connection states")
@Composable
private fun ConnectionPillPreview() {
    ScarGuardTheme {
        Column {
            ConnectionPill(ConnectionState.Connected("ScarGuard Sensor", "AA:BB:CC:DD:EE:FF"))
            ConnectionPill(ConnectionState.Scanning)
            ConnectionPill(ConnectionState.Disconnected)
            ConnectionPill(ConnectionState.Failed("Could not connect"))
        }
    }
}
