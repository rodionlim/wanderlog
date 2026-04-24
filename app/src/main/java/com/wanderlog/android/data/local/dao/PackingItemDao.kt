package com.wanderlog.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wanderlog.android.data.local.entity.PackingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PackingItemDao {

    @Query("SELECT * FROM packing_items WHERE trip_id = :tripId ORDER BY sort_order ASC")
    fun getItemsForTrip(tripId: String): Flow<List<PackingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PackingItemEntity)

    @Update
    suspend fun updateItem(item: PackingItemEntity)

    @Delete
    suspend fun deleteItem(item: PackingItemEntity)
}
