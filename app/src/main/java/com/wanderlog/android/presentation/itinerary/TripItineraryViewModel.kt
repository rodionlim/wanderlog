package com.wanderlog.android.presentation.itinerary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.TripDay
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.domain.usecase.itinerary.DeleteItineraryItemUseCase
import com.wanderlog.android.domain.usecase.itinerary.GetItineraryForDayUseCase
import com.wanderlog.android.domain.usecase.itinerary.UpdateItemOrderUseCase
import com.wanderlog.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ItineraryUiState(
    val tripId: String = "",
    val tripName: String = "",
    val tripDestination: String = "",
    val days: List<TripDay> = emptyList(),
    val selectedDayIndex: Int = 0,
    val itemsForSelectedDay: List<ItineraryItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class TripItineraryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val getItemsForDay: GetItineraryForDayUseCase,
    private val deleteItem: DeleteItineraryItemUseCase,
    private val updateOrder: UpdateItemOrderUseCase
) : ViewModel() {

    private val tripId = savedStateHandle.get<String>(Screen.Itinerary.ARG_TRIP_ID)!!

    private val _state = MutableStateFlow(ItineraryUiState(tripId = tripId))
    val state: StateFlow<ItineraryUiState> = _state.asStateFlow()

    private val selectedDayId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val itemsForDay: StateFlow<List<ItineraryItem>> = selectedDayId
        .flatMapLatest { dayId ->
            if (dayId != null) getItemsForDay(dayId)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadTripAndDays()
    }

    private fun loadTripAndDays() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val trip = tripRepository.getTripById(tripId)
            tripRepository.getDaysForTripFlow(tripId).collect { days ->
                val currentDay = days.getOrNull(_state.value.selectedDayIndex) ?: days.firstOrNull()
                selectedDayId.value = currentDay?.id
                _state.update { s ->
                    s.copy(
                        tripName = trip?.name ?: "",
                        tripDestination = trip?.destination ?: "",
                        days = days,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectDay(index: Int) {
        _state.update { it.copy(selectedDayIndex = index) }
        selectedDayId.value = _state.value.days.getOrNull(index)?.id
    }

    fun deleteItem(item: ItineraryItem) {
        viewModelScope.launch { deleteItem.invoke(item) }
    }

    fun reorderItems(items: List<ItineraryItem>) {
        viewModelScope.launch {
            items.forEachIndexed { index, item ->
                updateOrder(item.id, index)
            }
        }
    }
}
