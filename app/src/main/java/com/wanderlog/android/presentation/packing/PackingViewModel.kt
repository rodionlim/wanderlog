package com.wanderlog.android.presentation.packing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.PackingItem
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.domain.usecase.packing.AddPackingItemUseCase
import com.wanderlog.android.domain.usecase.packing.DeletePackingItemUseCase
import com.wanderlog.android.domain.usecase.packing.GetPackingItemsUseCase
import com.wanderlog.android.domain.usecase.packing.TogglePackingItemUseCase
import com.wanderlog.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class PackingUiState(
    val tripName: String = "",
    val items: List<PackingItem> = emptyList(),
    val newItemText: String = ""
)

@HiltViewModel
class PackingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    getItems: GetPackingItemsUseCase,
    private val addItem: AddPackingItemUseCase,
    private val toggleItem: TogglePackingItemUseCase,
    private val deleteItem: DeletePackingItemUseCase
) : ViewModel() {

    private val tripId = savedStateHandle.get<String>(Screen.Packing.ARG_TRIP_ID)!!

    private val _state = MutableStateFlow(PackingUiState())
    val state: StateFlow<PackingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val trip = tripRepository.getTripById(tripId)
            _state.update { it.copy(tripName = trip?.name ?: "") }
        }
        viewModelScope.launch {
            getItems(tripId).collect { items -> _state.update { it.copy(items = items) } }
        }
    }

    fun onNewItemTextChange(v: String) = _state.update { it.copy(newItemText = v) }

    fun addItem() {
        val text = _state.value.newItemText.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            addItem(PackingItem(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                title = text,
                sortOrder = _state.value.items.size
            ))
            _state.update { it.copy(newItemText = "") }
        }
    }

    fun toggleItem(item: PackingItem) {
        viewModelScope.launch { toggleItem.invoke(item) }
    }

    fun deleteItem(item: PackingItem) {
        viewModelScope.launch { deleteItem.invoke(item) }
    }
}
