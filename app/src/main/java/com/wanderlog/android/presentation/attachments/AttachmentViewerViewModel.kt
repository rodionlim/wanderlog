package com.wanderlog.android.presentation.attachments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.Attachment
import com.wanderlog.android.domain.repository.AttachmentRepository
import com.wanderlog.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class AttachmentViewerState(
    val attachment: Attachment? = null,
    val textContent: String? = null,
    val file: File? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AttachmentViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AttachmentRepository
) : ViewModel() {

    private val attachmentId: String = savedStateHandle.get<String>(Screen.AttachmentViewer.ARG_ATTACHMENT_ID)!!

    private val _state = MutableStateFlow(AttachmentViewerState())
    val state: StateFlow<AttachmentViewerState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val attachment = repository.getById(attachmentId)
            if (attachment == null) {
                _state.value = AttachmentViewerState(isLoading = false, error = "Not found")
                return@launch
            }
            val file = repository.getFile(attachment)
            val text = if (attachment.isTextLike()) runCatching { repository.readText(attachment) }.getOrNull() else null
            _state.value = AttachmentViewerState(
                attachment = attachment,
                textContent = text,
                file = file,
                isLoading = false
            )
        }
    }
}

fun Attachment.isTextLike(): Boolean =
    mimeType.startsWith("text/") ||
        mimeType == "application/json" ||
        mimeType == "application/xml" ||
        displayName.endsWith(".md", ignoreCase = true) ||
        displayName.endsWith(".txt", ignoreCase = true)

fun Attachment.isImage(): Boolean = mimeType.startsWith("image/")
