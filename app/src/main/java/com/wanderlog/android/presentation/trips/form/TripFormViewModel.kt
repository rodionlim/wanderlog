package com.wanderlog.android.presentation.trips.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.data.local.preferences.TravellerDefaultsStore
import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.model.TravellerProfile
import com.wanderlog.android.domain.repository.PlacesRepository
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.domain.usecase.packing.CopyPackingItemsFromTripUseCase
import com.wanderlog.android.domain.usecase.trip.CreateTripUseCase
import com.wanderlog.android.domain.usecase.trip.UpdateTripUseCase
import com.wanderlog.android.presentation.navigation.Screen
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class TravellerFormProfile(
    val name: String = "",
    val age: String = ""
)

data class PackingSourceTripOption(
    val id: String,
    val label: String
)

data class TripFormState(
    val tripId: String? = null,
    val name: String = "",
    val destination: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now().plusDays(6),
    val coverImageUri: String? = null,
    val budgetAmount: String = "",
    val currencyCode: String = "SGD",
    val travellerCount: String = "1",
    val travellerProfiles: List<TravellerFormProfile> = listOf(TravellerFormProfile(name = "Traveller 1")),
    val availablePackingSourceTrips: List<PackingSourceTripOption> = emptyList(),
    val selectedPackingSourceTripId: String? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TripFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val placesRepository: Lazy<PlacesRepository>,
    private val travellerDefaultsStore: TravellerDefaultsStore,
    private val copyPackingItemsFromTrip: CopyPackingItemsFromTripUseCase,
    private val createTrip: CreateTripUseCase,
    private val updateTrip: UpdateTripUseCase
) : ViewModel() {

    private var originalDestination: String? = null
    private var savedDefaultTravellerProfiles: List<TravellerProfile> = loadSavedTravellerDefaults()
    private var availablePackingSourceTrips: List<Trip> = emptyList()

    private val _state = MutableStateFlow(TripFormState())
    val state: StateFlow<TripFormState> = _state.asStateFlow()

    init {
        val tripId = savedStateHandle.get<String>(Screen.TripForm.ARG_TRIP_ID)
        if (tripId != null) {
            loadTrip(tripId)
        } else {
            applySavedTravellerDefaults()
            observePackingSourceTrips()
        }
    }

    private fun observePackingSourceTrips() {
        viewModelScope.launch {
            tripRepository.getAllTrips().collect { trips ->
                availablePackingSourceTrips = trips.sortedByDescending(Trip::startDate)
                _state.update { current ->
                    val selectedId = current.selectedPackingSourceTripId
                        ?.takeIf { selectedTripId -> availablePackingSourceTrips.any { it.id == selectedTripId } }
                    current.copy(
                        availablePackingSourceTrips = availablePackingSourceTrips.map { trip ->
                            PackingSourceTripOption(
                                id = trip.id,
                                label = trip.packingSourceLabel()
                            )
                        },
                        selectedPackingSourceTripId = selectedId
                    )
                }
            }
        }
    }

    private fun applySavedTravellerDefaults() {
        _state.update {
            it.copy(
                travellerCount = savedDefaultTravellerProfiles.size.toString(),
                travellerProfiles = savedDefaultTravellerProfiles.toFormProfiles()
            )
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
                    currencyCode = trip.currencyCode,
                    travellerCount = trip.travellerProfiles.ifEmpty { savedDefaultTravellerProfiles }.size.toString(),
                    travellerProfiles = trip.travellerProfiles.ifEmpty { savedDefaultTravellerProfiles }.toFormProfiles()
                )
            }
            originalDestination = trip.destination
        }
    }

    fun onNameChange(name: String) = _state.update { it.copy(name = name) }
    fun onDestinationChange(dest: String) = _state.update { it.copy(destination = dest) }
    fun onDateRangeChange(startDate: LocalDate, endDate: LocalDate) = _state.update {
        it.copy(startDate = startDate, endDate = endDate)
    }
    fun onBudgetChange(amount: String) = _state.update { it.copy(budgetAmount = amount) }
    fun onCurrencyChange(code: String) = _state.update { it.copy(currencyCode = code) }
    fun onPackingSourceTripChange(tripId: String?) = _state.update {
        it.copy(selectedPackingSourceTripId = tripId)
    }

    fun onTravellerCountChange(count: String) {
        val digitsOnly = count.filter(Char::isDigit)
        if (digitsOnly != count) return

        _state.update { current ->
            val requestedCount = digitsOnly.toIntOrNull() ?: 0
            current.copy(
                travellerCount = digitsOnly,
                travellerProfiles = current.travellerProfiles.resizeTravellerProfiles(
                    targetSize = requestedCount,
                    defaults = savedDefaultTravellerProfiles.toFormProfiles()
                )
            )
        }
    }

    fun onTravellerNameChange(index: Int, name: String) = _state.update { current ->
        current.copy(
            travellerProfiles = current.travellerProfiles.toMutableList().also { profiles ->
                if (index in profiles.indices) {
                    profiles[index] = profiles[index].copy(name = name)
                }
            }
        )
    }

    fun onTravellerAgeChange(index: Int, age: String) {
        val digitsOnly = age.filter(Char::isDigit)
        if (digitsOnly != age) return

        _state.update { current ->
            current.copy(
                travellerProfiles = current.travellerProfiles.toMutableList().also { profiles ->
                    if (index in profiles.indices) {
                        profiles[index] = profiles[index].copy(age = digitsOnly)
                    }
                }
            )
        }
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank() || s.destination.isBlank()) {
            _state.update { it.copy(error = "Name and destination are required.") }
            return
        }
        val travellerCount = s.travellerCount.toIntOrNull()
        if (travellerCount == null || travellerCount <= 0) {
            _state.update { it.copy(error = "Add at least one traveller.") }
            return
        }
        val travellerProfiles = s.travellerProfiles
            .take(travellerCount)
            .mapIndexed { index, value ->
                TravellerProfile(
                    name = value.name.trim().ifBlank { "Traveller ${index + 1}" },
                    age = value.age.toIntOrNull()
                )
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
                    currencyCode = s.currencyCode,
                    travellerProfiles = travellerProfiles
                )
                if (s.tripId == null) {
                    createTrip(trip)
                    selectedPackingSourceTrip(s.selectedPackingSourceTripId)?.let { sourceTrip ->
                        copyPackingItemsFromTrip(
                            sourceTripId = sourceTrip.id,
                            targetTripId = trip.id,
                            travellerNameMap = sourceTrip.travellerNameMapTo(trip)
                        )
                    }
                } else {
                    updateTrip(trip)
                }
            }.onSuccess {
                savedDefaultTravellerProfiles = travellerProfiles.ifEmpty { listOf(TravellerProfile(name = "Traveller 1")) }
                travellerDefaultsStore.saveTravellerProfiles(savedDefaultTravellerProfiles)
                _state.update { it.copy(isLoading = false, isSaved = true) }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadSavedTravellerDefaults(): List<TravellerProfile> =
        travellerDefaultsStore.getTravellerProfiles()
            .ifEmpty { listOf(TravellerProfile(name = "Traveller 1")) }

    private fun selectedPackingSourceTrip(selectedTripId: String?): Trip? =
        availablePackingSourceTrips.firstOrNull { it.id == selectedTripId }
}

