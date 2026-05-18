package com.wanderlog.android.domain.usecase.packing

import com.wanderlog.android.domain.repository.PackingRepository
import javax.inject.Inject

class CopyPackingItemsFromTripUseCase @Inject constructor(
    private val repo: PackingRepository
) {
    suspend operator fun invoke(
        sourceTripId: String,
        targetTripId: String,
        travellerNameMap: Map<String, String?> = emptyMap()
    ) = repo.copyItemsFromTrip(sourceTripId, targetTripId, travellerNameMap)
}
