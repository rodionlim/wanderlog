package com.wanderlog.android.domain.repository

import android.net.Uri
import com.wanderlog.android.domain.model.Attachment
import com.wanderlog.android.domain.model.ItemAttachmentLinkType
import com.wanderlog.android.domain.model.ItineraryItemAttachment
import kotlinx.coroutines.flow.Flow

interface ItineraryItemAttachmentRepository {
    fun getAttachmentsForItem(itemId: String): Flow<List<ItineraryItemAttachment>>
    fun getAttachmentCountsForTrip(
        tripId: String,
        linkType: ItemAttachmentLinkType? = null
    ): Flow<Map<String, Int>>
    suspend fun addAttachmentToItem(
        tripId: String,
        itemId: String,
        attachmentId: String,
        linkType: ItemAttachmentLinkType = ItemAttachmentLinkType.MANUAL
    )

    suspend fun addAttachmentToItems(
        tripId: String,
        itemIds: List<String>,
        attachmentId: String,
        linkType: ItemAttachmentLinkType
    )

    suspend fun importAttachmentForItem(
        tripId: String,
        itemId: String,
        uri: Uri,
        label: String? = null,
        tags: List<String> = emptyList(),
        linkType: ItemAttachmentLinkType = ItemAttachmentLinkType.MANUAL
    ): Attachment

    suspend fun getAttachmentIdsForItem(
        itemId: String,
        linkType: ItemAttachmentLinkType? = null
    ): List<String>

    suspend fun getItemIdsForAttachment(
        attachmentId: String,
        linkType: ItemAttachmentLinkType? = null
    ): List<String>

    suspend fun removeAttachmentFromItem(itemId: String, attachmentId: String)
    suspend fun deleteLinksForItems(itemIds: List<String>)
}
