package com.FreeRave.shredzilla.screens.today

import java.util.Date

data class DayItem(
    val dayOfMonth: String,
    val dayOfWeekShort: String, // e.g., "Mon"
    val fullDate: Date,
    var isSelected: Boolean = false,
    var hasActivity: Boolean = false
)
