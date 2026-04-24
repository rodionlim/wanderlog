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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.ItineraryItemType

@Composable
fun ItineraryItemFormSheet(
    tripId: String,
    dayId: String,
    editingItem: ItineraryItem?,
    onDismiss: () -> Unit,
    onPlaceSearchRequested: () -> Unit,
    viewModel: ItineraryItemFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(editingItem) {
        if (editingItem != null) viewModel.loadExisting(editingItem)
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDismiss()
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

        state.error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }

        Button(
            onClick = { viewModel.save(tripId, dayId, editingItem?.id) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }

        Spacer(Modifier.height(16.dp))
    }
}
