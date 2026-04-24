package com.wanderlog.android.presentation.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.Expense
import com.wanderlog.android.domain.model.ExpenseCategory
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.domain.usecase.expense.AddExpenseUseCase
import com.wanderlog.android.domain.usecase.expense.DeleteExpenseUseCase
import com.wanderlog.android.domain.usecase.expense.GetExpensesUseCase
import com.wanderlog.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class BudgetUiState(
    val tripName: String = "",
    val budget: Double? = null,
    val currencyCode: String = "USD",
    val expenses: List<Expense> = emptyList(),
    val totalSpent: Double = 0.0,
    val filterCategory: ExpenseCategory? = null,
    val addTitle: String = "",
    val addAmount: String = "",
    val addCategory: ExpenseCategory = ExpenseCategory.OTHER,
    val showAddForm: Boolean = false
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    getExpenses: GetExpensesUseCase,
    private val addExpense: AddExpenseUseCase,
    private val deleteExpense: DeleteExpenseUseCase
) : ViewModel() {

    private val tripId = savedStateHandle.get<String>(Screen.Budget.ARG_TRIP_ID)!!

    private val _state = MutableStateFlow(BudgetUiState())
    val state: StateFlow<BudgetUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val trip = tripRepository.getTripById(tripId)
            _state.update { it.copy(tripName = trip?.name ?: "", budget = trip?.budgetAmount, currencyCode = trip?.currencyCode ?: "USD") }
        }
        viewModelScope.launch {
            getExpenses(tripId).collect { expenses ->
                val total = expenses.sumOf { it.amount }
                _state.update { it.copy(expenses = expenses, totalSpent = total) }
            }
        }
    }

    fun toggleAddForm() = _state.update { it.copy(showAddForm = !it.showAddForm) }
    fun onTitleChange(v: String) = _state.update { it.copy(addTitle = v) }
    fun onAmountChange(v: String) = _state.update { it.copy(addAmount = v) }
    fun onCategoryChange(v: ExpenseCategory) = _state.update { it.copy(addCategory = v) }
    fun filterByCategory(cat: ExpenseCategory?) = _state.update { it.copy(filterCategory = cat) }

    fun addExpense() {
        val s = _state.value
        val amount = s.addAmount.toDoubleOrNull() ?: return
        viewModelScope.launch {
            addExpense(Expense(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                title = s.addTitle.trim(),
                amount = amount,
                currencyCode = s.currencyCode,
                category = s.addCategory
            ))
            _state.update { it.copy(addTitle = "", addAmount = "", showAddForm = false) }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { deleteExpense.invoke(expense) }
    }
}
