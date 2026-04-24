package com.wanderlog.android.domain.usecase.ai

import com.wanderlog.android.domain.model.TripDay
import com.wanderlog.android.domain.repository.AiRepository
import javax.inject.Inject

class GenerateItineraryUseCase @Inject constructor(private val repo: AiRepository) {

    suspend operator fun invoke(
        destination: String,
        startDate: String,
        endDate: String,
        preferences: String = "balanced mix of culture and food",
        travellers: Int = 1
    ): List<TripDay> = repo.generateItinerary(destination, startDate, endDate, preferences, travellers)
}
