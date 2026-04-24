package com.wanderlog.android.core.util

import android.util.Base64

fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

fun ByteArray.toDataUri(mimeType: String): String = "data:$mimeType;base64,${toBase64()}"

fun Double.toCurrencyString(currencyCode: String): String =
    "$currencyCode %.2f".format(this)
