package com.wanderlog.android.data.repository

import com.wanderlog.android.data.local.dao.TripDao
import com.wanderlog.android.data.local.dao.TripDayDao
import com.wanderlog.android.data.local.entity.TripDayEntity
import com.wanderlog.android.data.local.entity.TripEntity
import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.model.TripDay
import com.wanderlog.android.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TripRepositoryImpl @Inject constructor(
    private val tripDao: TripDao,
    private val tripDayDao: TripDayDao
) : TripRepository {

    override fun getAllTrips(): Flow<List<Trip>> =
        tripDao.getAllTrips().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getTripById(id: String): Trip? =
        tripDao.getTripById(id)?.toDomain()

    override suspend fun createTrip(trip: Trip) =
        tripDao.insertTrip(TripEntity.fromDomain(trip))

    override suspend fun updateTrip(trip: Trip) =
        tripDao.updateTrip(TripEntity.fromDomain(trip))

    override suspend fun deleteTrip(trip: Trip) =
        tripDao.deleteTrip(TripEntity.fromDomain(trip))

    override suspend fun getDaysForTrip(tripId: String): List<TripDay> =
        tripDayDao.getDaysForTripOnce(tripId).map { it.toDomain() }

    override fun getDaysForTripFlow(tripId: String): Flow<List<TripDay>> =
        tripDayDao.getDaysForTrip(tripId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun createDaysForTrip(days: List<TripDay>) =
        tripDayDao.insertAll(days.map { TripDayEntity.fromDomain(it) })

    override suspend fun deleteDaysForTrip(tripId: String) =
        tripDayDao.deleteAllForTrip(tripId)
}
