package com.wanderlog.android.domain.model

data class Attachment(
    val id: String,
    val tripId: String,
    val displayName: String,
    val mimeType: String,
    val localPath: String,
    val label: String? = null,
    val tags: List<String> = emptyList(),
    val sizeBytes: Long,
    val createdAt: Long
)

fun normalizeAttachmentTags(tags: Iterable<String>): List<String> =
    tags
        .flatMap { tag -> tag.split(',') }
        .map { tag -> tag.trim().removePrefix("#") }
        .filter { tag -> tag.isNotBlank() }
        .distinctBy { tag -> tag.lowercase() }

fun String.toAttachmentTags(): List<String> = normalizeAttachmentTags(split(','))

fun List<String>.toStoredAttachmentTags(): String = normalizeAttachmentTags(this).joinToString(",")

fun ItineraryItemType.defaultAttachmentTags(): List<String> = when (this) {
    ItineraryItemType.FLIGHT -> listOf("flight")
    ItineraryItemType.HOTEL -> listOf("hotel")
    ItineraryItemType.ACTIVITY -> listOf("activity")
    ItineraryItemType.TRANSPORT -> listOf("transport")
    ItineraryItemType.NOTE -> listOf("note")
    ItineraryItemType.PLACE -> listOf("place")
}
