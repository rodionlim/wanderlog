package com.wanderlog.android.domain.model

data class Attachment(
    val id: String,
    val tripId: String,
    val displayName: String,
    val mimeType: String,
    val localPath: String,
    val label: String? = null,
    val sizeBytes: Long,
    val createdAt: Long
)
