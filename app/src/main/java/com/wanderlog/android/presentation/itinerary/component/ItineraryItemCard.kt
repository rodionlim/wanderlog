package com.wanderlog.android.presentation.itinerary.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.ItineraryItemType

@Composable
fun ItineraryItemCard(
    item: ItineraryItem,
    modifier: Modifier = Modifier,
    dragHandle: @Composable (() -> Unit)? = null
) {
    val accent = item.itemType.accent()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored badge with the type icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.itemType.icon(),
                    contentDescription = item.itemType.name,
                    modifier = Modifier.size(22.dp),
                    tint = accent
                )
            }
            Spacer(Modifier.width(12.dp))

            // Title + metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                val hasMeta = item.startTime != null || item.place?.address != null
                if (hasMeta) Spacer(Modifier.height(3.dp))

                item.startTime?.let {
                    val time = if (item.endTime != null) "$it – ${item.endTime}" else it
                    Text(
                        time,
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        fontWeight = FontWeight.Medium
                    )
                }
                item.place?.address?.let { addr ->
                    Text(
                        addr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            dragHandle?.invoke()
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Drag",
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

private fun ItineraryItemType.icon(): ImageVector = when (this) {
    ItineraryItemType.FLIGHT -> Icons.Default.Flight
    ItineraryItemType.HOTEL -> Icons.Default.Hotel
    ItineraryItemType.ACTIVITY -> Icons.Default.Map
    ItineraryItemType.NOTE -> Icons.AutoMirrored.Filled.Note
    ItineraryItemType.TRANSPORT -> Icons.Default.Train
    ItineraryItemType.PLACE -> Icons.Default.Place
}

private fun ItineraryItemType.accent(): Color = when (this) {
    ItineraryItemType.FLIGHT -> Color(0xFF2D7FF9)      // sky blue
    ItineraryItemType.HOTEL -> Color(0xFF8B5CF6)       // purple
    ItineraryItemType.ACTIVITY -> Color(0xFFEF4444)    // red
    ItineraryItemType.NOTE -> Color(0xFF6B7280)        // slate
    ItineraryItemType.TRANSPORT -> Color(0xFF10B981)   // emerald
    ItineraryItemType.PLACE -> Color(0xFFF59E0B)       // amber
}
