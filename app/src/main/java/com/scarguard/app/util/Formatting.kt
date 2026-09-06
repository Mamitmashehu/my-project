package com.scarguard.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toReadableDateTime(): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(this))

fun Long.toReadableDate(): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(this))

fun Float.formatTemp(): String = "%.1f°C".format(this)

fun Float.formatDelta(): String {
    val sign = if (this >= 0) "+" else ""
    return "$sign%.1f".format(this)
}
