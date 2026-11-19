package com.example.selaras.core.database.converter

import androidx.room.TypeConverter
import com.example.selaras.domain.model.ScreeningCategory
import com.example.selaras.domain.model.ScreeningType

class ScreeningConverter {

    @TypeConverter
    fun fromScreeningType(value: ScreeningType?): String? {
        return value?.value
    }

    @TypeConverter
    fun toScreeningType(value: String?): ScreeningType? {
        return value?.let { ScreeningType.fromValue(it) }
    }

    @TypeConverter
    fun fromScreeningCategory(value: ScreeningCategory?): String? {
        return value?.value
    }

    @TypeConverter
    fun toScreeningCategory(value: String?): ScreeningCategory? {
        return value?.let { ScreeningCategory.fromValue(it) }
    }
}
