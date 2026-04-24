package com.wanderlog.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wanderlog.android.data.local.entity.ItineraryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItineraryItemDao {

    @Query("SELECT * FROM itinerary_items WHERE trip_day_id = :dayId ORDER BY sort_order ASC")
    fun getItemsForDay(dayId: String): Flow<List<ItineraryItemEntity>>

    @Query("SELECT * FROM itinerary_items WHERE trip_id = :tripId ORDER BY trip_day_id, sort_order ASC")
    fun getItemsForTrip(tripId: String): Flow<List<ItineraryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItineraryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItineraryItemEntity>)

    @Update
    suspend fun updateItem(item: ItineraryItemEntity)

    @Delete
    suspend fun deleteItem(item: ItineraryItemEntity)

    @Query("UPDATE itinerary_items SET sort_order = :order WHERE id = :itemId")
    suspend fun updateSortOrder(itemId: String, order: Int)
}
