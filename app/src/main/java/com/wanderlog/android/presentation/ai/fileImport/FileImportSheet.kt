package com.wanderlog.android.presentation.ai.fileImport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
    var rasterizePdfAsImages by remember { mutableStateOf(false) }
    var showPasteTextInput by remember { mutableStateOf(false) }
    var pastedText by remember { mutableStateOf("") }

    LaunchedEffect(step) {
        if (step is FileImportStep.Done) onDismiss()
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.parseUri(it, tripId, selectedHint, rasterizePdfAsImages) }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { viewModel.parseUri(it, tripId, selectedHint) } }

    val multiFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.parseUris(uris, tripId, selectedHint, rasterizePdfAsImages)
        }
    }

    val textLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.parseUri(it, tripId, selectedHint) } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Import Booking", style = MaterialTheme.typography.titleLarge)

        when (val s = step) {
            FileImportStep.Idle -> {
                Text(
                    "What is this document? (optional — improves parsing)",
                    style = MaterialTheme.typography.bodyMedium
                )
                HintChipsRow(
                    selected = selectedHint,
                    onSelect = { selectedHint = it }
                )
                Spacer(Modifier.height(4.dp))
                Text("Choose a file:")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rasterizePdfAsImages,
                        onCheckedChange = { rasterizePdfAsImages = it }
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Rasterize PDFs to images")
                        Text(
                            "Off by default: PDFs are parsed as extracted text. Turn this on for scanned or layout-heavy PDFs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        multiFileLauncher.launch(
                            arrayOf("application/pdf", "text/plain", "text/csv", "image/*")
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Import Multiple Files") }
                OutlinedButton(
                    onClick = { pdfLauncher.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Import PDF") }
                OutlinedButton(onClick = {
                    imageLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }, modifier = Modifier.fillMaxWidth()) { Text("Import Image / Screenshot") }
                OutlinedButton(
                    onClick = { textLauncher.launch(arrayOf("text/plain", "text/csv")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Import Text / CSV") }
                OutlinedButton(
                    onClick = { showPasteTextInput = !showPasteTextInput },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showPasteTextInput) "Hide Paste Text" else "Paste Text")
                }
                if (showPasteTextInput) {
                    Text(
                        "Paste an email body or any booking text here. This is useful when your mail app cannot share directly into Wanderlog.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pastedText,
                        onValueChange = { pastedText = it },
                        label = { Text("Booking text") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 6
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                pastedText = ""
                                showPasteTextInput = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                viewModel.parseText(pastedText.trim(), tripId, selectedHint)
                            },
                            enabled = pastedText.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Import Pasted Text")
                        }
                    }
                }
                Text(
                    "Use multiple files when one booking is split across PDFs, screenshots, or text exports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FileImportStep.Parsing -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Parsing document with AI…")
                }
            }

            is FileImportStep.Review -> {
                ImportedBookingReview(
                    step = s,
                    onUpdateItem = viewModel::updateReviewedItem,
                    onReset = viewModel::reset,
                    onCommit = { viewModel.commitItems(tripId, s.items) },
                    resetLabel = "Re-import",
                    commitLabel = "Add to Itinerary"
                )
            }

            is FileImportStep.Error -> {
                Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
                OutlinedButton(
                    onClick = viewModel::reset,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Try again") }
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
