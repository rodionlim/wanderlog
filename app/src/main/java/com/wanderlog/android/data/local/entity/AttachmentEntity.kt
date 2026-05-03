package com.wanderlog.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wanderlog.android.domain.model.Attachment
import com.wanderlog.android.domain.model.toAttachmentTags
import com.wanderlog.android.domain.model.toStoredAttachmentTags

@Entity(
    tableName = "attachments",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["trip_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("trip_id")]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "trip_id") val tripId: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "local_path") val localPath: String,
    val label: String? = null,
    val tags: String = "",
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = createdAt,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "last_modified_by_device_id") val lastModifiedByDeviceId: String = ""
) {
    fun toDomain() = Attachment(
        id = id,
        tripId = tripId,
        displayName = displayName,
        mimeType = mimeType,
        localPath = localPath,
        label = label,
        tags = tags.toAttachmentTags(),
        sizeBytes = sizeBytes,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(
            attachment: Attachment,
            updatedAt: Long = attachment.createdAt,
            deletedAt: Long? = null,
            lastModifiedByDeviceId: String = ""
        ) = AttachmentEntity(
            id = attachment.id,
            tripId = attachment.tripId,
            displayName = attachment.displayName,
            mimeType = attachment.mimeType,
            localPath = attachment.localPath,
            label = attachment.label,
            tags = attachment.tags.toStoredAttachmentTags(),
            sizeBytes = attachment.sizeBytes,
            createdAt = attachment.createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            lastModifiedByDeviceId = lastModifiedByDeviceId
        )
    }
}
