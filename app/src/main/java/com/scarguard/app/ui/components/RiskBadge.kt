package com.scarguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.scarguard.app.data.RiskLevel
import com.scarguard.app.ui.theme.RiskAlert
import com.scarguard.app.ui.theme.RiskAlertContainer
import com.scarguard.app.ui.theme.RiskNormal
import com.scarguard.app.ui.theme.RiskNormalContainer
import com.scarguard.app.ui.theme.RiskWatch
import com.scarguard.app.ui.theme.RiskWatchContainer
import com.scarguard.app.ui.theme.ScarGuardTheme

data class RiskStyle(val label: String, val dot: Color, val container: Color, val onContainer: Color)

fun RiskLevel.style(): RiskStyle = when (this) {
    RiskLevel.NORMAL -> RiskStyle("Normal", RiskNormal, RiskNormalContainer, RiskNormal)
    RiskLevel.WATCH -> RiskStyle("Watch", RiskWatch, RiskWatchContainer, RiskWatch)
    RiskLevel.ALERT -> RiskStyle("Alert", RiskAlert, RiskAlertContainer, RiskAlert)
}

@Composable
fun RiskBadge(level: RiskLevel, modifier: Modifier = Modifier) {
    val style = level.style()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(style.container, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier
                .size(8.dp)
                .background(style.dot, CircleShape)
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
        Text(style.label, color = style.onContainer, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview(showBackground = true, name = "Risk badges")
@Composable
private fun RiskBadgePreview() {
    ScarGuardTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RiskBadge(RiskLevel.NORMAL)
            RiskBadge(RiskLevel.WATCH)
            RiskBadge(RiskLevel.ALERT)
        }
    }
}
