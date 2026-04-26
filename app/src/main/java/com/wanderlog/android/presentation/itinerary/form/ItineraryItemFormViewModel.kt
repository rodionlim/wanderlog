package com.wanderlog.android.presentation.itinerary.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.core.util.ImportedMoneyParser
import com.wanderlog.android.domain.model.Expense
import com.wanderlog.android.domain.model.ExpenseCategory
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.ItineraryItemType
import com.wanderlog.android.domain.model.Place
import com.wanderlog.android.domain.usecase.expense.AddExpenseUseCase
import com.wanderlog.android.domain.usecase.expense.DeleteExpenseUseCase
import com.wanderlog.android.domain.usecase.expense.UpdateExpenseUseCase
import com.wanderlog.android.domain.usecase.itinerary.AddItineraryItemUseCase
import com.wanderlog.android.domain.usecase.itinerary.UpdateItineraryItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class ItemFormState(
    val title: String = "",
    val itemType: ItineraryItemType = ItineraryItemType.PLACE,
    val place: Place? = null,
    val startTime: String = "",
    val endTime: String = "",
    val notes: String = "",
    val bookingRef: String = "",
    val costAmount: String = "",
    val linkedExpenseId: String? = null,
    val linkedExpenseExists: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ItineraryItemFormViewModel @Inject constructor(
    private val addExpense: AddExpenseUseCase,
    private val updateExpense: UpdateExpenseUseCase,
    private val deleteExpense: DeleteExpenseUseCase,
    private val addItem: AddItineraryItemUseCase,
    private val updateItem: UpdateItineraryItemUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ItemFormState())
    val state: StateFlow<ItemFormState> = _state.asStateFlow()
    private var loadedLinkedExpense: Expense? = null

    fun loadExisting(item: ItineraryItem, linkedExpense: Expense?) {
        loadedLinkedExpense = linkedExpense
        _state.update {
            it.copy(
                title = item.title,
                itemType = item.itemType,
                place = item.place,
                startTime = item.startTime ?: "",
                endTime = item.endTime ?: "",
                notes = item.notes ?: "",
                bookingRef = item.bookingRef ?: "",
                costAmount = linkedExpense?.amount?.toEditableAmount().orEmpty(),
                linkedExpenseId = item.linkedExpenseId,
                linkedExpenseExists = linkedExpense != null,
                isSaved = false,
                error = null
            )
        }
    }

    fun resetForm() {
        loadedLinkedExpense = null
        _state.value = ItemFormState()
    }

    fun onTitleChange(v: String) = _state.update { it.copy(title = v) }
    fun onTypeChange(v: ItineraryItemType) = _state.update { it.copy(itemType = v) }
    fun onPlaceSelected(p: Place?) = _state.update { it.copy(place = p) }
    fun onStartTimeChange(v: String) = _state.update { it.copy(startTime = v) }
    fun onEndTimeChange(v: String) = _state.update { it.copy(endTime = v) }
    fun onNotesChange(v: String) = _state.update { it.copy(notes = v) }
    fun onBookingRefChange(v: String) = _state.update { it.copy(bookingRef = v) }
    fun onCostChange(v: String) = _state.update { it.copy(costAmount = v) }

    fun save(
        tripId: String,
        dayId: String,
        dayDate: LocalDate?,
        currencyCode: String,
        existingId: String?
    ) {
        val s = _state.value
        if (s.title.isBlank()) {
            _state.update { it.copy(error = "Title is required.") }
            return
        }

        val parsedCost = when {
            s.costAmount.isBlank() -> null
            else -> ImportedMoneyParser.parseAmount(s.costAmount)
        }
        if (s.costAmount.isNotBlank() && parsedCost == null) {
            _state.update { it.copy(error = "Enter a valid cost amount.") }
            return
        }

        val shouldLinkExpense = s.itemType == ItineraryItemType.ACTIVITY && parsedCost != null
        val linkedExpenseId = if (shouldLinkExpense) s.linkedExpenseId ?: UUID.randomUUID().toString() else null
        val item = ItineraryItem(
            id = existingId ?: UUID.randomUUID().toString(),
            tripDayId = dayId,
            tripId = tripId,
            title = s.title.trim(),
            itemType = s.itemType,
            place = s.place,
            startTime = s.startTime.takeIf { it.isNotBlank() },
            endTime = s.endTime.takeIf { it.isNotBlank() },
            notes = s.notes.takeIf { it.isNotBlank() },
            bookingRef = s.bookingRef.takeIf { it.isNotBlank() },
            linkedExpenseId = linkedExpenseId
        )
        viewModelScope.launch {
            runCatching {
                syncLinkedExpense(
                    item = item,
                    amount = parsedCost,
                    currencyCode = currencyCode,
                    dayDate = dayDate,
                    itemTitle = s.title.trim()
                )
                if (existingId == null) addItem(item) else updateItem(item)
            }.onSuccess {
                _state.update { it.copy(isSaved = true, error = null) }
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to save item.") }
            }
        }
    }

    private suspend fun syncLinkedExpense(
        item: ItineraryItem,
        amount: Double?,
        currencyCode: String,
        dayDate: LocalDate?,
        itemTitle: String
    ) {
        if (item.itemType == ItineraryItemType.ACTIVITY && amount != null) {
            val expense = Expense(
                id = item.linkedExpenseId ?: UUID.randomUUID().toString(),
                tripId = item.tripId,
                title = itemTitle,
                amount = amount,
                currencyCode = currencyCode,
                category = ExpenseCategory.ACTIVITY,
                date = dayDate
            )
            if (_state.value.linkedExpenseExists && loadedLinkedExpense != null) {
                updateExpense(expense)
            } else {
                addExpense(expense)
            }
            loadedLinkedExpense = expense
            _state.update {
                it.copy(
                    linkedExpenseId = expense.id,
                    linkedExpenseExists = true
                )
            }
            return
        }

        loadedLinkedExpense?.let { deleteExpense(it) }
        loadedLinkedExpense = null
        _state.update {
            it.copy(
                linkedExpenseId = null,
                linkedExpenseExists = false
            )
        }
    }
}

private fun Double.toEditableAmount(): String {
    val wholeNumber = toLong().toDouble() == this
    return if (wholeNumber) toLong().toString() else toString()
}
