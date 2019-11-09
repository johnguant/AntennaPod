package de.danoeh.antennapod.core.feed

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Favorites")
data class FavoritesEntity(
        @PrimaryKey
        @ColumnInfo(name = "id")
        val id: Int,

        @ColumnInfo(name = "feeditem")
        val feedItem: Int,

        @ColumnInfo(name = "feed")
        val feed: Int
)