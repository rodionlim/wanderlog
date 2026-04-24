package com.wanderlog.android.data.remote.openai

import com.wanderlog.android.data.remote.openai.dto.ChatCompletionRequest
import com.wanderlog.android.data.remote.openai.dto.ChatCompletionResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAiService {

    @POST("v1/chat/completions")
    suspend fun chatCompletion(@Body request: ChatCompletionRequest): ChatCompletionResponse
}
