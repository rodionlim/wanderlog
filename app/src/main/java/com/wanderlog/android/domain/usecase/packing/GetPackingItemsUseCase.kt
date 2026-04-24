package com.wanderlog.android.domain.usecase.packing

import com.wanderlog.android.domain.model.PackingItem
import com.wanderlog.android.domain.repository.PackingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPackingItemsUseCase @Inject constructor(private val repo: PackingRepository) {
    operator fun invoke(tripId: String): Flow<List<PackingItem>> = repo.getItemsForTrip(tripId)
}
