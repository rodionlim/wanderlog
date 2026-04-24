package com.wanderlog.android.domain.usecase.ai

import com.wanderlog.android.data.remote.openai.dto.TextPart
import com.wanderlog.android.domain.model.DocumentHint
import com.wanderlog.android.domain.model.ParsedBooking
import com.wanderlog.android.domain.repository.AiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ParseTextUseCase @Inject constructor(
    private val repo: AiRepository
) {
    suspend operator fun invoke(text: String, hint: DocumentHint? = null): ParsedBooking =
        withContext(Dispatchers.IO) {
            repo.parseFile(listOf(TextPart(text = text)), hint)
        }
}
