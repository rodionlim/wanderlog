package com.wanderlog.android.presentation.packing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wanderlog.android.core.ui.component.WanderTopBar

@Composable
fun PackingScreen(
    onBack: () -> Unit,
    viewModel: PackingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { WanderTopBar(title = "Packing List", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.newItemText,
                        onValueChange = viewModel::onNewItemTextChange,
                        label = { Text("Add item") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    TextButton(onClick = viewModel::addItem) {
                        Icon(Icons.Default.Add, "Add")
                    }
                }
            }

            items(state.items, key = { it.id }) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.isChecked,
                        onCheckedChange = { viewModel.toggleItem(item) }
                    )
                    Text(
                        text = item.title,
                        modifier = Modifier.weight(1f),
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                    )
                    IconButton(onClick = { viewModel.deleteItem(item) }) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            }
        }
    }
}
