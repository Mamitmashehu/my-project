package com.scarguard.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.scarguard.app.MainActivity
import com.scarguard.app.R
import com.scarguard.app.data.RiskLevel

object AlertNotifier {
    const val MONITORING_CHANNEL_ID = "monitoring_channel"
    const val ALERT_CHANNEL_ID = "alert_channel"
    const val MONITORING_NOTIFICATION_ID = 1001
    private const val ALERT_NOTIFICATION_ID = 2001

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                MONITORING_CHANNEL_ID,
                context.getString(R.string.monitoring_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = context.getString(R.string.monitoring_channel_desc) }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                ALERT_CHANNEL_ID,
                context.getString(R.string.alert_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.alert_channel_desc) }
        )
    }

    fun buildMonitoringNotification(
        context: Context,
        connected: Boolean,
        latestTemperatureC: Float?,
    ): android.app.Notification {
        val contentText = when {
            !connected -> "Not connected to sensor"
            latestTemperatureC != null -> "Live · ${"%.1f".format(latestTemperatureC)}°C"
            else -> "Connected · waiting for a reading"
        }
        val openIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, MONITORING_CHANNEL_ID)
            .setContentTitle("ScarGuard monitoring")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun notifyRisk(context: Context, level: RiskLevel, message: String) {
        if (level == RiskLevel.NORMAL) return
        val openIntent = PendingIntent.getActivity(
            context, 1, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val title = if (level == RiskLevel.ALERT) "Possible infection warning sign" else "Worth keeping an eye on"
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent)
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(ALERT_NOTIFICATION_ID, notification)
        }
    }
}
