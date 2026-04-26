package com.wanderlog.android.data.repository

import com.wanderlog.android.data.local.dao.ExpenseDao
import com.wanderlog.android.data.local.entity.ExpenseEntity
import com.wanderlog.android.domain.model.Expense
import com.wanderlog.android.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val dao: ExpenseDao
) : ExpenseRepository {

    override fun getExpensesForTrip(tripId: String): Flow<List<Expense>> =
        dao.getExpensesForTrip(tripId).map { it.map(ExpenseEntity::toDomain) }

    override fun getTotalSpent(tripId: String): Flow<Double?> =
        dao.getTotalSpent(tripId)

    override suspend fun insertExpense(expense: Expense) =
        dao.insertExpense(ExpenseEntity.fromDomain(expense))

    override suspend fun updateExpense(expense: Expense) =
        dao.updateExpense(
            id = expense.id,
            title = expense.title,
            amount = expense.amount,
            currencyCode = expense.currencyCode,
            category = expense.category.name,
            date = expense.date,
            notes = expense.notes
        )

    override suspend fun deleteExpense(expense: Expense) =
        dao.deleteExpense(ExpenseEntity.fromDomain(expense))
}
