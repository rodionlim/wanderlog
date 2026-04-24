package com.wanderlog.android.domain.usecase.expense

import com.wanderlog.android.domain.model.Expense
import com.wanderlog.android.domain.repository.ExpenseRepository
import javax.inject.Inject

class DeleteExpenseUseCase @Inject constructor(private val repo: ExpenseRepository) {
    suspend operator fun invoke(expense: Expense) = repo.deleteExpense(expense)
}
