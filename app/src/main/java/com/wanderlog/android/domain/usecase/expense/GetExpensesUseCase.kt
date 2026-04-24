package com.wanderlog.android.domain.usecase.expense

import com.wanderlog.android.domain.model.Expense
import com.wanderlog.android.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExpensesUseCase @Inject constructor(private val repo: ExpenseRepository) {
    operator fun invoke(tripId: String): Flow<List<Expense>> = repo.getExpensesForTrip(tripId)
}
