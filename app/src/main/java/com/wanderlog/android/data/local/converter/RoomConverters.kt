package com.wanderlog.android.data.local.converter

import androidx.room.TypeConverter
import java.time.LocalDate

class RoomConverters {

    private val listSeparator = "\u001F"

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromStringList(values: List<String>?): String? =
        values?.joinToString(separator = listSeparator)

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        value
            ?.takeIf { it.isNotBlank() }
            ?.split(listSeparator)
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?: emptyList()
}
