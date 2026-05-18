package com.wanderlog.android.presentation.budget

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wanderlog.android.core.util.ApproximateCurrencyConverter
import com.wanderlog.android.core.util.BudgetDisplayCurrencies
import com.wanderlog.android.core.ui.component.WanderTopBar
import com.wanderlog.android.core.util.toCurrencyString
import com.wanderlog.android.domain.model.ExpenseCategory
import androidx.compose.material3.DatePicker
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var expenseCurrencyMenuExpanded by remember { mutableStateOf(false) }
    var showAddOptions by remember { mutableStateOf(false) }
    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val categoryFilteredExpenses = if (state.filterCategory == null) state.expenses
                            else state.expenses.filter { it.category == state.filterCategory }
    val displayedExpenses = categoryFilteredExpenses.filter { expense ->
        matchesBudgetSearch(expense.title, searchQuery) ||
            matchesBudgetSearch(expense.category.name, searchQuery)
    }
    val displayedTotalSpent = displayedExpenses.sumOf { expense ->
        ApproximateCurrencyConverter.convert(
            amount = expense.amount,
            fromCurrency = expense.currencyCode,
            toCurrency = state.displayCurrencyCode
        )
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let(viewModel::startPhotoImport)
    }

    Scaffold(
        topBar = {
            WanderTopBar(
                title = "Budget",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { searchVisible = !searchVisible }) {
                        Icon(
                            imageVector = if (searchVisible) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (searchVisible) "Close search" else "Search budget items"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddOptions = true }) {
                Icon(Icons.Default.Add, "Add expense")
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                state.convertedBudget?.let { convertedBudget ->
                    val progress = (displayedTotalSpent / convertedBudget).coerceIn(0.0, 1.0).toFloat()
                    Column {
                        Text("Spent: ${displayedTotalSpent.toCurrencyString(state.displayCurrencyCode)} / ${convertedBudget.toCurrencyString(state.displayCurrencyCode)}")
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                        val originalBudget = state.budget
                        if (originalBudget != null && state.tripCurrencyCode != state.displayCurrencyCode) {
                            Text(
                                "Trip budget entered as ${originalBudget.toCurrencyString(state.tripCurrencyCode)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } ?: Text("Total spent: ${displayedTotalSpent.toCurrencyString(state.displayCurrencyCode)}")
                Text(
                    "Display currency: ${state.displayCurrencyCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.usesApproximateConversion) {
                    Text(
                        "Approximate offline FX rates are used when expenses were recorded in different currencies. Change the display currency in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(selected = state.filterCategory == null, onClick = { viewModel.filterByCategory(null) }, label = { Text("All") })
                    }
                    items(ExpenseCategory.values()) { cat ->
                        FilterChip(
                            selected = state.filterCategory == cat,
                            onClick = { viewModel.filterByCategory(if (state.filterCategory == cat) null else cat) },
                            label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (searchVisible) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search headers") },
                        placeholder = { Text("Try food*, *ticket, or taxi") },
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Text("Tag Expense to Day")
                    }
                }
            }

            if (state.showAddForm) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (state.editingExpenseId == null) "Add Expense" else "Edit Expense",
                                style = MaterialTheme.typography.titleMedium
                            )
                            OutlinedTextField(value = state.addTitle, onValueChange = viewModel::onTitleChange, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = state.addAmount, onValueChange = viewModel::onAmountChange, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Expense currency", style = MaterialTheme.typography.bodySmall)
                                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { expenseCurrencyMenuExpanded = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("${BudgetDisplayCurrencies.labelFor(state.addCurrencyCode)} (${state.addCurrencyCode})")
                                    }
                                    DropdownMenu(
                                        expanded = expenseCurrencyMenuExpanded,
                                        onDismissRequest = { expenseCurrencyMenuExpanded = false }
                                    ) {
                                        BudgetDisplayCurrencies.options.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text("${option.label} (${option.code})") },
                                                onClick = {
                                                    viewModel.onCurrencyCodeChange(option.code)
                                                    expenseCurrencyMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                ExpenseCategory.values().forEach { cat ->
                                    FilterChip(selected = state.addCategory == cat, onClick = { viewModel.onCategoryChange(cat) }, label = { Text(cat.name.take(4)) })
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = viewModel::cancelEditing) { Text("Cancel") }
                                TextButton(onClick = viewModel::saveExpense) {
                                    Text(if (state.editingExpenseId == null) "Add" else "Save")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            items(displayedExpenses, key = { it.id }) { expense ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.editExpense(expense)
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        }
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(expense.title, style = MaterialTheme.typography.titleMedium)
                            Text(expense.category.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (expense.currencyCode != state.displayCurrencyCode) {
                                Text(
                                    "Original: ${expense.amount.toCurrencyString(expense.currencyCode)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        val displayAmount = ApproximateCurrencyConverter.convert(
                            amount = expense.amount,
                            fromCurrency = expense.currencyCode,
                            toCurrency = state.displayCurrencyCode
                        )
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text(
                                text = displayAmount.toCurrencyString(state.displayCurrencyCode),
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (expense.currencyCode != state.displayCurrencyCode) {
                                Text(
                                    text = "Approx.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = { viewModel.duplicateExpense(expense) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Duplicate",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteExpense(expense) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddOptions) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddOptions = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add to Budget",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Choose how you want to add expenses.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        showAddOptions = false
                        viewModel.openAddForm()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manual entry")
                }
                OutlinedButton(
                    onClick = {
                        showAddOptions = false
                        photoPicker.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload photo")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    when (val importStep = state.photoImportStep) {
        BudgetPhotoImportStep.Idle -> Unit
        BudgetPhotoImportStep.Parsing,
        is BudgetPhotoImportStep.Review,
        is BudgetPhotoImportStep.Error -> {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = viewModel::resetPhotoImport,
                sheetState = sheetState
            ) {
                BudgetPhotoImportSheet(
                    step = importStep,
                    onUpdateExpense = viewModel::updateImportedExpense,
                    onRemoveExpense = viewModel::removeImportedExpense,
                    onDismiss = viewModel::resetPhotoImport,
                    onCommit = viewModel::commitImportedExpenses,
                    onReset = viewModel::resetPhotoImport
                )
            }
        }
    }

    if (showDatePicker) {
        val selectedDateMillis = state.selectedDate?.atStartOfDay(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.onDateChange(
                            Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            Surface {
                DatePicker(state = datePickerState)
            }
        }
    }
}

private fun matchesBudgetSearch(value: String, query: String): Boolean {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isBlank()) return true

    val text = value.trim()
    if (text.isBlank()) return false

    val normalizedText = text.lowercase()
    val normalizedQuery = trimmedQuery.lowercase()
    if (!normalizedQuery.contains('*') && !normalizedQuery.contains('?')) {
        return normalizedText.contains(normalizedQuery)
    }

    val pattern = normalizedQuery
        .replace(".", "\\.")
        .replace("*", ".*")
        .replace("?", ".")
    return normalizedText.matches(Regex("^$pattern$"))
}
