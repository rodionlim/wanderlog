package com.wanderlog.android.data.repository

import com.wanderlog.android.data.local.dao.PackingItemDao
import com.wanderlog.android.data.local.entity.PackingItemEntity
import com.wanderlog.android.domain.model.PackingItem
import com.wanderlog.android.domain.repository.PackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PackingRepositoryImpl @Inject constructor(
    private val dao: PackingItemDao
) : PackingRepository {

    override fun getItemsForTrip(tripId: String): Flow<List<PackingItem>> =
        dao.getItemsForTrip(tripId).map { it.map(PackingItemEntity::toDomain) }

    override suspend fun insertItem(item: PackingItem) =
        dao.insertItem(PackingItemEntity.fromDomain(item))

    override suspend fun replaceItems(tripId: String, items: List<PackingItem>) =
        dao.replaceItemsForTrip(tripId, items.map(PackingItemEntity::fromDomain))

    override suspend fun updateItem(item: PackingItem) =
        dao.updateItem(PackingItemEntity.fromDomain(item))

    override suspend fun deleteItem(item: PackingItem) =
        dao.deleteItem(PackingItemEntity.fromDomain(item))
}
