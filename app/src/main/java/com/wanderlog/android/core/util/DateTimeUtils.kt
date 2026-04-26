package com.wanderlog.android.core.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE
private val DISPLAY_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val SHORT_DATE = DateTimeFormatter.ofPattern("MMM d")
private val COMPACT_SLASH_DATE = DateTimeFormatter.ofPattern("d/M")
private val DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a")
private val FRIENDLY_DAY_DATE = DateTimeFormatter.ofPattern("EEE, MMM d")
private val FRIENDLY_TIME = DateTimeFormatter.ofPattern("h:mm a")
private val PARTIAL_DATE_TIME_REGEX = Regex("""(\d{4}-\d{2}-\d{2})[T\s](\d{2}:\d{2})""")

data class FriendlyDateTimeParts(
    val date: String,
    val time: String? = null
)

fun LocalDate.toIso(): String = ISO_DATE.format(this)

fun String.toLocalDate(): LocalDate = LocalDate.parse(this, ISO_DATE)

fun LocalDate.toDisplayString(): String = DISPLAY_DATE.format(this)

fun LocalDate.toShortDisplay(): String = SHORT_DATE.format(this)

fun LocalDate.toCompactSlashDisplay(): String = COMPACT_SLASH_DATE.format(this)

fun String.toDisplayDateTimeOrSelf(): String {
    val candidate = trim()
    if (candidate.isBlank()) return this

    return runCatching { OffsetDateTime.parse(candidate).format(DISPLAY_DATE_TIME) }.getOrNull()
        ?: runCatching { ZonedDateTime.parse(candidate).format(DISPLAY_DATE_TIME) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(candidate).format(DISPLAY_DATE_TIME) }.getOrNull()
        ?: runCatching { LocalDate.parse(candidate).toDisplayString() }.getOrNull()
        ?: this
}

fun String.toFriendlyDateTimeOrSelf(): String {
    val parts = toFriendlyDateTimePartsOrNull()
    return when {
        parts == null -> toDisplayDateTimeOrSelf().replace('T', ' ')
        parts.time == null -> parts.date
        else -> "${parts.date} • ${parts.time}"
    }
}

fun String.toFriendlyDateTimePartsOrNull(): FriendlyDateTimeParts? {
    val candidate = trim()
    if (candidate.isBlank()) return null

    parseFriendlyDateTime(candidate)?.let { return it }
    sanitizeOffset(candidate)?.let { sanitized ->
        parseFriendlyDateTime(sanitized)?.let { return it }
    }

    PARTIAL_DATE_TIME_REGEX.find(candidate)?.destructured?.let { (datePart, timePart) ->
        val date = runCatching {
            LocalDate.parse(datePart).format(FRIENDLY_DAY_DATE)
        }.getOrNull() ?: datePart
        return FriendlyDateTimeParts(date = date, time = timePart)
    }

    return runCatching {
        FriendlyDateTimeParts(date = LocalDate.parse(candidate).format(FRIENDLY_DAY_DATE))
    }.getOrNull()
}

private fun parseFriendlyDateTime(candidate: String): FriendlyDateTimeParts? {
    return runCatching {
        OffsetDateTime.parse(candidate).let {
            FriendlyDateTimeParts(it.format(FRIENDLY_DAY_DATE), it.format(FRIENDLY_TIME))
        }
    }.getOrNull()
        ?: runCatching {
            ZonedDateTime.parse(candidate).let {
                FriendlyDateTimeParts(it.format(FRIENDLY_DAY_DATE), it.format(FRIENDLY_TIME))
            }
        }.getOrNull()
        ?: runCatching {
            LocalDateTime.parse(candidate).let {
                FriendlyDateTimeParts(it.format(FRIENDLY_DAY_DATE), it.format(FRIENDLY_TIME))
            }
        }.getOrNull()
}

private fun sanitizeOffset(candidate: String): String? {
    return when {
        candidate.matches(Regex(""".*[+-]\d{2}:\d$""")) -> candidate + "0"
        candidate.matches(Regex(""".*[+-]\d{4}$""")) -> {
            candidate.replace(Regex("""([+-]\d{2})(\d{2})$"""), "$1:$2")
        }
        else -> null
    }
}

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
