package de.danoeh.antennapod.core.storage

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.feed.FeedItemEntity
import de.danoeh.antennapod.core.util.LongIntMap
import io.reactivex.Flowable


@Dao
interface FeedDao {

    @Transaction
    @Query("SELECT * FROM Feeds")
    fun loadAllFeeds(): MutableList<Feed>

    @Transaction
    @Query("SELECT * FROM Feeds")
    fun loadAllFeedsLive(): Flowable<MutableList<Feed>>

    data class MapHolder (
        @ColumnInfo(name = "feed")
        val key: Long,
        @ColumnInfo(name = "count")
        val value: Int
    )

    @Query("SELECT feed, COUNT(FeedItems.id) AS count FROM FeedItems LEFT JOIN FeedMedia ON FeedItems.id = FeedMedia.feeditem WHERE read = 1 IN (:feedIds) GROUP BY feed")
    fun getPlayedEpisodesCountersInternal(vararg feedIds: Long): Array<MapHolder>

    @Query("SELECT feed, COUNT(FeedItems.id) AS count FROM FeedItems LEFT JOIN FeedMedia ON FeedItems.id = FeedMedia.feeditem WHERE feed IN (:feedIds) AND (read = -1 OR read = 0)  GROUP BY feed")
    fun getFeedCountersNewUnplayedSumInternalLive(vararg feedIds: Long): Flowable<Array<MapHolder>>

    @Query("SELECT feed, COUNT(FeedItems.id) AS count FROM FeedItems LEFT JOIN FeedMedia ON FeedItems.id = FeedMedia.feeditem WHERE feed IN (:feedIds) AND read = -1 GROUP BY feed")
    fun getFeedCountersNewInternalLive(vararg feedIds: Long): Flowable<Array<MapHolder>>

    @Query("SELECT feed, COUNT(FeedItems.id) AS count FROM FeedItems LEFT JOIN FeedMedia ON FeedItems.id = FeedMedia.feeditem WHERE feed IN (:feedIds) AND read = 0 GROUP BY feed")
    fun getFeedCountersUnplayedInternalLive(vararg feedIds: Long): Flowable<Array<MapHolder>>

    @Query("SELECT feed, COUNT(FeedItems.id) AS count FROM FeedItems LEFT JOIN FeedMedia ON FeedItems.id = FeedMedia.feeditem WHERE feed IN (:feedIds) AND downloaded = 1 GROUP BY feed")
    fun getFeedCountersDownloadedInternalLive(vararg feedIds: Long): Flowable<Array<MapHolder>>

    @Query("SELECT feed, COUNT(FeedItems.id) AS count FROM FeedItems LEFT JOIN FeedMedia ON FeedItems.id = FeedMedia.feeditem WHERE read = 1 IN (:feedIds) GROUP BY feed")
    fun getPlayedEpisodesCountersInternalLive(vararg feedIds: Long): Flowable<Array<MapHolder>>

    @Transaction
    fun getPlayedEpisodesCounters(vararg feedIds: Long): LongIntMap {
        val counters = getPlayedEpisodesCountersInternal(*feedIds)
        val map = LongIntMap(counters.size)
        for (counter in counters) {
            map.put(counter.key, counter.value)
        }
        return map
    }

    @Query("SELECT COUNT(id) FROM Queue")
    fun getQueueSize(): Int

    @Query("SELECT COUNT(FeedItems.id) FROM FeedItems INNER JOIN Feeds ON FeedItems.feed = Feeds.id WHERE FeedItems.read = -1 AND Feeds.keep_updated > 0")
    fun getNumberOfNewItems(): Int

    @Query("SELECT COUNT(DISTINCT id) AS count FROM FeedMedia WHERE downloaded > 0")
    fun getNumberOfDownloadedEpisodes(): Int

    @Query("SELECT id, title, pubDate, read, link, payment_link, feed, has_simple_chapters, item_identifier, image_url, auto_download FROM FeedItems WHERE feed = :feedId ORDER BY pubDate ASC")
    fun loadFeedItems(feedId: Long): List<FeedItemEntity>

    @Query("UPDATE Feeds SET user_position = :userPosition WHERE id = :feedId")
    fun updatePosition(feedId: Long, userPosition: Int)

    @Transaction
    suspend fun movePosition(source: Int, target: Int) {
        if(source == target) return
        val id = getFeedIdAtPosition(source)
        if(target < source) {
            moveItemsUp(source, target)
        } else {
            moveItemsDown(source, target)
        }
        updatePosition(id, target)
    }

    @Query("UPDATE Feeds SET user_position = user_position + 1 WHERE user_position >= :target AND user_position < :source")
    fun moveItemsUp(source: Int, target: Int)

    @Query("UPDATE Feeds SET user_position = user_position - 1 WHERE user_position <= :target AND user_position > :source")
    fun moveItemsDown(source: Int, target: Int)

    @Query("SELECT id FROM Feeds WHERE user_position = :userPosition LIMIT 1")
    fun getFeedIdAtPosition(userPosition: Int): Long
}