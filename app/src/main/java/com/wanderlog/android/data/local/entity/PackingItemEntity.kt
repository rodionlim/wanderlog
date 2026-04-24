package com.wanderlog.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wanderlog.android.domain.model.PackingItem

@Entity(
    tableName = "packing_items",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["trip_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("trip_id")]
)
data class PackingItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "trip_id") val tripId: String,
    val title: String,
    @ColumnInfo(name = "is_checked") val isChecked: Boolean = false,
    val category: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0
) {
    fun toDomain() = PackingItem(
        id = id,
        tripId = tripId,
        title = title,
        isChecked = isChecked,
        category = category,
        sortOrder = sortOrder
    )

    companion object {
        fun fromDomain(item: PackingItem) = PackingItemEntity(
            id = item.id,
            tripId = item.tripId,
            title = item.title,
            isChecked = item.isChecked,
            category = item.category,
            sortOrder = item.sortOrder
        )
    }
}
