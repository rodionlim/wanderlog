package com.wanderlog.android.domain.repository

import com.wanderlog.android.domain.model.Place

interface PlacesRepository {
    suspend fun searchPlaces(query: String, latLng: Pair<Double, Double>?): List<Place>
    suspend fun fetchPlaceDetails(placeId: String): Place
}
