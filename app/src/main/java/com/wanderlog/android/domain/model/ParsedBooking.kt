package com.wanderlog.android.domain.model

data class ParsedBooking(
    val flights: List<ParsedFlight> = emptyList(),
    val hotels: List<ParsedHotel> = emptyList(),
    val activities: List<ParsedActivity> = emptyList()
)

data class ParsedFlight(
    val flightNumber: String? = null,
    val origin: String,
    val destination: String,
    val departureDateTime: String? = null,
    val arrivalDateTime: String? = null,
    val bookingRef: String? = null
)

data class ParsedHotel(
    val name: String,
    val address: String? = null,
    val checkIn: String? = null,
    val checkOut: String? = null,
    val bookingRef: String? = null
)

data class ParsedActivity(
    val title: String,
    val location: String? = null,
    val dateTime: String? = null,
    val notes: String? = null
)
