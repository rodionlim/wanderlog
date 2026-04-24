package com.wanderlog.android.domain.usecase.trip

import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.repository.TripRepository
import javax.inject.Inject

class DeleteTripUseCase @Inject constructor(private val repo: TripRepository) {
    suspend operator fun invoke(trip: Trip) = repo.deleteTrip(trip)
}
