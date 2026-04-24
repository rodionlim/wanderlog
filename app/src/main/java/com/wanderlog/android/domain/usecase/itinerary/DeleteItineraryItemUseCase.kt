package com.wanderlog.android.domain.usecase.itinerary

import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.repository.ItineraryRepository
import javax.inject.Inject

class DeleteItineraryItemUseCase @Inject constructor(private val repo: ItineraryRepository) {
    suspend operator fun invoke(item: ItineraryItem) = repo.deleteItem(item)
}
