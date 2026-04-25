package com.wanderlog.android.data.remote.places

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place as GmsPlace
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.wanderlog.android.domain.model.Place
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class PlacesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val placesClient: PlacesClient
) {

    private val placeFields = listOf(
        GmsPlace.Field.ID,
        GmsPlace.Field.NAME,
        GmsPlace.Field.ADDRESS,
        GmsPlace.Field.LAT_LNG,
        GmsPlace.Field.PHOTO_METADATAS
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

    suspend fun fetchDestinationCoverImage(destination: String, tripId: String): String? {
        if (destination.isBlank()) return null

        val placeId = searchPlaces(destination, null)
            .firstOrNull()
            ?.placeId
            ?: return null

        val request = FetchPlaceRequest.newInstance(placeId, placeFields)
        val place = placesClient.fetchPlace(request).await().place
        val metadata = place.photoMetadatas?.firstOrNull() ?: return null
        val photoRequest = FetchPhotoRequest.builder(metadata)
            .setMaxWidth(1600)
            .setMaxHeight(900)
            .build()
        val bitmap = placesClient.fetchPhoto(photoRequest).await().bitmap
        return cacheCoverBitmap(bitmap, tripId)
    }

    private suspend fun cacheCoverBitmap(bitmap: Bitmap, tripId: String): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "trip-covers").apply { mkdirs() }
        val file = File(dir, "$tripId.jpg")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
        }
        Uri.fromFile(file).toString()
    }
}
