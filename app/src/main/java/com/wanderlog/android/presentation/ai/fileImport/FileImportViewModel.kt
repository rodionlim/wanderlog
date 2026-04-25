package com.wanderlog.android.presentation.ai.fileImport

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.DocumentHint
import com.wanderlog.android.domain.model.Expense
import com.wanderlog.android.domain.model.ExpenseCategory
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.ItineraryItemType
import com.wanderlog.android.domain.model.ParsedBooking
import com.wanderlog.android.domain.model.ParsedFlight
import com.wanderlog.android.domain.model.Place
import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.model.TripDay
import com.wanderlog.android.domain.repository.AttachmentRepository
import com.wanderlog.android.domain.repository.ItineraryRepository
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.domain.usecase.ai.ParseFileUseCase
import com.wanderlog.android.domain.usecase.ai.ParseTextUseCase
import com.wanderlog.android.domain.usecase.expense.AddExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject

sealed class FileImportStep {
    data object Idle : FileImportStep()
    data object Parsing : FileImportStep()
    data class Review(
        val parsedBooking: ParsedBooking,
        val items: List<ItineraryItem>,
        val days: List<TripDay>,
        val storesLocalAttachment: Boolean
    ) : FileImportStep()
    data object Done : FileImportStep()
    data class Error(val message: String) : FileImportStep()
}

