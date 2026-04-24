package com.wanderlog.android.data.repository

import com.google.android.gms.maps.model.LatLng
import com.wanderlog.android.data.remote.places.PlacesDataSource
import com.wanderlog.android.domain.model.Place
import com.wanderlog.android.domain.repository.PlacesRepository
import javax.inject.Inject

class PlacesRepositoryImpl @Inject constructor(
    private val dataSource: PlacesDataSource
) : PlacesRepository {

    override suspend fun searchPlaces(query: String, latLng: Pair<Double, Double>?): List<Place> =
        dataSource.searchPlaces(query, latLng?.let { LatLng(it.first, it.second) })

    override suspend fun fetchPlaceDetails(placeId: String): Place =
        dataSource.fetchPlaceDetails(placeId)
}
