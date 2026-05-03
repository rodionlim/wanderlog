package com.wanderlog.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wanderlog.android.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Query(
        """
        SELECT * FROM attachments
        WHERE trip_id = :tripId
          AND deleted_at IS NULL
          AND EXISTS (
              SELECT 1 FROM trips
              WHERE trips.id = attachments.trip_id
                AND trips.deleted_at IS NULL
          )
        ORDER BY created_at DESC
        """
    )
    fun getAttachmentsForTrip(tripId: String): Flow<List<AttachmentEntity>>

    @Query(
        """
        SELECT * FROM attachments
        WHERE trip_id = :tripId
          AND deleted_at IS NULL
          AND EXISTS (
              SELECT 1 FROM trips
              WHERE trips.id = attachments.trip_id
                AND trips.deleted_at IS NULL
          )
        ORDER BY created_at DESC
        """
    )
    suspend fun getAttachmentsForTripOnce(tripId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE trip_id = :tripId ORDER BY created_at DESC")
    suspend fun getAttachmentsForTripForSync(tripId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getByIdIncludingDeleted(id: String): AttachmentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(attachment: AttachmentEntity)

    @Update
    suspend fun update(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE deleted_at IS NOT NULL")
    suspend fun purgeDeletedAttachments()

    @Query(
        """
        UPDATE attachments
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt,
            last_modified_by_device_id = :lastModifiedByDeviceId
        WHERE id = :attachmentId AND deleted_at IS NULL
        """
    )
    suspend fun markDeleted(
        attachmentId: String,
        deletedAt: Long,
        lastModifiedByDeviceId: String
    )
}
