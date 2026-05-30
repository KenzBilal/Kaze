package com.watchlater.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.watchlater.model.WatchItem

@Database(
    entities = [
        WatchItem::class,
        SeriesCache::class,
        SeasonEpisode::class,
        EpisodeProgress::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class WatchLaterDatabase : RoomDatabase() {

    abstract fun watchItemDao(): WatchItemDao
    abstract fun seriesCacheDao(): SeriesCacheDao
    abstract fun seasonEpisodeDao(): SeasonEpisodeDao
    abstract fun episodeProgressDao(): EpisodeProgressDao

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
                // Add imdbId to existing watch_items
                db.execSQL("ALTER TABLE watch_items ADD COLUMN imdbId TEXT NOT NULL DEFAULT ''")

                // Series metadata cache
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS series_cache (
                        imdbId TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        totalSeasons INTEGER NOT NULL,
                        cachedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Episode list cache per season
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

                // User's per-episode watch progress
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

        @Volatile
        private var INSTANCE: WatchLaterDatabase? = null

        fun getInstance(context: Context): WatchLaterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WatchLaterDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
