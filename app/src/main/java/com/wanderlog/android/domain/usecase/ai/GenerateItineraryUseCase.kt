package com.wanderlog.android.domain.usecase.ai

import com.wanderlog.android.domain.model.TravellerProfile
import com.wanderlog.android.domain.model.TripDay
import com.wanderlog.android.domain.repository.AiRepository
import javax.inject.Inject

class GenerateItineraryUseCase @Inject constructor(private val repo: AiRepository) {

    suspend operator fun invoke(
        destination: String,
        startDate: String,
        endDate: String,
        preferences: String = "balanced mix of culture and food",
        travellerProfiles: List<TravellerProfile> = emptyList(),
        updatePrompt: String? = null,
        existingDays: List<TripDay> = emptyList()
    ): List<TripDay> = repo.generateItinerary(
        destination = destination,
        startDate = startDate,
        endDate = endDate,
        preferences = preferences,
        travellerProfiles = travellerProfiles,
        updatePrompt = updatePrompt,
        existingDays = existingDays
    )
}
