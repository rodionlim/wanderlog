package com.wanderlog.android.domain.usecase.itinerary

import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.repository.ItineraryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetItineraryForDayUseCase @Inject constructor(private val repo: ItineraryRepository) {
    operator fun invoke(dayId: String): Flow<List<ItineraryItem>> = repo.getItemsForDay(dayId)
}
