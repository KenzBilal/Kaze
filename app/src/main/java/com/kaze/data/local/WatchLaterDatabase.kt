package com.kaze.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.kaze.model.WatchItem

@Database(
    entities = [
        WatchItem::class,
        SeriesCache::class,
        SeasonEpisode::class,
        EpisodeProgress::class,
        PendingAction::class
    ],
    version = 8,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class WatchLaterDatabase : RoomDatabase() {

    abstract fun watchItemDao(): WatchItemDao
    abstract fun seriesCacheDao(): SeriesCacheDao
    abstract fun seasonEpisodeDao(): SeasonEpisodeDao
    abstract fun episodeProgressDao(): EpisodeProgressDao
    abstract fun pendingActionDao(): PendingActionDao
    abstract fun whatToWatchDao(): WhatToWatchDao

    companion object {
        private const val DATABASE_NAME = "watch_later.db"

        /** v1 → v2: add posterUrl and genres columns */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watch_items ADD COLUMN posterUrl TEXT")
                db.execSQL("ALTER TABLE watch_items ADD COLUMN genres TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v2 → v3: add imdbId to watch_items + new series/episode tables */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watch_items ADD COLUMN imdbId TEXT NOT NULL DEFAULT ''")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS series_cache (
                        imdbId TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        totalSeasons INTEGER NOT NULL,
                        cachedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS season_episodes (
                        imdbId TEXT NOT NULL,
                        season INTEGER NOT NULL,
                        episodeNumber INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        released TEXT NOT NULL DEFAULT '',
                        imdbRating TEXT NOT NULL DEFAULT '',
                        cachedAt INTEGER NOT NULL,
                        PRIMARY KEY (imdbId, season, episodeNumber)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS episode_progress (
                        watchItemId INTEGER NOT NULL,
                        season INTEGER NOT NULL,
                        episodeNumber INTEGER NOT NULL,
                        isWatched INTEGER NOT NULL DEFAULT 0,
                        watchedAt INTEGER,
                        PRIMARY KEY (watchItemId, season, episodeNumber)
                    )
                """.trimIndent())
            }
        }

        /** v3 → v4: add indexes for query performance */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_watch_items_isWatched ON watch_items (isWatched)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_watch_items_type ON watch_items (type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_watch_items_dateAdded ON watch_items (dateAdded)")
            }
        }

        /** v4 → v5: add offline sync queue */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS pending_actions (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        actionType TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        targetId TEXT NOT NULL DEFAULT '',
                        payload TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        /** v5 → v6: add imdbId index on watch_items; add FK + index on episode_progress */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // imdbId index (Bug 39)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_watch_items_imdbId ON watch_items (imdbId)")

                // Recreate episode_progress with FK + watchItemId index (Bug 40)
                // SQLite cannot add FK constraints with ALTER TABLE — must recreate the table.
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS episode_progress_new (
                        watchItemId INTEGER NOT NULL,
                        season INTEGER NOT NULL,
                        episodeNumber INTEGER NOT NULL,
                        isWatched INTEGER NOT NULL DEFAULT 0,
                        watchedAt INTEGER,
                        PRIMARY KEY (watchItemId, season, episodeNumber),
                        FOREIGN KEY (watchItemId) REFERENCES watch_items(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                // Prevent SQLiteConstraintException by removing orphaned rows before inserting
                db.execSQL("DELETE FROM episode_progress WHERE watchItemId NOT IN (SELECT id FROM watch_items)")
                db.execSQL("INSERT INTO episode_progress_new SELECT * FROM episode_progress")
                db.execSQL("DROP TABLE episode_progress")
                db.execSQL("ALTER TABLE episode_progress_new RENAME TO episode_progress")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_episode_progress_watchItemId ON episode_progress (watchItemId)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE series_cache ADD COLUMN isFinished INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v7 → v8: add plot + trailerUrl to watch_items, plot to season_episodes */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watch_items ADD COLUMN plot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE watch_items ADD COLUMN trailerUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE season_episodes ADD COLUMN plot TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var INSTANCE: WatchLaterDatabase? = null

        fun getInstance(context: Context): WatchLaterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WatchLaterDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
