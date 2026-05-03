package com.wanderlog.android.data.sync

import android.content.Context
import androidx.room.withTransaction
import com.wanderlog.android.core.util.TravellerProfilesCodec
import com.wanderlog.android.data.local.WanderlogDatabase
import com.wanderlog.android.data.local.dao.AttachmentDao
import com.wanderlog.android.data.local.dao.ExpenseDao
import com.wanderlog.android.data.local.dao.ItineraryItemAttachmentLinkDao
import com.wanderlog.android.data.local.dao.ItineraryItemDao
import com.wanderlog.android.data.local.dao.PackingItemDao
import com.wanderlog.android.data.local.dao.TripDao
import com.wanderlog.android.data.local.dao.TripDayDao
import com.wanderlog.android.data.local.entity.AttachmentEntity
import com.wanderlog.android.data.local.entity.ExpenseEntity
import com.wanderlog.android.data.local.entity.ItineraryItemAttachmentLinkEntity
import com.wanderlog.android.data.local.entity.ItineraryItemEntity
import com.wanderlog.android.data.local.entity.PackingItemEntity
import com.wanderlog.android.data.local.entity.TripDayEntity
import com.wanderlog.android.data.local.entity.TripEntity
import com.wanderlog.android.domain.model.toStoredAttachmentTags
import com.wanderlog.android.domain.model.sync.SyncAttachmentPayload
import com.wanderlog.android.domain.model.sync.SyncExpensePayload
import com.wanderlog.android.domain.model.sync.SyncItemAttachmentLinkPayload
import com.wanderlog.android.domain.model.sync.SyncItineraryItemPayload
import com.wanderlog.android.domain.model.sync.SyncPackingItemPayload
import com.wanderlog.android.domain.model.sync.SyncTripDayPayload
import com.wanderlog.android.domain.model.sync.SyncTripPayload
import com.wanderlog.android.domain.model.sync.TripSyncBundle
import com.wanderlog.android.domain.model.sync.TripSyncManifest
import com.wanderlog.android.domain.model.sync.toSyncRecord
import com.wanderlog.android.domain.usecase.sync.SyncMergePolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

data class TripSyncApplyResult(
    val appliedRecords: Int,
    val skippedRecords: Int
)

