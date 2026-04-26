package com.wanderlog.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wanderlog.android.core.util.TravellerProfilesCodec
import com.wanderlog.android.domain.model.Trip
import java.time.LocalDate

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val name: String,
    val destination: String,
    @ColumnInfo(name = "start_date") val startDate: LocalDate,
    @ColumnInfo(name = "end_date") val endDate: LocalDate,
    @ColumnInfo(name = "cover_image_uri") val coverImageUri: String? = null,
    @ColumnInfo(name = "budget_amount") val budgetAmount: Double? = null,
    @ColumnInfo(name = "currency_code") val currencyCode: String = "USD",
    @ColumnInfo(name = "traveller_names") val travellerProfilesRaw: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = Trip(
        id = id,
        name = name,
        destination = destination,
        startDate = startDate,
        endDate = endDate,
        coverImageUri = coverImageUri,
        budgetAmount = budgetAmount,
        currencyCode = currencyCode,
        travellerProfiles = TravellerProfilesCodec.decode(travellerProfilesRaw)
    )

    companion object {
        fun fromDomain(trip: Trip, now: Long = System.currentTimeMillis()) = TripEntity(
            id = trip.id,
            name = trip.name,
            destination = trip.destination,
            startDate = trip.startDate,
            endDate = trip.endDate,
            coverImageUri = trip.coverImageUri,
            budgetAmount = trip.budgetAmount,
            currencyCode = trip.currencyCode,
            travellerProfilesRaw = TravellerProfilesCodec.encode(trip.travellerProfiles),
            createdAt = now,
            updatedAt = now
        )
    }
}
