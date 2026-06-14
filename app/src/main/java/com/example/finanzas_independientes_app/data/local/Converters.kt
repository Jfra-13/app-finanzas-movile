package com.example.finanzas_independientes_app.data.local

import androidx.room.TypeConverter

/** Stores List<Int> (e.g. working days) as a comma-separated string. */
class Converters {

    @TypeConverter
    fun fromIntList(value: List<Int>?): String =
        value?.joinToString(separator = ",") ?: ""

    @TypeConverter
    fun toIntList(value: String?): List<Int> =
        value?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?: emptyList()
}
