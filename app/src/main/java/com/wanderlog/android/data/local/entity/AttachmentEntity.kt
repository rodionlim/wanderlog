package com.wanderlog.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wanderlog.android.domain.model.Attachment

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
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = Attachment(
        id = id,
        tripId = tripId,
        displayName = displayName,
        mimeType = mimeType,
        localPath = localPath,
        label = label,
        sizeBytes = sizeBytes,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(attachment: Attachment) = AttachmentEntity(
            id = attachment.id,
            tripId = attachment.tripId,
            displayName = attachment.displayName,
            mimeType = attachment.mimeType,
            localPath = attachment.localPath,
            label = attachment.label,
            sizeBytes = attachment.sizeBytes,
            createdAt = attachment.createdAt
        )
    }
}
