package com.wanderlog.android.presentation.itinerary.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wanderlog.android.domain.model.Expense
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.ItineraryItemType
import com.wanderlog.android.domain.model.Place
import java.time.LocalDate

@Composable
fun ItineraryItemFormSheet(
    tripId: String,
    dayId: String,
    dayDate: LocalDate?,
    currencyCode: String,
    editingItem: ItineraryItem?,
    linkedExpense: Expense? = null,
    selectedPlace: Place? = null,
    onSelectedPlaceApplied: () -> Unit = {},
    onDismiss: () -> Unit,
    onDeleteRequested: (() -> Unit)? = null,
    onPlaceSearchRequested: () -> Unit,
    viewModel: ItineraryItemFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(editingItem, linkedExpense) {
        if (editingItem != null) viewModel.loadExisting(editingItem, linkedExpense) else viewModel.resetForm()
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDismiss()
    }

    LaunchedEffect(selectedPlace) {
        if (selectedPlace != null) {
            viewModel.onPlaceSelected(selectedPlace)
            onSelectedPlaceApplied()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (editingItem == null) "Add Item" else "Edit Item",
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::onTitleChange,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Type chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ItineraryItemType.values().forEach { type ->
                FilterChip(
                    selected = state.itemType == type,
                    onClick = { viewModel.onTypeChange(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        OutlinedButton(onClick = onPlaceSearchRequested, modifier = Modifier.fillMaxWidth()) {
            Text(state.place?.name ?: "Search & add place")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.startTime,
                onValueChange = viewModel::onStartTimeChange,
                label = { Text("Start (HH:mm)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = state.endTime,
                onValueChange = viewModel::onEndTimeChange,
                label = { Text("End (HH:mm)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::onNotesChange,
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        OutlinedTextField(
            value = state.bookingRef,
            onValueChange = viewModel::onBookingRefChange,
            label = { Text("Booking reference (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (state.itemType == ItineraryItemType.ACTIVITY) {
            OutlinedTextField(
                value = state.costAmount,
                onValueChange = viewModel::onCostChange,
                label = { Text("Cost ($currencyCode, optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                supportingText = {
                    Text("This creates or updates a linked Activity expense in Budget.")
                }
            )
        }

        state.error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }

        Button(
            onClick = { viewModel.save(tripId, dayId, dayDate, currencyCode, editingItem?.id) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }

        if (editingItem != null && onDeleteRequested != null) {
            OutlinedButton(
                onClick = onDeleteRequested,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete item")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
