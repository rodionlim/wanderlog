package com.wanderlog.android.domain.model

data class TripAssistantMessage(
    val role: TripAssistantRole,
    val text: String
)

enum class TripAssistantRole {
    USER,
    ASSISTANT
}
