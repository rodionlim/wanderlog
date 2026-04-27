package com.wanderlog.android.presentation.itinerary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.Expense
import com.wanderlog.android.domain.model.ExpenseCategory
import com.wanderlog.android.domain.model.ItemAttachmentLinkType
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.model.TripDay
import com.wanderlog.android.domain.repository.AttachmentRepository
import com.wanderlog.android.domain.repository.ItineraryRepository
import com.wanderlog.android.domain.repository.ItineraryItemAttachmentRepository
import com.wanderlog.android.domain.repository.PlacesRepository
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.domain.usecase.expense.DeleteExpenseUseCase
import com.wanderlog.android.domain.usecase.expense.GetExpensesUseCase
import com.wanderlog.android.domain.usecase.itinerary.DeleteItineraryItemUseCase
import com.wanderlog.android.domain.usecase.itinerary.GetItineraryForDayUseCase
import com.wanderlog.android.domain.usecase.itinerary.UpdateItemOrderUseCase
import com.wanderlog.android.presentation.navigation.Screen
import dagger.Lazy
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import javax.inject.Inject

data class ItineraryUiState(
    val tripId: String = "",
    val tripName: String = "",
    val tripDestination: String = "",
    val tripCurrencyCode: String = "USD",
    val tripCoverImageUri: String? = null,
    val linkedExpensesById: Map<String, Expense> = emptyMap(),
    val attachmentCountsByItemId: Map<String, Int> = emptyMap(),
    val importAttachmentCountsByItemId: Map<String, Int> = emptyMap(),
    val days: List<TripDay> = emptyList(),
    val selectedDayIndex: Int = 0,
    val activeHotelsForSelectedDay: List<ItineraryItem> = emptyList(),
    val itemsForSelectedDay: List<ItineraryItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class TripItineraryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val itineraryRepository: ItineraryRepository,
    private val attachmentRepository: AttachmentRepository,
    private val itineraryItemAttachmentRepository: ItineraryItemAttachmentRepository,
    private val placesRepository: Lazy<PlacesRepository>,
    private val getExpenses: GetExpensesUseCase,
    private val deleteExpense: DeleteExpenseUseCase,
    private val getItemsForDay: GetItineraryForDayUseCase,
    private val deleteItem: DeleteItineraryItemUseCase,
    private val updateOrder: UpdateItemOrderUseCase
) : ViewModel() {

    private val tripId = savedStateHandle.get<String>(Screen.Itinerary.ARG_TRIP_ID)!!

    private val _state = MutableStateFlow(ItineraryUiState(tripId = tripId))
    val state: StateFlow<ItineraryUiState> = _state.asStateFlow()

    private val selectedDayId = MutableStateFlow<String?>(null)
    private val allTripItems = MutableStateFlow<List<ItineraryItem>>(emptyList())
    private var attemptedCoverBackfill = false

    @OptIn(ExperimentalCoroutinesApi::class)
    val itemsForDay: StateFlow<List<ItineraryItem>> = selectedDayId
        .flatMapLatest { dayId ->
            if (dayId != null) getItemsForDay(dayId)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        observeExpenses()
        observeTripItems()
        observeAttachmentCounts()
        loadTripAndDays()
    }

    private fun observeTripItems() {
        viewModelScope.launch {
            itineraryRepository.getItemsForTrip(tripId).collect { items ->
                allTripItems.value = items
                updateActiveHotelsForSelectedDay(allItems = items)
            }
        }
    }

    private fun observeExpenses() {
        viewModelScope.launch {
            getExpenses(tripId).collect { expenses ->
                _state.update {
                    it.copy(linkedExpensesById = expenses.associateBy(Expense::id))
                }
            }
        }
    }

    private fun observeAttachmentCounts() {
        viewModelScope.launch {
            itineraryItemAttachmentRepository.getAttachmentCountsForTrip(tripId).collect { counts ->
                _state.update { it.copy(attachmentCountsByItemId = counts) }
            }
        }
        viewModelScope.launch {
            itineraryItemAttachmentRepository.getAttachmentCountsForTrip(
                tripId,
                linkType = ItemAttachmentLinkType.IMPORT_SOURCE
            ).collect { counts ->
                _state.update { it.copy(importAttachmentCountsByItemId = counts) }
            }
        }
    }

    private fun loadTripAndDays() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            tripRepository.getDaysForTripFlow(tripId).collect { days ->
                val trip = ensureCoverImage(tripRepository.getTripById(tripId))
                val currentDay = days.getOrNull(_state.value.selectedDayIndex) ?: days.firstOrNull()
                selectedDayId.value = currentDay?.id
                _state.update { s ->
                    s.copy(
                        tripName = trip?.name ?: "",
                        tripDestination = trip?.destination ?: "",
                        tripCurrencyCode = trip?.currencyCode ?: "USD",
                        tripCoverImageUri = trip?.coverImageUri,
                        days = days,
                        isLoading = false
                    )
                }
                updateActiveHotelsForSelectedDay(days = days)
            }
        }
    }

    private suspend fun ensureCoverImage(trip: Trip?): Trip? {
        if (trip == null || trip.destination.isBlank() || !trip.coverImageUri.isNullOrBlank()) {
            return trip
        }
        if (attemptedCoverBackfill) {
            return trip
        }

        attemptedCoverBackfill = true
        val coverImageUri = runCatching {
            placesRepository.get().fetchDestinationCoverImage(trip.destination, trip.id)
        }.getOrNull()

        if (coverImageUri.isNullOrBlank()) {
            return trip
        }

        val updatedTrip = trip.copy(coverImageUri = coverImageUri)
        tripRepository.updateTrip(updatedTrip)
        return updatedTrip
    }

    fun selectDay(index: Int) {
        _state.update { it.copy(selectedDayIndex = index) }
        selectedDayId.value = _state.value.days.getOrNull(index)?.id
        updateActiveHotelsForSelectedDay(selectedDayIndex = index)
    }

    private fun updateActiveHotelsForSelectedDay(
        days: List<TripDay> = _state.value.days,
        selectedDayIndex: Int = _state.value.selectedDayIndex,
        allItems: List<ItineraryItem> = allTripItems.value
    ) {
        val selectedDay = days.firstOrNull { it.id == selectedDayId.value }
            ?: days.getOrNull(selectedDayIndex)
            ?: days.firstOrNull()

        val activeHotels = if (selectedDay == null) {
            emptyList()
        } else {
            val visibleItemIds = allItems
                .asSequence()
                .filter { it.tripDayId == selectedDay.id }
                .map(ItineraryItem::id)
                .toSet()

            allItems
                .asSequence()
                .filter { it.itemType == com.wanderlog.android.domain.model.ItineraryItemType.HOTEL }
                .filterNot { it.id in visibleItemIds }
                .filter { hotel -> hotelOverlapsDay(hotel, selectedDay.date) }
                .sortedWith(compareBy<ItineraryItem>({ it.startTime ?: "" }, { it.sortOrder }, { it.title.lowercase() }))
                .toList()
        }

        _state.update { it.copy(activeHotelsForSelectedDay = activeHotels) }
    }

    private fun hotelOverlapsDay(hotel: ItineraryItem, selectedDate: LocalDate): Boolean {
        val checkIn = parseItemDate(hotel.startTime) ?: return false
        val checkOut = parseItemDate(hotel.endTime) ?: checkIn
        val lastStayDate = if (checkOut.isAfter(checkIn)) checkOut.minusDays(1) else checkIn
        return !selectedDate.isBefore(checkIn) && !selectedDate.isAfter(lastStayDate)
    }

    private fun parseItemDate(value: String?): LocalDate? {
        val candidate = value?.trim().orEmpty()
        if (candidate.isBlank()) return null

        return runCatching { OffsetDateTime.parse(candidate).toLocalDate() }.getOrNull()
            ?: runCatching { ZonedDateTime.parse(candidate).toLocalDate() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(candidate).toLocalDate() }.getOrNull()
            ?: runCatching { LocalDate.parse(candidate) }.getOrNull()
    }

    fun deleteItem(item: ItineraryItem) {
        viewModelScope.launch {
            val importAttachmentIds = itineraryItemAttachmentRepository.getAttachmentIdsForItem(
                item.id,
                linkType = ItemAttachmentLinkType.IMPORT_SOURCE
            )
            val itemsToDelete = importAttachmentIds
                .flatMap { attachmentId ->
                    itineraryItemAttachmentRepository.getItemIdsForAttachment(
                        attachmentId,
                        linkType = ItemAttachmentLinkType.IMPORT_SOURCE
                    )
                }
                .distinct()
                .mapNotNull { itemId -> allTripItems.value.find { candidate -> candidate.id == itemId } }
                .ifEmpty { listOf(item) }

            val linkedExpenses = itemsToDelete
                .mapNotNull { linkedItem -> linkedItem.linkedExpenseId }
                .distinct()
                .mapNotNull(_state.value.linkedExpensesById::get)

            val legacyImportedExpenses = if (linkedExpenses.isEmpty()) {
                findLegacyImportedExpenses(itemsToDelete)
            } else {
                emptyList()
            }

            (linkedExpenses + legacyImportedExpenses)
                .distinctBy(Expense::id)
                .forEach { linkedExpense -> deleteExpense(linkedExpense) }

            itemsToDelete.forEach { linkedItem ->
                deleteItem.invoke(linkedItem)
            }

            itineraryItemAttachmentRepository.deleteLinksForItems(itemsToDelete.map(ItineraryItem::id))

            importAttachmentIds.distinct().forEach { attachmentId ->
                val attachment = attachmentRepository.getById(attachmentId)
                if (attachment != null) {
                    attachmentRepository.delete(attachment)
                }
            }
        }
    }

    private fun findLegacyImportedExpenses(items: List<ItineraryItem>): List<Expense> {
        val bookingRefs = items
            .mapNotNull { candidate -> candidate.bookingRef?.trim()?.takeIf { it.isNotBlank() } }
            .toSet()

        val hotelExpenseKeys = items
            .filter { it.itemType == com.wanderlog.android.domain.model.ItineraryItemType.HOTEL }
            .mapNotNull { candidate ->
                val startDate = parseItemDate(candidate.startTime) ?: return@mapNotNull null
                candidate.title.trim().takeIf { it.isNotBlank() }?.let { title -> title to startDate }
            }
            .toSet()

        if (bookingRefs.isEmpty() && hotelExpenseKeys.isEmpty()) {
            return emptyList()
        }

        return _state.value.linkedExpensesById.values.filter { expense ->
            when (expense.category) {
                ExpenseCategory.TRANSPORT -> {
                    bookingRefs.any { bookingRef ->
                        expense.notes?.contains("Booking reference: $bookingRef") == true
                    }
                }

                ExpenseCategory.ACCOMMODATION -> {
                    bookingRefs.any { bookingRef ->
                        expense.notes?.contains("Booking reference: $bookingRef") == true
                    } || hotelExpenseKeys.any { (title, date) ->
                        expense.title.trim() == title && expense.date == date
                    }
                }

                else -> false
            }
        }
    }

    fun reorderItems(items: List<ItineraryItem>) {
        viewModelScope.launch {
            items.forEachIndexed { index, item ->
                updateOrder(item.id, index)
            }
        }
    }
}
