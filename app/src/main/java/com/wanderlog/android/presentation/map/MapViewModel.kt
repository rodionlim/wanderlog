package com.wanderlog.android.presentation.map

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.ItineraryItemType
import com.wanderlog.android.domain.repository.ItineraryRepository
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val items: List<ItineraryItem> = emptyList(),
    val selectedItemId: String? = null,
    val mapError: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val itineraryRepository: ItineraryRepository,
    private val tripRepository: TripRepository,
    @ApplicationContext context: Context
) : ViewModel() {

    private val tripId = savedStateHandle.get<String>(Screen.Map.ARG_TRIP_ID)!!
    private val dayId = savedStateHandle.get<String>(Screen.Map.ARG_DAY_ID)

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    init {
        _state.update { it.copy(mapError = resolveMapError(context)) }
        viewModelScope.launch {
            val tripDestination = tripRepository.getTripById(tripId)?.destination.orEmpty()
            val flow = if (dayId != null)
                itineraryRepository.getItemsForDay(dayId) // dayId specific
            else
                itineraryRepository.getItemsForTrip(tripId)

            flow.collect { all ->
                val mappable = all.filter { item ->
                    item.place?.latitude != null &&
                        item.place.longitude != null &&
                        shouldPlotOnMap(item, tripDestination)
                }
                _state.update { it.copy(items = mappable) }
            }
        }
    }

    fun selectItem(id: String?) = _state.update { it.copy(selectedItemId = id) }

    private fun shouldPlotOnMap(item: ItineraryItem, tripDestination: String): Boolean {
        if (item.itemType != ItineraryItemType.FLIGHT) {
            return true
        }
        if (tripDestination.isBlank()) {
            return true
        }

        val placeText = listOfNotNull(item.place?.name, item.place?.address)
            .joinToString(" ")
            .normalizeForComparison()
        val destinationText = tripDestination.normalizeForComparison()

        return placeText.isNotBlank() && placeText.contains(destinationText)
    }

    private fun String.normalizeForComparison(): String =
        trim().lowercase().replace(Regex("[^a-z0-9]+"), " ")

    private fun resolveMapError(context: Context): String? {
        val manifestKey = runCatching {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            appInfo.metaData?.getString("com.google.android.geo.API_KEY").orEmpty().trim()
        }.getOrDefault("")

        return when {
            manifestKey.isBlank() || manifestKey.contains("\${") -> {
                "Google Maps needs MAPS_API_KEY in local.properties and an app rebuild. The runtime key in Settings helps place search, but the map screen uses the manifest SDK key."
            }
            else -> null
        }
    }
}
