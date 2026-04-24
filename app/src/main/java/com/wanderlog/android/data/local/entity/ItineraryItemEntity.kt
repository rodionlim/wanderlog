package com.wanderlog.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.ItineraryItemType
import com.wanderlog.android.domain.model.Place

@Entity(
    tableName = "itinerary_items",
    foreignKeys = [ForeignKey(
        entity = TripDayEntity::class,
        parentColumns = ["id"],
        childColumns = ["trip_day_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("trip_day_id"), Index("trip_id")]
)
data class ItineraryItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "trip_day_id") val tripDayId: String,
    @ColumnInfo(name = "trip_id") val tripId: String,
    val title: String,
    @ColumnInfo(name = "item_type") val itemType: String,
    @ColumnInfo(name = "place_id") val placeId: String? = null,
    @ColumnInfo(name = "place_name") val placeName: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @ColumnInfo(name = "start_time") val startTime: String? = null,
    @ColumnInfo(name = "end_time") val endTime: String? = null,
    val notes: String? = null,
    @ColumnInfo(name = "booking_ref") val bookingRef: String? = null,
    @ColumnInfo(name = "confirmation_url") val confirmationUrl: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ItineraryItem {
        val place = if (placeName != null) Place(placeId, placeName, address, latitude, longitude) else null
        return ItineraryItem(
            id = id,
            tripDayId = tripDayId,
            tripId = tripId,
            title = title,
            itemType = ItineraryItemType.valueOf(itemType),
            place = place,
            startTime = startTime,
            endTime = endTime,
            notes = notes,
            bookingRef = bookingRef,
            confirmationUrl = confirmationUrl,
            sortOrder = sortOrder
        )
    }

    companion object {
        fun fromDomain(item: ItineraryItem) = ItineraryItemEntity(
            id = item.id,
            tripDayId = item.tripDayId,
            tripId = item.tripId,
            title = item.title,
            itemType = item.itemType.name,
            placeId = item.place?.placeId,
            placeName = item.place?.name,
            address = item.place?.address,
            latitude = item.place?.latitude,
            longitude = item.place?.longitude,
            startTime = item.startTime,
            endTime = item.endTime,
            notes = item.notes,
            bookingRef = item.bookingRef,
            confirmationUrl = item.confirmationUrl,
            sortOrder = item.sortOrder
        )
    }
}