@Singleton
class TripSyncBundleApplier @Inject constructor(
    private val database: WanderlogDatabase?,
    private val tripDao: TripDao,
    private val tripDayDao: TripDayDao,
    private val itineraryItemDao: ItineraryItemDao,
    private val itineraryItemAttachmentLinkDao: ItineraryItemAttachmentLinkDao,
    private val expenseDao: ExpenseDao,
    private val packingItemDao: PackingItemDao,
    private val attachmentDao: AttachmentDao,
    @ApplicationContext private val context: Context
) {

    suspend fun applyBundle(bundle: TripSyncBundle): TripSyncApplyResult =
        requireNotNull(database).withTransaction { applyBundleInternal(bundle) }

    internal suspend fun applyBundleInternal(bundle: TripSyncBundle): TripSyncApplyResult {
        require(bundle.protocolVersion == TripSyncManifest.CURRENT_PROTOCOL_VERSION) {
            "Unsupported sync protocol version ${bundle.protocolVersion}"
        }

        var applied = 0
        var skipped = 0

        if (applyTrip(bundle.trip)) applied++ else skipped++

        for (tripDay in bundle.tripDays) {
            if (applyTripDay(tripDay)) applied++ else skipped++
        }
        for (expense in bundle.expenses) {
            if (applyExpense(expense)) applied++ else skipped++
        }
        for (item in bundle.itineraryItems) {
            if (applyItineraryItem(item)) applied++ else skipped++
        }
        for (attachment in bundle.attachments) {
            if (applyAttachment(attachment)) applied++ else skipped++
        }
        for (itemAttachmentLink in bundle.itemAttachmentLinks) {
            if (applyItemAttachmentLink(itemAttachmentLink)) applied++ else skipped++
        }
        for (packingItem in bundle.packingItems) {
            if (applyPackingItem(packingItem)) applied++ else skipped++
        }

        return TripSyncApplyResult(appliedRecords = applied, skippedRecords = skipped)
    }

    private suspend fun applyTrip(payload: SyncTripPayload): Boolean {
        val local = tripDao.getTripByIdIncludingDeleted(payload.id)?.toSyncRecord()
        if (!SyncMergePolicy.shouldApplyIncoming(local, payload.toSyncRecord())) return false

        val existing = tripDao.getTripByIdIncludingDeleted(payload.id)
        val resolvedCoverImageUri = materializeTripCover(context, payload)
        tripDao.insertTrip(
            payload.toEntity(
                createdAt = existing?.createdAt ?: payload.metadata.updatedAt,
                resolvedCoverImageUri = resolvedCoverImageUri
            )
        )
        return true
    }

    private suspend fun applyTripDay(payload: SyncTripDayPayload): Boolean {
        val local = tripDayDao.getByIdIncludingDeleted(payload.id)?.toSyncRecord()
        if (!SyncMergePolicy.shouldApplyIncoming(local, payload.toSyncRecord())) return false

        val existing = tripDayDao.getByIdIncludingDeleted(payload.id)
        tripDayDao.insertAll(listOf(payload.toEntity(existing?.createdAt ?: payload.metadata.updatedAt)))
        return true
    }

    private suspend fun applyExpense(payload: SyncExpensePayload): Boolean {
        val local = expenseDao.getByIdIncludingDeleted(payload.id)?.toSyncRecord()
        if (!SyncMergePolicy.shouldApplyIncoming(local, payload.toSyncRecord())) return false

        expenseDao.insertExpense(payload.toEntity())
        return true
    }

    private suspend fun applyItineraryItem(payload: SyncItineraryItemPayload): Boolean {
        val local = itineraryItemDao.getByIdIncludingDeleted(payload.id)?.toSyncRecord()
        if (!SyncMergePolicy.shouldApplyIncoming(local, payload.toSyncRecord())) return false

        val existing = itineraryItemDao.getByIdIncludingDeleted(payload.id)
        itineraryItemDao.insertItem(payload.toEntity(existing?.createdAt ?: payload.metadata.updatedAt))
        return true
    }

    private suspend fun applyPackingItem(payload: SyncPackingItemPayload): Boolean {
        val local = packingItemDao.getByIdIncludingDeleted(payload.id)?.toSyncRecord()
        if (!SyncMergePolicy.shouldApplyIncoming(local, payload.toSyncRecord())) return false

        val existing = packingItemDao.getByIdIncludingDeleted(payload.id)
        packingItemDao.insertItem(payload.toEntity(existing?.createdAt ?: payload.metadata.updatedAt))
        return true
    }

    private suspend fun applyItemAttachmentLink(payload: SyncItemAttachmentLinkPayload): Boolean {
        val local = itineraryItemAttachmentLinkDao.getByIdIncludingDeleted(payload.id)?.toSyncPayload()?.toSyncRecord()
        if (!SyncMergePolicy.shouldApplyIncoming(local, payload.toSyncRecord())) return false

        val existing = itineraryItemAttachmentLinkDao.getByIdIncludingDeleted(payload.id)
        itineraryItemAttachmentLinkDao.insert(
            payload.toEntity(existing?.createdAt ?: payload.metadata.updatedAt)
        )
        return true
    }

    private suspend fun applyAttachment(payload: SyncAttachmentPayload): Boolean {
        val localEntity = attachmentDao.getByIdIncludingDeleted(payload.id)
        val local = localEntity?.toSyncRecord(contentHash = hashFileSha256(File(context.filesDir, localEntity.localPath)))
        if (!SyncMergePolicy.shouldApplyIncoming(local, payload.toSyncRecord())) return false

        val targetFile = File(context.filesDir, payload.localPath)
        if (payload.metadata.deletedAt != null) {
            if (targetFile.exists()) {
                targetFile.delete()
            }
        } else if (payload.contentBase64 != null) {
            targetFile.parentFile?.mkdirs()
            val decoded = Base64.getDecoder().decode(payload.contentBase64)
            val incomingHash = payload.contentHash
            if (incomingHash != null) {
                require(hashBytesSha256(decoded) == incomingHash) {
                    "Attachment content hash mismatch for ${payload.id}"
                }
            }
            targetFile.writeBytes(decoded)
        }

        val entity = payload.toEntity()
        if (localEntity == null) {
            attachmentDao.insert(entity)
        } else {
            attachmentDao.update(entity)
        }
        return true
    }
}

private fun materializeTripCover(context: Context, payload: SyncTripPayload): String? {
    val coverDir = File(context.filesDir, "trip-covers").apply { mkdirs() }
    val targetFile = File(coverDir, "${payload.id}.jpg")

    if (payload.coverImageContentBase64 != null) {
        val decoded = Base64.getDecoder().decode(payload.coverImageContentBase64)
        payload.coverImageContentHash?.let { expectedHash ->
            require(hashBytesSha256(decoded) == expectedHash) {
                "Trip cover content hash mismatch for ${payload.id}"
            }
        }
        targetFile.writeBytes(decoded)
        return targetFile.toURI().toString()
    }

    if (payload.coverImageUri.isNullOrBlank()) {
        if (targetFile.exists()) {
            targetFile.delete()
        }
        return null
    }

    return when {
        payload.coverImageUri.startsWith("http://") ||
            payload.coverImageUri.startsWith("https://") ||
            payload.coverImageUri.startsWith("content://") -> payload.coverImageUri
        targetFile.exists() -> targetFile.toURI().toString()
        else -> null
    }
}

