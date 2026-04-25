package com.wanderlog.android.presentation.trips.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.Lazy
import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.repository.PlacesRepository
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.domain.usecase.trip.CreateTripUseCase
import com.wanderlog.android.domain.usecase.trip.UpdateTripUseCase
import com.wanderlog.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class TripFormState(
    val tripId: String? = null,
    val name: String = "",
    val destination: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now().plusDays(6),
    val coverImageUri: String? = null,
    val budgetAmount: String = "",
    val currencyCode: String = "USD",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TripFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val placesRepository: Lazy<PlacesRepository>,
    private val createTrip: CreateTripUseCase,
    private val updateTrip: UpdateTripUseCase
) : ViewModel() {

    private var originalDestination: String? = null

    private val _state = MutableStateFlow(TripFormState())
    val state: StateFlow<TripFormState> = _state.asStateFlow()

    init {
        val tripId = savedStateHandle.get<String>(Screen.TripForm.ARG_TRIP_ID)
        if (tripId != null) {
            loadTrip(tripId)
        }
    }

    private fun loadTrip(tripId: String) {
        viewModelScope.launch {
            val trip = tripRepository.getTripById(tripId) ?: return@launch
            _state.update {
                it.copy(
                    tripId = trip.id,
                    name = trip.name,
                    destination = trip.destination,
                    startDate = trip.startDate,
                    endDate = trip.endDate,
                    coverImageUri = trip.coverImageUri,
                    budgetAmount = trip.budgetAmount?.toString() ?: "",
                    currencyCode = trip.currencyCode
                )
            }
            originalDestination = trip.destination
        }
    }

    fun onNameChange(name: String) = _state.update { it.copy(name = name) }
    fun onDestinationChange(dest: String) = _state.update { it.copy(destination = dest) }
    fun onStartDateChange(date: LocalDate) = _state.update {
        it.copy(startDate = date, endDate = if (date.isAfter(it.endDate)) date else it.endDate)
    }
    fun onEndDateChange(date: LocalDate) = _state.update { it.copy(endDate = date) }
    fun onBudgetChange(amount: String) = _state.update { it.copy(budgetAmount = amount) }
    fun onCurrencyChange(code: String) = _state.update { it.copy(currencyCode = code) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank() || s.destination.isBlank()) {
            _state.update { it.copy(error = "Name and destination are required.") }
            return
        }
        if (s.endDate.isBefore(s.startDate)) {
            _state.update { it.copy(error = "End date must be after start date.") }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val tripId = s.tripId ?: UUID.randomUUID().toString()
                val normalizedDestination = s.destination.trim()
                val existingCover = s.coverImageUri
                val coverImageUri = when {
                    s.tripId != null && normalizedDestination == originalDestination && !existingCover.isNullOrBlank() -> existingCover
                    else -> runCatching {
                        placesRepository.get().fetchDestinationCoverImage(
                            destination = normalizedDestination,
                            tripId = tripId
                        )
                    }.getOrNull()
                }
                val trip = Trip(
                    id = tripId,
                    name = s.name.trim(),
                    destination = normalizedDestination,
                    startDate = s.startDate,
                    endDate = s.endDate,
                    coverImageUri = coverImageUri,
                    budgetAmount = s.budgetAmount.toDoubleOrNull(),
                    currencyCode = s.currencyCode
                )
                if (s.tripId == null) createTrip(trip) else updateTrip(trip)
            }.onSuccess {
                _state.update { it.copy(isLoading = false, isSaved = true) }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
