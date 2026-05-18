package com.wanderlog.android.data.repository

import com.wanderlog.android.data.local.dao.PackingItemDao
import com.wanderlog.android.data.local.entity.PackingItemEntity
import com.wanderlog.android.data.sync.SyncMetadataStamp
import com.wanderlog.android.domain.model.PackingItem
import com.wanderlog.android.domain.repository.PackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class PackingRepositoryImpl @Inject constructor(
    private val dao: PackingItemDao,
    private val syncMetadataStamp: SyncMetadataStamp
) : PackingRepository {

    override fun getItemsForTrip(tripId: String): Flow<List<PackingItem>> =
        dao.getItemsForTrip(tripId).map { it.map(PackingItemEntity::toDomain) }

    override suspend fun insertItem(item: PackingItem) {
        val now = syncMetadataStamp.now()
        val deviceId = syncMetadataStamp.currentDeviceId()
        dao.insertItem(
            PackingItemEntity.fromDomain(
                item = item,
                createdAt = now,
                updatedAt = now,
                lastModifiedByDeviceId = deviceId
            )
        )
    }

    override suspend fun copyItemsFromTrip(
        sourceTripId: String,
        targetTripId: String,
        travellerNameMap: Map<String, String?>
    ) {
        val sourceItems = dao.getItemsForTripOnce(sourceTripId)
        if (sourceItems.isEmpty()) return

        val now = syncMetadataStamp.now()
        val deviceId = syncMetadataStamp.currentDeviceId()
        dao.insertItems(
            sourceItems.map { item ->
                PackingItemEntity.fromDomain(
                    item = item.toDomain().copy(
                        id = UUID.randomUUID().toString(),
                        tripId = targetTripId,
                        isChecked = false,
                        travellerName = item.travellerName?.let { travellerNameMap[it] ?: it }
                    ),
                    createdAt = now,
                    updatedAt = now,
                    lastModifiedByDeviceId = deviceId
                )
            }
        )
    }

    override suspend fun replaceItems(tripId: String, items: List<PackingItem>) {
        val now = syncMetadataStamp.now()
        val deviceId = syncMetadataStamp.currentDeviceId()
        val existingItems = dao.getItemsForTripOnce(tripId)
        val existingById = existingItems.associateBy(PackingItemEntity::id)

        existingItems.forEach { existingItem ->
            dao.markDeleted(
                itemId = existingItem.id,
                deletedAt = now,
                lastModifiedByDeviceId = deviceId
            )
        }

        if (items.isNotEmpty()) {
            dao.insertItems(
                items.map {
                    PackingItemEntity.fromDomain(
                        item = it,
                        createdAt = existingById[it.id]?.createdAt ?: now,
                        updatedAt = now,
                        lastModifiedByDeviceId = deviceId
                    )
                }
            )
        }
    }

    override suspend fun updateItem(item: PackingItem) {
        val existing = dao.getByIdIncludingDeleted(item.id)
        val now = syncMetadataStamp.now()
        val deviceId = syncMetadataStamp.currentDeviceId()
        dao.updateItem(
            PackingItemEntity.fromDomain(
                item = item,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                deletedAt = existing?.deletedAt,
                lastModifiedByDeviceId = deviceId
            )
        )
    }

    override suspend fun deleteItem(item: PackingItem) {
        dao.markDeleted(
            itemId = item.id,
            deletedAt = syncMetadataStamp.now(),
            lastModifiedByDeviceId = syncMetadataStamp.currentDeviceId()
        )
    }
}
