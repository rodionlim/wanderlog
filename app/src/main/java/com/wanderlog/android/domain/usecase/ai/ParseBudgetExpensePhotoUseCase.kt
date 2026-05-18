package com.wanderlog.android.domain.usecase.ai

import android.content.Context
import android.net.Uri
import com.wanderlog.android.core.util.FileUtils
import com.wanderlog.android.core.util.toDataUri
import com.wanderlog.android.data.remote.openai.dto.ImagePart
import com.wanderlog.android.data.remote.openai.dto.ImageUrlDto
import com.wanderlog.android.domain.model.ParsedBudgetExpenseImport
import com.wanderlog.android.domain.repository.AiRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ParseBudgetExpensePhotoUseCase @Inject constructor(
    private val repo: AiRepository,
    @ApplicationContext private val context: Context
) {

    suspend operator fun invoke(
        uri: Uri,
        fallbackCurrencyCode: String
    ): ParsedBudgetExpenseImport = withContext(Dispatchers.IO) {
        val bytes = FileUtils.readBytes(context, uri)
        val compressed = FileUtils.compressImageToJpeg(bytes)
        val parts = listOf(
            ImagePart(imageUrl = ImageUrlDto(url = compressed.toDataUri("image/jpeg")))
        )
        repo.parseBudgetExpenses(parts, fallbackCurrencyCode)
    }
}
