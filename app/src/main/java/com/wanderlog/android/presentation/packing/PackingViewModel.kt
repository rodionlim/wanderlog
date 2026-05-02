package com.wanderlog.android.presentation.packing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.PackingItem
import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.domain.usecase.packing.AddPackingItemUseCase
import com.wanderlog.android.domain.usecase.packing.DeletePackingItemUseCase
import com.wanderlog.android.domain.usecase.packing.GetPackingItemsUseCase
import com.wanderlog.android.domain.usecase.packing.TogglePackingItemUseCase
import com.wanderlog.android.domain.usecase.packing.UpdatePackingItemUseCase
import com.wanderlog.android.domain.usecase.packing.UpdatePackingListWithAiUseCase
import com.wanderlog.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class PackingAggregateItem(
    val key: String,
    val title: String,
    val items: List<PackingItem>
) {
    val checkedCount: Int get() = items.count(PackingItem::isChecked)
    val totalCount: Int get() = items.size
    val totalQuantity: Int get() = items.sumOf(PackingItem::quantity)
    val allChecked: Boolean get() = items.isNotEmpty() && checkedCount == totalCount
    val travellerLabel: String get() = items.mapNotNull(PackingItem::travellerName).distinct().joinToString(", ")
}

data class PackingUiState(
    val tripName: String = "",
    val travellerNames: List<String> = emptyList(),
    val items: List<PackingItem> = emptyList(),
    val newItemText: String = "",
    val selectedTabIndex: Int = 0,
    val aiPrompt: String = "",
    val isAiUpdating: Boolean = false,
    val aiMessage: String? = null,
    val aiError: String? = null
)

@HiltViewModel
class PackingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    getItems: GetPackingItemsUseCase,
    private val addItem: AddPackingItemUseCase,
    private val toggleItem: TogglePackingItemUseCase,
    private val deleteItem: DeletePackingItemUseCase,
    private val updateItem: UpdatePackingItemUseCase,
    private val updatePackingListWithAi: UpdatePackingListWithAiUseCase
) : ViewModel() {

    private val tripId = savedStateHandle.get<String>(Screen.Packing.ARG_TRIP_ID)!!
    private var trip: Trip? = null

    private val _state = MutableStateFlow(PackingUiState())
    val state: StateFlow<PackingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val loadedTrip = tripRepository.getTripById(tripId)
            trip = loadedTrip
            _state.update {
                it.copy(
                    tripName = loadedTrip?.name ?: "",
                    travellerNames = loadedTrip?.travellerNames ?: emptyList()
                )
            }
        }
        viewModelScope.launch {
            getItems(tripId).collect { items -> _state.update { it.copy(items = items) } }
        }
    }

    fun onNewItemTextChange(v: String) = _state.update { it.copy(newItemText = v) }
    fun onAiPromptChange(v: String) = _state.update { it.copy(aiPrompt = v, aiError = null, aiMessage = null) }
    fun selectTab(index: Int) = _state.update { it.copy(selectedTabIndex = index) }

    fun visibleIndividualItems(): List<PackingItem> {
        val current = _state.value
        val travellerName = current.selectedTravellerName()
        return when {
            current.travellerNames.isEmpty() -> current.items
            travellerName == null -> current.items
            else -> current.items.filter { it.travellerName == travellerName }
        }
    }

    fun aggregateItems(): List<PackingAggregateItem> =
        _state.value.items
            .groupBy { item -> item.title.trim().lowercase() }
            .values
            .map { groupedItems ->
                PackingAggregateItem(
                    key = groupedItems.first().title.trim().lowercase(),
                    title = groupedItems.first().title,
                    items = groupedItems.sortedBy(PackingItem::sortOrder)
                )
            }
            .sortedBy { aggregate -> aggregate.items.minOfOrNull(PackingItem::sortOrder) ?: 0 }

    fun addItem() {
        val current = _state.value
        val text = current.newItemText.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            val travellers = when {
                current.travellerNames.isEmpty() -> listOf<String?>(null)
                current.selectedTabIndex == 0 -> current.travellerNames.map { it }
                else -> listOf(current.selectedTravellerName())
            }

            travellers.filterNotNullOrSingleNull().forEachIndexed { index, travellerName ->
                addItem(
                    PackingItem(
                        id = UUID.randomUUID().toString(),
                        tripId = tripId,
                        title = text,
                        quantity = 1,
                        travellerName = travellerName,
                        sortOrder = current.items.size + index
                    )
                )
            }
            _state.update { it.copy(newItemText = "") }
        }
    }

    fun applyAiUpdate() {
        val current = _state.value
        val loadedTrip = trip
        val prompt = current.aiPrompt.trim()

        when {
            loadedTrip == null -> {
                _state.update { it.copy(aiError = "Trip details are still loading.") }
                return
            }

            prompt.isBlank() -> {
                _state.update { it.copy(aiError = "Enter a prompt for the packing update.") }
                return
            }
        }

        _state.update { it.copy(isAiUpdating = true, aiError = null, aiMessage = null) }
        viewModelScope.launch {
            runCatching {
                updatePackingListWithAi(
                    trip = loadedTrip,
                    existingItems = current.items,
                    userPrompt = prompt
                )
            }.onSuccess { updatedItems ->
                _state.update {
                    it.copy(
                        aiPrompt = "",
                        isAiUpdating = false,
                        aiMessage = if (updatedItems.isEmpty()) {
                            "Packing list updated. No items returned."
                        } else {
                            "Packing list updated with ${updatedItems.size} items."
                        }
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isAiUpdating = false,
                        aiError = error.message ?: "Unable to update packing list."
                    )
                }
            }
        }
    }

    fun toggleItem(item: PackingItem) {
        viewModelScope.launch { toggleItem.invoke(item) }
    }

    fun toggleAggregateItem(group: PackingAggregateItem) {
        val targetChecked = !group.allChecked
        viewModelScope.launch {
            group.items
                .filter { it.isChecked != targetChecked }
                .forEach { item -> toggleItem.invoke(item) }
        }
    }

    fun deleteItem(item: PackingItem) {
        viewModelScope.launch { deleteItem.invoke(item) }
    }

    fun deleteAggregateItem(group: PackingAggregateItem) {
        viewModelScope.launch {
            group.items.forEach { item -> deleteItem.invoke(item) }
        }
    }

    fun updateItem(item: PackingItem, title: String, quantity: Int) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank() || quantity <= 0) return

        viewModelScope.launch {
            updateItem.invoke(
                item.copy(
                    title = normalizedTitle,
                    quantity = quantity
                )
            )
        }
    }

    fun updateAggregateItem(group: PackingAggregateItem, title: String, quantity: Int) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank() || quantity <= 0) return

        viewModelScope.launch {
            group.items.forEach { item ->
                updateItem.invoke(
                    item.copy(
                        title = normalizedTitle,
                        quantity = quantity
                    )
                )
            }
        }
    }

    private fun PackingUiState.selectedTravellerName(): String? =
        travellerNames.getOrNull(selectedTabIndex - 1)
}

private fun List<String?>.filterNotNullOrSingleNull(): List<String?> =
    if (size == 1 && firstOrNull() == null) this else filterNotNull()
