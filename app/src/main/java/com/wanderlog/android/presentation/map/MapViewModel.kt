package com.wanderlog.android.presentation.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.repository.ItineraryRepository
import com.wanderlog.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val items: List<ItineraryItem> = emptyList(),
    val selectedItemId: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val itineraryRepository: ItineraryRepository
) : ViewModel() {

    private val tripId = savedStateHandle.get<String>(Screen.Map.ARG_TRIP_ID)!!
    private val dayId = savedStateHandle.get<String>(Screen.Map.ARG_DAY_ID)

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val flow = if (dayId != null)
                itineraryRepository.getItemsForDay(dayId) // dayId specific
            else
                itineraryRepository.getItemsForTrip(tripId)

            flow.collect { all ->
                val mappable = all.filter { it.place?.latitude != null && it.place.longitude != null }
                _state.update { it.copy(items = mappable) }
            }
        }
    }

    fun selectItem(id: String?) = _state.update { it.copy(selectedItemId = id) }
}
