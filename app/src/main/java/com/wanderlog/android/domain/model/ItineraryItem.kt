package com.wanderlog.android.domain.model

data class ItineraryItem(
    val id: String,
    val tripDayId: String,
    val tripId: String,
    val title: String,
    val itemType: ItineraryItemType,
    val place: Place? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val notes: String? = null,
    val bookingRef: String? = null,
    val confirmationUrl: String? = null,
    val sortOrder: Int = 0
)

enum class ItineraryItemType {
    PLACE, FLIGHT, HOTEL, ACTIVITY, NOTE, TRANSPORT
}
