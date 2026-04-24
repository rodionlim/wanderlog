package com.wanderlog.android.domain.model

data class PackingItem(
    val id: String,
    val tripId: String,
    val title: String,
    val isChecked: Boolean = false,
    val category: String? = null,
    val sortOrder: Int = 0
)
