package com.wanderlog.android.presentation.itinerary

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wanderlog.android.core.ui.component.ConfirmDialog
import com.wanderlog.android.core.ui.component.WanderTopBar
import com.wanderlog.android.core.ui.component.destinationVisualFor
import com.wanderlog.android.core.util.toCompactSlashDisplay
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.Place
import com.wanderlog.android.presentation.ai.fileImport.ImportSheet
import com.wanderlog.android.presentation.itinerary.component.ItineraryItemCard
import com.wanderlog.android.presentation.itinerary.form.ItineraryItemFormSheet
import com.wanderlog.android.presentation.placeSearch.PlaceSearchSheet
import coil.compose.AsyncImage
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TripItineraryScreen(
    tripId: String,
    onBack: () -> Unit,
    onOpenMap: (String?) -> Unit,
    onOpenBudget: () -> Unit,
    onOpenPacking: () -> Unit,
    onOpenAiGenerate: () -> Unit,
    onOpenAskTrip: () -> Unit,
    onOpenItemAttachments: (String) -> Unit,
    onOpenSync: () -> Unit,
    onOpenAttachments: () -> Unit,
    viewModel: TripItineraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val rawItems by viewModel.itemsForDay.collectAsState()
    val uriHandler = LocalUriHandler.current
    val activeHotels = state.activeHotelsForSelectedDay
    val items = remember(rawItems) {
        rawItems.sortedWith(
            compareBy<ItineraryItem>(
                { it.startTime == null },
                { parseItinerarySortDateTime(it.startTime) },
                { it.sortOrder },
                { it.title.lowercase() }
            )
        )
    }

    var showItemForm by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ItineraryItem?>(null) }
    var showPlaceSearch by remember { mutableStateOf(false) }
    var selectedPlaceForForm by remember { mutableStateOf<Place?>(null) }
    var initialPlaceQuery by remember { mutableStateOf<String?>(null) }
    var showFileImport by remember { mutableStateOf(false) }
    var showAddOptions by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ItineraryItem?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val mutable = items.toMutableList()
        mutable.add(to.index, mutable.removeAt(from.index))
        viewModel.reorderItems(mutable)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.tripName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenMap(null) }) {
                        Icon(Icons.Default.Map, contentDescription = "Map")
                    }
                    IconButton(onClick = onOpenBudget) {
                        Icon(Icons.Default.AttachMoney, contentDescription = "Budget")
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Packing") },
                                leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenPacking()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ask About Trip") },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenAskTrip()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("AI Generate") },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenAiGenerate()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sync trip") },
                                leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenSync()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import") },
                                leadingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showFileImport = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Attachments") },
                                leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenAttachments()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddOptions = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.days.isEmpty()) {
                Text("Loading...", modifier = Modifier.align(Alignment.Center))
            } else {
                val visual = destinationVisualFor(state.tripDestination)
                val selectedDay = state.days.getOrNull(state.selectedDayIndex) ?: state.days.firstOrNull()
                androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
                    // Destination hero strip
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(visual.gradientStart, visual.gradientEnd)
                                )
                            )
                    ) {
                        state.tripCoverImageUri?.takeIf { it.isNotBlank() }?.let { cover ->
                            AsyncImage(
                                model = cover,
                                contentDescription = state.tripDestination,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.52f))
                                        )
                                    )
                            )
                        }
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                visual.emoji,
                                fontSize = if (state.tripCoverImageUri.isNullOrBlank()) 40.sp else 28.sp
                            )
                            androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
                            androidx.compose.foundation.layout.Column {
                                Text(
                                    state.tripDestination.ifBlank { "Your trip" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    buildString {
                                        selectedDay?.let {
                                            append("Day ${it.dayNumber} • ${it.date.toCompactSlashDisplay()}")
                                        }
                                        if (state.days.isNotEmpty()) {
                                            if (isNotEmpty()) append("   ")
                                            append("${state.days.size} day")
                                            if (state.days.size != 1) append("s")
                                        }
                                    },
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    ScrollableTabRow(selectedTabIndex = state.selectedDayIndex) {
                        state.days.forEachIndexed { index, day ->
                            Tab(
                                selected = state.selectedDayIndex == index,
                                onClick = { viewModel.selectDay(index) },
                                text = { Text("Day ${day.dayNumber}") }
                            )
                        }
                    }

                    if (items.isEmpty() && activeHotels.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No items. Tap + to add.")
                        }
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activeHotels.isNotEmpty()) {
                                item(key = "active-hotels-header") {
                                    Text(
                                        text = "Where you're staying",
                                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                items(activeHotels, key = { "active-hotel-${it.id}" }) { item ->
                                    ItineraryItemCard(
                                        item = item,
                                        linkedExpense = item.linkedExpenseId?.let(state.linkedExpensesById::get),
                                        attachmentCount = state.attachmentCountsByItemId[item.id] ?: 0,
                                        onOpenInMaps = item.place?.toGoogleMapsUrl()?.let { mapsUrl ->
                                            { uriHandler.openUri(mapsUrl) }
                                        },
                                        onManageAttachments = if ((state.attachmentCountsByItemId[item.id] ?: 0) > 0) {
                                            { onOpenItemAttachments(item.id) }
                                        } else {
                                            null
                                        }
                                    )
                                }

                                if (items.isNotEmpty()) {
                                    item(key = "active-hotels-divider") {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                }
                            }

                            items(items, key = { it.id }) { item ->
                                ReorderableItem(reorderableState, key = item.id) { isDragging ->
                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { value ->
                                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                                itemToDelete = item
                                            }
                                            false // don't dismiss — handled by confirm dialog
                                        }
                                    )
                                    SwipeToDismissBox(
                                        state = dismissState,
                                        backgroundContent = {}
                                    ) {
                                        ItineraryItemCard(
                                            item = item,
                                            linkedExpense = item.linkedExpenseId?.let(state.linkedExpensesById::get),
                                            attachmentCount = state.attachmentCountsByItemId[item.id] ?: 0,
                                            onClick = {
                                                editingItem = item
                                                showItemForm = true
                                            },
                                            onOpenInMaps = item.place?.toGoogleMapsUrl()?.let { mapsUrl ->
                                                { uriHandler.openUri(mapsUrl) }
                                            },
                                            onManageAttachments = if ((state.attachmentCountsByItemId[item.id] ?: 0) > 0) {
                                                { onOpenItemAttachments(item.id) }
                                            } else {
                                                null
                                            },
                                            dragHandle = {
                                                Icon(
                                                    Icons.Default.DragHandle,
                                                    contentDescription = "Drag",
                                                    tint = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
                                                    modifier = Modifier.draggableHandle()
                                                )
                                            }
                                        )
                                    }
                                }
                            }
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
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add to Trip",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Choose how you want to add something to this trip.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        showAddOptions = false
                        showFileImport = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import from file / clipboard")
                }
                OutlinedButton(
                    onClick = {
                        showAddOptions = false
                        editingItem = null
                        showItemForm = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manual Entry")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showItemForm) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showItemForm = false },
            sheetState = sheetState
        ) {
            val selectedDay = state.days.getOrNull(state.selectedDayIndex)
            if (selectedDay != null) {
                ItineraryItemFormSheet(
                    tripId = tripId,
                    dayId = selectedDay.id,
                    dayDate = selectedDay.date,
                    currencyCode = state.tripCurrencyCode,
                    editingItem = editingItem,
                    linkedExpense = editingItem?.linkedExpenseId?.let(state.linkedExpensesById::get),
                    selectedPlace = selectedPlaceForForm,
                    onSelectedPlaceApplied = { selectedPlaceForForm = null },
                    onDismiss = { showItemForm = false },
                    onDeleteRequested = editingItem?.let { item ->
                        {
                            showItemForm = false
                            itemToDelete = item
                        }
                    },
                    onManageAttachmentsRequested = editingItem?.let { item ->
                        {
                            showItemForm = false
                            onOpenItemAttachments(item.id)
                        }
                    },
                    onPlaceSearchRequested = { query ->
                        initialPlaceQuery = query
                        showPlaceSearch = true
                    }
                )
            }
        }
    }

    if (showPlaceSearch) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPlaceSearch = false },
            sheetState = sheetState
        ) {
            PlaceSearchSheet(
                initialQuery = initialPlaceQuery,
                onDismiss = { showPlaceSearch = false },
                onPlaceSelected = { place ->
                    selectedPlaceForForm = place
                    showPlaceSearch = false
                }
            )
        }
    }

    if (showFileImport) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFileImport = false },
            sheetState = sheetState
        ) {
            ImportSheet(
                tripId = tripId,
                onDismiss = { showFileImport = false }
            )
        }
    }

    itemToDelete?.let { item ->
        val isImportedGroup = (state.importAttachmentCountsByItemId[item.id] ?: 0) > 0
        ConfirmDialog(
            title = if (isImportedGroup) "Delete imported entries" else "Delete item",
            message = if (isImportedGroup) {
                "Remove \"${item.title}\" and the other itinerary entries, budget items, and attachment imported from the same file?"
            } else {
                "Remove \"${item.title}\"?"
            },
            onConfirm = { viewModel.deleteItem(item); itemToDelete = null },
            onDismiss = { itemToDelete = null }
        )
    }
}

private fun parseItinerarySortDateTime(value: String?): LocalDateTime? {
    val candidate = value?.trim().orEmpty()
    if (candidate.isBlank()) return null

    return runCatching { OffsetDateTime.parse(candidate).toLocalDateTime() }.getOrNull()
        ?: runCatching { ZonedDateTime.parse(candidate).toLocalDateTime() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(candidate) }.getOrNull()
}

private fun Place.toGoogleMapsUrl(): String? {
    val coordinates = listOfNotNull(latitude, longitude)
        .takeIf { it.size == 2 }
        ?.joinToString(",")

    val query = address?.takeIf { it.isNotBlank() }
        ?: name.takeIf { it.isNotBlank() }
        ?: coordinates
    return query?.let { "https://www.google.com/maps/search/?api=1&query=${Uri.encode(it)}" }
}
