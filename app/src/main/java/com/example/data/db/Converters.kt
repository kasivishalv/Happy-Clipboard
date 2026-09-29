package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.ContentType

class Converters {
    @TypeConverter
    fun fromContentType(value: ContentType?): String {
        return value?.name ?: ContentType.TEXT.name
    }

    @TypeConverter
    fun toContentType(value: String?): ContentType {
        return try {
            if (value != null) ContentType.valueOf(value) else ContentType.TEXT
        } catch (_: Exception) {
            ContentType.TEXT
        }
    }
}
