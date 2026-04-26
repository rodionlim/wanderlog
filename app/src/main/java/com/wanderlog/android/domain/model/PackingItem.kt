package com.wanderlog.android.domain.model

data class PackingItem(
    val id: String,
    val tripId: String,
    val title: String,
    val quantity: Int = 1,
    val isChecked: Boolean = false,
    val travellerName: String? = null,
    val category: String? = null,
    val sortOrder: Int = 0
)
