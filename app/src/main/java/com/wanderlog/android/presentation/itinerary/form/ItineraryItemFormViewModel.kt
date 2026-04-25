package com.wanderlog.android.presentation.itinerary.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.ItineraryItemType
import com.wanderlog.android.domain.model.Place
import com.wanderlog.android.domain.usecase.itinerary.AddItineraryItemUseCase
import com.wanderlog.android.domain.usecase.itinerary.UpdateItineraryItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ItineraryItemFormViewModel @Inject constructor(
    private val addItem: AddItineraryItemUseCase,
    private val updateItem: UpdateItineraryItemUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ItemFormState())
    val state: StateFlow<ItemFormState> = _state.asStateFlow()

    fun loadExisting(item: ItineraryItem) {
        _state.update {
            it.copy(
                title = item.title,
                itemType = item.itemType,
                place = item.place,
                startTime = item.startTime ?: "",
                endTime = item.endTime ?: "",
                notes = item.notes ?: "",
                bookingRef = item.bookingRef ?: "",
                isSaved = false,
                error = null
            )
        }
    }

    fun resetForm() {
        _state.value = ItemFormState()
    }

    fun onTitleChange(v: String) = _state.update { it.copy(title = v) }
    fun onTypeChange(v: ItineraryItemType) = _state.update { it.copy(itemType = v) }
    fun onPlaceSelected(p: Place?) = _state.update { it.copy(place = p) }
    fun onStartTimeChange(v: String) = _state.update { it.copy(startTime = v) }
    fun onEndTimeChange(v: String) = _state.update { it.copy(endTime = v) }
    fun onNotesChange(v: String) = _state.update { it.copy(notes = v) }
    fun onBookingRefChange(v: String) = _state.update { it.copy(bookingRef = v) }

    fun save(tripId: String, dayId: String, existingId: String?) {
        val s = _state.value
        if (s.title.isBlank()) {
            _state.update { it.copy(error = "Title is required.") }
            return
        }
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
            bookingRef = s.bookingRef.takeIf { it.isNotBlank() }
        )
        viewModelScope.launch {
            if (existingId == null) addItem(item) else updateItem(item)
            _state.update { it.copy(isSaved = true) }
        }
    }
}
