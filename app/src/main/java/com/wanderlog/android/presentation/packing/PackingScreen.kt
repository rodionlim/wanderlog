package com.wanderlog.android.presentation.packing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wanderlog.android.core.ui.component.WanderTopBar
import com.wanderlog.android.domain.model.PackingItem

@Composable
fun PackingScreen(
    onBack: () -> Unit,
    viewModel: PackingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val showAggregateTab = state.travellerNames.isNotEmpty()
    val aggregateItems = viewModel.aggregateItems()
    val visibleIndividualItems = viewModel.visibleIndividualItems()
    var editingItem by remember { mutableStateOf<PackingItem?>(null) }
    var editingAggregateItem by remember { mutableStateOf<PackingAggregateItem?>(null) }

    Scaffold(topBar = { WanderTopBar(title = "Packing List", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showAggregateTab) {
                item {
                    ScrollableTabRow(selectedTabIndex = state.selectedTabIndex) {
                        Tab(
                            selected = state.selectedTabIndex == 0,
                            onClick = { viewModel.selectTab(0) },
                            text = { Text("All") }
                        )
                        state.travellerNames.forEachIndexed { index, travellerName ->
                            Tab(
                                selected = state.selectedTabIndex == index + 1,
                                onClick = { viewModel.selectTab(index + 1) },
                                text = { Text(travellerName) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AI packing update",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Ask AI to rewrite the current packing list using this trip's destination, dates, and travellers automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = state.aiPrompt,
                        onValueChange = viewModel::onAiPromptChange,
                        label = { Text("Describe how to update the packing list") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isAiUpdating
                    )
                    Button(
                        onClick = viewModel::applyAiUpdate,
                        enabled = !state.isAiUpdating,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (state.isAiUpdating) "Updating..." else "Update with AI")
                    }
                    state.aiMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    state.aiError?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.newItemText,
                        onValueChange = viewModel::onNewItemTextChange,
                        label = {
                            Text(
                                when {
                                    !showAggregateTab -> "Add item"
                                    state.selectedTabIndex == 0 -> "Add item for all travellers"
                                    else -> "Add item for ${state.travellerNames[state.selectedTabIndex - 1]}"
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    TextButton(onClick = viewModel::addItem) {
                        Icon(Icons.Default.Add, "Add")
                    }
                }
            }

            if (showAggregateTab && state.selectedTabIndex == 0) {
                items(aggregateItems, key = { it.key }) { group ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = group.allChecked,
                                onCheckedChange = { viewModel.toggleAggregateItem(group) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = group.title,
                                    textDecoration = if (group.allChecked) TextDecoration.LineThrough else null
                                )
                                Text(
                                    text = "Qty ${group.totalQuantity} • ${group.checkedCount}/${group.totalCount} packed${group.travellerLabel.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { editingAggregateItem = group }) {
                                Icon(Icons.Default.Edit, "Edit")
                            }
                            IconButton(onClick = { viewModel.deleteAggregateItem(group) }) {
                                Icon(Icons.Default.Delete, "Delete")
                            }
                        }
                    }
                }
            } else {
                items(visibleIndividualItems, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.isChecked,
                            onCheckedChange = { viewModel.toggleItem(item) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                            )
                            Text(
                                text = buildString {
                                    append("Qty ${item.quantity}")
                                    item.travellerName?.let { travellerName ->
                                        append(" • ")
                                        append(travellerName)
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { editingItem = item }) {
                            Icon(Icons.Default.Edit, "Edit")
                        }
                        IconButton(onClick = { viewModel.deleteItem(item) }) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    }
                }
            }
        }
    }

    editingItem?.let { item ->
        PackingItemEditDialog(
            title = "Edit packing item",
            initialTitle = item.title,
            initialQuantity = item.quantity,
            supportingText = null,
            onDismiss = { editingItem = null },
            onSave = { updatedTitle, updatedQuantity ->
                viewModel.updateItem(item, updatedTitle, updatedQuantity)
                editingItem = null
            }
        )
    }

    editingAggregateItem?.let { group ->
        PackingItemEditDialog(
            title = "Edit packing item",
            initialTitle = group.title,
            initialQuantity = group.items.firstOrNull()?.quantity ?: 1,
            supportingText = "Applies to all ${group.totalCount} matching traveller entries.",
            onDismiss = { editingAggregateItem = null },
            onSave = { updatedTitle, updatedQuantity ->
                viewModel.updateAggregateItem(group, updatedTitle, updatedQuantity)
                editingAggregateItem = null
            }
        )
    }
}

@Composable
private fun PackingItemEditDialog(
    title: String,
    initialTitle: String,
    initialQuantity: Int,
    supportingText: String?,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var itemTitle by remember(initialTitle) { mutableStateOf(initialTitle) }
    var quantityText by remember(initialQuantity) { mutableStateOf(initialQuantity.toString()) }

    val parsedQuantity = quantityText.toIntOrNull()
    val isValid = itemTitle.trim().isNotBlank() && parsedQuantity != null && parsedQuantity > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                supportingText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = itemTitle,
                    onValueChange = { itemTitle = it },
                    label = { Text("Item") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(itemTitle.trim(), parsedQuantity ?: 1) },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
