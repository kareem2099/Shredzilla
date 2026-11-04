package com.FreeRave.shredzilla.utils

import java.util.Locale

private const val DEFAULT_REST_SECONDS = 60

/**
 * The rest preference is stored in Firestore as text: "2min", "1min 30s" or "45s".
 * Anything unreadable falls back to one minute.
 */
fun parseRestPreference(preference: String): Int {
    val minutes = preference.substringBefore("min", missingDelimiterValue = "").trim().toIntOrNull()
    val secondsPart = if (minutes != null) preference.substringAfter("min") else preference
    val seconds = secondsPart.substringBefore("s", missingDelimiterValue = "").trim().toIntOrNull()
    if (minutes == null && seconds == null) return DEFAULT_REST_SECONDS
    return ((minutes ?: 0) * 60 + (seconds ?: 0)).takeIf { it > 0 } ?: DEFAULT_REST_SECONDS
}

/** Inverse of [parseRestPreference]: 120 → "2min", 90 → "1min 30s". */
fun restPreferenceOf(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (seconds == 0) "${minutes}min" else "${minutes}min ${seconds}s"
}

/** Clock-style label, e.g. 90 → "1:30". */
fun formatRestDuration(totalSeconds: Int): String =
    String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