@HiltViewModel
class FileImportViewModel @Inject constructor(
    private val parseFile: ParseFileUseCase,
    private val parseText: ParseTextUseCase,
    private val tripRepository: TripRepository,
    private val attachmentRepository: AttachmentRepository,
    private val itineraryRepository: ItineraryRepository,
    private val addExpense: AddExpenseUseCase
) : ViewModel() {

    private var pendingSourceUri: Uri? = null

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
                    pendingSourceUri = uri
                    val items = bookingToItems(booking, tripId, days)
                    _step.value = FileImportStep.Review(booking, items, days, storesLocalAttachment = true)
                }
                .onFailure { e ->
                    pendingSourceUri = null
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
                    pendingSourceUri = null
                    val items = bookingToItems(booking, tripId, days)
                    _step.value = FileImportStep.Review(booking, items, days, storesLocalAttachment = false)
                }
                .onFailure { e ->
                    pendingSourceUri = null
                    _step.value = FileImportStep.Error(e.message ?: "Parse failed")
                }
        }
    }

    fun commitItems(tripId: String, items: List<ItineraryItem>) {
        viewModelScope.launch {
            runCatching {
                val review = _step.value as? FileImportStep.Review
                val trip = tripRepository.getTripById(tripId)
                val attachmentReference = pendingSourceUri
                    ?.let { uri -> attachmentRepository.importFromUri(tripId, uri, "Imported booking") }
                    ?.let { attachment -> "attachment://${attachment.id}" }

                val linkedItems = if (attachmentReference == null) items else {
                    items.map { item ->
                        item.copy(confirmationUrl = item.confirmationUrl ?: attachmentReference)
                    }
                }

                itineraryRepository.insertItems(linkedItems)

                review
                    ?.let { createImportedExpenses(it.parsedBooking, tripId, trip?.currencyCode ?: "USD") }
                    ?.forEach { expense -> addExpense(expense) }
            }
                .onSuccess {
                    pendingSourceUri = null
                    _step.value = FileImportStep.Done
                }
                .onFailure { e ->
                    _step.value = FileImportStep.Error(e.message ?: "Import failed")
                }
        }
    }

    fun updateReviewedItem(updatedItem: ItineraryItem) {
        val current = _step.value
        if (current !is FileImportStep.Review) return

        _step.value = current.copy(
            items = current.items.map { item ->
                if (item.id == updatedItem.id) updatedItem else item
            }
        )
    }

    fun reset() {
        pendingSourceUri = null
        _step.value = FileImportStep.Idle
    }

    private fun bookingToItems(booking: ParsedBooking, tripId: String, days: List<TripDay>): List<ItineraryItem> {
        val defaultDayId = days.firstOrNull()?.id ?: ""
        val result = mutableListOf<ItineraryItem>()
        booking.flights.forEach { f ->
            result.add(ItineraryItem(
                id = UUID.randomUUID().toString(),
                tripDayId = resolveTripDayId(days, f.departureDateTime ?: f.arrivalDateTime, defaultDayId),
                tripId = tripId,
                title = buildFlightTitle(f),
                itemType = ItineraryItemType.FLIGHT,
                startTime = f.departureDateTime,
                endTime = f.arrivalDateTime,
                notes = buildFlightNotes(f),
                bookingRef = f.bookingRef
            ))
        }
        booking.hotels.forEach { h ->
            result.add(ItineraryItem(
                id = UUID.randomUUID().toString(),
                tripDayId = resolveTripDayId(days, h.checkIn ?: h.checkOut, defaultDayId),
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
                tripDayId = resolveTripDayId(days, a.dateTime, defaultDayId),
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

    private fun buildFlightTitle(flight: ParsedFlight): String {
        val airlineAndNumber = listOfNotNull(flight.airline, flight.flightNumber).joinToString(" ").trim()
        val route = listOf(flight.origin, flight.destination).filter { it.isNotBlank() }.joinToString(" → ")
        return listOf(airlineAndNumber.ifBlank { null }, route.ifBlank { null })
            .filterNotNull()
            .joinToString("  •  ")
            .ifBlank { "Flight" }
    }

    private fun buildFlightNotes(flight: ParsedFlight): String? {
        val details = listOfNotNull(
            buildFlightEndpointLabel(
                label = "Departure",
                airport = flight.origin,
                terminal = flight.departureTerminal
            ),
            buildFlightEndpointLabel(
                label = "Arrival",
                airport = flight.destination,
                terminal = flight.arrivalTerminal
            ),
            flight.flightType?.let { "Type: $it" },
            flight.price?.let { "Cost: $it" }
        )
        return details.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    private fun buildFlightEndpointLabel(
        label: String,
        airport: String,
        terminal: String?
    ): String? {
        val normalizedAirport = airport.trim()
        val normalizedTerminal = terminal?.trim().orEmpty()
        if (normalizedAirport.isBlank() && normalizedTerminal.isBlank()) return null

        val suffix = normalizedTerminal.takeIf { it.isNotBlank() }?.let { " (Terminal: $it)" }.orEmpty()
        val value = normalizedAirport.ifBlank { "Unknown airport" }
        return "$label: $value$suffix"
    }

    private fun resolveTripDayId(days: List<TripDay>, dateTime: String?, fallbackDayId: String): String {
        val localDate = parseImportedDate(dateTime) ?: return fallbackDayId
        return days.firstOrNull { it.date == localDate }?.id ?: fallbackDayId
    }

    private fun createImportedExpenses(
        booking: ParsedBooking,
        tripId: String,
        fallbackCurrencyCode: String
    ): List<Expense> {
        return booking.flights
            .distinctBy { flight ->
                listOf(
                    flight.bookingRef.orEmpty(),
                    flight.price.orEmpty(),
                    flight.departureDateTime.orEmpty(),
                    flight.flightNumber.orEmpty()
                ).joinToString("|")
            }
            .mapNotNull { flight ->
                val parsedAmount = parseImportedAmount(flight.price) ?: return@mapNotNull null
                val expenseDate = parseImportedDate(flight.departureDateTime ?: flight.arrivalDateTime)
                Expense(
                    id = UUID.randomUUID().toString(),
                    tripId = tripId,
                    title = buildFlightExpenseTitle(flight),
                    amount = parsedAmount,
                    currencyCode = inferCurrencyCode(flight.price, fallbackCurrencyCode),
                    category = ExpenseCategory.TRANSPORT,
                    date = expenseDate,
                    notes = listOfNotNull(
                        flight.bookingRef?.let { "Booking reference: $it" },
                        flight.price?.let { "Imported total: $it" }
                    ).takeIf { it.isNotEmpty() }?.joinToString("\n")
                )
            }
    }

    private fun buildFlightExpenseTitle(flight: ParsedFlight): String {
        val route = listOf(flight.origin, flight.destination)
            .filter { it.isNotBlank() }
            .joinToString(" -> ")
        val label = listOfNotNull(flight.airline, flight.flightNumber)
            .joinToString(" ")
            .ifBlank { "Flight ticket" }
        return if (route.isBlank()) label else "$label ($route)"
    }

    private fun parseImportedAmount(priceText: String?): Double? {
        val candidate = priceText?.trim().orEmpty()
        if (candidate.isBlank()) return null

        val match = MONEY_REGEX.find(candidate)?.value ?: return null
        return match.replace(",", "").toDoubleOrNull()
    }

    private fun inferCurrencyCode(priceText: String?, fallbackCurrencyCode: String): String {
        val candidate = priceText?.trim().orEmpty().uppercase()
        CURRENCY_CODE_REGEX.find(candidate)?.value?.let { return it }

        return when {
            "$" in candidate -> fallbackCurrencyCode
            "€" in candidate -> "EUR"
            "£" in candidate -> "GBP"
            "¥" in candidate || "JPY" in candidate -> "JPY"
            else -> fallbackCurrencyCode
        }
    }

    private fun parseImportedDate(value: String?): LocalDate? {
        val candidate = value?.trim().orEmpty()
        if (candidate.isBlank()) return null

        return runCatching { OffsetDateTime.parse(candidate).toLocalDate() }.getOrNull()
            ?: runCatching { ZonedDateTime.parse(candidate).toLocalDate() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(candidate).toLocalDate() }.getOrNull()
            ?: runCatching { LocalDate.parse(candidate) }.getOrNull()
    }

    companion object {
        private val MONEY_REGEX = Regex("""\d{1,3}(?:,\d{3})*(?:\.\d+)?|\d+(?:\.\d+)?""")
        private val CURRENCY_CODE_REGEX = Regex("""\b[A-Z]{3}\b""")
    }
}
