package com.wanderlog.android.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.wanderlog.android.core.ui.component.WanderTopBar
import java.time.format.DateTimeFormatter

@Composable
fun MapScreen(
    onBack: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedDayKey by rememberSaveable { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    val visiblePoints = state.points

    val pointsByDay = remember(visiblePoints) {
        visiblePoints.groupBy { it.dayNumber ?: Int.MAX_VALUE }
            .toSortedMap()
    }

    val legendEntries = remember(state.points) {
        state.points
            .groupBy { it.dayNumber ?: Int.MAX_VALUE }
            .toSortedMap()
            .values
            .mapNotNull { points ->
                points.firstOrNull()?.let { point ->
                    LegendEntry(
                        dayKey = point.dayNumber ?: Int.MAX_VALUE,
                        point = point,
                        count = points.size
                    )
                }
            }
    }

    LaunchedEffect(legendEntries) {
        val currentSelection = selectedDayKey ?: return@LaunchedEffect
        if (legendEntries.none { it.dayKey == currentSelection }) {
            selectedDayKey = null
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        val firstPoint = visiblePoints.firstOrNull()?.item
        if (firstPoint?.place?.latitude != null) {
            position = CameraPosition.fromLatLngZoom(
                LatLng(firstPoint.place.latitude, firstPoint.place.longitude!!), 12f
            )
        }
    }

    LaunchedEffect(visiblePoints) {
        val latLngs = visiblePoints.mapNotNull { point ->
            val lat = point.item.place?.latitude ?: return@mapNotNull null
            val lng = point.item.place.longitude ?: return@mapNotNull null
            LatLng(lat, lng)
        }

        when (latLngs.size) {
            0 -> return@LaunchedEffect
            1 -> {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(latLngs.first(), 12f)
            }
            else -> {
                val bounds = LatLngBounds.builder().apply {
                    latLngs.forEach(::include)
                }.build()
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngBounds(bounds, 120)
                )
            }
        }
    }

    Scaffold(topBar = { WanderTopBar(title = "Map", onBack = onBack) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.mapError != null -> {
                    val mapError = state.mapError.orEmpty()
                    Text(
                        text = mapError,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }

                state.isResolving && state.points.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                    ) {
                        Surface(
                            modifier = Modifier.align(Alignment.Center),
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 8.dp,
                            shadowElevation = 2.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                Text(
                                    text = "Resolving places for the map...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                state.points.isEmpty() -> {
                    Text(
                        "No items with resolved map coordinates on this trip yet. Imported flights may need a better place match or a valid Maps key.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }

                else -> {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        onMapClick = { viewModel.selectItem(null) }
                    ) {
                        pointsByDay.values.forEachIndexed { index, dayPoints ->
                            val dayKey = dayPoints.firstOrNull()?.dayNumber ?: Int.MAX_VALUE
                            val isDimmed = selectedDayKey != null && dayKey != selectedDayKey
                            val color = dayColor(index)
                            dayPoints.forEachIndexed { pointIndex, point ->
                                val item = point.item
                                val lat = item.place?.latitude ?: return@forEachIndexed
                                val lng = item.place.longitude ?: return@forEachIndexed
                                val markerLabel = point.dayNumber?.let { dayNumber ->
                                    "Day $dayNumber • ${pointIndex + 1}"
                                } ?: item.title
                                Marker(
                                    state = MarkerState(position = LatLng(lat, lng)),
                                    title = markerLabel,
                                    snippet = listOfNotNull(item.title, item.place?.name, item.place?.address)
                                        .distinct()
                                        .joinToString("\n"),
                                    icon = BitmapDescriptorFactory.defaultMarker(dayMarkerHue(index)),
                                    alpha = if (isDimmed) 0.28f else 1f,
                                    zIndex = if (isDimmed) 0f else 1f,
                                    onClick = { viewModel.selectItem(item.id); false }
                                )
                            }

                            val dayPolylinePoints = dayPoints.mapNotNull { point ->
                                val lat = point.item.place?.latitude ?: return@mapNotNull null
                                val lng = point.item.place.longitude ?: return@mapNotNull null
                                LatLng(lat, lng)
                            }
                            if (dayPolylinePoints.size >= 2) {
                                Polyline(
                                    points = dayPolylinePoints,
                                    color = if (isDimmed) color.copy(alpha = 0.18f) else color,
                                    width = 8f
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        if (legendEntries.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                tonalElevation = 6.dp,
                                shadowElevation = 2.dp,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    legendEntries.forEachIndexed { index, entry ->
                                        CompactDayLegendChip(
                                            point = entry.point,
                                            count = entry.count,
                                            color = dayColor(index),
                                            selected = entry.dayKey == selectedDayKey,
                                            onClick = {
                                                selectedDayKey = if (selectedDayKey == entry.dayKey) {
                                                    null
                                                } else {
                                                    entry.dayKey
                                                }
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
}

private data class LegendEntry(
    val dayKey: Int,
    val point: MapPointUi,
    val count: Int
)

@Composable
private fun CompactDayLegendChip(
    point: MapPointUi,
    count: Int,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val dayNumberText = point.dayNumber?.toString() ?: "?"
    val dateLabel = point.dayDate?.format(DateTimeFormatter.ofPattern("MMM d")).orEmpty()

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) color.copy(alpha = 0.70f) else color.copy(alpha = 0.28f),
                shape = RoundedCornerShape(999.dp)
            )
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) color.copy(alpha = 0.08f) else Color.Transparent)
            .padding(0.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color)
                    .border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayNumberText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                if (dateLabel.isNotBlank()) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun dayColor(index: Int): Color = when (index % 6) {
    0 -> Color(0xFF1565C0)
    1 -> Color(0xFF2E7D32)
    2 -> Color(0xFFEF6C00)
    3 -> Color(0xFF6A1B9A)
    4 -> Color(0xFFC62828)
    else -> Color(0xFF00838F)
}

private fun dayMarkerHue(index: Int): Float = when (index % 6) {
    0 -> BitmapDescriptorFactory.HUE_AZURE
    1 -> BitmapDescriptorFactory.HUE_GREEN
    2 -> BitmapDescriptorFactory.HUE_ORANGE
    3 -> BitmapDescriptorFactory.HUE_VIOLET
    4 -> BitmapDescriptorFactory.HUE_RED
    else -> BitmapDescriptorFactory.HUE_CYAN
}
