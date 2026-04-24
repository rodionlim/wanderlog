package com.wanderlog.android.presentation.ai.fileImport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wanderlog.android.domain.model.DocumentHint

@Composable
fun FileImportSheet(
    tripId: String,
    onDismiss: () -> Unit,
    viewModel: FileImportViewModel = hiltViewModel()
) {
    val step by viewModel.step.collectAsState()
    var selectedHint by remember { mutableStateOf<DocumentHint?>(null) }

    LaunchedEffect(step) {
        if (step is FileImportStep.Done) onDismiss()
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.parseUri(it, tripId, selectedHint) } }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { viewModel.parseUri(it, tripId, selectedHint) } }

    val textLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.parseUri(it, tripId, selectedHint) } }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Import Booking", style = MaterialTheme.typography.titleLarge)

        when (val s = step) {
            FileImportStep.Idle -> {
                Text("What is this document? (optional — improves parsing)", style = MaterialTheme.typography.bodyMedium)
                HintChipsRow(
                    selected = selectedHint,
                    onSelect = { selectedHint = it }
                )
                Spacer(Modifier.height(4.dp))
                Text("Choose a file:")
                OutlinedButton(onClick = { pdfLauncher.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.fillMaxWidth()) { Text("Import PDF") }
                OutlinedButton(onClick = {
                    imageLauncher.launch(androidx.activity.result.PickVisualMediaRequest(
                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                }, modifier = Modifier.fillMaxWidth()) { Text("Import Image / Screenshot") }
                OutlinedButton(onClick = { textLauncher.launch(arrayOf("text/plain", "text/csv")) },
                    modifier = Modifier.fillMaxWidth()) { Text("Import Text / CSV") }
            }

            FileImportStep.Parsing -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Parsing document with AI…")
                }
            }

            is FileImportStep.Review -> {
                Text("Review extracted items (${s.items.size})", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    items(s.items) { item ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(item.title, style = MaterialTheme.typography.bodyLarge)
                            item.startTime?.let { Text("Date/time: $it", style = MaterialTheme.typography.bodySmall) }
                            item.bookingRef?.let { Text("Ref: $it", style = MaterialTheme.typography.bodySmall) }
                        }
                        HorizontalDivider()
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::reset, modifier = Modifier.weight(1f)) { Text("Re-import") }
                    Button(onClick = { viewModel.commitItems(tripId, s.items) }, modifier = Modifier.weight(1f)) {
                        Text("Add to Itinerary")
                    }
                }
            }

            is FileImportStep.Error -> {
                Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = viewModel::reset, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
            }

            FileImportStep.Done -> {}
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HintChipsRow(
    selected: DocumentHint?,
    onSelect: (DocumentHint?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("Auto") }
        )
        DocumentHint.entries.forEach { hint ->
            FilterChip(
                selected = selected == hint,
                onClick = { onSelect(hint) },
                label = { Text(hint.label) }
            )
        }
    }
}
