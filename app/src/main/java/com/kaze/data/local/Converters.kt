package com.kaze.data.local

import androidx.room.TypeConverter
import com.kaze.model.MediaType

class Converters {
    @TypeConverter
    fun fromMediaType(type: MediaType): String = type.name

    @TypeConverter
    fun toMediaType(value: String): MediaType = runCatching {
        MediaType.valueOf(value)
    }.getOrDefault(MediaType.MOVIE)
}
