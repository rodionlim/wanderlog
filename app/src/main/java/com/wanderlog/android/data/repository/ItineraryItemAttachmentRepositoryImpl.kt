package com.wanderlog.android.data.repository

import android.net.Uri
import com.wanderlog.android.data.local.dao.ItineraryItemAttachmentLinkDao
import com.wanderlog.android.data.local.dao.ItemAttachmentCountRow
import com.wanderlog.android.data.local.dao.LinkedAttachmentRow
import com.wanderlog.android.data.local.entity.ItineraryItemAttachmentLinkEntity
import com.wanderlog.android.data.sync.SyncMetadataStamp
import com.wanderlog.android.domain.model.Attachment
import com.wanderlog.android.domain.model.ItemAttachmentLinkType
import com.wanderlog.android.domain.model.ItineraryItemAttachment
import com.wanderlog.android.domain.repository.AttachmentRepository
import com.wanderlog.android.domain.repository.ItineraryItemAttachmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ItineraryItemAttachmentRepositoryImpl @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    private val linkDao: ItineraryItemAttachmentLinkDao,
    private val syncMetadataStamp: SyncMetadataStamp
) : ItineraryItemAttachmentRepository {

    override fun getAttachmentsForItem(itemId: String): Flow<List<ItineraryItemAttachment>> =
        linkDao.getAttachmentsForItem(itemId).map { rows -> rows.map(LinkedAttachmentRow::toDomain) }

    override fun getAttachmentCountsForTrip(
        tripId: String,
        linkType: ItemAttachmentLinkType?
    ): Flow<Map<String, Int>> =
        linkDao.getAttachmentCountsForTrip(tripId, linkType?.name).map { rows -> rows.toCountMap() }

    override suspend fun addAttachmentToItem(
        tripId: String,
        itemId: String,
        attachmentId: String,
        linkType: ItemAttachmentLinkType
    ) {
        val now = syncMetadataStamp.now()
        val deviceId = syncMetadataStamp.currentDeviceId()
        val existing = linkDao.getByItemAndAttachmentIncludingDeleted(itemId, attachmentId)
        linkDao.insert(
            ItineraryItemAttachmentLinkEntity.create(
                tripId = tripId,
                itemId = itemId,
                attachmentId = attachmentId,
                linkType = linkType,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                deletedAt = null,
                lastModifiedByDeviceId = deviceId
            )
        )
    }

    override suspend fun addAttachmentToItems(
        tripId: String,
        itemIds: List<String>,
        attachmentId: String,
        linkType: ItemAttachmentLinkType
    ) {
        if (itemIds.isEmpty()) return

        val now = syncMetadataStamp.now()
        val deviceId = syncMetadataStamp.currentDeviceId()
        val links = itemIds.map { itemId ->
            val existing = linkDao.getByItemAndAttachmentIncludingDeleted(itemId, attachmentId)
            ItineraryItemAttachmentLinkEntity.create(
                tripId = tripId,
                itemId = itemId,
                attachmentId = attachmentId,
                linkType = linkType,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                deletedAt = null,
                lastModifiedByDeviceId = deviceId
            )
        }
        linkDao.insertAll(links)
    }

    override suspend fun importAttachmentForItem(
        tripId: String,
        itemId: String,
        uri: Uri,
        label: String?,
        tags: List<String>,
        linkType: ItemAttachmentLinkType
    ): Attachment {
        val attachment = attachmentRepository.importFromUri(tripId, uri, label, tags)
        addAttachmentToItem(tripId, itemId, attachment.id, linkType)
        return attachment
    }

    override suspend fun getAttachmentIdsForItem(
        itemId: String,
        linkType: ItemAttachmentLinkType?
    ): List<String> = linkDao.getAttachmentIdsForItem(itemId, linkType?.name)

    override suspend fun getItemIdsForAttachment(
        attachmentId: String,
        linkType: ItemAttachmentLinkType?
    ): List<String> = linkDao.getItemIdsForAttachment(attachmentId, linkType?.name)

    override suspend fun removeAttachmentFromItem(itemId: String, attachmentId: String) {
        linkDao.markDeleted(
            linkId = ItineraryItemAttachmentLinkEntity.buildId(itemId, attachmentId),
            deletedAt = syncMetadataStamp.now(),
            lastModifiedByDeviceId = syncMetadataStamp.currentDeviceId()
        )
    }

    override suspend fun deleteLinksForItems(itemIds: List<String>) {
        if (itemIds.isEmpty()) return
        linkDao.markDeletedForItems(
            itemIds = itemIds,
            deletedAt = syncMetadataStamp.now(),
            lastModifiedByDeviceId = syncMetadataStamp.currentDeviceId()
        )
    }
}

private fun LinkedAttachmentRow.toDomain(): ItineraryItemAttachment = ItineraryItemAttachment(
    attachment = attachment.toDomain(),
    linkType = ItemAttachmentLinkType.valueOf(linkType)
)

private fun List<ItemAttachmentCountRow>.toCountMap(): Map<String, Int> =
    associate { row -> row.itemId to row.attachmentCount }
