package com.wanderlog.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wanderlog.android.data.local.converter.RoomConverters
import com.wanderlog.android.data.local.dao.AttachmentDao
import com.wanderlog.android.data.local.dao.ExpenseDao
import com.wanderlog.android.data.local.dao.ItineraryItemDao
import com.wanderlog.android.data.local.dao.PackingItemDao
import com.wanderlog.android.data.local.dao.TripDao
import com.wanderlog.android.data.local.dao.TripDayDao
import com.wanderlog.android.data.local.entity.AttachmentEntity
import com.wanderlog.android.data.local.entity.ExpenseEntity
import com.wanderlog.android.data.local.entity.ItineraryItemEntity
import com.wanderlog.android.data.local.entity.PackingItemEntity
import com.wanderlog.android.data.local.entity.TripDayEntity
import com.wanderlog.android.data.local.entity.TripEntity

@Database(
    entities = [
        TripEntity::class,
        TripDayEntity::class,
        ItineraryItemEntity::class,
        ExpenseEntity::class,
        PackingItemEntity::class,
        AttachmentEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class WanderlogDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun tripDayDao(): TripDayDao
    abstract fun itineraryItemDao(): ItineraryItemDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun packingItemDao(): PackingItemDao
    abstract fun attachmentDao(): AttachmentDao
}
