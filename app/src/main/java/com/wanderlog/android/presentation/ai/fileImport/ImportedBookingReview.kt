package com.wanderlog.android.presentation.ai.fileImport

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wanderlog.android.core.util.toDisplayDateTimeOrSelf
import com.wanderlog.android.core.util.toShortDisplay
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.TripDay

@Composable
fun ImportedBookingReview(
    step: FileImportStep.Review,
    onUpdateItem: (ItineraryItem) -> Unit,
    onReset: () -> Unit,
    onCommit: () -> Unit,
    resetLabel: String,
    commitLabel: String
) {
    var editingItemId by remember { mutableStateOf<String?>(null) }

    Text("Review extracted items (${step.items.size})", style = MaterialTheme.typography.titleMedium)
    Text(
        "You can edit imported details before saving them to the itinerary.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (step.storesLocalAttachment) {
        Text(
            "The original document will be stored locally and linked to each imported itinerary entry.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(step.items, key = { it.id }) { item ->
            val dayLabel = step.days.firstOrNull { it.id == item.tripDayId }
                ?.let { "Day ${it.dayNumber} • ${it.date.toShortDisplay()}" }
                ?: "Unassigned day"
            ImportedItemCard(
                item = item,
                dayLabel = dayLabel,
                onEdit = { editingItemId = item.id }
            )
        }
    }

    step.items.firstOrNull { it.id == editingItemId }?.let { item ->
        ImportedItemEditDialog(
            item = item,
            days = step.days,
            onDismiss = { editingItemId = null },
            onSave = { updatedItem ->
                onUpdateItem(updatedItem)
                editingItemId = null
            }
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text(resetLabel) }
        Button(onClick = onCommit, modifier = Modifier.weight(1f)) { Text(commitLabel) }
    }
}

@Composable
private fun ImportedItemCard(
    item: ItineraryItem,
    dayLabel: String,
    onEdit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                dayLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            item.startTime?.let { start ->
                val timeLabel = item.endTime?.let { end ->
                    "${start.toDisplayDateTimeOrSelf()} -> ${end.toDisplayDateTimeOrSelf()}"
                } ?: start.toDisplayDateTimeOrSelf()
                Text(timeLabel, style = MaterialTheme.typography.bodySmall)
            }

            item.bookingRef?.let {
                Text(
                    "Booking reference: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item.notes?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text("Edit details")
            }
        }
    }
}

@Composable
private fun ImportedItemEditDialog(
    item: ItineraryItem,
    days: List<TripDay>,
    onDismiss: () -> Unit,
    onSave: (ItineraryItem) -> Unit
) {
    var title by remember(item.id) { mutableStateOf(item.title) }
    var startTime by remember(item.id) { mutableStateOf(item.startTime.orEmpty()) }
    var endTime by remember(item.id) { mutableStateOf(item.endTime.orEmpty()) }
    var bookingRef by remember(item.id) { mutableStateOf(item.bookingRef.orEmpty()) }
    var notes by remember(item.id) { mutableStateOf(item.notes.orEmpty()) }
    var selectedDayId by remember(item.id) { mutableStateOf(item.tripDayId) }
    var dayMenuExpanded by remember { mutableStateOf(false) }

    val selectedDay = days.firstOrNull { it.id == selectedDayId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit imported item") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { dayMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            selectedDay?.let { "Day ${it.dayNumber} • ${it.date.toShortDisplay()}" }
                                ?: "Select day"
                        )
                    }
                    DropdownMenu(
                        expanded = dayMenuExpanded,
                        onDismissRequest = { dayMenuExpanded = false }
                    ) {
                        days.forEach { day ->
                            DropdownMenuItem(
                                text = { Text("Day ${day.dayNumber} • ${day.date.toShortDisplay()}") },
                                onClick = {
                                    selectedDayId = day.id
                                    dayMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Start date/time") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("End date/time") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = bookingRef,
                    onValueChange = { bookingRef = it },
                    label = { Text("Booking reference") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        item.copy(
                            tripDayId = selectedDayId,
                            title = title.trim().ifBlank { item.title },
                            startTime = startTime.trim().takeIf { it.isNotBlank() },
                            endTime = endTime.trim().takeIf { it.isNotBlank() },
                            bookingRef = bookingRef.trim().takeIf { it.isNotBlank() },
                            notes = notes.trim().takeIf { it.isNotBlank() }
                        )
                    )
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
