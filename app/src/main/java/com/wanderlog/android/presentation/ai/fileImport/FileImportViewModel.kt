package com.wanderlog.android.presentation.ai.fileImport

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.DocumentHint
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.ItineraryItemType
import com.wanderlog.android.domain.model.ParsedBooking
import com.wanderlog.android.domain.model.Place
import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.repository.ItineraryRepository
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.domain.usecase.ai.ParseFileUseCase
import com.wanderlog.android.domain.usecase.ai.ParseTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class FileImportStep {
    data object Idle : FileImportStep()
    data object Parsing : FileImportStep()
    data class Review(val parsedBooking: ParsedBooking, val items: List<ItineraryItem>) : FileImportStep()
    data object Done : FileImportStep()
    data class Error(val message: String) : FileImportStep()
}

@HiltViewModel
class FileImportViewModel @Inject constructor(
    private val parseFile: ParseFileUseCase,
    private val parseText: ParseTextUseCase,
    private val tripRepository: TripRepository,
    private val itineraryRepository: ItineraryRepository
) : ViewModel() {

    private val _step = MutableStateFlow<FileImportStep>(FileImportStep.Idle)
    val step: StateFlow<FileImportStep> = _step.asStateFlow()

    val trips: StateFlow<List<Trip>> = tripRepository.getAllTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun parseUri(uri: Uri, tripId: String, hint: DocumentHint? = null) {
        _step.value = FileImportStep.Parsing
        viewModelScope.launch {
            runCatching { parseFile(uri, hint) }
                .onSuccess { booking ->
                    val days = tripRepository.getDaysForTrip(tripId)
                    val firstDayId = days.firstOrNull()?.id ?: ""
                    val items = bookingToItems(booking, tripId, firstDayId)
                    _step.value = FileImportStep.Review(booking, items)
                }
                .onFailure { e ->
                    _step.value = FileImportStep.Error(e.message ?: "Parse failed")
                }
        }
    }

    fun parseText(text: String, tripId: String, hint: DocumentHint? = null) {
        _step.value = FileImportStep.Parsing
        viewModelScope.launch {
            runCatching { parseText.invoke(text, hint) }
                .onSuccess { booking ->
                    val days = tripRepository.getDaysForTrip(tripId)
                    val firstDayId = days.firstOrNull()?.id ?: ""
                    val items = bookingToItems(booking, tripId, firstDayId)
                    _step.value = FileImportStep.Review(booking, items)
                }
                .onFailure { e ->
                    _step.value = FileImportStep.Error(e.message ?: "Parse failed")
                }
        }
    }

    fun commitItems(tripId: String, items: List<ItineraryItem>) {
        viewModelScope.launch {
            itineraryRepository.insertItems(items)
            _step.value = FileImportStep.Done
        }
    }

    fun reset() { _step.value = FileImportStep.Idle }

    private fun bookingToItems(booking: ParsedBooking, tripId: String, dayId: String): List<ItineraryItem> {
        val result = mutableListOf<ItineraryItem>()
        booking.flights.forEach { f ->
            result.add(ItineraryItem(
                id = UUID.randomUUID().toString(),
                tripDayId = dayId,
                tripId = tripId,
                title = "Flight ${f.flightNumber ?: ""} ${f.origin} → ${f.destination}".trim(),
                itemType = ItineraryItemType.FLIGHT,
                startTime = f.departureDateTime,
                bookingRef = f.bookingRef
            ))
        }
        booking.hotels.forEach { h ->
            result.add(ItineraryItem(
                id = UUID.randomUUID().toString(),
                tripDayId = dayId,
                tripId = tripId,
                title = h.name,
                itemType = ItineraryItemType.HOTEL,
                place = h.address?.let { Place(name = h.name, address = it) },
                startTime = h.checkIn,
                endTime = h.checkOut,
                bookingRef = h.bookingRef
            ))
        }
        booking.activities.forEach { a ->
            result.add(ItineraryItem(
                id = UUID.randomUUID().toString(),
                tripDayId = dayId,
                tripId = tripId,
                title = a.title,
                itemType = ItineraryItemType.ACTIVITY,
                place = a.location?.let { Place(name = it) },
                startTime = a.dateTime,
                notes = a.notes
            ))
        }
        return result
    }
}
