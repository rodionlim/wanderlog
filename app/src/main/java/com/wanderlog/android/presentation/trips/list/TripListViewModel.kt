package com.wanderlog.android.presentation.trips.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.usecase.trip.DeleteTripUseCase
import com.wanderlog.android.domain.usecase.trip.GetTripsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripListViewModel @Inject constructor(
    getTrips: GetTripsUseCase,
    private val deleteTrip: DeleteTripUseCase
) : ViewModel() {

    val trips: StateFlow<List<Trip>> = getTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch { deleteTrip.invoke(trip) }
    }
}
