package com.wanderlog.android.domain.model

data class ItineraryItemAttachment(
    val attachment: Attachment,
    val linkType: ItemAttachmentLinkType
)

enum class ItemAttachmentLinkType {
    IMPORT_SOURCE,
    MANUAL
}