private fun List<TravellerProfile>.toFormProfiles(): List<TravellerFormProfile> =
    map { TravellerFormProfile(name = it.name, age = it.age?.toString().orEmpty()) }

private fun Trip.packingSourceLabel(): String = buildString {
    append(name)
    if (destination.isNotBlank()) {
        append(" • ")
        append(destination)
    }
}

private fun Trip.travellerNameMapTo(targetTrip: Trip): Map<String, String?> {
    if (travellerProfiles.isEmpty() || targetTrip.travellerProfiles.isEmpty()) return emptyMap()

    val targetNames = targetTrip.travellerProfiles.map(TravellerProfile::name)
    return travellerProfiles.mapIndexedNotNull { index, travellerProfile ->
        val sourceName = travellerProfile.name.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
        val mappedName = targetNames.getOrNull(index)
            ?: targetNames.firstOrNull()
        sourceName to mappedName
    }.toMap()
}

private fun List<TravellerFormProfile>.resizeTravellerProfiles(
    targetSize: Int,
    defaults: List<TravellerFormProfile>
): List<TravellerFormProfile> {
    if (targetSize <= 0) return emptyList()
    val resized = toMutableList()
    while (resized.size < targetSize) {
        resized += defaults.getOrNull(resized.size)
            ?: TravellerFormProfile(name = "Traveller ${resized.size + 1}")
    }
    return resized.take(targetSize)
}
