package com.wanderlog.android.domain.model

import java.time.LocalDate

data class Trip(
    val id: String,
    val name: String,
    val destination: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val coverImageUri: String? = null,
    val budgetAmount: Double? = null,
    val currencyCode: String = "USD",
    val days: List<TripDay> = emptyList()
) {
    val durationDays: Int get() = (endDate.toEpochDay() - startDate.toEpochDay() + 1).toInt()
}
