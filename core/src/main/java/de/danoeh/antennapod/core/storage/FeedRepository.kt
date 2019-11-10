package de.danoeh.antennapod.core.storage

import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.preferences.UserPreferences
import de.danoeh.antennapod.core.util.LongIntMap
import io.reactivex.Flowable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedRepository {
    private val feedDao = PodDBAdapter.getDBInstance().feedDao()

    fun getSubscriptions(): Flowable<SubscriptionData> {
        return getSubscriptions(-1)
    }

    fun getSubscriptions(feedOrderOverride: Int): Flowable<SubscriptionData> {
        val feeds = feedDao.loadAllFeedsLive()

        return feeds.switchMap { feeds ->
            val feedIds = LongArray(feeds.size)
            for (i in feeds.indices) {
                feedIds[i] = feeds[i].id
            }

            getFeedCountersLive(*feedIds).map { feedCounters ->
                val comparator: java.util.Comparator<Feed>
                val feedOrder = when (feedOrderOverride) {
                    -1 -> UserPreferences.getFeedOrder()
                    else -> feedOrderOverride
                }
                comparator = when (feedOrder) {
                    UserPreferences.FEED_ORDER_COUNTER -> {
                        Comparator { lhs: Feed, rhs: Feed ->
                            val counterLhs = feedCounters[lhs.id].toLong()
                            val counterRhs = feedCounters[rhs.id].toLong()
                            when {
                                counterLhs > counterRhs -> { // reverse natural order: podcast with most unplayed episodes first
                                    -1
                                }
                                counterLhs == counterRhs -> {
                                    lhs.title.compareTo(rhs.title, ignoreCase = true)
                                }
                                else -> {
                                    1
                                }
                            }
                        }
                    }
                    UserPreferences.FEED_ORDER_ALPHABETICAL -> {
                        Comparator { lhs: Feed, rhs: Feed ->
                            val t1 = lhs.title
                            val t2 = rhs.title
                            when {
                                t1 == null -> {
                                    1
                                }
                                t2 == null -> {
                                    -1
                                }
                                else -> {
                                    t1.compareTo(t2, ignoreCase = true)
                                }
                            }
                        }
                    }
                    UserPreferences.FEED_ORDER_MOST_PLAYED -> {
                        val playedCounters = feedDao.getPlayedEpisodesCounters(*feedIds)
                        Comparator { lhs: Feed, rhs: Feed ->
                            val counterLhs = playedCounters[lhs.id].toLong()
                            val counterRhs = playedCounters[rhs.id].toLong()
                            when {
                                counterLhs > counterRhs -> { // podcast with most played episodes first
                                    -1
                                }
                                counterLhs == counterRhs -> {
                                    lhs.title.compareTo(rhs.title, ignoreCase = true)
                                }
                                else -> {
                                    1
                                }
                            }
                        }
                    }
                    UserPreferences.FEED_ORDER_DRAG_AND_DROP -> {
                        Comparator {lhs: Feed, rhs: Feed ->
                            lhs.userPosition.compareTo(rhs.userPosition)
                        }
                    }
                    else -> {
                        Comparator { lhs: Feed, rhs: Feed ->
                            if (lhs.items == null || lhs.items.size == 0) {
                                val items = feedDao.loadFeedItems(lhs.id)
                                lhs.feedItems = items
                            }
                            if (rhs.items == null || rhs.items.size == 0) {
                                val items = feedDao.loadFeedItems(rhs.id)
                                rhs.feedItems = items
                            }
                            when {
                                lhs.mostRecentFeedItem == null -> {
                                    1
                                }
                                rhs.mostRecentFeedItem == null -> {
                                    -1
                                }
                                else -> {
                                    val d1 = lhs.mostRecentFeedItem.pubDate
                                    val d2 = rhs.mostRecentFeedItem.pubDate
                                    d2.compareTo(d1)
                                }
                            }
                        }
                    }
                }
                feeds.sortWith(comparator)

                SubscriptionData(feeds, feedCounters) }
        }
    }

    private fun getFeedCountersLive(vararg feedIds: Long): Flowable<LongIntMap> {
        return when (UserPreferences.getFeedCounterSetting()) {
            UserPreferences.FEED_COUNTER_SHOW_NEW_UNPLAYED_SUM -> feedDao.getFeedCountersNewUnplayedSumInternalLive(*feedIds)
            UserPreferences.FEED_COUNTER_SHOW_NEW -> feedDao.getFeedCountersNewInternalLive(*feedIds)
            UserPreferences.FEED_COUNTER_SHOW_UNPLAYED -> feedDao.getFeedCountersUnplayedInternalLive(*feedIds)
            UserPreferences.FEED_COUNTER_SHOW_DOWNLOADED -> feedDao.getFeedCountersDownloadedInternalLive(*feedIds)
            UserPreferences.FEED_COUNTER_SHOW_NONE -> {
                Flowable.just(arrayOf())
            }
            else -> {
                // TODO Make this crash
                Flowable.just(arrayOf())
            }
        }.map { counters ->
            val map = LongIntMap(counters.size)
                        for (counter in counters) {
                map.put(counter.key, counter.value)
            }
            map
        }
    }

    fun updateUserPositions(feeds: List<Feed>) {
        for(i in feeds.indices) {
            feeds[i].userPosition = i
            feedDao.updatePosition(feeds[i].id, i)
        }
    }

    fun moveSubscription(source: Int, target: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                feedDao.movePosition(source, target)
            }
        }
    }

    data class SubscriptionData(
            val feeds: List<Feed>,
            val feedCounters: LongIntMap
    )
}