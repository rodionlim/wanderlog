package com.wanderlog.android.data.remote.places

import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.wanderlog.android.domain.model.Place
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.google.android.libraries.places.api.model.Place as GmsPlace

class PlacesDataSource @Inject constructor(
    private val placesClient: PlacesClient
) {

    private val placeFields = listOf(
        GmsPlace.Field.ID,
        GmsPlace.Field.NAME,
        GmsPlace.Field.ADDRESS,
        GmsPlace.Field.LAT_LNG
    )

    suspend fun searchPlaces(query: String, latLng: LatLng?): List<Place> {
        if (query.isBlank()) return emptyList()
        val token = AutocompleteSessionToken.newInstance()
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setSessionToken(token)
            .apply { if (latLng != null) setOrigin(latLng) }
            .build()

        return try {
            val response = placesClient.findAutocompletePredictions(request).await()
            response.autocompletePredictions.map { prediction ->
                Place(
                    placeId = prediction.placeId,
                    name = prediction.getPrimaryText(null).toString(),
                    address = prediction.getSecondaryText(null).toString()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchPlaceDetails(placeId: String): Place {
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)
        val response = placesClient.fetchPlace(request).await()
        val gmsPlace = response.place
        return Place(
            placeId = gmsPlace.id,
            name = gmsPlace.name ?: "",
            address = gmsPlace.address,
            latitude = gmsPlace.latLng?.latitude,
            longitude = gmsPlace.latLng?.longitude
        )
    }
}
