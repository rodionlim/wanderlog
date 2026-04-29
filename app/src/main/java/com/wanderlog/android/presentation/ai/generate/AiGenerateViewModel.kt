package com.wanderlog.android.presentation.ai.generate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.core.util.PromptTokenEstimator
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.TravellerProfile
import com.wanderlog.android.domain.model.TripDay
import com.wanderlog.android.domain.repository.ItineraryRepository
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.domain.usecase.ai.GenerateItineraryUseCase
import com.wanderlog.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class AiGenerateMode {
    FULL_TRIP,
    UPDATE_MULTIPLE_DAYS
}

data class AiGenerateState(
    val destination: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now().plusDays(6),
    val availableDays: List<TripDay> = emptyList(),
    val mode: AiGenerateMode = AiGenerateMode.FULL_TRIP,
    val preferences: String = "",
    val isLoading: Boolean = false,
    val generatedDays: List<TripDay> = emptyList(),
    val selectedGeneratedItemIds: Set<String> = emptySet(),
    val overlappingGeneratedItemIds: Set<String> = emptySet(),
    val estimatedContextTokens: Int = 0,
    val estimatedInputTokens: Int = 0,
    val estimatedTotalTokens: Int = 0,
    val error: String? = null,
    val committed: Boolean = false
)

@HiltViewModel
class AiGenerateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val itineraryRepository: ItineraryRepository,
    private val generateItinerary: GenerateItineraryUseCase
) : ViewModel() {

    private val tripId = savedStateHandle.get<String>(Screen.AiGenerate.ARG_TRIP_ID)!!

    private val _state = MutableStateFlow(AiGenerateState())
    val state: StateFlow<AiGenerateState> = _state.asStateFlow()
    private var availableDaysWithItems: List<TripDay> = emptyList()
    private var tripTravellerProfiles: List<TravellerProfile> = emptyList()

    init {
        viewModelScope.launch {
            val trip = tripRepository.getTripById(tripId) ?: return@launch
            val days = tripRepository.getDaysForTrip(tripId)
            tripTravellerProfiles = trip.travellerProfiles
            availableDaysWithItems = days.map { day ->
                day.copy(items = itineraryRepository.getItemsForDay(day.id).first())
            }
            _state.update {
                it.copy(
                    destination = trip.destination,
                    startDate = trip.startDate,
                    endDate = trip.endDate,
                    availableDays = days
                )
            }
            refreshPromptEstimates()
        }
    }

    fun onDestinationChange(v: String) {
        _state.update { it.copy(destination = v) }
        refreshPromptEstimates()
    }

    fun onModeChange(v: AiGenerateMode) {
        _state.update {
            it.copy(
                mode = v,
                generatedDays = emptyList(),
                selectedGeneratedItemIds = emptySet(),
                overlappingGeneratedItemIds = emptySet(),
                error = null
            )
        }
        refreshPromptEstimates()
    }

    fun onPreferencesChange(v: String) {
        _state.update { it.copy(preferences = v) }
        refreshPromptEstimates()
    }

    fun onGeneratedItemSelectionChanged(itemId: String, selected: Boolean) {
        _state.update { current ->
            val updated = current.selectedGeneratedItemIds.toMutableSet()
            if (selected) updated += itemId else updated -= itemId
            current.copy(selectedGeneratedItemIds = updated)
        }
    }

    fun selectAllGeneratedItems() {
        val allIds = _state.value.generatedDays.flatMap { day -> day.items.map(ItineraryItem::id) }.toSet()
        _state.update { it.copy(selectedGeneratedItemIds = allIds) }
    }

    fun clearGeneratedItemSelection() {
        _state.update { it.copy(selectedGeneratedItemIds = emptySet()) }
    }

    fun generate() {
        val s = _state.value
        if (s.destination.isBlank()) { _state.update { it.copy(error = "Destination required") }; return }
        if (s.mode != AiGenerateMode.FULL_TRIP && s.preferences.isBlank()) {
            _state.update { it.copy(error = "Tell AI what should change and it can choose the best existing days") }
            return
        }
        _state.update {
            it.copy(
                isLoading = true,
                error = null,
                generatedDays = emptyList(),
                selectedGeneratedItemIds = emptySet(),
                overlappingGeneratedItemIds = emptySet()
            )
        }
        viewModelScope.launch {
            runCatching {
                val existingDays = if (s.mode == AiGenerateMode.UPDATE_MULTIPLE_DAYS) {
                    s.availableDays.map { day ->
                        day.copy(items = itineraryRepository.getItemsForDay(day.id).first())
                    }
                } else {
                    emptyList()
                }
                generateItinerary(
                    destination = s.destination,
                    startDate = s.startDate.toString(),
                    endDate = s.endDate.toString(),
                    preferences = s.preferences.ifBlank { "balanced mix of culture and food" },
                    travellerProfiles = tripTravellerProfiles,
                    updatePrompt = if (s.mode == AiGenerateMode.FULL_TRIP) null else s.preferences,
                    existingDays = existingDays
                )
            }.onSuccess { days ->
                val overlapIds = if (s.mode == AiGenerateMode.UPDATE_MULTIPLE_DAYS) {
                    buildOverlappingGeneratedItemIds(days, availableDaysWithItems)
                } else {
                    emptySet()
                }
                val defaultSelectedIds = days
                    .flatMap { day -> day.items }
                    .map(ItineraryItem::id)
                    .filterNot { it in overlapIds }
                    .toSet()

                _state.update {
                    it.copy(
                        isLoading = false,
                        generatedDays = days,
                        selectedGeneratedItemIds = defaultSelectedIds,
                        overlappingGeneratedItemIds = overlapIds
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message ?: "Generation failed") }
            }
        }
    }

    fun commitToItinerary() {
        val state = _state.value
        val days = state.generatedDays.map { day ->
            day.copy(items = day.items.filter { it.id in state.selectedGeneratedItemIds })
        }
        if (days.isEmpty()) return
        viewModelScope.launch {
            when (state.mode) {
                AiGenerateMode.UPDATE_MULTIPLE_DAYS -> {
                    val matchedItems = buildList {
                        days.forEach { generatedDay ->
                            if (generatedDay.items.isEmpty()) return@forEach
                            val existingDay = state.availableDays.firstOrNull {
                                it.date == generatedDay.date || it.dayNumber == generatedDay.dayNumber
                            } ?: return@forEach

                            val existingItems = itineraryRepository.getItemsForDay(existingDay.id).first()
                            val startingSortOrder = existingItems.maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
                            addAll(
                                generatedDay.items.mapIndexed { index, item ->
                                    item.copy(
                                        tripId = tripId,
                                        tripDayId = existingDay.id,
                                        sortOrder = startingSortOrder + index
                                    )
                                }
                            )
                        }
                    }

                    if (matchedItems.isEmpty()) {
                        _state.update {
                            it.copy(error = "AI did not return any updates for existing trip days")
                        }
                        return@launch
                    }

                    itineraryRepository.insertItems(matchedItems)
                }

                AiGenerateMode.FULL_TRIP -> {
                    if (days.none { it.items.isNotEmpty() }) {
                        _state.update { it.copy(error = "Select at least one generated item to add") }
                        return@launch
                    }
                    tripRepository.deleteDaysForTrip(tripId)
                    tripRepository.createDaysForTrip(days.map { it.copy(tripId = tripId) })
                    val allItems: List<ItineraryItem> = days.flatMap { day ->
                        day.items.map { item -> item.copy(tripId = tripId, tripDayId = day.id) }
                    }
                    itineraryRepository.insertItems(allItems)
                }
            }
            _state.update { it.copy(committed = true) }
        }
    }

    private fun refreshPromptEstimates() {
        val state = _state.value
        val estimate = when (state.mode) {
            AiGenerateMode.FULL_TRIP -> estimateFullTripPromptTokens(state)
            AiGenerateMode.UPDATE_MULTIPLE_DAYS -> estimateUpdateDaysPromptTokens(state, availableDaysWithItems)
        }

        _state.update {
            it.copy(
                estimatedContextTokens = estimate.contextTokens,
                estimatedInputTokens = estimate.inputTokens,
                estimatedTotalTokens = estimate.totalTokens
            )
        }
    }

    private fun estimateFullTripPromptTokens(state: AiGenerateState): PromptTokenEstimate {
        val systemPrompt = "You are a travel planning assistant. Always respond with valid JSON only. Never include markdown code blocks or any text outside the JSON."
        val travellerCount = tripTravellerProfiles.size.coerceAtLeast(1)
        val travellerContext = tripTravellerProfiles.joinToString(", ") { it.displayName }
            .ifBlank { "No named travellers saved" }
        val contextPrompt = """
            Generate a day-by-day travel itinerary. Output ONLY valid JSON with this schema:
            {
              \"days\": [
                {
                  \"day_number\": 1,
                  \"date\": \"YYYY-MM-DD\",
                  \"items\": [
                    {
                      \"title\": \"...\",
                      \"type\": \"PLACE|HOTEL|ACTIVITY|TRANSPORT|FLIGHT|NOTE\",
                      \"location\": \"...\",
                      \"start_time\": \"HH:mm\",
                      \"end_time\": \"HH:mm\",
                      \"notes\": \"...\"
                    }
                  ]
                }
              ]
            }

            Trip details:
            - Destination: ${state.destination}
            - Start date: ${state.startDate}
            - End date: ${state.endDate}
            - Traveller count: $travellerCount
            - Saved travellers: $travellerContext
        """.trimIndent()
        val inputText = state.preferences.ifBlank { "balanced mix of culture and food" }
        val contextTokens = PromptTokenEstimator.estimate("$systemPrompt\n\n$contextPrompt")
        val inputTokens = PromptTokenEstimator.estimate(inputText)
        return PromptTokenEstimate(
            contextTokens = contextTokens,
            inputTokens = inputTokens,
            totalTokens = contextTokens + inputTokens
        )
    }

    private fun estimateUpdateDaysPromptTokens(
        state: AiGenerateState,
        existingDays: List<TripDay>
    ): PromptTokenEstimate {
        val systemPrompt = "You are a travel planning assistant. Always respond with valid JSON only. Never include markdown code blocks or any text outside the JSON."
        val travellerCount = tripTravellerProfiles.size.coerceAtLeast(1)
        val travellerContext = tripTravellerProfiles.joinToString(", ") { it.displayName }
            .ifBlank { "No named travellers saved" }
        val daysText = existingDays
            .sortedBy { it.dayNumber }
            .joinToString("\n\n") { day ->
                """
                Day ${day.dayNumber} - ${day.date}
                Existing items:
                ${formatExistingItemsForEstimate(day.items)}
                """.trimIndent()
            }
            .ifBlank { "No existing trip days yet." }

        val contextPrompt = """
            Update an existing itinerary by choosing which existing days should receive new items.
            Output ONLY valid JSON using this schema:
            {
              \"days\": [
                {
                  \"day_number\": 1,
                  \"date\": \"YYYY-MM-DD\",
                  \"items\": [
                    {
                      \"title\": \"...\",
                      \"type\": \"PLACE|HOTEL|ACTIVITY|TRANSPORT|FLIGHT|NOTE\",
                      \"location\": \"...\",
                      \"start_time\": \"HH:mm\",
                      \"end_time\": \"HH:mm\",
                      \"notes\": \"...\"
                    }
                  ]
                }
              ]
            }

            Important rules:
            - Choose only from the existing trip days listed below.
            - Return only the subset of days that should receive new items.
            - Return only new itinerary items to add; do not repeat unchanged existing items.
            - Use the provided day date and day number exactly.
            - Spread additions across multiple days when that fits the request better than forcing everything onto one day.
            - Avoid obvious duplicates with existing items.
            - Choose realistic visit times and durations when the user does not specify them.

            Trip details:
            - Destination: ${state.destination}
            - Traveller count: $travellerCount
            - Saved travellers: $travellerContext

            Existing trip days:
            $daysText

            User request:
        """.trimIndent()

        val inputText = state.preferences
        val contextTokens = PromptTokenEstimator.estimate("$systemPrompt\n\n$contextPrompt")
        val inputTokens = PromptTokenEstimator.estimate(inputText)
        return PromptTokenEstimate(
            contextTokens = contextTokens,
            inputTokens = inputTokens,
            totalTokens = contextTokens + inputTokens
        )
    }

    private fun formatExistingItemsForEstimate(items: List<ItineraryItem>): String {
        return items
            .sortedBy { it.sortOrder }
            .joinToString("\n") { item ->
                buildString {
                    append("- ")
                    item.startTime?.takeIf { it.isNotBlank() }?.let {
                        append("[$it")
                        item.endTime?.takeIf { value -> value.isNotBlank() }?.let { end -> append("-$end") }
                        append("] ")
                    }
                    append(item.title)
                    item.place?.name?.takeIf { it.isNotBlank() }?.let { append(" @ $it") }
                    item.notes?.takeIf { it.isNotBlank() }?.let { append(" — $it") }
                }
            }
            .ifBlank { "- No items yet for this day." }
    }

    private fun buildOverlappingGeneratedItemIds(
        generatedDays: List<TripDay>,
        existingDays: List<TripDay>
    ): Set<String> {
        val existingByDayKey = existingDays.associateBy { dayKey(it.dayNumber, it.date.toString()) }
        return buildSet {
            generatedDays.forEach { generatedDay ->
                val existingItems = existingByDayKey[dayKey(generatedDay.dayNumber, generatedDay.date.toString())]?.items.orEmpty()
                generatedDay.items
                    .filter { generatedItem -> isPotentialOverlap(generatedItem, existingItems) }
                    .forEach { generatedItem -> add(generatedItem.id) }
            }
        }
    }

    private fun isPotentialOverlap(candidate: ItineraryItem, existingItems: List<ItineraryItem>): Boolean {
        val candidateTitle = candidate.title.normalizedForComparison()
        val candidateStart = candidate.startTime.normalizedForComparison()
        val candidatePlace = listOfNotNull(candidate.place?.name, candidate.place?.address)
            .joinToString(" ")
            .normalizedForComparison()

        return existingItems.any { existingItem ->
            val titleMatches = candidateTitle.isNotBlank() && candidateTitle == existingItem.title.normalizedForComparison()
            if (!titleMatches) return@any false

            val existingStart = existingItem.startTime.normalizedForComparison()
            val existingPlace = listOfNotNull(existingItem.place?.name, existingItem.place?.address)
                .joinToString(" ")
                .normalizedForComparison()

            val timeMatches = candidateStart.isNotBlank() && candidateStart == existingStart
            val placeMatches = candidatePlace.isNotBlank() && candidatePlace == existingPlace
            val fallbackMatches = candidateStart.isBlank() && existingStart.isBlank()

            timeMatches || placeMatches || fallbackMatches
        }
    }

    private fun String?.normalizedForComparison(): String =
        this.orEmpty().trim().lowercase().replace(Regex("[^a-z0-9]+"), " ")

    private fun dayKey(dayNumber: Int, date: String): String = "$dayNumber|$date"
}

private data class PromptTokenEstimate(
    val contextTokens: Int,
    val inputTokens: Int,
    val totalTokens: Int
)
