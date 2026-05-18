package com.wanderlog.android.domain.repository

import com.wanderlog.android.domain.model.PackingItem
import kotlinx.coroutines.flow.Flow

interface PackingRepository {
    fun getItemsForTrip(tripId: String): Flow<List<PackingItem>>
    suspend fun insertItem(item: PackingItem)
    suspend fun copyItemsFromTrip(sourceTripId: String, targetTripId: String, travellerNameMap: Map<String, String?> = emptyMap())
    suspend fun replaceItems(tripId: String, items: List<PackingItem>)
    suspend fun updateItem(item: PackingItem)
    suspend fun deleteItem(item: PackingItem)
}
