package com.wanderlog.android.domain.usecase.ai

import com.wanderlog.android.data.remote.openai.dto.ContentPartDto
import com.wanderlog.android.domain.model.Expense
import com.wanderlog.android.domain.model.PackingItem
import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.model.TripAssistantMessage
import com.wanderlog.android.domain.model.TripDay
import com.wanderlog.android.domain.repository.AiRepository
import javax.inject.Inject

class AskAboutTripUseCase @Inject constructor(
    private val repo: AiRepository
) {

    suspend operator fun invoke(
        trip: Trip,
        days: List<TripDay>,
        expenses: List<Expense>,
        packingItems: List<PackingItem>,
        conversation: List<TripAssistantMessage>,
        question: String,
        attachmentParts: List<ContentPartDto> = emptyList(),
        selectedAttachmentNames: List<String> = emptyList()
    ): String = repo.askAboutTrip(
        trip = trip,
        days = days,
        expenses = expenses,
        packingItems = packingItems,
        conversation = conversation,
        question = question,
        attachmentParts = attachmentParts,
        selectedAttachmentNames = selectedAttachmentNames
    )
}
