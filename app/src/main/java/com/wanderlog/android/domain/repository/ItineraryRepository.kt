package com.wanderlog.android.domain.repository

import com.wanderlog.android.domain.model.ItineraryItem
import kotlinx.coroutines.flow.Flow

interface ItineraryRepository {
    fun getItemsForDay(dayId: String): Flow<List<ItineraryItem>>
    fun getItemsForTrip(tripId: String): Flow<List<ItineraryItem>>
    suspend fun insertItem(item: ItineraryItem)
    suspend fun insertItems(items: List<ItineraryItem>)
    suspend fun updateItem(item: ItineraryItem)
    suspend fun deleteItem(item: ItineraryItem)
    suspend fun updateSortOrder(itemId: String, order: Int)
}
