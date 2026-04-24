package com.wanderlog.android.domain.repository

import com.wanderlog.android.data.remote.openai.dto.ContentPartDto
import com.wanderlog.android.domain.model.DocumentHint
import com.wanderlog.android.domain.model.ParsedBooking
import com.wanderlog.android.domain.model.TripDay

interface AiRepository {
    suspend fun generateItinerary(
        destination: String,
        startDate: String,
        endDate: String,
        preferences: String,
        travellers: Int
    ): List<TripDay>

    suspend fun parseFile(contentParts: List<ContentPartDto>, hint: DocumentHint? = null): ParsedBooking
}
