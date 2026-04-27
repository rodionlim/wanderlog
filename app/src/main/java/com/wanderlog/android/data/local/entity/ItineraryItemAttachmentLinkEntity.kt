package com.wanderlog.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wanderlog.android.domain.model.ItemAttachmentLinkType

@Entity(
    tableName = "item_attachment_links",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["trip_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ItineraryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itinerary_item_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AttachmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["attachment_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("trip_id"),
        Index("itinerary_item_id"),
        Index("attachment_id"),
        Index(value = ["itinerary_item_id", "attachment_id"])
    ]
)
data class ItineraryItemAttachmentLinkEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "trip_id") val tripId: String,
    @ColumnInfo(name = "itinerary_item_id") val itineraryItemId: String,
    @ColumnInfo(name = "attachment_id") val attachmentId: String,
    @ColumnInfo(name = "link_type") val linkType: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = createdAt,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "last_modified_by_device_id") val lastModifiedByDeviceId: String = ""
) {
    companion object {
        fun buildId(itemId: String, attachmentId: String): String = "${itemId}__${attachmentId}"

        fun create(
            tripId: String,
            itemId: String,
            attachmentId: String,
            linkType: ItemAttachmentLinkType,
            createdAt: Long,
            updatedAt: Long = createdAt,
            deletedAt: Long? = null,
            lastModifiedByDeviceId: String = ""
        ) = ItineraryItemAttachmentLinkEntity(
            id = buildId(itemId, attachmentId),
            tripId = tripId,
            itineraryItemId = itemId,
            attachmentId = attachmentId,
            linkType = linkType.name,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            lastModifiedByDeviceId = lastModifiedByDeviceId
        )
    }
}
