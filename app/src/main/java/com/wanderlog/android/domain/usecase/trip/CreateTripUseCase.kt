package com.wanderlog.android.domain.usecase.trip

import com.wanderlog.android.core.util.generateDateRange
import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.model.TripDay
import com.wanderlog.android.domain.repository.TripRepository
import java.util.UUID
import javax.inject.Inject

class CreateTripUseCase @Inject constructor(private val repo: TripRepository) {

    suspend operator fun invoke(trip: Trip) {
        repo.createTrip(trip)
        val days = generateDateRange(trip.startDate, trip.endDate)
            .mapIndexed { index, date ->
                TripDay(
                    id = UUID.randomUUID().toString(),
                    tripId = trip.id,
                    date = date,
                    dayNumber = index + 1
                )
            }
        repo.createDaysForTrip(days)
    }
}
