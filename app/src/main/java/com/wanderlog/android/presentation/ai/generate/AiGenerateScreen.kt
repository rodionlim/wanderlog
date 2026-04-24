package com.wanderlog.android.presentation.ai.generate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wanderlog.android.core.ui.component.LoadingOverlay
import com.wanderlog.android.core.ui.component.WanderTopBar

@Composable
fun AiGenerateScreen(
    onBack: () -> Unit,
    viewModel: AiGenerateViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.committed) {
        if (state.committed) onBack()
    }

    Scaffold(topBar = { WanderTopBar(title = "AI Itinerary", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.destination,
                        onValueChange = viewModel::onDestinationChange,
                        label = { Text("Destination") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state.preferences,
                        onValueChange = viewModel::onPreferencesChange,
                        label = { Text("Travel style / preferences") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    OutlinedTextField(
                        value = state.travellers,
                        onValueChange = viewModel::onTravellersChange,
                        label = { Text("Number of travellers") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = viewModel::generate,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) CircularProgressIndicator()
                        else Text(if (state.generatedDays.isEmpty()) "Generate Itinerary" else "Regenerate")
                    }
                }
            }

            if (state.generatedDays.isNotEmpty()) {
                item {
                    Text("Preview — ${state.generatedDays.size} days generated",
                        style = MaterialTheme.typography.titleMedium)
                }

                itemsIndexed(state.generatedDays) { _, day ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Day ${day.dayNumber} — ${day.date}",
                                style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            day.items.forEach { item ->
                                val timePrefix = item.startTime?.let { "[$it] " } ?: ""
                                Text("• $timePrefix${item.title}",
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Discard") }
                        Button(onClick = viewModel::commitToItinerary, modifier = Modifier.weight(1f)) {
                            Text("Add to Itinerary")
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
        if (state.isLoading) LoadingOverlay()
    }
}
