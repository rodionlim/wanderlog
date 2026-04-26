package com.wanderlog.android.presentation.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wanderlog.android.core.ui.component.WanderTopBar
import com.wanderlog.android.core.util.toCurrencyString
import com.wanderlog.android.domain.model.ExpenseCategory

@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val displayedExpenses = if (state.filterCategory == null) state.expenses
                            else state.expenses.filter { it.category == state.filterCategory }

    Scaffold(
        topBar = { WanderTopBar(title = "Budget", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::toggleAddForm) {
                Icon(Icons.Default.Add, "Add expense")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                state.budget?.let { budget ->
                    val progress = (state.totalSpent / budget).coerceIn(0.0, 1.0).toFloat()
                    Column {
                        Text("Spent: ${state.totalSpent.toCurrencyString(state.currencyCode)} / ${budget.toCurrencyString(state.currencyCode)}")
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    }
                } ?: Text("Total spent: ${state.totalSpent.toCurrencyString(state.currencyCode)}")
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
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        .clickable { viewModel.editExpense(expense) }
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(expense.title, style = MaterialTheme.typography.titleMedium)
                            Text(expense.category.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(expense.amount.toCurrencyString(expense.currencyCode),
                            style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { viewModel.deleteExpense(expense) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}
