package com.wanderlog.android.domain.model

import java.util.UUID

data class ParsedBudgetExpense(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amountText: String,
    val currencyCode: String,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val dateText: String? = null,
    val notes: String? = null
)

data class ParsedBudgetExpenseImport(
    val items: List<ParsedBudgetExpense> = emptyList()
)
