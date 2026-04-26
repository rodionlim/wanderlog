package com.wanderlog.android.domain.model

data class TravellerProfile(
    val name: String,
    val age: Int? = null
) {
    val displayName: String
        get() = age?.let { "$name ($it)" } ?: name
}
