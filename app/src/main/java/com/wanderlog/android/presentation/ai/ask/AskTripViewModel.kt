package com.wanderlog.android.presentation.ai.ask

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.core.util.TripAssistantPromptSupport
import com.wanderlog.android.data.remote.openai.dto.ContentPartDto
import com.wanderlog.android.domain.model.Attachment
import com.wanderlog.android.domain.model.Expense
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.PackingItem
import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.model.TripAssistantMessage
import com.wanderlog.android.domain.model.TripAssistantRole
import com.wanderlog.android.domain.model.TripDay
import com.wanderlog.android.domain.repository.AttachmentRepository
import com.wanderlog.android.domain.repository.ItineraryRepository
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.domain.usecase.ai.AskAboutTripUseCase
import com.wanderlog.android.domain.usecase.ai.BuildAttachmentContextPartsUseCase
import com.wanderlog.android.domain.usecase.expense.GetExpensesUseCase
import com.wanderlog.android.domain.usecase.packing.GetPackingItemsUseCase
import com.wanderlog.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AskTripUiState(
    val tripName: String = "",
    val tripDestination: String = "",
    val attachments: List<Attachment> = emptyList(),
    val selectedAttachmentIds: Set<String> = emptySet(),
    val messages: List<TripAssistantMessage> = emptyList(),
    val draftQuestion: String = "",
    val estimatedContextTokens: Int = 0,
    val estimatedInputTokens: Int = 0,
    val estimatedTotalTokens: Int = 0,
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AskTripViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val itineraryRepository: ItineraryRepository,
    private val attachmentRepository: AttachmentRepository,
    private val getExpenses: GetExpensesUseCase,
    private val getPackingItems: GetPackingItemsUseCase,
    private val askAboutTrip: AskAboutTripUseCase,
    private val buildAttachmentContextParts: BuildAttachmentContextPartsUseCase
) : ViewModel() {

    private val tripId: String = savedStateHandle.get<String>(Screen.AskTrip.ARG_TRIP_ID)!!

    private val _state = MutableStateFlow(AskTripUiState())
    val state: StateFlow<AskTripUiState> = _state.asStateFlow()

    private var trip: Trip? = null
    private var days: List<TripDay> = emptyList()
    private var itineraryItems: List<ItineraryItem> = emptyList()
    private var expenses: List<Expense> = emptyList()
    private var packingItems: List<PackingItem> = emptyList()
    private var cachedAttachmentSelection: Set<String> = emptySet()
    private var cachedAttachmentParts: List<ContentPartDto> = emptyList()
    private var estimateJob: Job? = null

    init {
        loadTrip()
        observeDays()
        observeItineraryItems()
        observeExpenses()
        observePackingItems()
        observeAttachments()
    }

    fun onQuestionChange(value: String) {
        _state.update { it.copy(draftQuestion = value, error = null) }
        refreshPromptEstimates()
    }

    fun toggleAttachmentSelection(attachmentId: String) {
        _state.update { current ->
            val selected = current.selectedAttachmentIds.toMutableSet()
            if (!selected.add(attachmentId)) {
                selected.remove(attachmentId)
            }
            current.copy(selectedAttachmentIds = selected, error = null)
        }
        refreshPromptEstimates()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun sendQuestion() {
        val question = state.value.draftQuestion.trim()
        if (question.isBlank()) {
            _state.update { it.copy(error = "Enter a question first") }
            return
        }

        val currentTrip = trip
        if (currentTrip == null) {
            _state.update { it.copy(error = "Trip not found") }
            return
        }

        val selectedAttachments = state.value.attachments.filter { attachment ->
            attachment.id in state.value.selectedAttachmentIds
        }
        val conversation = state.value.messages

        viewModelScope.launch {
            _state.update { it.copy(isSending = true, error = null) }

            runCatching {
                val itineraryDays = currentPromptDays()
                val attachmentParts = attachmentPartsForSelection(selectedAttachments)
                askAboutTrip(
                    trip = currentTrip,
                    days = itineraryDays,
                    expenses = expenses,
                    packingItems = packingItems,
                    conversation = conversation,
                    question = question,
                    attachmentParts = attachmentParts,
                    selectedAttachmentNames = selectedAttachments.map(Attachment::displayName)
                )
            }.onSuccess { answer ->
                _state.update {
                    it.copy(
                        draftQuestion = "",
                        isSending = false,
                        messages = it.messages +
                            TripAssistantMessage(TripAssistantRole.USER, question) +
                            TripAssistantMessage(TripAssistantRole.ASSISTANT, answer)
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isSending = false,
                        error = error.message ?: "Failed to ask about this trip"
                    )
                }
            }
        }
    }

    private fun loadTrip() {
        viewModelScope.launch {
            val loadedTrip = tripRepository.getTripById(tripId)
            trip = loadedTrip
            _state.update {
                it.copy(
                    tripName = loadedTrip?.name.orEmpty(),
                    tripDestination = loadedTrip?.destination.orEmpty(),
                    isLoading = false,
                    error = if (loadedTrip == null) "Trip not found" else it.error
                )
            }
            refreshPromptEstimates()
        }
    }

    private fun observeDays() {
        viewModelScope.launch {
            tripRepository.getDaysForTripFlow(tripId).collect { tripDays ->
                days = tripDays
                refreshPromptEstimates()
            }
        }
    }

    private fun observeItineraryItems() {
        viewModelScope.launch {
            itineraryRepository.getItemsForTrip(tripId).collect { items ->
                itineraryItems = items
                refreshPromptEstimates()
            }
        }
    }

    private fun observeExpenses() {
        viewModelScope.launch {
            getExpenses(tripId).collect { tripExpenses ->
                expenses = tripExpenses
                refreshPromptEstimates()
            }
        }
    }

    private fun observePackingItems() {
        viewModelScope.launch {
            getPackingItems(tripId).collect { items ->
                packingItems = items
                refreshPromptEstimates()
            }
        }
    }

    private fun observeAttachments() {
        viewModelScope.launch {
            attachmentRepository.getAttachmentsForTrip(tripId).collect { tripAttachments ->
                val availableIds = tripAttachments.mapTo(mutableSetOf(), Attachment::id)
                _state.update {
                    it.copy(
                        attachments = tripAttachments,
                        selectedAttachmentIds = it.selectedAttachmentIds.filterTo(mutableSetOf()) { id ->
                            id in availableIds
                        }
                    )
                }
                refreshPromptEstimates()
            }
        }
    }

    private fun refreshPromptEstimates() {
        val currentTrip = trip ?: run {
            _state.update {
                it.copy(
                    estimatedContextTokens = 0,
                    estimatedInputTokens = 0,
                    estimatedTotalTokens = 0
                )
            }
            return
        }

        val stateSnapshot = _state.value
        val selectedAttachments = stateSnapshot.attachments.filter { attachment ->
            attachment.id in stateSnapshot.selectedAttachmentIds
        }
        val question = stateSnapshot.draftQuestion.trim()
        val conversation = stateSnapshot.messages

        estimateJob?.cancel()
        estimateJob = viewModelScope.launch {
            val attachmentParts = attachmentPartsForSelection(selectedAttachments)
            val estimate = TripAssistantPromptSupport.estimatePrompt(
                trip = currentTrip,
                days = currentPromptDays(),
                expenses = expenses,
                packingItems = packingItems,
                conversation = conversation,
                question = question,
                attachmentParts = attachmentParts,
                selectedAttachmentNames = selectedAttachments.map(Attachment::displayName)
            )
            _state.update {
                it.copy(
                    estimatedContextTokens = estimate.contextTokens,
                    estimatedInputTokens = estimate.inputTokens,
                    estimatedTotalTokens = estimate.totalTokens
                )
            }
        }
    }

    private fun currentPromptDays(): List<TripDay> {
        val itemsByDay = itineraryItems.groupBy(ItineraryItem::tripDayId)
        return days
            .sortedBy { it.dayNumber }
            .map { day ->
                day.copy(items = itemsByDay[day.id].orEmpty().sortedBy(ItineraryItem::sortOrder))
            }
    }

    private suspend fun attachmentPartsForSelection(selectedAttachments: List<Attachment>): List<ContentPartDto> {
        val selectedIds = selectedAttachments.mapTo(linkedSetOf(), Attachment::id)
        if (selectedIds == cachedAttachmentSelection) {
            return cachedAttachmentParts
        }

        val parts = if (selectedAttachments.isEmpty()) {
            emptyList()
        } else {
            buildAttachmentContextParts(selectedAttachments)
        }
        cachedAttachmentSelection = selectedIds
        cachedAttachmentParts = parts
        return parts
    }
}
