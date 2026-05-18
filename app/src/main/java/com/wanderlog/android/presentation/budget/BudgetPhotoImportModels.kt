package com.wanderlog.android.presentation.budget

import com.wanderlog.android.domain.model.ParsedBudgetExpense

sealed class BudgetPhotoImportStep {
    data object Idle : BudgetPhotoImportStep()
    data object Parsing : BudgetPhotoImportStep()
    data class Review(val items: List<ParsedBudgetExpense>) : BudgetPhotoImportStep()
    data class Error(val message: String) : BudgetPhotoImportStep()
}
