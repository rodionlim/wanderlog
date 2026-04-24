package com.wanderlog.android.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.wanderlog.android.data.local.dao.AttachmentDao
import com.wanderlog.android.data.local.entity.AttachmentEntity
import com.wanderlog.android.domain.model.Attachment
import com.wanderlog.android.domain.repository.AttachmentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class AttachmentRepositoryImpl @Inject constructor(
    private val dao: AttachmentDao,
    @ApplicationContext private val context: Context
) : AttachmentRepository {

    override fun getAttachmentsForTrip(tripId: String): Flow<List<Attachment>> =
        dao.getAttachmentsForTrip(tripId).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Attachment? =
        dao.getById(id)?.toDomain()

    override suspend fun importFromUri(tripId: String, uri: Uri, label: String?): Attachment =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "application/octet-stream"
            val displayName = queryDisplayName(uri) ?: defaultNameFor(mimeType)

            val dir = File(context.filesDir, "attachments/$tripId").apply { mkdirs() }
            val id = UUID.randomUUID().toString()
            val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val target = File(dir, "${id}_$safeName")

            resolver.openInputStream(uri)!!.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }

            val relativePath = "attachments/$tripId/${target.name}"
            val attachment = Attachment(
                id = id,
                tripId = tripId,
                displayName = displayName,
                mimeType = mimeType,
                localPath = relativePath,
                label = label,
                sizeBytes = target.length(),
                createdAt = System.currentTimeMillis()
            )
            dao.insert(AttachmentEntity.fromDomain(attachment))
            attachment
        }

    override suspend fun delete(attachment: Attachment) = withContext(Dispatchers.IO) {
        getFile(attachment).delete()
        dao.delete(AttachmentEntity.fromDomain(attachment))
    }

    override suspend fun readText(attachment: Attachment): String = withContext(Dispatchers.IO) {
        getFile(attachment).readText()
    }

    override fun getFile(attachment: Attachment): File =
        File(context.filesDir, attachment.localPath)

    private fun queryDisplayName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx < 0) return null
            return it.getString(idx)
        }
    }

    private fun defaultNameFor(mimeType: String): String = when {
        mimeType.startsWith("text/markdown") -> "note.md"
        mimeType.startsWith("text/") -> "note.txt"
        mimeType == "application/pdf" -> "document.pdf"
        mimeType.startsWith("image/jpeg") -> "image.jpg"
        mimeType.startsWith("image/png") -> "image.png"
        mimeType.startsWith("image/") -> "image"
        else -> "attachment"
    }
}
