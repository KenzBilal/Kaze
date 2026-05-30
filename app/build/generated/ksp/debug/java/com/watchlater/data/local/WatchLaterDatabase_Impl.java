package com.watchlater.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WatchLaterDatabase_Impl extends WatchLaterDatabase {
  private volatile WatchItemDao _watchItemDao;

  private volatile SeriesCacheDao _seriesCacheDao;

  private volatile SeasonEpisodeDao _seasonEpisodeDao;

  private volatile EpisodeProgressDao _episodeProgressDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `watch_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `year` INTEGER NOT NULL, `type` TEXT NOT NULL, `isWatched` INTEGER NOT NULL, `rating` REAL NOT NULL, `season` INTEGER, `episode` INTEGER, `notes` TEXT NOT NULL, `posterUrl` TEXT, `genres` TEXT NOT NULL, `imdbId` TEXT NOT NULL, `dateAdded` INTEGER NOT NULL, `lastUpdated` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `series_cache` (`imdbId` TEXT NOT NULL, `title` TEXT NOT NULL, `totalSeasons` INTEGER NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`imdbId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `season_episodes` (`imdbId` TEXT NOT NULL, `season` INTEGER NOT NULL, `episodeNumber` INTEGER NOT NULL, `title` TEXT NOT NULL, `released` TEXT NOT NULL, `imdbRating` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`imdbId`, `season`, `episodeNumber`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `episode_progress` (`watchItemId` INTEGER NOT NULL, `season` INTEGER NOT NULL, `episodeNumber` INTEGER NOT NULL, `isWatched` INTEGER NOT NULL, `watchedAt` INTEGER, PRIMARY KEY(`watchItemId`, `season`, `episodeNumber`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '5080d4b5078a5f92709677b952186b54')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `watch_items`");
        db.execSQL("DROP TABLE IF EXISTS `series_cache`");
        db.execSQL("DROP TABLE IF EXISTS `season_episodes`");
        db.execSQL("DROP TABLE IF EXISTS `episode_progress`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsWatchItems = new HashMap<String, TableInfo.Column>(14);
        _columnsWatchItems.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatchItems.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatchItems.put("year", new TableInfo.Column("year", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatchItems.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatchItems.put("isWatched", new TableInfo.Column("isWatched", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatchItems.put("rating", new TableInfo.Column("rating", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatchItems.put("season", new TableInfo.Column("season", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatchItems.put("episode", new TableInfo.Column("episode", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatchItems.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatchItems.put("posterUrl", new TableInfo.Column("posterUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatchItems.put("genres", new TableInfo.Column("genres", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatchItems.put("imdbId", new TableInfo.Column("imdbId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatchItems.put("dateAdded", new TableInfo.Column("dateAdded", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatchItems.put("lastUpdated", new TableInfo.Column("lastUpdated", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWatchItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWatchItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWatchItems = new TableInfo("watch_items", _columnsWatchItems, _foreignKeysWatchItems, _indicesWatchItems);
        final TableInfo _existingWatchItems = TableInfo.read(db, "watch_items");
        if (!_infoWatchItems.equals(_existingWatchItems)) {
          return new RoomOpenHelper.ValidationResult(false, "watch_items(com.watchlater.model.WatchItem).\n"
                  + " Expected:\n" + _infoWatchItems + "\n"
                  + " Found:\n" + _existingWatchItems);
        }
        final HashMap<String, TableInfo.Column> _columnsSeriesCache = new HashMap<String, TableInfo.Column>(4);
        _columnsSeriesCache.put("imdbId", new TableInfo.Column("imdbId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeriesCache.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeriesCache.put("totalSeasons", new TableInfo.Column("totalSeasons", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeriesCache.put("cachedAt", new TableInfo.Column("cachedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSeriesCache = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSeriesCache = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSeriesCache = new TableInfo("series_cache", _columnsSeriesCache, _foreignKeysSeriesCache, _indicesSeriesCache);
        final TableInfo _existingSeriesCache = TableInfo.read(db, "series_cache");
        if (!_infoSeriesCache.equals(_existingSeriesCache)) {
          return new RoomOpenHelper.ValidationResult(false, "series_cache(com.watchlater.data.local.SeriesCache).\n"
                  + " Expected:\n" + _infoSeriesCache + "\n"
                  + " Found:\n" + _existingSeriesCache);
        }
        final HashMap<String, TableInfo.Column> _columnsSeasonEpisodes = new HashMap<String, TableInfo.Column>(7);
        _columnsSeasonEpisodes.put("imdbId", new TableInfo.Column("imdbId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeasonEpisodes.put("season", new TableInfo.Column("season", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeasonEpisodes.put("episodeNumber", new TableInfo.Column("episodeNumber", "INTEGER", true, 3, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeasonEpisodes.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeasonEpisodes.put("released", new TableInfo.Column("released", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeasonEpisodes.put("imdbRating", new TableInfo.Column("imdbRating", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeasonEpisodes.put("cachedAt", new TableInfo.Column("cachedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSeasonEpisodes = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSeasonEpisodes = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSeasonEpisodes = new TableInfo("season_episodes", _columnsSeasonEpisodes, _foreignKeysSeasonEpisodes, _indicesSeasonEpisodes);
        final TableInfo _existingSeasonEpisodes = TableInfo.read(db, "season_episodes");
        if (!_infoSeasonEpisodes.equals(_existingSeasonEpisodes)) {
          return new RoomOpenHelper.ValidationResult(false, "season_episodes(com.watchlater.data.local.SeasonEpisode).\n"
                  + " Expected:\n" + _infoSeasonEpisodes + "\n"
                  + " Found:\n" + _existingSeasonEpisodes);
        }
        final HashMap<String, TableInfo.Column> _columnsEpisodeProgress = new HashMap<String, TableInfo.Column>(5);
        _columnsEpisodeProgress.put("watchItemId", new TableInfo.Column("watchItemId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpisodeProgress.put("season", new TableInfo.Column("season", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpisodeProgress.put("episodeNumber", new TableInfo.Column("episodeNumber", "INTEGER", true, 3, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpisodeProgress.put("isWatched", new TableInfo.Column("isWatched", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpisodeProgress.put("watchedAt", new TableInfo.Column("watchedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysEpisodeProgress = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesEpisodeProgress = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoEpisodeProgress = new TableInfo("episode_progress", _columnsEpisodeProgress, _foreignKeysEpisodeProgress, _indicesEpisodeProgress);
        final TableInfo _existingEpisodeProgress = TableInfo.read(db, "episode_progress");
        if (!_infoEpisodeProgress.equals(_existingEpisodeProgress)) {
          return new RoomOpenHelper.ValidationResult(false, "episode_progress(com.watchlater.data.local.EpisodeProgress).\n"
                  + " Expected:\n" + _infoEpisodeProgress + "\n"
                  + " Found:\n" + _existingEpisodeProgress);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "5080d4b5078a5f92709677b952186b54", "b3bf0c447d1bc2c9c9f2fa3e6681ebc3");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "watch_items","series_cache","season_episodes","episode_progress");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `watch_items`");
      _db.execSQL("DELETE FROM `series_cache`");
      _db.execSQL("DELETE FROM `season_episodes`");
      _db.execSQL("DELETE FROM `episode_progress`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(WatchItemDao.class, WatchItemDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SeriesCacheDao.class, SeriesCacheDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SeasonEpisodeDao.class, SeasonEpisodeDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(EpisodeProgressDao.class, EpisodeProgressDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public WatchItemDao watchItemDao() {
    if (_watchItemDao != null) {
      return _watchItemDao;
    } else {
      synchronized(this) {
        if(_watchItemDao == null) {
          _watchItemDao = new WatchItemDao_Impl(this);
        }
        return _watchItemDao;
      }
    }
  }

  @Override
  public SeriesCacheDao seriesCacheDao() {
    if (_seriesCacheDao != null) {
      return _seriesCacheDao;
    } else {
      synchronized(this) {
        if(_seriesCacheDao == null) {
          _seriesCacheDao = new SeriesCacheDao_Impl(this);
        }
        return _seriesCacheDao;
      }
    }
  }

  @Override
  public SeasonEpisodeDao seasonEpisodeDao() {
    if (_seasonEpisodeDao != null) {
      return _seasonEpisodeDao;
    } else {
      synchronized(this) {
        if(_seasonEpisodeDao == null) {
          _seasonEpisodeDao = new SeasonEpisodeDao_Impl(this);
        }
        return _seasonEpisodeDao;
      }
    }
  }

  @Override
  public EpisodeProgressDao episodeProgressDao() {
    if (_episodeProgressDao != null) {
      return _episodeProgressDao;
    } else {
      synchronized(this) {
        if(_episodeProgressDao == null) {
          _episodeProgressDao = new EpisodeProgressDao_Impl(this);
        }
        return _episodeProgressDao;
      }
    }
  }
}
