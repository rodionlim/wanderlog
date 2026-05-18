package com.wanderlog.android.presentation.itinerary.form

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.ItineraryItemType
import com.wanderlog.android.domain.model.TripDay
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ItineraryItemFormContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun editingItem_canSwitchToAnotherDayBeforeSaving() {
        val days = listOf(
            TripDay(id = "day-7", tripId = "trip-1", date = LocalDate.of(2026, 5, 7), dayNumber = 7),
            TripDay(id = "day-8", tripId = "trip-1", date = LocalDate.of(2026, 5, 8), dayNumber = 8)
        )
        val editingItem = ItineraryItem(
            id = "item-1",
            tripDayId = "day-7",
            tripId = "trip-1",
            title = "Museum",
            itemType = ItineraryItemType.ACTIVITY
        )
        var savedDayId: String? = null
        var savedDayDate: LocalDate? = null

        composeRule.setContent {
            MaterialTheme {
                var selectedDayId by remember { mutableStateOf(editingItem.tripDayId) }
                ItineraryItemFormContent(
                    state = ItemFormState(
                        title = "Museum",
                        itemType = ItineraryItemType.ACTIVITY
                    ),
                    availableDays = days,
                    editingItem = editingItem,
                    expenseCurrencyCode = "SGD",
                    dayId = "day-7",
                    dayDate = days.first().date,
                    selectedDayId = selectedDayId,
                    onSelectedDayIdChange = { selectedDayId = it },
                    onPlaceSearchRequested = {},
                    onSave = { dayId, dayDate ->
                        savedDayId = dayId
                        savedDayDate = dayDate
                    },
                    onManageAttachmentsRequested = null,
                    onDeleteRequested = null,
                    onTitleChange = {},
                    onTypeChange = {},
                    onStartTimeChange = {},
                    onEndTimeChange = {},
                    onNotesChange = {},
                    onBookingRefChange = {},
                    onCostChange = {},
                    onPlaceButtonClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Day 8").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Save").performClick()

        assertEquals("day-8", savedDayId)
        assertEquals(LocalDate.of(2026, 5, 8), savedDayDate)
    }
}
