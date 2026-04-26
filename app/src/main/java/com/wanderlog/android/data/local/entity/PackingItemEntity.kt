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
    val quantity: Int = 1,
    @ColumnInfo(name = "is_checked") val isChecked: Boolean = false,
    @ColumnInfo(name = "traveller_name") val travellerName: String? = null,
    val category: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0
) {
    fun toDomain() = PackingItem(
        id = id,
        tripId = tripId,
        title = title,
        quantity = quantity,
        isChecked = isChecked,
        travellerName = travellerName,
        category = category,
        sortOrder = sortOrder
    )

    companion object {
        fun fromDomain(item: PackingItem) = PackingItemEntity(
            id = item.id,
            tripId = item.tripId,
            title = item.title,
            quantity = item.quantity,
            isChecked = item.isChecked,
            travellerName = item.travellerName,
            category = item.category,
            sortOrder = item.sortOrder
        )
    }
}
