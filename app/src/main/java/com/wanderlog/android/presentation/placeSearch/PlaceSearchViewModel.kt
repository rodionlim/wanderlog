package com.wanderlog.android.presentation.placeSearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.Place
import com.wanderlog.android.domain.repository.PlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaceSearchState(
    val query: String = "",
    val results: List<Place> = emptyList(),
    val selectedPlace: Place? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class PlaceSearchViewModel @Inject constructor(
    private val placesRepository: PlacesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PlaceSearchState())
    val state: StateFlow<PlaceSearchState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        observeQuery()
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeQuery() {
        queryFlow
            .debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                flow {
                    if (query.isBlank()) { emit(emptyList<Place>()); return@flow }
                    _state.update { it.copy(isLoading = true) }
                    val results = runCatching { placesRepository.searchPlaces(query, null) }.getOrDefault(emptyList())
                    emit(results)
                }
            }
            .onEach { results -> _state.update { it.copy(results = results, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q) }
        queryFlow.value = q
    }

    fun initializeQuery(q: String?) {
        val normalized = q?.trim().orEmpty()
        _state.update { it.copy(query = normalized, selectedPlace = null) }
        queryFlow.value = normalized
    }

    fun selectPlace(place: Place) {
        viewModelScope.launch {
            val details = runCatching {
                if (place.placeId != null) placesRepository.fetchPlaceDetails(place.placeId) else place
            }.getOrDefault(place)
            _state.update { it.copy(selectedPlace = details) }
        }
    }

    fun clearSelection() = _state.update { it.copy(selectedPlace = null) }

    fun reset() {
        _state.value = PlaceSearchState()
        queryFlow.value = ""
    }
}
