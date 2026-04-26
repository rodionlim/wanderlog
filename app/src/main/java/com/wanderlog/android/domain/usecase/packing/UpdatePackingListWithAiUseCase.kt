package com.wanderlog.android.domain.usecase.packing

import com.wanderlog.android.domain.model.PackingItem
import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.repository.AiRepository
import com.wanderlog.android.domain.repository.PackingRepository
import javax.inject.Inject

class UpdatePackingListWithAiUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val packingRepository: PackingRepository
) {
    suspend operator fun invoke(
        trip: Trip,
        existingItems: List<PackingItem>,
        userPrompt: String
    ): List<PackingItem> {
        val updatedItems = aiRepository.updatePackingList(
            trip = trip,
            existingItems = existingItems,
            userPrompt = userPrompt
        )
        packingRepository.replaceItems(trip.id, updatedItems)
        return updatedItems
    }
}
