package com.wanderlog.android.presentation.share

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wanderlog.android.domain.model.DocumentHint
import com.wanderlog.android.presentation.ai.fileImport.FileImportStep
import com.wanderlog.android.presentation.ai.fileImport.FileImportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareImportScreen(
    onDone: (tripId: String) -> Unit,
    onCancel: () -> Unit,
    viewModel: FileImportViewModel = hiltViewModel()
) {
    val sharedText = remember { PendingShare.text?.also { PendingShare.text = null }.orEmpty() }
    val step by viewModel.step.collectAsState()
    val trips by viewModel.trips.collectAsState()
    var selectedTripId by remember { mutableStateOf<String?>(null) }
    var selectedHint by remember { mutableStateOf<DocumentHint?>(null) }

    LaunchedEffect(trips) {
        if (selectedTripId == null && trips.size == 1) selectedTripId = trips.first().id
    }

    LaunchedEffect(step) {
        if (step is FileImportStep.Done) selectedTripId?.let(onDone)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from Share") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (val s = step) {
                FileImportStep.Idle -> IdleContent(
                    sharedText = sharedText,
                    trips = trips,
                    selectedTripId = selectedTripId,
                    onSelectTrip = { selectedTripId = it },
                    selectedHint = selectedHint,
                    onSelectHint = { selectedHint = it },
                    onImport = {
                        selectedTripId?.let { tid ->
                            viewModel.parseText(sharedText, tid, selectedHint)
                        }
                    },
                    onCancel = onCancel
                )

                FileImportStep.Parsing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Parsing with AI…")
                    }
                }

                is FileImportStep.Review -> ReviewContent(
                    step = s,
                    onReset = viewModel::reset,
                    onCommit = {
                        selectedTripId?.let { tid ->
                            viewModel.commitItems(tid, s.items)
                        }
                    }
                )

                is FileImportStep.Error -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        Button(onClick = viewModel::reset, modifier = Modifier.weight(1f)) { Text("Try again") }
                    }
                }

                FileImportStep.Done -> {}
            }
        }
    }
}

@Composable
private fun IdleContent(
    sharedText: String,
    trips: List<com.wanderlog.android.domain.model.Trip>,
    selectedTripId: String?,
    onSelectTrip: (String) -> Unit,
    selectedHint: DocumentHint?,
    onSelectHint: (DocumentHint?) -> Unit,
    onImport: () -> Unit,
    onCancel: () -> Unit
) {
    if (sharedText.isBlank()) {
        Text("No shared content received.", color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        return
    }

    Text("Add this booking to a trip", style = MaterialTheme.typography.titleMedium)

    if (trips.isEmpty()) {
        Text(
            "You have no trips yet. Create one first, then come back to share again.",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(trips) { trip ->
            FilterChip(
                selected = selectedTripId == trip.id,
                onClick = { onSelectTrip(trip.id) },
                label = { Text(trip.name) }
            )
        }
    }

    Text("What is this? (optional)", style = MaterialTheme.typography.titleSmall)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = selectedHint == null,
            onClick = { onSelectHint(null) },
            label = { Text("Auto") }
        )
        DocumentHint.entries.forEach { hint ->
            FilterChip(
                selected = selectedHint == hint,
                onClick = { onSelectHint(hint) },
                label = { Text(hint.label) }
            )
        }
    }

    Text("Preview", style = MaterialTheme.typography.titleSmall)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 240.dp)
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(sharedText.take(4000), style = MaterialTheme.typography.bodySmall)
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
        Button(
            onClick = onImport,
            enabled = selectedTripId != null,
            modifier = Modifier.weight(1f)
        ) { Text("Parse with AI") }
    }
}

@Composable
private fun ReviewContent(
    step: FileImportStep.Review,
    onReset: () -> Unit,
    onCommit: () -> Unit
) {
    Text("Review extracted items (${step.items.size})", style = MaterialTheme.typography.titleMedium)
    LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
        items(step.items) { item ->
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge)
                item.startTime?.let { Text("Date/time: $it", style = MaterialTheme.typography.bodySmall) }
                item.bookingRef?.let { Text("Ref: $it", style = MaterialTheme.typography.bodySmall) }
            }
            HorizontalDivider()
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("Re-parse") }
        Button(onClick = onCommit, modifier = Modifier.weight(1f)) { Text("Add to Itinerary") }
    }
}
