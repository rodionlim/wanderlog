package com.wanderlog.android.domain.usecase.packing

import com.wanderlog.android.domain.model.PackingItem
import com.wanderlog.android.domain.repository.PackingRepository
import javax.inject.Inject

class DeletePackingItemUseCase @Inject constructor(private val repo: PackingRepository) {
    suspend operator fun invoke(item: PackingItem) = repo.deleteItem(item)
}
