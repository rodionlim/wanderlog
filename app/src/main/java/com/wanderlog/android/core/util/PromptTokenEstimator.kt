package com.wanderlog.android.core.util

import kotlin.math.ceil
import kotlin.math.max

object PromptTokenEstimator {
    fun estimate(text: String): Int {
        val normalized = text.trim()
        if (normalized.isBlank()) return 0

        val charEstimate = ceil(normalized.length / 4.0).toInt()
        val wordEstimate = ceil(normalized.split(Regex("\\s+")).count { it.isNotBlank() } * 1.35).toInt()
        return max(charEstimate, wordEstimate)
    }
}
