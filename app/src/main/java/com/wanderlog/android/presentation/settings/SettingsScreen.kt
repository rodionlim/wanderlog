package com.wanderlog.android.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wanderlog.android.core.ui.component.WanderTopBar

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var parsingModelMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(topBar = { WanderTopBar(title = "Settings", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("API Keys", style = MaterialTheme.typography.titleMedium)

            val selectedOption = OpenAiModels.options.firstOrNull { it.id == state.openAiModel }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("OpenAI Model", style = MaterialTheme.typography.bodyMedium)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { modelMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${OpenAiModels.labelFor(state.openAiModel)} (${state.openAiModel})")
                    }

                    DropdownMenu(
                    expanded = modelMenuExpanded,
                    onDismissRequest = { modelMenuExpanded = false }
                    ) {
                        OpenAiModels.options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("${option.label} (${option.id})") },
                                onClick = {
                                    viewModel.onOpenAiModelChange(option.id)
                                    modelMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    selectedOption?.usageTier ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val selectedParsingOption = OpenAiModels.parsingOptions.firstOrNull { it.id == state.openAiParsingModel }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Booking Image/PDF Parsing Model", style = MaterialTheme.typography.bodyMedium)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { parsingModelMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${OpenAiModels.labelFor(state.openAiParsingModel)} (${state.openAiParsingModel})")
                    }

                    DropdownMenu(
                        expanded = parsingModelMenuExpanded,
                        onDismissRequest = { parsingModelMenuExpanded = false }
                    ) {
                        OpenAiModels.parsingOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("${option.label} (${option.id})") },
                                onClick = {
                                    viewModel.onOpenAiParsingModelChange(option.id)
                                    parsingModelMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    selectedParsingOption?.usageTier ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = state.openAiKey,
                onValueChange = viewModel::onOpenAiKeyChange,
                label = { Text("OpenAI API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.mapsKey,
                onValueChange = viewModel::onMapsKeyChange,
                label = { Text("Google Maps API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            Text(
                "Keys are stored encrypted on your device using Android Keystore.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
        }
    }
}
