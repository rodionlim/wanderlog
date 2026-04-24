package com.wanderlog.android.domain.usecase.ai

import android.content.Context
import android.net.Uri
import com.wanderlog.android.core.util.toDataUri
import com.wanderlog.android.core.util.FileUtils
import com.wanderlog.android.data.remote.openai.dto.ContentPartDto
import com.wanderlog.android.data.remote.openai.dto.ImagePart
import com.wanderlog.android.data.remote.openai.dto.ImageUrlDto
import com.wanderlog.android.data.remote.openai.dto.TextPart
import com.wanderlog.android.domain.model.DocumentHint
import com.wanderlog.android.domain.model.ParsedBooking
import com.wanderlog.android.domain.repository.AiRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ParseFileUseCase @Inject constructor(
    private val repo: AiRepository,
    @ApplicationContext private val context: Context
) {

    suspend operator fun invoke(uri: Uri, hint: DocumentHint? = null): ParsedBooking = withContext(Dispatchers.IO) {
        val mimeType = FileUtils.getMimeType(context, uri) ?: "application/octet-stream"
        val parts: List<ContentPartDto> = when {
            mimeType.startsWith("image/") -> {
                val bytes = FileUtils.readBytes(context, uri)
                val compressed = FileUtils.compressImageToJpeg(bytes)
                listOf(ImagePart(imageUrl = ImageUrlDto(url = compressed.toDataUri("image/jpeg"))))
            }
            mimeType == "application/pdf" -> {
                FileUtils.rasterizePdf(context, uri).map { pageBytes ->
                    ImagePart(imageUrl = ImageUrlDto(url = pageBytes.toDataUri("image/jpeg")))
                }
            }
            else -> {
                val text = FileUtils.readText(context, uri)
                listOf(TextPart(text = text))
            }
        }
        repo.parseFile(parts, hint)
    }
}
