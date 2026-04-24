package com.wanderlog.android.domain.usecase.packing

import com.wanderlog.android.domain.model.PackingItem
import com.wanderlog.android.domain.repository.PackingRepository
import javax.inject.Inject

class TogglePackingItemUseCase @Inject constructor(private val repo: PackingRepository) {
    suspend operator fun invoke(item: PackingItem) = repo.updateItem(item.copy(isChecked = !item.isChecked))
}
