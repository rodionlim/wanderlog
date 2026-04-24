package com.wanderlog.android.domain.model

import java.time.LocalDate

data class TripDay(
    val id: String,
    val tripId: String,
    val date: LocalDate,
    val dayNumber: Int,
    val notes: String? = null,
    val items: List<ItineraryItem> = emptyList()
)
