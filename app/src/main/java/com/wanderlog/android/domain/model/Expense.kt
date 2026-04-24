package com.wanderlog.android.domain.model

import java.time.LocalDate

data class Expense(
    val id: String,
    val tripId: String,
    val title: String,
    val amount: Double,
    val currencyCode: String,
    val category: ExpenseCategory,
    val date: LocalDate? = null,
    val notes: String? = null
)

enum class ExpenseCategory {
    TRANSPORT, ACCOMMODATION, FOOD, ACTIVITY, SHOPPING, OTHER
}
