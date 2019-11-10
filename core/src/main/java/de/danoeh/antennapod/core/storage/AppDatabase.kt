package de.danoeh.antennapod.core.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.danoeh.antennapod.core.feed.*
import de.danoeh.antennapod.core.service.download.DownloadStatus

@Database(entities = [FeedEntity::class, FeedItemEntity::class, FeedMedia::class, DownloadStatus::class, QueueEntity::class, Chapter::class, FavoritesEntity::class], version = 1070404)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun feedDao(): FeedDao

    companion object {
        private val INSTANCE: AppDatabase? = null


        val MIGRATION_1070401_1070402: Migration = object : Migration(1070401, 1070402) {
            override fun migrate(database: SupportSQLiteDatabase) {}
        }
        val MIGRATION_1070402_1070403: Migration = object : Migration(1070402, 1070403) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `Feeds_Temp` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `file_url` TEXT, `download_url` TEXT, `downloaded` INTEGER NOT NULL, `title` TEXT, `custom_title` TEXT, `feed_identifier` TEXT, `link` TEXT, `description` TEXT, `language` TEXT, `author` TEXT, `image_url` TEXT, `last_update` TEXT, `payment_link` TEXT, `type` TEXT, `is_paged` INTEGER NOT NULL DEFAULT 0, `next_page_link` TEXT, `last_update_failed` INTEGER DEFAULT 0, `hide` TEXT, `sort_order` TEXT, `auto_download` INTEGER DEFAULT 1, `keep_updated` INTEGER DEFAULT 1, `auto_delete_action` INTEGER DEFAULT 0, `username` TEXT, `password` TEXT, `feed_playback_speed` REAL DEFAULT -1.0, `include_filter` TEXT DEFAULT '', `exclude_filter` TEXT DEFAULT '')")
                database.execSQL("INSERT INTO `Feeds_Temp` (id, file_url, download_url, downloaded, title, custom_title, feed_identifier, link, description, language, author, image_url, last_update, payment_link, type, is_paged, next_page_link, last_update_failed, hide, sort_order, auto_download, keep_updated, auto_delete_action, username, password, feed_playback_speed, include_filter, exclude_filter) SELECT id, file_url, download_url, downloaded, title, custom_title, feed_identifier, link, description, language, author, image_url, last_update, payment_link, type, is_paged, next_page_link, last_update_failed, hide, sort_order, auto_download, keep_updated, auto_delete_action, username, password, feed_playback_speed, include_filter, exclude_filter FROM `Feeds`")
                database.execSQL("DROP TABLE `Feeds`")
                database.execSQL("ALTER TABLE `Feeds_Temp` RENAME TO `Feeds`")

                database.execSQL("CREATE TABLE IF NOT EXISTS `FeedItems_Temp` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `item_identifier` TEXT, `title` TEXT, `description` TEXT, `content_encoded` TEXT, `link` TEXT, `pubDate` INTEGER, `feed` INTEGER NOT NULL, `read` INTEGER NOT NULL, `payment_link` TEXT, `has_simple_chapters` INTEGER NOT NULL, `image_url` TEXT, `auto_download` INTEGER NOT NULL)")
                database.execSQL("INSERT INTO `FeedItems_Temp` (id, item_identifier, title, description, content_encoded, link, pubDate, feed, read, payment_link, has_simple_chapters, image_url, auto_download) SELECT id, item_identifier, title, description, content_encoded, link, pubDate, feed, read, payment_link, has_simple_chapters, image_url, auto_download FROM `FeedItems`")
                database.execSQL("DROP TABLE `FeedItems`")
                database.execSQL("ALTER TABLE `FeedItems_Temp` RENAME TO `FeedItems`")
                database.execSQL("CREATE INDEX IF NOT EXISTS `FeedItems_feed` ON `FeedItems` (`feed`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `FeedItems_pubDate` ON `FeedItems` (`pubDate`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `FeedItems_read` ON `FeedItems` (`read`)")

                database.execSQL("CREATE TABLE IF NOT EXISTS `FeedMedia_Temp` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `file_url` TEXT, `download_url` TEXT, `downloaded` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `position` INTEGER NOT NULL, `last_played_time` INTEGER NOT NULL, `played_duration` INTEGER NOT NULL DEFAULT 0, `filesize` INTEGER NOT NULL, `mime_type` TEXT, `feeditem` INTEGER NOT NULL, `playback_completion_date` INTEGER, `has_embedded_picture` INTEGER)")
                database.execSQL("INSERT INTO `FeedMedia_Temp` (id, file_url, download_url, downloaded, duration, position, last_played_time, played_duration, filesize, mime_type, feeditem, playback_completion_date, has_embedded_picture) SELECT id, file_url, download_url, downloaded, duration, position, last_played_time, played_duration, filesize, mime_type, feeditem, playback_completion_date, has_embedded_picture FROM `FeedMedia`")
                database.execSQL("DROP TABLE `FeedMedia`")
                database.execSQL("ALTER TABLE `FeedMedia_Temp` RENAME TO `FeedMedia`")
                database.execSQL("CREATE INDEX IF NOT EXISTS `FeedMedia_feeditem` ON `FeedMedia` (`feeditem`)")

                database.execSQL("CREATE TABLE IF NOT EXISTS `Queue_Temp` (`id` INTEGER NOT NULL, `feed` INTEGER NOT NULL, `feeditem` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                database.execSQL("INSERT INTO `Queue_Temp` (id, feed, feeditem) SELECT id, feed, feeditem FROM `Queue`")
                database.execSQL("DROP TABLE `Queue`")
                database.execSQL("ALTER TABLE `Queue_Temp` RENAME TO `Queue`")
                database.execSQL("CREATE INDEX IF NOT EXISTS `Queue_feeditem` ON `Queue` (`feeditem`)")

                database.execSQL("CREATE TABLE IF NOT EXISTS `SimpleChapters_Temp` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `start` INTEGER, `title` TEXT, `link` TEXT, `feeditem` INTEGER, `type` INTEGER)")
                database.execSQL("INSERT INTO `SimpleChapters_Temp` (id, start, title, link, feeditem, type) SELECT id, start, title, link, feeditem, type FROM `SimpleChapters`")
                database.execSQL("DROP TABLE `SimpleChapters`")
                database.execSQL("ALTER TABLE `SimpleChapters_Temp` RENAME TO `SimpleChapters`")
                database.execSQL("CREATE INDEX IF NOT EXISTS `SimpleChapters_feeditem` ON `SimpleChapters` (`feeditem`)")

                database.execSQL("CREATE TABLE IF NOT EXISTS `Favorites_Temp` (`id` INTEGER NOT NULL, `feeditem` INTEGER NOT NULL, `feed` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                database.execSQL("INSERT INTO `Favorites_Temp` (id, feeditem, feed) SELECT id, feeditem, feed FROM `Favorites`")
                database.execSQL("DROP TABLE `Favorites`")
                database.execSQL("ALTER TABLE `Favorites_Temp` RENAME TO `Favorites`")
            }
        }

        val MIGRATION_1070403_1070404: Migration = object : Migration(1070403, 1070404) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `Feeds` ADD `user_position` INTEGER NOT NULL DEFAULT 0;")
                database.execSQL("CREATE TABLE IF NOT EXISTS `Feeds_Temp` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `file_url` TEXT, `download_url` TEXT, `downloaded` INTEGER NOT NULL, `title` TEXT, `custom_title` TEXT, `feed_identifier` TEXT, `link` TEXT, `description` TEXT, `language` TEXT, `author` TEXT, `image_url` TEXT, `last_update` TEXT, `payment_link` TEXT, `type` TEXT, `is_paged` INTEGER NOT NULL DEFAULT 0, `next_page_link` TEXT, `last_update_failed` INTEGER DEFAULT 0, `hide` TEXT, `sort_order` TEXT, `auto_download` INTEGER DEFAULT 1, `keep_updated` INTEGER DEFAULT 1, `auto_delete_action` INTEGER DEFAULT 0, `username` TEXT, `password` TEXT, `feed_playback_speed` REAL DEFAULT -1.0, `include_filter` TEXT DEFAULT '', `exclude_filter` TEXT DEFAULT '', `user_position` INTEGER NOT NULL)")
                database.execSQL("INSERT INTO `Feeds_Temp` (id, file_url, download_url, downloaded, title, custom_title, feed_identifier, link, description, language, author, image_url, last_update, payment_link, type, is_paged, next_page_link, last_update_failed, hide, sort_order, auto_download, keep_updated, auto_delete_action, username, password, feed_playback_speed, include_filter, exclude_filter, user_position) SELECT id, file_url, download_url, downloaded, title, custom_title, feed_identifier, link, description, language, author, image_url, last_update, payment_link, type, is_paged, next_page_link, last_update_failed, hide, sort_order, auto_download, keep_updated, auto_delete_action, username, password, feed_playback_speed, include_filter, exclude_filter, user_position FROM `Feeds`")
                database.execSQL("DROP TABLE `Feeds`")
                database.execSQL("ALTER TABLE `Feeds_Temp` RENAME TO `Feeds`")
            }
        }
    }
}

