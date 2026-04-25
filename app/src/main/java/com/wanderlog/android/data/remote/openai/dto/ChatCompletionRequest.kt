package com.wanderlog.android.data.remote.openai.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    val model: String = "gpt-5.4-mini",
    val messages: List<MessageDto>,
    @Json(name = "response_format") val responseFormat: ResponseFormatDto? = null,
    @Json(name = "max_tokens") val maxTokens: Int = 4096,
    val temperature: Double = 0.3
)

@JsonClass(generateAdapter = true)
data class ResponseFormatDto(val type: String)

// content can be either a plain String or a List<ContentPartDto>.
// Serialisation is handled by ContentAdapter in NetworkModule.
data class MessageDto(
    val role: String,
    val content: Any  // String | List<ContentPartDto>
)
