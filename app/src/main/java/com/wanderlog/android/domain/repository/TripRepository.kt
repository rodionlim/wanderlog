package com.wanderlog.android.domain.repository

import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.model.TripDay
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    fun getAllTrips(): Flow<List<Trip>>
    suspend fun getTripById(id: String): Trip?
    suspend fun createTrip(trip: Trip)
    suspend fun updateTrip(trip: Trip)
    suspend fun deleteTrip(trip: Trip)
    suspend fun getDaysForTrip(tripId: String): List<TripDay>
    fun getDaysForTripFlow(tripId: String): Flow<List<TripDay>>
    suspend fun createDaysForTrip(days: List<TripDay>)
    suspend fun deleteDaysForTrip(tripId: String)
}
