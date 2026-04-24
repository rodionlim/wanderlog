package com.wanderlog.android.presentation.attachments

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.Attachment
import com.wanderlog.android.domain.repository.AttachmentRepository
import com.wanderlog.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttachmentsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AttachmentRepository
) : ViewModel() {

    val tripId: String = savedStateHandle.get<String>(Screen.Attachments.ARG_TRIP_ID)!!

    val attachments: StateFlow<List<Attachment>> = repository.getAttachmentsForTrip(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    fun import(uri: Uri, label: String?) {
        viewModelScope.launch {
            _isImporting.value = true
            runCatching { repository.importFromUri(tripId, uri, label) }
                .onFailure { _error.value = it.message ?: "Import failed" }
            _isImporting.value = false
        }
    }

    fun delete(attachment: Attachment) {
        viewModelScope.launch { repository.delete(attachment) }
    }

    fun clearError() { _error.value = null }
}
