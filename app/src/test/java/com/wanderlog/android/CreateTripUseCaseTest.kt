package com.wanderlog.android

import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.model.TripDay
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.domain.usecase.trip.CreateTripUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CreateTripUseCaseTest {

    private val tripRepository = mockk<TripRepository>(relaxed = true)
    private val useCase = CreateTripUseCase(tripRepository)

    @Test
    fun `createTrip persists trip and generates correct number of days`() = runTest {
        val startDate = LocalDate.of(2025, 6, 1)
        val endDate = LocalDate.of(2025, 6, 5) // 5 days
        val trip = Trip(
            id = "trip-1",
            name = "Paris Trip",
            destination = "Paris",
            startDate = startDate,
            endDate = endDate
        )

        val daysSlot = slot<List<TripDay>>()
        coEvery { tripRepository.createDaysForTrip(capture(daysSlot)) } returns Unit

        useCase(trip)

        coVerify { tripRepository.createTrip(trip) }
        coVerify { tripRepository.createDaysForTrip(any()) }

        val days = daysSlot.captured
        assertEquals(5, days.size)
        assertEquals(1, days[0].dayNumber)
        assertEquals(5, days[4].dayNumber)
        assertEquals(startDate, days[0].date)
        assertEquals(endDate, days[4].date)
    }
}
