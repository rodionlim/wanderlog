package com.wanderlog.android.presentation.ai.ask

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.Attachment
import com.wanderlog.android.domain.model.Expense
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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AskTripUiState(
    val tripName: String = "",
    val tripDestination: String = "",
    val attachments: List<Attachment> = emptyList(),
    val selectedAttachmentIds: Set<String> = emptySet(),
    val messages: List<TripAssistantMessage> = emptyList(),
    val draftQuestion: String = "",
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
    private var expenses: List<Expense> = emptyList()
    private var packingItems: List<PackingItem> = emptyList()

    init {
        loadTrip()
        observeDays()
        observeExpenses()
        observePackingItems()
        observeAttachments()
    }

    fun onQuestionChange(value: String) {
        _state.update { it.copy(draftQuestion = value, error = null) }
    }

    fun toggleAttachmentSelection(attachmentId: String) {
        _state.update { current ->
            val selected = current.selectedAttachmentIds.toMutableSet()
            if (!selected.add(attachmentId)) {
                selected.remove(attachmentId)
            }
            current.copy(selectedAttachmentIds = selected, error = null)
        }
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
                val itineraryDays = days.sortedBy { it.dayNumber }.map { day ->
                    day.copy(items = itineraryRepository.getItemsForDay(day.id).first())
                }
                val attachmentParts = buildAttachmentContextParts(selectedAttachments)
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
        }
    }

    private fun observeDays() {
        viewModelScope.launch {
            tripRepository.getDaysForTripFlow(tripId).collect { tripDays ->
                days = tripDays
            }
        }
    }

    private fun observeExpenses() {
        viewModelScope.launch {
            getExpenses(tripId).collect { tripExpenses ->
                expenses = tripExpenses
            }
        }
    }

    private fun observePackingItems() {
        viewModelScope.launch {
            getPackingItems(tripId).collect { items ->
                packingItems = items
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
            }
        }
    }
}
