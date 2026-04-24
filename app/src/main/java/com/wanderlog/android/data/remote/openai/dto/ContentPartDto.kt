package com.wanderlog.android.data.remote.openai.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

sealed class ContentPartDto

@JsonClass(generateAdapter = true)
data class TextPart(
    val type: String = "text",
    val text: String
) : ContentPartDto()

@JsonClass(generateAdapter = true)
data class ImagePart(
    val type: String = "image_url",
    @Json(name = "image_url") val imageUrl: ImageUrlDto
) : ContentPartDto()

@JsonClass(generateAdapter = true)
data class ImageUrlDto(
    val url: String,
    val detail: String = "high"
)
