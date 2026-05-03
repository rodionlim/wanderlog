package com.wanderlog.android.domain.repository

import android.net.Uri
import com.wanderlog.android.domain.model.Attachment
import kotlinx.coroutines.flow.Flow
import java.io.File

interface AttachmentRepository {
    fun getAttachmentsForTrip(tripId: String): Flow<List<Attachment>>
    suspend fun getById(id: String): Attachment?
    suspend fun importFromUri(tripId: String, uri: Uri, label: String?, tags: List<String> = emptyList()): Attachment
    suspend fun update(attachment: Attachment)
    suspend fun delete(attachment: Attachment)
    suspend fun readText(attachment: Attachment): String
    fun getFile(attachment: Attachment): File
}
