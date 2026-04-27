package com.wanderlog.android.presentation.ai.fileImport

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.DocumentHint
import com.wanderlog.android.domain.model.Expense
import com.wanderlog.android.domain.model.ExpenseCategory
import com.wanderlog.android.domain.model.ItemAttachmentLinkType
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.ItineraryItemType
import com.wanderlog.android.domain.model.ParsedBooking
import com.wanderlog.android.domain.model.ParsedFlight
import com.wanderlog.android.domain.model.Place
import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.model.TripDay
import com.wanderlog.android.domain.repository.AttachmentRepository
import com.wanderlog.android.domain.repository.ItineraryRepository
import com.wanderlog.android.domain.repository.ItineraryItemAttachmentRepository
import com.wanderlog.android.domain.repository.PlacesRepository
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.core.util.ImportedMoneyParser
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
    private val itineraryItemAttachmentRepository: ItineraryItemAttachmentRepository,
    private val placesRepository: PlacesRepository,
    private val addExpense: AddExpenseUseCase
) : ViewModel() {

    private var pendingSourceUris: List<Uri> = emptyList()

    private val _step = MutableStateFlow<FileImportStep>(FileImportStep.Idle)
    val step: StateFlow<FileImportStep> = _step.asStateFlow()

    val trips: StateFlow<List<Trip>> = tripRepository.getAllTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun parseUri(
        uri: Uri,
        tripId: String,
        hint: DocumentHint? = null,
        rasterizePdfAsImages: Boolean = false
    ) {
        parseUris(listOf(uri), tripId, hint, rasterizePdfAsImages)
    }

    fun parseUris(
        uris: List<Uri>,
        tripId: String,
        hint: DocumentHint? = null,
        rasterizePdfAsImages: Boolean = false
    ) {
        if (uris.isEmpty()) return
        _step.value = FileImportStep.Parsing
        viewModelScope.launch {
            runCatching { parseFile(uris, hint, rasterizePdfAsImages) }
                .onSuccess { booking ->
                    val days = tripRepository.getDaysForTrip(tripId)
                    val trip = tripRepository.getTripById(tripId)
                    pendingSourceUris = uris
                    val items = bookingToItems(booking, tripId, days, trip?.currencyCode ?: "USD")
                    _step.value = FileImportStep.Review(booking, items, days, storesLocalAttachment = true)
                }
                .onFailure { e ->
                    pendingSourceUris = emptyList()
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
                    val trip = tripRepository.getTripById(tripId)
                    pendingSourceUris = emptyList()
                    val items = bookingToItems(booking, tripId, days, trip?.currencyCode ?: "USD")
                    _step.value = FileImportStep.Review(booking, items, days, storesLocalAttachment = false)
                }
                .onFailure { e ->
                    pendingSourceUris = emptyList()
                    _step.value = FileImportStep.Error(e.message ?: "Parse failed")
                }
        }
    }

    fun commitItems(tripId: String, items: List<ItineraryItem>) {
        viewModelScope.launch {
            runCatching {
                val review = _step.value as? FileImportStep.Review
                val trip = tripRepository.getTripById(tripId)
                val importedAttachments = pendingSourceUris.mapIndexed { index, uri ->
                    val label = if (pendingSourceUris.size == 1) {
                        "Imported booking"
                    } else {
                        "Imported booking ${index + 1}"
                    }
                    attachmentRepository.importFromUri(tripId, uri, label)
                }
                itineraryRepository.insertItems(items)
                importedAttachments.forEach { attachment ->
                    itineraryItemAttachmentRepository.addAttachmentToItems(
                        tripId = tripId,
                        itemIds = items.map(ItineraryItem::id),
                        attachmentId = attachment.id,
                        linkType = ItemAttachmentLinkType.IMPORT_SOURCE
                    )
                }

                review
                    ?.let { createImportedExpenses(it.parsedBooking, tripId, trip?.currencyCode ?: "USD") }
                    ?.forEach { expense -> addExpense(expense) }
            }
                .onSuccess {
                    pendingSourceUris = emptyList()
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
        pendingSourceUris = emptyList()
        _step.value = FileImportStep.Idle
    }

    private suspend fun bookingToItems(
        booking: ParsedBooking,
        tripId: String,
        days: List<TripDay>,
        fallbackCurrencyCode: String
    ): List<ItineraryItem> {
        val defaultDayId = days.firstOrNull()?.id ?: ""
        val placeCache = mutableMapOf<String, Place?>()
        val linkedExpenseIdsByFlightIndex = buildImportedFlightExpenseCandidates(booking, fallbackCurrencyCode)
            .associate { it.flightIndex to it.expenseId }
        val linkedExpenseIdsByHotelIndex = buildImportedHotelExpenseCandidates(booking, fallbackCurrencyCode)
            .mapIndexed { index, candidate -> index to candidate.expenseId }
            .toMap()
        val result = mutableListOf<ItineraryItem>()
        booking.flights.forEachIndexed { index, f ->
            result.add(ItineraryItem(
                id = UUID.randomUUID().toString(),
                tripDayId = resolveTripDayId(days, f.departureDateTime ?: f.arrivalDateTime, defaultDayId),
                tripId = tripId,
                title = buildFlightTitle(f),
                itemType = ItineraryItemType.FLIGHT,
                place = buildFlightPlace(f, placeCache),
                startTime = f.departureDateTime,
                endTime = f.arrivalDateTime,
                notes = buildFlightNotes(f),
                bookingRef = f.bookingRef,
                linkedExpenseId = linkedExpenseIdsByFlightIndex[index]
            ))
        }
        booking.hotels.forEachIndexed { index, h ->
            result.add(ItineraryItem(
                id = UUID.randomUUID().toString(),
                tripDayId = resolveTripDayId(days, h.checkIn ?: h.checkOut, defaultDayId),
                tripId = tripId,
                title = h.name,
                itemType = ItineraryItemType.HOTEL,
                place = h.address?.let { Place(name = h.name, address = it) },
                startTime = h.checkIn,
                endTime = h.checkOut,
                notes = buildHotelNotes(h),
                bookingRef = h.bookingRef,
                linkedExpenseId = linkedExpenseIdsByHotelIndex[index]
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

    private suspend fun buildFlightPlace(
        flight: ParsedFlight,
        placeCache: MutableMap<String, Place?>
    ): Place? {
        val destination = flight.destination.trim()
        val origin = flight.origin.trim()
        val inferredName = destination.ifBlank { origin }
        if (inferredName.isBlank()) return null

        val queryCandidates = buildList {
            if (!destination.isBlank()) {
                if (!destination.contains("airport", ignoreCase = true)) add("$destination airport")
                add(destination)
            }
            if (origin.isNotBlank() && origin != destination) {
                if (!origin.contains("airport", ignoreCase = true)) add("$origin airport")
                add(origin)
            }
        }.distinct()

        for (query in queryCandidates) {
            if (query.isBlank()) continue
            val cached = placeCache[query]
            if (cached != null || placeCache.containsKey(query)) {
                return cached ?: Place(name = inferredName)
            }

            val resolved = runCatching {
                val match = placesRepository.searchPlaces(query, null).firstOrNull() ?: return@runCatching null
                match.placeId?.let { placesRepository.fetchPlaceDetails(it) } ?: match
            }.getOrNull()

            placeCache[query] = resolved
            if (resolved != null) return resolved
        }

        return Place(name = inferredName)
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

    private fun buildHotelNotes(hotel: com.wanderlog.android.domain.model.ParsedHotel): String? {
        return listOfNotNull(
            hotel.hostPhone?.takeIf { it.isNotBlank() }?.let { "Host phone: $it" }
        ).takeIf { it.isNotEmpty() }?.joinToString("\n")
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
        val flightExpenses = buildImportedFlightExpenseCandidates(booking, fallbackCurrencyCode)
            .groupBy(ImportedFlightExpenseCandidate::groupKey)
            .values
            .map { candidates ->
                val representative = candidates.first()
                val expenseDate = parseImportedDate(
                    representative.flight.departureDateTime ?: representative.flight.arrivalDateTime
                )
                Expense(
                    id = representative.expenseId,
                    tripId = tripId,
                    title = buildFlightExpenseTitle(candidates.map { it.flight }),
                    amount = representative.amount,
                    currencyCode = representative.currencyCode,
                    category = ExpenseCategory.TRANSPORT,
                    date = expenseDate,
                    notes = listOfNotNull(
                        representative.flight.bookingRef?.let { "Booking reference: $it" },
                        representative.flight.price?.let { "Imported total: $it" },
                        candidates.takeIf { it.size > 1 }?.let { "Imported once for ${it.size} flight segments" }
                    ).takeIf { it.isNotEmpty() }?.joinToString("\n")
                )
            }

        val hotelExpenses = buildImportedHotelExpenseCandidates(booking, fallbackCurrencyCode)
            .groupBy(ImportedHotelExpenseCandidate::groupKey)
            .values
            .map { candidates ->
                val representative = candidates.first()
                Expense(
                    id = representative.expenseId,
                    tripId = tripId,
                    title = buildHotelExpenseTitle(candidates.map { it.hotel }),
                    amount = representative.amount,
                    currencyCode = representative.currencyCode,
                    category = ExpenseCategory.ACCOMMODATION,
                    date = parseImportedDate(representative.hotel.checkIn ?: representative.hotel.checkOut),
                    notes = listOfNotNull(
                        representative.hotel.bookingRef?.let { "Booking reference: $it" },
                        representative.hotel.price?.let { "Imported total: $it" },
                        representative.hotel.hostPhone?.let { "Host phone: $it" }
                    ).takeIf { it.isNotEmpty() }?.joinToString("\n")
                )
            }

        return flightExpenses + hotelExpenses
    }

    private fun buildImportedFlightExpenseCandidates(
        booking: ParsedBooking,
        fallbackCurrencyCode: String
    ): List<ImportedFlightExpenseCandidate> {
        val pricedFlights = booking.flights.mapIndexedNotNull { index, flight ->
            val parsedAmount = parseImportedAmount(flight.price) ?: return@mapIndexedNotNull null
            val currencyCode = inferCurrencyCode(flight.price, fallbackCurrencyCode)
            ImportedFlightExpenseCandidate(
                flightIndex = index,
                flight = flight,
                amount = parsedAmount,
                currencyCode = currencyCode,
                groupKey = "",
                expenseId = ""
            )
        }

        val repeatedUnreferencedTotals = pricedFlights
            .filter { it.flight.bookingRef.isNullOrBlank() }
            .groupingBy { it.amount to it.currencyCode }
            .eachCount()

        return pricedFlights.map { candidate ->
            val groupKey = buildImportedFlightExpenseGroupKey(candidate, repeatedUnreferencedTotals)
            candidate.copy(
                groupKey = groupKey,
                expenseId = UUID.nameUUIDFromBytes(groupKey.toByteArray()).toString()
            )
        }
    }

    private fun buildImportedHotelExpenseCandidates(
        booking: ParsedBooking,
        fallbackCurrencyCode: String
    ): List<ImportedHotelExpenseCandidate> {
        return booking.hotels.mapNotNull { hotel ->
            val parsedAmount = parseImportedAmount(hotel.price) ?: return@mapNotNull null
            val currencyCode = inferCurrencyCode(hotel.price, fallbackCurrencyCode)
            val groupKey = buildImportedHotelExpenseGroupKey(hotel, parsedAmount, currencyCode)
            ImportedHotelExpenseCandidate(
                hotel = hotel,
                amount = parsedAmount,
                currencyCode = currencyCode,
                groupKey = groupKey,
                expenseId = UUID.nameUUIDFromBytes(groupKey.toByteArray()).toString()
            )
        }
    }

    private fun buildImportedFlightExpenseGroupKey(
        candidate: ImportedFlightExpenseCandidate,
        repeatedUnreferencedTotals: Map<Pair<Double, String>, Int>
    ): String {
        val bookingRef = candidate.flight.bookingRef?.trim().orEmpty()
        return when {
            bookingRef.isNotBlank() -> "booking:$bookingRef|${candidate.amount}|${candidate.currencyCode}"
            repeatedUnreferencedTotals[candidate.amount to candidate.currencyCode].orZero() > 1 -> {
                // Some ticket PDFs repeat the same booking total on every segment; only import that total once.
                "shared-total:${candidate.amount}|${candidate.currencyCode}"
            }

            else -> listOf(
                candidate.flight.flightNumber.orEmpty(),
                candidate.flight.departureDateTime.orEmpty(),
                candidate.flight.origin.trim(),
                candidate.flight.destination.trim(),
                candidate.amount.toString(),
                candidate.currencyCode
            ).joinToString("|")
        }
    }

    private fun buildFlightExpenseTitle(flights: List<ParsedFlight>): String {
        val representative = flights.first()
        val label = flights
            .mapNotNull { listOfNotNull(it.airline, it.flightNumber).joinToString(" ").ifBlank { null } }
            .distinct()
            .firstOrNull()
            ?: if (flights.size > 1) "Flight tickets" else "Flight ticket"

        val routes = flights
            .map { flight ->
                listOf(flight.origin, flight.destination)
                    .filter { it.isNotBlank() }
                    .joinToString(" -> ")
            }
            .filter { it.isNotBlank() }
            .distinct()

        val routeSummary = when {
            routes.isEmpty() -> listOf(representative.origin, representative.destination)
                .filter { it.isNotBlank() }
                .joinToString(" -> ")
                .ifBlank { null }
            routes.size == 1 -> routes.first()
            else -> routes.joinToString(" / ")
        }

        return routeSummary?.let { "$label ($it)" } ?: label
    }

    private fun buildImportedHotelExpenseGroupKey(
        hotel: com.wanderlog.android.domain.model.ParsedHotel,
        amount: Double,
        currencyCode: String
    ): String {
        val bookingRef = hotel.bookingRef?.trim().orEmpty()
        return when {
            bookingRef.isNotBlank() -> "hotel-booking:$bookingRef|$amount|$currencyCode"
            else -> listOf(
                hotel.name.trim(),
                hotel.checkIn.orEmpty(),
                hotel.checkOut.orEmpty(),
                amount.toString(),
                currencyCode
            ).joinToString("|")
        }
    }

    private fun buildHotelExpenseTitle(hotels: List<com.wanderlog.android.domain.model.ParsedHotel>): String {
        val representative = hotels.first()
        return representative.name.trim().ifBlank { "Accommodation" }
    }

    private fun parseImportedAmount(priceText: String?): Double? {
        return ImportedMoneyParser.parseAmount(priceText)
    }

    private fun inferCurrencyCode(priceText: String?, fallbackCurrencyCode: String): String {
        val candidate = priceText?.trim().orEmpty().uppercase()
        CURRENCY_CODE_REGEX.find(candidate)?.value?.let { return it }

        return when {
            "A$" in candidate || "AU$" in candidate -> "AUD"
            "S$" in candidate || "SG$" in candidate -> "SGD"
            "US$" in candidate -> "USD"
            "C$" in candidate || "CA$" in candidate -> "CAD"
            "NZ$" in candidate -> "NZD"
            "HK$" in candidate -> "HKD"
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
        private val CURRENCY_CODE_REGEX = Regex("""\b[A-Z]{3}\b""")
    }
}

private data class ImportedFlightExpenseCandidate(
    val flightIndex: Int,
    val flight: ParsedFlight,
    val amount: Double,
    val currencyCode: String,
    val groupKey: String,
    val expenseId: String
)

private data class ImportedHotelExpenseCandidate(
    val hotel: com.wanderlog.android.domain.model.ParsedHotel,
    val amount: Double,
    val currencyCode: String,
    val groupKey: String,
    val expenseId: String
)

private fun Int?.orZero(): Int = this ?: 0
