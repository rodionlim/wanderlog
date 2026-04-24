package com.wanderlog.android.data.repository

import com.squareup.moshi.Moshi
import com.wanderlog.android.data.remote.openai.OpenAiService
import com.wanderlog.android.data.remote.openai.dto.ChatCompletionRequest
import com.wanderlog.android.data.remote.openai.dto.ContentPartDto
import com.wanderlog.android.data.remote.openai.dto.MessageDto
import com.wanderlog.android.data.remote.openai.dto.ResponseFormatDto
import com.wanderlog.android.data.remote.openai.dto.TextPart
import com.wanderlog.android.domain.model.DocumentHint
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.ItineraryItemType
import com.wanderlog.android.domain.model.ParsedActivity
import com.wanderlog.android.domain.model.ParsedBooking
import com.wanderlog.android.domain.model.ParsedFlight
import com.wanderlog.android.domain.model.ParsedHotel
import com.wanderlog.android.domain.model.Place
import com.wanderlog.android.domain.model.TripDay
import com.wanderlog.android.domain.repository.AiRepository
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val openAiService: OpenAiService,
    private val moshi: Moshi
) : AiRepository {

    override suspend fun generateItinerary(
        destination: String,
        startDate: String,
        endDate: String,
        preferences: String,
        travellers: Int
    ): List<TripDay> {
        val systemPrompt = """
            You are a travel planning assistant. Always respond with valid JSON only.
            Never include markdown code blocks or any text outside the JSON.
        """.trimIndent()

        val userPrompt = """
            Generate a day-by-day travel itinerary. Output ONLY valid JSON with this schema:
            {
              "days": [
                {
                  "day_number": 1,
                  "date": "YYYY-MM-DD",
                  "items": [
                    {
                      "title": "...",
                      "type": "PLACE|HOTEL|ACTIVITY|TRANSPORT|FLIGHT|NOTE",
                      "location": "...",
                      "start_time": "HH:mm",
                      "end_time": "HH:mm",
                      "notes": "..."
                    }
                  ]
                }
              ]
            }

            Trip details:
            - Destination: $destination
            - Start date: $startDate
            - End date: $endDate
            - Travel style: $preferences
            - Number of travellers: $travellers
        """.trimIndent()

        val request = ChatCompletionRequest(
            messages = listOf(
                MessageDto("system", systemPrompt),
                MessageDto("user", userPrompt)
            ),
            responseFormat = ResponseFormatDto("json_object")
        )

        val response = openAiService.chatCompletion(request)
        val json = response.choices.first().message.content
        return parseItineraryJson(json, destination)
    }

    override suspend fun parseFile(contentParts: List<ContentPartDto>, hint: DocumentHint?): ParsedBooking {
        val hintLine = hint?.promptAddendum()?.let { "\n\n$it" }.orEmpty()
        val systemPrompt = "You are a travel document parser. Always respond with valid JSON only.$hintLine"
        val parsePrompt = TextPart(text = """
            Extract all travel details from this document.
            Output ONLY valid JSON with this schema:
            {
              "flights": [{"flightNumber":null,"origin":"","destination":"","departureDateTime":null,"arrivalDateTime":null,"bookingRef":null}],
              "hotels": [{"name":"","address":null,"checkIn":null,"checkOut":null,"bookingRef":null}],
              "activities": [{"title":"","location":null,"dateTime":null,"notes":null}]
            }
        """.trimIndent())

        val allParts: List<Any> = listOf(parsePrompt) + contentParts
        val request = ChatCompletionRequest(
            messages = listOf(
                MessageDto("system", systemPrompt),
                MessageDto("user", allParts)
            ),
            responseFormat = ResponseFormatDto("json_object")
        )

        val response = openAiService.chatCompletion(request)
        val json = response.choices.first().message.content
        return parseBookingJson(json)
    }

    private fun parseItineraryJson(json: String, tripId: String): List<TripDay> {
        val root = JSONObject(json)
        val daysArray = root.getJSONArray("days")
        val result = mutableListOf<TripDay>()

        for (i in 0 until daysArray.length()) {
            val dayObj = daysArray.getJSONObject(i)
            val dayId = UUID.randomUUID().toString()
            val itemsArray = dayObj.optJSONArray("items")
            val items = mutableListOf<ItineraryItem>()

            if (itemsArray != null) {
                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    val location = itemObj.optString("location").takeIf { it.isNotBlank() }
                    items.add(
                        ItineraryItem(
                            id = UUID.randomUUID().toString(),
                            tripDayId = dayId,
                            tripId = tripId,
                            title = itemObj.optString("title", ""),
                            itemType = runCatching {
                                ItineraryItemType.valueOf(itemObj.optString("type", "PLACE"))
                            }.getOrDefault(ItineraryItemType.PLACE),
                            place = location?.let { Place(name = it) },
                            startTime = itemObj.optString("start_time").takeIf { it.isNotBlank() },
                            endTime = itemObj.optString("end_time").takeIf { it.isNotBlank() },
                            notes = itemObj.optString("notes").takeIf { it.isNotBlank() },
                            sortOrder = j
                        )
                    )
                }
            }

            result.add(
                TripDay(
                    id = dayId,
                    tripId = tripId,
                    date = java.time.LocalDate.parse(dayObj.getString("date")),
                    dayNumber = dayObj.getInt("day_number"),
                    items = items
                )
            )
        }
        return result
    }

    private fun parseBookingJson(json: String): ParsedBooking {
        val root = JSONObject(json)

        val flights = buildList {
            val arr = root.optJSONArray("flights") ?: return@buildList
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(ParsedFlight(
                    flightNumber = o.optString("flightNumber").takeIf { it.isNotBlank() },
                    origin = o.optString("origin", ""),
                    destination = o.optString("destination", ""),
                    departureDateTime = o.optString("departureDateTime").takeIf { it.isNotBlank() },
                    arrivalDateTime = o.optString("arrivalDateTime").takeIf { it.isNotBlank() },
                    bookingRef = o.optString("bookingRef").takeIf { it.isNotBlank() }
                ))
            }
        }

        val hotels = buildList {
            val arr = root.optJSONArray("hotels") ?: return@buildList
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(ParsedHotel(
                    name = o.optString("name", ""),
                    address = o.optString("address").takeIf { it.isNotBlank() },
                    checkIn = o.optString("checkIn").takeIf { it.isNotBlank() },
                    checkOut = o.optString("checkOut").takeIf { it.isNotBlank() },
                    bookingRef = o.optString("bookingRef").takeIf { it.isNotBlank() }
                ))
            }
        }

        val activities = buildList {
            val arr = root.optJSONArray("activities") ?: return@buildList
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(ParsedActivity(
                    title = o.optString("title", ""),
                    location = o.optString("location").takeIf { it.isNotBlank() },
                    dateTime = o.optString("dateTime").takeIf { it.isNotBlank() },
                    notes = o.optString("notes").takeIf { it.isNotBlank() }
                ))
            }
        }

        return ParsedBooking(flights, hotels, activities)
    }
}
