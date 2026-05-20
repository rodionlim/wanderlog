package com.wanderlog.android.data.sync

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wanderlog.android.domain.model.sync.SyncEntityType
import com.wanderlog.android.domain.model.sync.TripSyncBundle
import com.wanderlog.android.domain.model.sync.TripSyncManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyTripSyncTransportParsingTest {

    private val bundleAdapter = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(TripSyncBundle::class.java)

    private val bundlePayloadAdapter = Moshi.Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()
      .adapter<Map<String, Any?>>(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))

    @Test
    fun `buildIncomingTripSyncManifest defaults null records to empty list`() {
      val manifest = buildIncomingTripSyncManifest(
        controlTripId = "trip-1",
        tripIdValue = null,
        protocolVersionValue = null,
        generatedAtValue = 123L,
        recordsValue = null
      )

        assertEquals("trip-1", manifest?.tripId)
        assertEquals(123L, manifest?.generatedAt)
        assertTrue(manifest?.records?.isEmpty() == true)
    }

    @Test
    fun `buildIncomingTripSyncManifest coerces malformed records and keeps valid entries`() {
      val malformedManifest = buildIncomingTripSyncManifest(
        controlTripId = null,
        tripIdValue = "trip-1",
        protocolVersionValue = null,
        generatedAtValue = 123L,
        recordsValue = mapOf("unexpected" to true)
      )

        assertTrue(malformedManifest?.records?.isEmpty() == true)

      val mixedManifest = buildIncomingTripSyncManifest(
        controlTripId = null,
        tripIdValue = "trip-1",
        protocolVersionValue = null,
        generatedAtValue = 456L,
        recordsValue = listOf(
          null,
          mapOf(
            "entityType" to "UNKNOWN",
            "id" to "bad-1",
            "updatedAt" to 1L
          ),
          mapOf(
            "entityType" to "TRIP",
            "id" to "trip-1",
            "updatedAt" to 789L,
            "lastModifiedByDeviceId" to "pixel7"
          )
        )
      )

        assertEquals(1, mixedManifest?.records?.size)
        assertEquals(SyncEntityType.TRIP, mixedManifest?.records?.singleOrNull()?.entityType)
        assertEquals("trip-1", mixedManifest?.records?.singleOrNull()?.id)
    }

    @Test
    fun `parseIncomingTripSyncBundle coerces non-positive protocol version`() {
        val bundle = parseIncomingTripSyncBundle(
            payload =
                """
                {
                  "protocolVersion": 0,
                  "generatedAt": 123,
                  "trip": {
                    "id": "trip-1",
                    "name": "Perth",
                    "destination": "Perth",
                    "startDate": "2026-05-20",
                    "endDate": "2026-05-25",
                    "currencyCode": "AUD",
                    "travellerProfiles": [],
                    "metadata": {
                      "updatedAt": 123,
                      "lastModifiedByDeviceId": "pixel7"
                    }
                  }
                }
                """.trimIndent(),
              bundleAdapter = bundleAdapter,
              bundlePayloadAdapter = bundlePayloadAdapter
        )

        assertEquals(TripSyncManifest.CURRENT_PROTOCOL_VERSION, bundle?.protocolVersion)
        assertEquals("trip-1", bundle?.trip?.id)
    }
}
