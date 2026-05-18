package com.wanderlog.android.presentation.budget

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.core.util.ApproximateCurrencyConverter
import com.wanderlog.android.core.util.BudgetDisplayCurrencies
import com.wanderlog.android.domain.model.Expense
import com.wanderlog.android.domain.model.ExpenseCategory
import com.wanderlog.android.domain.model.ParsedBudgetExpense
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.domain.usecase.ai.ParseBudgetExpensePhotoUseCase
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
import java.time.LocalDate

data class BudgetUiState(
    val tripName: String = "",
    val budget: Double? = null,
    val tripCurrencyCode: String = "SGD",
    val displayCurrencyCode: String = BudgetDisplayCurrencies.DEFAULT,
    val expenses: List<Expense> = emptyList(),
    val totalSpent: Double = 0.0,
    val convertedBudget: Double? = null,
    val usesApproximateConversion: Boolean = false,
    val filterCategory: ExpenseCategory? = null,
    val addTitle: String = "",
    val addAmount: String = "",
    val addCurrencyCode: String = "SGD",
    val addCategory: ExpenseCategory = ExpenseCategory.OTHER,
    val showAddForm: Boolean = false,
    val editingExpenseId: String? = null,
    val photoImportStep: BudgetPhotoImportStep = BudgetPhotoImportStep.Idle,
    val selectedDate: LocalDate? = null
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
    private val tripRepository: TripRepository,
    getExpenses: GetExpensesUseCase,
    private val parseBudgetExpensePhoto: ParseBudgetExpensePhotoUseCase,
    private val addExpense: AddExpenseUseCase,
    private val updateExpense: UpdateExpenseUseCase,
    private val deleteExpense: DeleteExpenseUseCase
) : ViewModel() {

    private val tripId = savedStateHandle.get<String>(Screen.Budget.ARG_TRIP_ID)!!
    private val displayCurrencyCode = SettingsViewModel.getBudgetDisplayCurrency(context)
    private var latestTripBudget: Double? = null
    private var latestTripCurrencyCode: String = "SGD"

    private val _state = MutableStateFlow(BudgetUiState())
    val state: StateFlow<BudgetUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val trip = tripRepository.getTripById(tripId)
            latestTripBudget = trip?.budgetAmount
            latestTripCurrencyCode = trip?.currencyCode ?: "SGD"
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

    fun openAddForm() = _state.update {
        it.copy(
            showAddForm = true,
            editingExpenseId = null,
            addTitle = "",
            addAmount = "",
            addCurrencyCode = latestTripCurrencyCode,
            addCategory = ExpenseCategory.OTHER
        )
    }

    fun closeAddForm() = _state.update {
        it.copy(
            showAddForm = false,
            editingExpenseId = null,
            addTitle = "",
            addAmount = "",
            addCurrencyCode = latestTripCurrencyCode,
            addCategory = ExpenseCategory.OTHER
        )
    }
    fun onTitleChange(v: String) = _state.update { it.copy(addTitle = v) }
    fun onAmountChange(v: String) = _state.update { it.copy(addAmount = v) }
    fun onCurrencyCodeChange(v: String) = _state.update { it.copy(addCurrencyCode = BudgetDisplayCurrencies.sanitize(v)) }
    fun onCategoryChange(v: ExpenseCategory) = _state.update { it.copy(addCategory = v) }
    fun filterByCategory(cat: ExpenseCategory?) = _state.update { it.copy(filterCategory = cat) }
    fun onDateChange(date: LocalDate) {
        _state.update {
            it.copy(selectedDate = date)
        }
    }

    fun startPhotoImport(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(photoImportStep = BudgetPhotoImportStep.Parsing) }
            runCatching {
                parseBudgetExpensePhoto(uri, latestTripCurrencyCode)
            }.onSuccess { parsed ->
                _state.update {
                    it.copy(photoImportStep = BudgetPhotoImportStep.Review(parsed.items))
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(photoImportStep = BudgetPhotoImportStep.Error(error.message ?: "Photo import failed"))
                }
            }
        }
    }

    fun updateImportedExpense(updatedExpense: ParsedBudgetExpense) {
        _state.update { current ->
            val step = current.photoImportStep as? BudgetPhotoImportStep.Review ?: return@update current
            current.copy(
                photoImportStep = step.copy(
                    items = step.items.map { item -> if (item.id == updatedExpense.id) updatedExpense else item }
                )
            )
        }
    }

    fun removeImportedExpense(expenseId: String) {
        _state.update { current ->
            val step = current.photoImportStep as? BudgetPhotoImportStep.Review ?: return@update current
            current.copy(photoImportStep = step.copy(items = step.items.filterNot { it.id == expenseId }))
        }
    }

    fun resetPhotoImport() {
        _state.update { it.copy(photoImportStep = BudgetPhotoImportStep.Idle) }
    }

    fun commitImportedExpenses() {
        val review = (_state.value.photoImportStep as? BudgetPhotoImportStep.Review) ?: return
        viewModelScope.launch {
            runCatching {
                review.items.forEach { parsed ->
                    val amount = parsed.amountText.trim().toDoubleOrNull()
                        ?: throw IllegalStateException("Enter a valid amount for ${parsed.title}")
                    addExpense(
                        Expense(
                            id = UUID.randomUUID().toString(),
                            tripId = tripId,
                            title = parsed.title.trim(),
                            amount = amount,
                            currencyCode = parsed.currencyCode,
                            category = parsed.category,
                            date = parsed.dateText?.trim()?.takeIf { it.isNotBlank() }?.let {
                                runCatching { LocalDate.parse(it) }.getOrNull()
                            },
                            notes = parsed.notes?.trim()?.takeIf { it.isNotBlank() }
                        )
                    )
                }
            }.onSuccess {
                _state.update { it.copy(photoImportStep = BudgetPhotoImportStep.Idle) }
            }.onFailure { error ->
                _state.update {
                    it.copy(photoImportStep = BudgetPhotoImportStep.Error(error.message ?: "Failed to import expenses"))
                }
            }
        }
    }

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

    fun duplicateExpense(expense: Expense) {
        viewModelScope.launch {
            addExpense(
                expense.copy(
                    id = UUID.randomUUID().toString(),
                    title = nextDuplicateTitle(expense)
                )
            )
        }
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

    private fun nextDuplicateTitle(expense: Expense): String {
        val baseTitle = normalizeDuplicateBaseTitle(expense.title)
        val matchingNumbers = _state.value.expenses.mapNotNull { existingExpense ->
            val (existingBase, suffixNumber) = parseDuplicateTitle(existingExpense.title)
            if (existingBase.equals(baseTitle, ignoreCase = true)) suffixNumber else null
        }

        val nextNumber = (matchingNumbers.maxOrNull() ?: 0) + 1
        return "$baseTitle $nextNumber"
    }

    private fun normalizeDuplicateBaseTitle(title: String): String =
        title.trim().replace(duplicateSuffixRegex, "").trim().ifBlank { title.trim() }

    private fun parseDuplicateTitle(title: String): Pair<String, Int?> {
        val trimmed = title.trim()
        val match = duplicateSuffixRegex.find(trimmed) ?: return trimmed to null
        val base = trimmed.substring(0, match.range.first).trim()
        return base to match.groupValues[1].toIntOrNull()
    }

    private companion object {
        private val duplicateSuffixRegex = Regex("\\s+(\\d+)$")
    }
}
