package com.wanderlog.android.data.repository

import com.wanderlog.android.data.local.dao.ItineraryItemDao
import com.wanderlog.android.data.local.entity.ItineraryItemEntity
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.repository.ItineraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ItineraryRepositoryImpl @Inject constructor(
    private val dao: ItineraryItemDao
) : ItineraryRepository {

    override fun getItemsForDay(dayId: String): Flow<List<ItineraryItem>> =
        dao.getItemsForDay(dayId).map { it.map(ItineraryItemEntity::toDomain) }

    override fun getItemsForTrip(tripId: String): Flow<List<ItineraryItem>> =
        dao.getItemsForTrip(tripId).map { it.map(ItineraryItemEntity::toDomain) }

    override suspend fun insertItem(item: ItineraryItem) =
        dao.insertItem(ItineraryItemEntity.fromDomain(item))

    override suspend fun insertItems(items: List<ItineraryItem>) =
        dao.insertItems(items.map { ItineraryItemEntity.fromDomain(it) })

    override suspend fun updateItem(item: ItineraryItem) =
        dao.updateItem(ItineraryItemEntity.fromDomain(item))

    override suspend fun deleteItem(item: ItineraryItem) =
        dao.deleteItem(ItineraryItemEntity.fromDomain(item))

    override suspend fun updateSortOrder(itemId: String, order: Int) =
        dao.updateSortOrder(itemId, order)
}
