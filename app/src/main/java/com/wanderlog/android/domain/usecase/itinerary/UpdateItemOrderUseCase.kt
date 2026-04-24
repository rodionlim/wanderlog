package com.wanderlog.android.domain.usecase.itinerary

import com.wanderlog.android.domain.repository.ItineraryRepository
import javax.inject.Inject

class UpdateItemOrderUseCase @Inject constructor(private val repo: ItineraryRepository) {
    suspend operator fun invoke(itemId: String, order: Int) = repo.updateSortOrder(itemId, order)
}
