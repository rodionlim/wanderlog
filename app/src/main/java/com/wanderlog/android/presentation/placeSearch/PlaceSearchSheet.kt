package com.wanderlog.android.presentation.placeSearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wanderlog.android.domain.model.Place

@Composable
fun PlaceSearchSheet(
    initialQuery: String? = null,
    onDismiss: () -> Unit,
    onPlaceSelected: ((Place) -> Unit)? = null,
    viewModel: PlaceSearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.selectedPlace) {
        if (state.selectedPlace != null) {
            onPlaceSelected?.invoke(state.selectedPlace!!)
            viewModel.clearSelection()
            onDismiss()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Search Places", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            label = { Text("Type a place or address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = { if (state.isLoading) CircularProgressIndicator() }
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(state.results, key = { it.placeId ?: it.name }) { place ->
                ListItem(
                    headlineContent = { Text(place.name) },
                    supportingContent = place.address?.let { { Text(it, maxLines = 1) } },
                    modifier = Modifier.clickable { viewModel.selectPlace(place) }
                )
                HorizontalDivider()
            }
        }
    }

    LaunchedEffect(initialQuery) {
        viewModel.initializeQuery(initialQuery)
    }
}
