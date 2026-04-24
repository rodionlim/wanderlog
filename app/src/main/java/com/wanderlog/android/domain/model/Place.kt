package com.wanderlog.android.domain.model

data class Place(
    val placeId: String? = null,
    val name: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
