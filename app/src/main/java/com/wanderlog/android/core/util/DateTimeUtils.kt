package com.wanderlog.android.core.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE
private val DISPLAY_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val SHORT_DATE = DateTimeFormatter.ofPattern("MMM d")

fun LocalDate.toIso(): String = ISO_DATE.format(this)

fun String.toLocalDate(): LocalDate = LocalDate.parse(this, ISO_DATE)

fun LocalDate.toDisplayString(): String = DISPLAY_DATE.format(this)

fun LocalDate.toShortDisplay(): String = SHORT_DATE.format(this)

fun LocalDate.toDayOfWeekDisplay(): String =
    dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

fun generateDateRange(start: LocalDate, end: LocalDate): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var current = start
    while (!current.isAfter(end)) {
        dates.add(current)
        current = current.plusDays(1)
    }
    return dates
}
