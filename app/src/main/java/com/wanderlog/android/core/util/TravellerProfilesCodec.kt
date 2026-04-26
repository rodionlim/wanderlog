package com.wanderlog.android.core.util

import com.wanderlog.android.domain.model.TravellerProfile
import org.json.JSONArray
import org.json.JSONObject

object TravellerProfilesCodec {

    private const val LEGACY_SEPARATOR = "\u001F"

    fun encode(profiles: List<TravellerProfile>): String =
        JSONArray().apply {
            profiles.forEach { profile ->
                put(
                    JSONObject().apply {
                        put("name", profile.name)
                        if (profile.age != null) {
                            put("age", profile.age)
                        }
                    }
                )
            }
        }.toString()

    fun decode(raw: String?): List<TravellerProfile> {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return emptyList()

        return if (trimmed.startsWith("[")) {
            decodeJson(trimmed)
        } else {
            trimmed
                .split(LEGACY_SEPARATOR)
                .map(String::trim)
                .filter(String::isNotBlank)
                .map { TravellerProfile(name = it) }
        }
    }

    private fun decodeJson(raw: String): List<TravellerProfile> {
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.opt(index)
                when (value) {
                    is JSONObject -> {
                        val name = value.optString("name").trim()
                        if (name.isNotBlank()) {
                            add(
                                TravellerProfile(
                                    name = name,
                                    age = value.optInt("age").takeIf { !value.isNull("age") && it >= 0 }
                                )
                            )
                        }
                    }

                    is String -> {
                        val name = value.trim()
                        if (name.isNotBlank()) {
                            add(TravellerProfile(name = name))
                        }
                    }
                }
            }
        }
    }
}
