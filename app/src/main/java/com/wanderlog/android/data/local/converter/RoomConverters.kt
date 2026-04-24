package com.wanderlog.android.data.local.converter

import androidx.room.TypeConverter
import java.time.LocalDate

class RoomConverters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }
}
