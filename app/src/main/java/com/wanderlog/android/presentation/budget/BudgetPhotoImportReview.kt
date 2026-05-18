package com.wanderlog.android.presentation.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wanderlog.android.core.util.BudgetDisplayCurrencies
import com.wanderlog.android.domain.model.ExpenseCategory
import com.wanderlog.android.domain.model.ParsedBudgetExpense

@Composable
fun BudgetPhotoImportSheet(
    step: BudgetPhotoImportStep,
    onUpdateExpense: (ParsedBudgetExpense) -> Unit,
    onRemoveExpense: (String) -> Unit,
    onDismiss: () -> Unit,
    onCommit: () -> Unit,
    onReset: () -> Unit
) {
    when (step) {
        BudgetPhotoImportStep.Parsing -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Parsing photo…", style = MaterialTheme.typography.titleLarge)
                Text(
                    "We're extracting expense entries from the image so you can review them before adding them to Budget.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        is BudgetPhotoImportStep.Review -> BudgetPhotoImportReviewContent(
            items = step.items,
            onUpdateExpense = onUpdateExpense,
            onRemoveExpense = onRemoveExpense,
            onDismiss = onDismiss,
            onCommit = onCommit,
            onReset = onReset
        )

        is BudgetPhotoImportStep.Error -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Could not parse photo", style = MaterialTheme.typography.titleLarge)
            Text(
                step.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                    Text("Dismiss")
                }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Try again")
                }
            }
        }

        BudgetPhotoImportStep.Idle -> Unit
    }
}

@Composable
private fun BudgetPhotoImportReviewContent(
    items: List<ParsedBudgetExpense>,
    onUpdateExpense: (ParsedBudgetExpense) -> Unit,
    onRemoveExpense: (String) -> Unit,
    onDismiss: () -> Unit,
    onCommit: () -> Unit,
    onReset: () -> Unit
) {
    var editingExpenseId by remember { mutableStateOf<String?>(null) }

    Text("Review extracted expenses (${items.size})", style = MaterialTheme.typography.titleMedium)
    Text(
        "Edit the parsed entries before saving them to Budget.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.id }) { item ->
            BudgetExpenseCard(
                item = item,
                onEdit = { editingExpenseId = item.id },
                onRemove = { onRemoveExpense(item.id) }
            )
        }
    }

    items.firstOrNull { it.id == editingExpenseId }?.let { item ->
        BudgetExpenseEditDialog(
            item = item,
            onDismiss = { editingExpenseId = null },
            onSave = { updatedItem ->
                onUpdateExpense(updatedItem)
                editingExpenseId = null
            }
        )
    }

    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
            Text("Cancel")
        }
        Button(onClick = onCommit, modifier = Modifier.weight(1f), enabled = items.isNotEmpty()) {
            Text("Add to Budget")
        }
    }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onDismiss) {
        Text("Close")
    }
}

@Composable
private fun BudgetExpenseCard(
    item: ParsedBudgetExpense,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${item.amountText} ${item.currencyCode} • ${item.category.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                item.dateText?.takeIf { it.isNotBlank() }?.let { dateText ->
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    Text(notes, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onEdit) {
                Text("Edit")
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove expense")
            }
        }
    }
}

@Composable
private fun BudgetExpenseEditDialog(
    item: ParsedBudgetExpense,
    onDismiss: () -> Unit,
    onSave: (ParsedBudgetExpense) -> Unit
) {
    var title by remember(item.id) { mutableStateOf(item.title) }
    var amountText by remember(item.id) { mutableStateOf(item.amountText) }
    var currencyCode by remember(item.id) { mutableStateOf(BudgetDisplayCurrencies.sanitize(item.currencyCode)) }
    var notes by remember(item.id) { mutableStateOf(item.notes.orEmpty()) }
    var dateText by remember(item.id) { mutableStateOf(item.dateText.orEmpty()) }
    var category by remember(item.id) { mutableStateOf(item.category) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { currencyMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${BudgetDisplayCurrencies.labelFor(currencyCode)} (${currencyCode})")
                    }
                    DropdownMenu(
                        expanded = currencyMenuExpanded,
                        onDismissRequest = { currencyMenuExpanded = false }
                    ) {
                        BudgetDisplayCurrencies.options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("${option.label} (${option.code})") },
                                onClick = {
                                    currencyCode = option.code
                                    currencyMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Text("Category", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExpenseCategory.values().forEach { expenseCategory ->
                        FilterChip(
                            selected = category == expenseCategory,
                            onClick = { category = expenseCategory },
                            label = { Text(expenseCategory.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    item.copy(
                        title = title.trim().ifBlank { item.title },
                        amountText = amountText.trim().ifBlank { item.amountText },
                        currencyCode = currencyCode,
                        category = category,
                        dateText = dateText.trim().takeIf { it.isNotBlank() },
                        notes = notes.trim().takeIf { it.isNotBlank() }
                    )
                )
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
