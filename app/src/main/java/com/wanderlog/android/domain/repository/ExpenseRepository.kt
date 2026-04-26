package com.wanderlog.android.domain.repository

import com.wanderlog.android.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getExpensesForTrip(tripId: String): Flow<List<Expense>>
    fun getTotalSpent(tripId: String): Flow<Double?>
    suspend fun insertExpense(expense: Expense)
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expense: Expense)
}
