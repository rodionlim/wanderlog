package com.wanderlog.android.domain.repository

import com.wanderlog.android.data.remote.openai.dto.ContentPartDto
import com.wanderlog.android.domain.model.DocumentHint
import com.wanderlog.android.domain.model.PackingItem
import com.wanderlog.android.domain.model.ParsedBooking
import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.model.TripDay

interface AiRepository {
    suspend fun generateItinerary(
        destination: String,
        startDate: String,
        endDate: String,
        preferences: String,
        travellers: Int,
        updatePrompt: String? = null,
        existingDays: List<TripDay> = emptyList()
    ): List<TripDay>

    suspend fun updatePackingList(
        trip: Trip,
        existingItems: List<PackingItem>,
        userPrompt: String
    ): List<PackingItem>

    suspend fun parseFile(contentParts: List<ContentPartDto>, hint: DocumentHint? = null): ParsedBooking
}