private fun SyncTripPayload.toEntity(createdAt: Long, resolvedCoverImageUri: String?): TripEntity = TripEntity(
    id = id,
    name = name,
    destination = destination,
    startDate = LocalDate.parse(startDate),
    endDate = LocalDate.parse(endDate),
    coverImageUri = resolvedCoverImageUri,
    budgetAmount = budgetAmount,
    currencyCode = currencyCode,
    travellerProfilesRaw = TravellerProfilesCodec.encode(travellerProfiles),
    createdAt = createdAt,
    updatedAt = metadata.updatedAt,
    deletedAt = metadata.deletedAt,
    lastModifiedByDeviceId = metadata.lastModifiedByDeviceId
)

private fun SyncTripDayPayload.toEntity(createdAt: Long): TripDayEntity = TripDayEntity(
    id = id,
    tripId = tripId,
    date = LocalDate.parse(date),
    dayNumber = dayNumber,
    notes = notes,
    createdAt = createdAt,
    updatedAt = metadata.updatedAt,
    deletedAt = metadata.deletedAt,
    lastModifiedByDeviceId = metadata.lastModifiedByDeviceId
)

private fun SyncExpensePayload.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    tripId = tripId,
    title = title,
    amount = amount,
    currencyCode = currencyCode,
    category = category,
    date = date?.let(LocalDate::parse),
    notes = notes,
    createdAt = createdAt,
    updatedAt = metadata.updatedAt,
    deletedAt = metadata.deletedAt,
    lastModifiedByDeviceId = metadata.lastModifiedByDeviceId
)

private fun SyncItineraryItemPayload.toEntity(createdAt: Long): ItineraryItemEntity = ItineraryItemEntity(
    id = id,
    tripDayId = tripDayId,
    tripId = tripId,
    title = title,
    itemType = itemType,
    placeId = placeId,
    placeName = placeName,
    address = address,
    latitude = latitude,
    longitude = longitude,
    startTime = startTime,
    endTime = endTime,
    notes = notes,
    bookingRef = bookingRef,
    linkedExpenseId = linkedExpenseId,
    confirmationUrl = confirmationUrl,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = metadata.updatedAt,
    deletedAt = metadata.deletedAt,
    lastModifiedByDeviceId = metadata.lastModifiedByDeviceId
)

private fun SyncPackingItemPayload.toEntity(createdAt: Long): PackingItemEntity = PackingItemEntity(
    id = id,
    tripId = tripId,
    title = title,
    quantity = quantity,
    isChecked = isChecked,
    travellerName = travellerName,
    category = category,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = metadata.updatedAt,
    deletedAt = metadata.deletedAt,
    lastModifiedByDeviceId = metadata.lastModifiedByDeviceId
)

private fun SyncItemAttachmentLinkPayload.toEntity(createdAt: Long): ItineraryItemAttachmentLinkEntity =
    ItineraryItemAttachmentLinkEntity(
        id = id,
        tripId = tripId,
        itineraryItemId = itineraryItemId,
        attachmentId = attachmentId,
        linkType = linkType,
        createdAt = createdAt,
        updatedAt = metadata.updatedAt,
        deletedAt = metadata.deletedAt,
        lastModifiedByDeviceId = metadata.lastModifiedByDeviceId
    )

private fun ItineraryItemAttachmentLinkEntity.toSyncPayload(): SyncItemAttachmentLinkPayload = SyncItemAttachmentLinkPayload(
    id = id,
    tripId = tripId,
    itineraryItemId = itineraryItemId,
    attachmentId = attachmentId,
    linkType = linkType,
    metadata = com.wanderlog.android.domain.model.sync.SyncMetadata(
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        lastModifiedByDeviceId = lastModifiedByDeviceId
    )
)

private fun SyncAttachmentPayload.toEntity(): AttachmentEntity = AttachmentEntity(
    id = id,
    tripId = tripId,
    displayName = displayName,
    mimeType = mimeType,
    localPath = localPath,
    label = label,
    tags = tags.toStoredAttachmentTags(),
    sizeBytes = sizeBytes,
    createdAt = createdAt,
    updatedAt = metadata.updatedAt,
    deletedAt = metadata.deletedAt,
    lastModifiedByDeviceId = metadata.lastModifiedByDeviceId
)
