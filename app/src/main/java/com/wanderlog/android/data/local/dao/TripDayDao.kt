package com.wanderlog.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wanderlog.android.data.local.entity.TripDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDayDao {

    @Query("SELECT * FROM trip_days WHERE trip_id = :tripId ORDER BY day_number ASC")
    fun getDaysForTrip(tripId: String): Flow<List<TripDayEntity>>

    @Query("SELECT * FROM trip_days WHERE trip_id = :tripId ORDER BY day_number ASC")
    suspend fun getDaysForTripOnce(tripId: String): List<TripDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(days: List<TripDayEntity>)

    @Query("DELETE FROM trip_days WHERE trip_id = :tripId")
    suspend fun deleteAllForTrip(tripId: String)
}
