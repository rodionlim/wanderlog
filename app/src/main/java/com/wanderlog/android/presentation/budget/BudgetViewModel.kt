package com.wanderlog.android.presentation.budget

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.core.util.ApproximateCurrencyConverter
import com.wanderlog.android.core.util.BudgetDisplayCurrencies
import com.wanderlog.android.domain.model.Expense
import com.wanderlog.android.domain.model.ExpenseCategory
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.domain.usecase.expense.AddExpenseUseCase
import com.wanderlog.android.domain.usecase.expense.DeleteExpenseUseCase
import com.wanderlog.android.domain.usecase.expense.GetExpensesUseCase
import com.wanderlog.android.domain.usecase.expense.UpdateExpenseUseCase
import com.wanderlog.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.wanderlog.android.presentation.settings.SettingsViewModel

data class BudgetUiState(
    val tripName: String = "",
    val budget: Double? = null,
    val tripCurrencyCode: String = "USD",
    val displayCurrencyCode: String = BudgetDisplayCurrencies.DEFAULT,
    val expenses: List<Expense> = emptyList(),
    val totalSpent: Double = 0.0,
    val convertedBudget: Double? = null,
    val usesApproximateConversion: Boolean = false,
    val filterCategory: ExpenseCategory? = null,
    val addTitle: String = "",
    val addAmount: String = "",
    val addCurrencyCode: String = "USD",
    val addCategory: ExpenseCategory = ExpenseCategory.OTHER,
    val showAddForm: Boolean = false,
    val editingExpenseId: String? = null
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
    private val tripRepository: TripRepository,
    getExpenses: GetExpensesUseCase,
    private val addExpense: AddExpenseUseCase,
    private val updateExpense: UpdateExpenseUseCase,
    private val deleteExpense: DeleteExpenseUseCase
) : ViewModel() {

    private val tripId = savedStateHandle.get<String>(Screen.Budget.ARG_TRIP_ID)!!
    private val displayCurrencyCode = SettingsViewModel.getBudgetDisplayCurrency(context)
    private var latestTripBudget: Double? = null
    private var latestTripCurrencyCode: String = "USD"

    private val _state = MutableStateFlow(BudgetUiState())
    val state: StateFlow<BudgetUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val trip = tripRepository.getTripById(tripId)
            latestTripBudget = trip?.budgetAmount
            latestTripCurrencyCode = trip?.currencyCode ?: "USD"
            recomputeBudgetState(
                expenses = _state.value.expenses,
                tripName = trip?.name ?: ""
            )
        }
        viewModelScope.launch {
            getExpenses(tripId).collect { expenses ->
                recomputeBudgetState(expenses = expenses)
            }
        }
    }

    fun toggleAddForm() = _state.update {
        val showForm = !it.showAddForm
        if (showForm) {
            it.copy(
                showAddForm = true,
                editingExpenseId = null,
                addTitle = "",
                addAmount = "",
                addCurrencyCode = latestTripCurrencyCode,
                addCategory = ExpenseCategory.OTHER
            )
        } else {
            it.copy(
                showAddForm = false,
                editingExpenseId = null,
                addTitle = "",
                addAmount = "",
                addCurrencyCode = latestTripCurrencyCode,
                addCategory = ExpenseCategory.OTHER
            )
        }
    }
    fun onTitleChange(v: String) = _state.update { it.copy(addTitle = v) }
    fun onAmountChange(v: String) = _state.update { it.copy(addAmount = v) }
    fun onCurrencyCodeChange(v: String) = _state.update { it.copy(addCurrencyCode = BudgetDisplayCurrencies.sanitize(v)) }
    fun onCategoryChange(v: ExpenseCategory) = _state.update { it.copy(addCategory = v) }
    fun filterByCategory(cat: ExpenseCategory?) = _state.update { it.copy(filterCategory = cat) }

    fun editExpense(expense: Expense) {
        _state.update {
            it.copy(
                addTitle = expense.title,
                addAmount = expense.amount.toString(),
                addCurrencyCode = expense.currencyCode,
                addCategory = expense.category,
                showAddForm = true,
                editingExpenseId = expense.id
            )
        }
    }

    fun cancelEditing() {
        _state.update {
            it.copy(
                addTitle = "",
                addAmount = "",
                addCurrencyCode = latestTripCurrencyCode,
                addCategory = ExpenseCategory.OTHER,
                showAddForm = false,
                editingExpenseId = null
            )
        }
    }

    fun saveExpense() {
        val s = _state.value
        val amount = s.addAmount.toDoubleOrNull() ?: return
        viewModelScope.launch {
            val expense = Expense(
                id = s.editingExpenseId ?: UUID.randomUUID().toString(),
                tripId = tripId,
                title = s.addTitle.trim(),
                amount = amount,
                currencyCode = s.addCurrencyCode,
                category = s.addCategory
            )
            if (s.editingExpenseId == null) {
                addExpense(expense)
            } else {
                updateExpense(expense)
            }
            _state.update {
                it.copy(
                    addTitle = "",
                    addAmount = "",
                    addCurrencyCode = latestTripCurrencyCode,
                    addCategory = ExpenseCategory.OTHER,
                    showAddForm = false,
                    editingExpenseId = null
                )
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { deleteExpense.invoke(expense) }
    }

    private fun recomputeBudgetState(
        expenses: List<Expense>,
        tripName: String = _state.value.tripName
    ) {
        val total = expenses.sumOf { expense ->
            ApproximateCurrencyConverter.convert(
                amount = expense.amount,
                fromCurrency = expense.currencyCode,
                toCurrency = displayCurrencyCode
            )
        }
        val convertedBudget = latestTripBudget?.let { budgetAmount ->
            ApproximateCurrencyConverter.convert(
                amount = budgetAmount,
                fromCurrency = latestTripCurrencyCode,
                toCurrency = displayCurrencyCode
            )
        }
        val usesApproximateConversion = expenses.any { expense ->
            expense.currencyCode != displayCurrencyCode &&
                ApproximateCurrencyConverter.canConvert(expense.currencyCode, displayCurrencyCode)
        } || (latestTripBudget != null && latestTripCurrencyCode != displayCurrencyCode)

        _state.update {
            it.copy(
                tripName = tripName,
                budget = latestTripBudget,
                tripCurrencyCode = latestTripCurrencyCode,
                displayCurrencyCode = displayCurrencyCode,
                expenses = expenses,
                totalSpent = total,
                convertedBudget = convertedBudget,
                usesApproximateConversion = usesApproximateConversion,
                addCurrencyCode = it.addCurrencyCode.ifBlank { latestTripCurrencyCode }
            )
        }
    }
}
