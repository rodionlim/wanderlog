package com.wanderlog.android.domain.usecase.ai

import android.content.Context
import com.wanderlog.android.core.util.FileUtils
import com.wanderlog.android.core.util.toDataUri
import com.wanderlog.android.data.remote.openai.dto.ContentPartDto
import com.wanderlog.android.data.remote.openai.dto.ImagePart
import com.wanderlog.android.data.remote.openai.dto.ImageUrlDto
import com.wanderlog.android.data.remote.openai.dto.TextPart
import com.wanderlog.android.domain.model.Attachment
import com.wanderlog.android.domain.repository.AttachmentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BuildAttachmentContextPartsUseCase @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    @ApplicationContext private val context: Context
) {

    suspend operator fun invoke(attachments: List<Attachment>): List<ContentPartDto> =
        withContext(Dispatchers.IO) {
            attachments.flatMapIndexed { index, attachment ->
                buildList {
                    add(
                        TextPart(
                            text = "Selected attachment ${index + 1}: ${attachment.displayName} (${attachment.mimeType}). Use this only if it is relevant to the current question."
                        )
                    )
                    addAll(contentPartsForAttachment(attachment))
                }
            }
        }

    private suspend fun contentPartsForAttachment(attachment: Attachment): List<ContentPartDto> {
        val file = attachmentRepository.getFile(attachment)
        return when {
            attachment.mimeType == "application/pdf" || attachment.displayName.endsWith(".pdf", ignoreCase = true) -> {
                val text = FileUtils.readPdfText(context, file)
                listOf(TextPart(text = text))
            }

            attachment.mimeType.startsWith("image/") -> {
                val bytes = file.readBytes()
                val compressed = FileUtils.compressImageToJpeg(bytes)
                listOf(ImagePart(imageUrl = ImageUrlDto(url = compressed.toDataUri("image/jpeg"))))
            }

            attachment.isTextLike() -> {
                val text = attachmentRepository.readText(attachment)
                listOf(TextPart(text = text))
            }

            else -> {
                listOf(
                    TextPart(
                        text = "The selected attachment ${attachment.displayName} (${attachment.mimeType}) cannot be inlined as text or image content. Answer using the trip context and any other selected attachments."
                    )
                )
            }
        }
    }

    private fun Attachment.isTextLike(): Boolean =
        mimeType.startsWith("text/") ||
            mimeType == "application/json" ||
            mimeType == "application/xml" ||
            displayName.endsWith(".md", ignoreCase = true) ||
            displayName.endsWith(".txt", ignoreCase = true)
}
