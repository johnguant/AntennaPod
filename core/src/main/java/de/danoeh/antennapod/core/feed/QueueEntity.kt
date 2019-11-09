package de.danoeh.antennapod.core.feed

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "Queue", indices = [
        Index(name = "Queue_feeditem", value = ["feeditem"])
])
data class QueueEntity(
        @PrimaryKey
        @ColumnInfo(name = "id")
        val id: Int,

        @ColumnInfo(name = "feed")
        val feed: Int,

        @ColumnInfo(name = "feeditem")
        val feedItem: Int
)