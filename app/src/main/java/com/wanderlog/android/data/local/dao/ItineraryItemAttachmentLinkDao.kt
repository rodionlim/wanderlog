package com.wanderlog.android.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wanderlog.android.data.local.entity.AttachmentEntity
import com.wanderlog.android.data.local.entity.ItineraryItemAttachmentLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItineraryItemAttachmentLinkDao {

    @Query(
        """
        SELECT attachments.*, item_attachment_links.link_type AS link_type
        FROM item_attachment_links
        INNER JOIN attachments ON attachments.id = item_attachment_links.attachment_id
        WHERE item_attachment_links.itinerary_item_id = :itemId
          AND item_attachment_links.deleted_at IS NULL
          AND attachments.deleted_at IS NULL
          AND EXISTS (
              SELECT 1 FROM itinerary_items
              WHERE itinerary_items.id = item_attachment_links.itinerary_item_id
                AND itinerary_items.deleted_at IS NULL
          )
        ORDER BY item_attachment_links.created_at DESC
        """
    )
    fun getAttachmentsForItem(itemId: String): Flow<List<LinkedAttachmentRow>>

    @Query(
        """
        SELECT item_attachment_links.itinerary_item_id AS item_id,
               COUNT(item_attachment_links.attachment_id) AS attachment_count
        FROM item_attachment_links
        INNER JOIN attachments ON attachments.id = item_attachment_links.attachment_id
        WHERE item_attachment_links.trip_id = :tripId
          AND item_attachment_links.deleted_at IS NULL
          AND attachments.deleted_at IS NULL
                    AND (:linkType IS NULL OR item_attachment_links.link_type = :linkType)
        GROUP BY item_attachment_links.itinerary_item_id
        """
    )
        fun getAttachmentCountsForTrip(tripId: String, linkType: String?): Flow<List<ItemAttachmentCountRow>>

    @Query(
        "SELECT * FROM item_attachment_links WHERE itinerary_item_id = :itemId AND attachment_id = :attachmentId"
    )
    suspend fun getByItemAndAttachmentIncludingDeleted(
        itemId: String,
        attachmentId: String
    ): ItineraryItemAttachmentLinkEntity?

    @Query("SELECT * FROM item_attachment_links WHERE id = :id")
    suspend fun getByIdIncludingDeleted(id: String): ItineraryItemAttachmentLinkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: ItineraryItemAttachmentLinkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(links: List<ItineraryItemAttachmentLinkEntity>)

    @Query(
        """
        SELECT attachment_id FROM item_attachment_links
        WHERE itinerary_item_id = :itemId
          AND deleted_at IS NULL
          AND (:linkType IS NULL OR link_type = :linkType)
        ORDER BY created_at DESC
        """
    )
    suspend fun getAttachmentIdsForItem(itemId: String, linkType: String?): List<String>

    @Query(
        """
        SELECT itinerary_item_id FROM item_attachment_links
        WHERE attachment_id = :attachmentId
          AND deleted_at IS NULL
          AND (:linkType IS NULL OR link_type = :linkType)
        ORDER BY created_at DESC
        """
    )
    suspend fun getItemIdsForAttachment(attachmentId: String, linkType: String?): List<String>

    @Query(
        "SELECT * FROM item_attachment_links WHERE trip_id = :tripId ORDER BY created_at DESC"
    )
    suspend fun getLinksForTripForSync(tripId: String): List<ItineraryItemAttachmentLinkEntity>

    @Query(
        """
        UPDATE item_attachment_links
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt,
            last_modified_by_device_id = :lastModifiedByDeviceId
        WHERE id = :linkId AND deleted_at IS NULL
        """
    )
    suspend fun markDeleted(
        linkId: String,
        deletedAt: Long,
        lastModifiedByDeviceId: String
    )

    @Query(
        """
        UPDATE item_attachment_links
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt,
            last_modified_by_device_id = :lastModifiedByDeviceId
        WHERE itinerary_item_id IN (:itemIds) AND deleted_at IS NULL
        """
    )
    suspend fun markDeletedForItems(
        itemIds: List<String>,
        deletedAt: Long,
        lastModifiedByDeviceId: String
    )

    @Query(
        """
        UPDATE item_attachment_links
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt,
            last_modified_by_device_id = :lastModifiedByDeviceId
        WHERE attachment_id = :attachmentId AND deleted_at IS NULL
        """
    )
    suspend fun markDeletedForAttachment(
        attachmentId: String,
        deletedAt: Long,
        lastModifiedByDeviceId: String
    )

    @Query("DELETE FROM item_attachment_links WHERE deleted_at IS NOT NULL")
    suspend fun purgeDeletedLinks()
}

data class LinkedAttachmentRow(
    @Embedded val attachment: AttachmentEntity,
    @ColumnInfo(name = "link_type") val linkType: String
)

data class ItemAttachmentCountRow(
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "attachment_count") val attachmentCount: Int
)
