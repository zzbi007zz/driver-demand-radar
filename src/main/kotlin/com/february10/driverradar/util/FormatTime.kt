package com.february10.driverradar.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Shared date/time formatters for the UI. UTC instant is parsed/produced so
 * the strings are stable across devices (device-local is what users expect).
 */
val dateFormat: SimpleDateFormat =
    SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH)
        .apply { timeZone = TimeZone.getDefault() }

val timeFormat: SimpleDateFormat =
    SimpleDateFormat("h:mm a", Locale.ENGLISH)
        .apply { timeZone = TimeZone.getDefault() }

/** "Oct 27, 11:05 PM" — the standard history-row label. */
fun formatWhen(epochMillis: Long): String = "${dateFormat.format(epochMillis)}, ${timeFormat.format(epochMillis)}"
