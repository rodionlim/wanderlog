package com.wanderlog.android.core.util

import android.util.Base64
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.ItineraryItemType

fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

fun ByteArray.toDataUri(mimeType: String): String = "data:$mimeType;base64,${toBase64()}"

fun Double.toCurrencyString(currencyCode: String): String =
    "$currencyCode %.2f".format(this)

fun ItineraryItem.localAttachmentId(): String? =
    confirmationUrl
        ?.takeIf { it.startsWith("attachment://") }
        ?.removePrefix("attachment://")

fun ItineraryItem.flightDetailLine(prefix: String): String? =
    notes
        ?.lineSequence()
        ?.map(String::trim)
        ?.firstOrNull { line -> line.startsWith("$prefix:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.trim()
        ?.takeIf(String::isNotBlank)

fun ItineraryItem.notesForDisplay(): String? {
    val cleaned = notes
        ?.lineSequence()
        ?.map(String::trim)
        ?.filter { line ->
            when {
                itemType != ItineraryItemType.FLIGHT -> true
                line.startsWith("Departure:", ignoreCase = true) -> false
                line.startsWith("Arrival:", ignoreCase = true) -> false
                else -> true
            }
        }
        ?.filter(String::isNotBlank)
        ?.joinToString("\n")
        ?.trim()

    return cleaned?.takeIf(String::isNotBlank)
}
