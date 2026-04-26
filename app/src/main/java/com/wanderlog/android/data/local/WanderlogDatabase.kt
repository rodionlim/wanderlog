package com.wanderlog.android.data.local

import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 5,
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

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE itinerary_items ADD COLUMN linked_expense_id TEXT"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE trips ADD COLUMN traveller_names TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE packing_items ADD COLUMN traveller_name TEXT"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE packing_items ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1"
                )
            }
        }
    }
}
