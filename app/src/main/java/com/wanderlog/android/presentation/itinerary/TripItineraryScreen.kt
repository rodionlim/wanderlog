package com.wanderlog.android.presentation.itinerary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wanderlog.android.core.ui.component.ConfirmDialog
import com.wanderlog.android.core.ui.component.WanderTopBar
import com.wanderlog.android.core.ui.component.destinationVisualFor
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.presentation.ai.fileImport.FileImportSheet
import com.wanderlog.android.presentation.itinerary.component.ItineraryItemCard
import com.wanderlog.android.presentation.itinerary.form.ItineraryItemFormSheet
import com.wanderlog.android.presentation.placeSearch.PlaceSearchSheet
import coil.compose.AsyncImage
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TripItineraryScreen(
    tripId: String,
    onBack: () -> Unit,
    onOpenMap: (String?) -> Unit,
    onOpenBudget: () -> Unit,
    onOpenPacking: () -> Unit,
    onOpenAiGenerate: () -> Unit,
    onOpenAttachments: () -> Unit,
    onOpenAttachment: (String) -> Unit,
    viewModel: TripItineraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val items by viewModel.itemsForDay.collectAsState()

    var showItemForm by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ItineraryItem?>(null) }
    var showPlaceSearch by remember { mutableStateOf(false) }
    var showFileImport by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ItineraryItem?>(null) }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val mutable = items.toMutableList()
        mutable.add(to.index, mutable.removeAt(from.index))
        viewModel.reorderItems(mutable)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.tripName) },
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
                        Icon(Icons.Default.Money, contentDescription = "Budget")
                    }
                    IconButton(onClick = onOpenPacking) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Packing")
                    }
                    IconButton(onClick = onOpenAiGenerate) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Generate")
                    }
                    IconButton(onClick = { showFileImport = true }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Import file")
                    }
                    IconButton(onClick = onOpenAttachments) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Attachments")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingItem = null; showItemForm = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.days.isEmpty()) {
                Text("Loading...", modifier = Modifier.align(Alignment.Center))
            } else {
                val visual = destinationVisualFor(state.tripDestination)
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
                                    "${state.days.size} day${if (state.days.size != 1) "s" else ""}",
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

                    if (items.isEmpty()) {
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
                                            onClick = {
                                                editingItem = item
                                                showItemForm = true
                                            },
                                            onOpenAttachment = item.localAttachmentId()?.let { attachmentId ->
                                                { onOpenAttachment(attachmentId) }
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
                    editingItem = editingItem,
                    onDismiss = { showItemForm = false },
                    onDeleteRequested = editingItem?.let { item ->
                        {
                            showItemForm = false
                            itemToDelete = item
                        }
                    },
                    onPlaceSearchRequested = { showPlaceSearch = true }
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
            PlaceSearchSheet(onDismiss = { showPlaceSearch = false })
        }
    }

    if (showFileImport) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFileImport = false },
            sheetState = sheetState
        ) {
            FileImportSheet(
                tripId = tripId,
                onDismiss = { showFileImport = false }
            )
        }
    }

    itemToDelete?.let { item ->
        ConfirmDialog(
            title = "Delete item",
            message = "Remove \"${item.title}\"?",
            onConfirm = { viewModel.deleteItem(item); itemToDelete = null },
            onDismiss = { itemToDelete = null }
        )
    }
}

private fun ItineraryItem.localAttachmentId(): String? =
    confirmationUrl
        ?.takeIf { it.startsWith("attachment://") }
        ?.removePrefix("attachment://")
