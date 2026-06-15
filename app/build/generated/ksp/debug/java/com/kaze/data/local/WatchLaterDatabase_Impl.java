package com.kaze.data.local;

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
import java.util.Arrays;
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

  private volatile PendingActionDao _pendingActionDao;

  private volatile WhatToWatchDao _whatToWatchDao;

  private volatile CastCacheDao _castCacheDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(12) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `watch_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `year` INTEGER NOT NULL, `type` TEXT NOT NULL, `isWatched` INTEGER NOT NULL, `rating` REAL NOT NULL, `season` INTEGER, `episode` INTEGER, `notes` TEXT NOT NULL, `posterUrl` TEXT, `genres` TEXT NOT NULL, `imdbId` TEXT NOT NULL, `dateAdded` INTEGER NOT NULL, `lastUpdated` INTEGER NOT NULL, `plot` TEXT NOT NULL, `trailerUrl` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_items_isWatched` ON `watch_items` (`isWatched`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_items_type` ON `watch_items` (`type`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_items_dateAdded` ON `watch_items` (`dateAdded`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_items_imdbId` ON `watch_items` (`imdbId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `series_cache` (`imdbId` TEXT NOT NULL, `title` TEXT NOT NULL, `totalSeasons` INTEGER NOT NULL, `isFinished` INTEGER NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`imdbId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `season_episodes` (`imdbId` TEXT NOT NULL, `season` INTEGER NOT NULL, `episodeNumber` INTEGER NOT NULL, `title` TEXT NOT NULL, `released` TEXT NOT NULL, `imdbRating` TEXT NOT NULL, `episodeImdbId` TEXT NOT NULL, `plot` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`imdbId`, `season`, `episodeNumber`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `episode_progress` (`watchItemId` INTEGER NOT NULL, `season` INTEGER NOT NULL, `episodeNumber` INTEGER NOT NULL, `isWatched` INTEGER NOT NULL, `watchedAt` INTEGER, PRIMARY KEY(`watchItemId`, `season`, `episodeNumber`), FOREIGN KEY(`watchItemId`) REFERENCES `watch_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_episode_progress_watchItemId` ON `episode_progress` (`watchItemId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `pending_actions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `actionType` TEXT NOT NULL, `userId` TEXT NOT NULL, `targetId` TEXT NOT NULL, `payload` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pending_actions_actionType_userId_targetId_payload` ON `pending_actions` (`actionType`, `userId`, `targetId`, `payload`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `cast_cache` (`imdbId` TEXT NOT NULL, `actorName` TEXT NOT NULL, `characterName` TEXT NOT NULL, `imageUrl` TEXT, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`imdbId`, `actorName`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cast_cache_imdbId` ON `cast_cache` (`imdbId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3f8860486b90b5e2299b1dd944f1debc')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `watch_items`");
        db.execSQL("DROP TABLE IF EXISTS `series_cache`");
        db.execSQL("DROP TABLE IF EXISTS `season_episodes`");
        db.execSQL("DROP TABLE IF EXISTS `episode_progress`");
        db.execSQL("DROP TABLE IF EXISTS `pending_actions`");
        db.execSQL("DROP TABLE IF EXISTS `cast_cache`");
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
        db.execSQL("PRAGMA foreign_keys = ON");
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
        final HashMap<String, TableInfo.Column> _columnsWatchItems = new HashMap<String, TableInfo.Column>(17);
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
        _columnsWatchItems.put("plot", new TableInfo.Column("plot", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatchItems.put("trailerUrl", new TableInfo.Column("trailerUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatchItems.put("isFavorite", new TableInfo.Column("isFavorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWatchItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWatchItems = new HashSet<TableInfo.Index>(4);
        _indicesWatchItems.add(new TableInfo.Index("index_watch_items_isWatched", false, Arrays.asList("isWatched"), Arrays.asList("ASC")));
        _indicesWatchItems.add(new TableInfo.Index("index_watch_items_type", false, Arrays.asList("type"), Arrays.asList("ASC")));
        _indicesWatchItems.add(new TableInfo.Index("index_watch_items_dateAdded", false, Arrays.asList("dateAdded"), Arrays.asList("ASC")));
        _indicesWatchItems.add(new TableInfo.Index("index_watch_items_imdbId", false, Arrays.asList("imdbId"), Arrays.asList("ASC")));
        final TableInfo _infoWatchItems = new TableInfo("watch_items", _columnsWatchItems, _foreignKeysWatchItems, _indicesWatchItems);
        final TableInfo _existingWatchItems = TableInfo.read(db, "watch_items");
        if (!_infoWatchItems.equals(_existingWatchItems)) {
          return new RoomOpenHelper.ValidationResult(false, "watch_items(com.kaze.model.WatchItem).\n"
                  + " Expected:\n" + _infoWatchItems + "\n"
                  + " Found:\n" + _existingWatchItems);
        }
        final HashMap<String, TableInfo.Column> _columnsSeriesCache = new HashMap<String, TableInfo.Column>(5);
        _columnsSeriesCache.put("imdbId", new TableInfo.Column("imdbId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeriesCache.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeriesCache.put("totalSeasons", new TableInfo.Column("totalSeasons", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeriesCache.put("isFinished", new TableInfo.Column("isFinished", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeriesCache.put("cachedAt", new TableInfo.Column("cachedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSeriesCache = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSeriesCache = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSeriesCache = new TableInfo("series_cache", _columnsSeriesCache, _foreignKeysSeriesCache, _indicesSeriesCache);
        final TableInfo _existingSeriesCache = TableInfo.read(db, "series_cache");
        if (!_infoSeriesCache.equals(_existingSeriesCache)) {
          return new RoomOpenHelper.ValidationResult(false, "series_cache(com.kaze.data.local.SeriesCache).\n"
                  + " Expected:\n" + _infoSeriesCache + "\n"
                  + " Found:\n" + _existingSeriesCache);
        }
        final HashMap<String, TableInfo.Column> _columnsSeasonEpisodes = new HashMap<String, TableInfo.Column>(9);
        _columnsSeasonEpisodes.put("imdbId", new TableInfo.Column("imdbId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeasonEpisodes.put("season", new TableInfo.Column("season", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeasonEpisodes.put("episodeNumber", new TableInfo.Column("episodeNumber", "INTEGER", true, 3, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeasonEpisodes.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeasonEpisodes.put("released", new TableInfo.Column("released", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeasonEpisodes.put("imdbRating", new TableInfo.Column("imdbRating", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeasonEpisodes.put("episodeImdbId", new TableInfo.Column("episodeImdbId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeasonEpisodes.put("plot", new TableInfo.Column("plot", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeasonEpisodes.put("cachedAt", new TableInfo.Column("cachedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSeasonEpisodes = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSeasonEpisodes = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSeasonEpisodes = new TableInfo("season_episodes", _columnsSeasonEpisodes, _foreignKeysSeasonEpisodes, _indicesSeasonEpisodes);
        final TableInfo _existingSeasonEpisodes = TableInfo.read(db, "season_episodes");
        if (!_infoSeasonEpisodes.equals(_existingSeasonEpisodes)) {
          return new RoomOpenHelper.ValidationResult(false, "season_episodes(com.kaze.data.local.SeasonEpisode).\n"
                  + " Expected:\n" + _infoSeasonEpisodes + "\n"
                  + " Found:\n" + _existingSeasonEpisodes);
        }
        final HashMap<String, TableInfo.Column> _columnsEpisodeProgress = new HashMap<String, TableInfo.Column>(5);
        _columnsEpisodeProgress.put("watchItemId", new TableInfo.Column("watchItemId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpisodeProgress.put("season", new TableInfo.Column("season", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpisodeProgress.put("episodeNumber", new TableInfo.Column("episodeNumber", "INTEGER", true, 3, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpisodeProgress.put("isWatched", new TableInfo.Column("isWatched", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpisodeProgress.put("watchedAt", new TableInfo.Column("watchedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysEpisodeProgress = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysEpisodeProgress.add(new TableInfo.ForeignKey("watch_items", "CASCADE", "NO ACTION", Arrays.asList("watchItemId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesEpisodeProgress = new HashSet<TableInfo.Index>(1);
        _indicesEpisodeProgress.add(new TableInfo.Index("index_episode_progress_watchItemId", false, Arrays.asList("watchItemId"), Arrays.asList("ASC")));
        final TableInfo _infoEpisodeProgress = new TableInfo("episode_progress", _columnsEpisodeProgress, _foreignKeysEpisodeProgress, _indicesEpisodeProgress);
        final TableInfo _existingEpisodeProgress = TableInfo.read(db, "episode_progress");
        if (!_infoEpisodeProgress.equals(_existingEpisodeProgress)) {
          return new RoomOpenHelper.ValidationResult(false, "episode_progress(com.kaze.data.local.EpisodeProgress).\n"
                  + " Expected:\n" + _infoEpisodeProgress + "\n"
                  + " Found:\n" + _existingEpisodeProgress);
        }
        final HashMap<String, TableInfo.Column> _columnsPendingActions = new HashMap<String, TableInfo.Column>(6);
        _columnsPendingActions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingActions.put("actionType", new TableInfo.Column("actionType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingActions.put("userId", new TableInfo.Column("userId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingActions.put("targetId", new TableInfo.Column("targetId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingActions.put("payload", new TableInfo.Column("payload", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingActions.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPendingActions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPendingActions = new HashSet<TableInfo.Index>(1);
        _indicesPendingActions.add(new TableInfo.Index("index_pending_actions_actionType_userId_targetId_payload", true, Arrays.asList("actionType", "userId", "targetId", "payload"), Arrays.asList("ASC", "ASC", "ASC", "ASC")));
        final TableInfo _infoPendingActions = new TableInfo("pending_actions", _columnsPendingActions, _foreignKeysPendingActions, _indicesPendingActions);
        final TableInfo _existingPendingActions = TableInfo.read(db, "pending_actions");
        if (!_infoPendingActions.equals(_existingPendingActions)) {
          return new RoomOpenHelper.ValidationResult(false, "pending_actions(com.kaze.data.local.PendingAction).\n"
                  + " Expected:\n" + _infoPendingActions + "\n"
                  + " Found:\n" + _existingPendingActions);
        }
        final HashMap<String, TableInfo.Column> _columnsCastCache = new HashMap<String, TableInfo.Column>(5);
        _columnsCastCache.put("imdbId", new TableInfo.Column("imdbId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCastCache.put("actorName", new TableInfo.Column("actorName", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCastCache.put("characterName", new TableInfo.Column("characterName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCastCache.put("imageUrl", new TableInfo.Column("imageUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCastCache.put("cachedAt", new TableInfo.Column("cachedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCastCache = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCastCache = new HashSet<TableInfo.Index>(1);
        _indicesCastCache.add(new TableInfo.Index("index_cast_cache_imdbId", false, Arrays.asList("imdbId"), Arrays.asList("ASC")));
        final TableInfo _infoCastCache = new TableInfo("cast_cache", _columnsCastCache, _foreignKeysCastCache, _indicesCastCache);
        final TableInfo _existingCastCache = TableInfo.read(db, "cast_cache");
        if (!_infoCastCache.equals(_existingCastCache)) {
          return new RoomOpenHelper.ValidationResult(false, "cast_cache(com.kaze.data.local.CastCacheEntity).\n"
                  + " Expected:\n" + _infoCastCache + "\n"
                  + " Found:\n" + _existingCastCache);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "3f8860486b90b5e2299b1dd944f1debc", "4a0265fff4fd5841d081eb3e6208e837");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "watch_items","series_cache","season_episodes","episode_progress","pending_actions","cast_cache");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `watch_items`");
      _db.execSQL("DELETE FROM `series_cache`");
      _db.execSQL("DELETE FROM `season_episodes`");
      _db.execSQL("DELETE FROM `episode_progress`");
      _db.execSQL("DELETE FROM `pending_actions`");
      _db.execSQL("DELETE FROM `cast_cache`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
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
    _typeConvertersMap.put(PendingActionDao.class, PendingActionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(WhatToWatchDao.class, WhatToWatchDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CastCacheDao.class, CastCacheDao_Impl.getRequiredConverters());
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

  @Override
  public PendingActionDao pendingActionDao() {
    if (_pendingActionDao != null) {
      return _pendingActionDao;
    } else {
      synchronized(this) {
        if(_pendingActionDao == null) {
          _pendingActionDao = new PendingActionDao_Impl(this);
        }
        return _pendingActionDao;
      }
    }
  }

  @Override
  public WhatToWatchDao whatToWatchDao() {
    if (_whatToWatchDao != null) {
      return _whatToWatchDao;
    } else {
      synchronized(this) {
        if(_whatToWatchDao == null) {
          _whatToWatchDao = new WhatToWatchDao_Impl(this);
        }
        return _whatToWatchDao;
      }
    }
  }

  @Override
  public CastCacheDao castCacheDao() {
    if (_castCacheDao != null) {
      return _castCacheDao;
    } else {
      synchronized(this) {
        if(_castCacheDao == null) {
          _castCacheDao = new CastCacheDao_Impl(this);
        }
        return _castCacheDao;
      }
    }
  }
}
