package com.wanderlog.android.presentation.ai.generate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.TripDay
import com.wanderlog.android.domain.repository.ItineraryRepository
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.domain.usecase.ai.GenerateItineraryUseCase
import com.wanderlog.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AiGenerateState(
    val destination: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now().plusDays(6),
    val preferences: String = "",
    val travellers: String = "1",
    val isLoading: Boolean = false,
    val generatedDays: List<TripDay> = emptyList(),
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

    init {
        viewModelScope.launch {
            val trip = tripRepository.getTripById(tripId) ?: return@launch
            _state.update {
                it.copy(
                    destination = trip.destination,
                    startDate = trip.startDate,
                    endDate = trip.endDate
                )
            }
        }
    }

    fun onDestinationChange(v: String) = _state.update { it.copy(destination = v) }
    fun onPreferencesChange(v: String) = _state.update { it.copy(preferences = v) }
    fun onTravellersChange(v: String) = _state.update { it.copy(travellers = v) }

    fun generate() {
        val s = _state.value
        if (s.destination.isBlank()) { _state.update { it.copy(error = "Destination required") }; return }
        _state.update { it.copy(isLoading = true, error = null, generatedDays = emptyList()) }
        viewModelScope.launch {
            runCatching {
                generateItinerary(
                    destination = s.destination,
                    startDate = s.startDate.toString(),
                    endDate = s.endDate.toString(),
                    preferences = s.preferences.ifBlank { "balanced mix of culture and food" },
                    travellers = s.travellers.toIntOrNull() ?: 1
                )
            }.onSuccess { days ->
                _state.update { it.copy(isLoading = false, generatedDays = days) }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message ?: "Generation failed") }
            }
        }
    }

    fun commitToItinerary() {
        val days = _state.value.generatedDays
        if (days.isEmpty()) return
        viewModelScope.launch {
            // Replace existing days
            tripRepository.deleteDaysForTrip(tripId)
            tripRepository.createDaysForTrip(days.map { it.copy(tripId = tripId) })
            val allItems: List<ItineraryItem> = days.flatMap { day ->
                day.items.map { item -> item.copy(tripId = tripId, tripDayId = day.id) }
            }
            itineraryRepository.insertItems(allItems)
            _state.update { it.copy(committed = true) }
        }
    }
}
