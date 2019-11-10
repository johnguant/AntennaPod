package de.danoeh.antennapod.core.storage

import androidx.room.TypeConverter
import java.util.*

class DateConverter {
    @TypeConverter
    fun toLong(date: Date): Long {
        return date.time
    }

    @TypeConverter
    fun toDate(date: Long): Date {
        return Date(date)
    }
}