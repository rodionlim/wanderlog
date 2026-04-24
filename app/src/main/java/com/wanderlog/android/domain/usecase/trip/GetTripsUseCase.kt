package com.wanderlog.android.domain.usecase.trip

import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTripsUseCase @Inject constructor(private val repo: TripRepository) {
    operator fun invoke(): Flow<List<Trip>> = repo.getAllTrips()
}
