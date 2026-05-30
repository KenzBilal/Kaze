package com.watchlater.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.watchlater.model.MediaType;
import com.watchlater.model.WatchItem;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WatchItemDao_Impl implements WatchItemDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WatchItem> __insertionAdapterOfWatchItem;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<WatchItem> __deletionAdapterOfWatchItem;

  private final EntityDeletionOrUpdateAdapter<WatchItem> __updateAdapterOfWatchItem;

  private final SharedSQLiteStatement __preparedStmtOfDeleteItemById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public WatchItemDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWatchItem = new EntityInsertionAdapter<WatchItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `watch_items` (`id`,`title`,`year`,`type`,`isWatched`,`rating`,`season`,`episode`,`notes`,`posterUrl`,`genres`,`imdbId`,`dateAdded`,`lastUpdated`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WatchItem entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindLong(3, entity.getYear());
        final String _tmp = __converters.fromMediaType(entity.getType());
        statement.bindString(4, _tmp);
        final int _tmp_1 = entity.isWatched() ? 1 : 0;
        statement.bindLong(5, _tmp_1);
        statement.bindDouble(6, entity.getRating());
        if (entity.getSeason() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getSeason());
        }
        if (entity.getEpisode() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getEpisode());
        }
        statement.bindString(9, entity.getNotes());
        if (entity.getPosterUrl() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getPosterUrl());
        }
        statement.bindString(11, entity.getGenres());
        statement.bindString(12, entity.getImdbId());
        statement.bindLong(13, entity.getDateAdded());
        statement.bindLong(14, entity.getLastUpdated());
      }
    };
    this.__deletionAdapterOfWatchItem = new EntityDeletionOrUpdateAdapter<WatchItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `watch_items` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WatchItem entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfWatchItem = new EntityDeletionOrUpdateAdapter<WatchItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `watch_items` SET `id` = ?,`title` = ?,`year` = ?,`type` = ?,`isWatched` = ?,`rating` = ?,`season` = ?,`episode` = ?,`notes` = ?,`posterUrl` = ?,`genres` = ?,`imdbId` = ?,`dateAdded` = ?,`lastUpdated` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WatchItem entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindLong(3, entity.getYear());
        final String _tmp = __converters.fromMediaType(entity.getType());
        statement.bindString(4, _tmp);
        final int _tmp_1 = entity.isWatched() ? 1 : 0;
        statement.bindLong(5, _tmp_1);
        statement.bindDouble(6, entity.getRating());
        if (entity.getSeason() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getSeason());
        }
        if (entity.getEpisode() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getEpisode());
        }
        statement.bindString(9, entity.getNotes());
        if (entity.getPosterUrl() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getPosterUrl());
        }
        statement.bindString(11, entity.getGenres());
        statement.bindString(12, entity.getImdbId());
        statement.bindLong(13, entity.getDateAdded());
        statement.bindLong(14, entity.getLastUpdated());
        statement.bindLong(15, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteItemById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM watch_items WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM watch_items";
        return _query;
      }
    };
  }

  @Override
  public Object insertItem(final WatchItem item, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfWatchItem.insertAndReturnId(item);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<WatchItem> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWatchItem.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteItem(final WatchItem item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfWatchItem.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateItem(final WatchItem item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfWatchItem.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteItemById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteItemById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteItemById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<WatchItem>> getAllItems() {
    final String _sql = "SELECT * FROM watch_items ORDER BY dateAdded DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"watch_items"}, new Callable<List<WatchItem>>() {
      @Override
      @NonNull
      public List<WatchItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIsWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "isWatched");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfSeason = CursorUtil.getColumnIndexOrThrow(_cursor, "season");
          final int _cursorIndexOfEpisode = CursorUtil.getColumnIndexOrThrow(_cursor, "episode");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfGenres = CursorUtil.getColumnIndexOrThrow(_cursor, "genres");
          final int _cursorIndexOfImdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "imdbId");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
          final List<WatchItem> _result = new ArrayList<WatchItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WatchItem _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final MediaType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toMediaType(_tmp);
            final boolean _tmpIsWatched;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsWatched);
            _tmpIsWatched = _tmp_1 != 0;
            final float _tmpRating;
            _tmpRating = _cursor.getFloat(_cursorIndexOfRating);
            final Integer _tmpSeason;
            if (_cursor.isNull(_cursorIndexOfSeason)) {
              _tmpSeason = null;
            } else {
              _tmpSeason = _cursor.getInt(_cursorIndexOfSeason);
            }
            final Integer _tmpEpisode;
            if (_cursor.isNull(_cursorIndexOfEpisode)) {
              _tmpEpisode = null;
            } else {
              _tmpEpisode = _cursor.getInt(_cursorIndexOfEpisode);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpPosterUrl;
            if (_cursor.isNull(_cursorIndexOfPosterUrl)) {
              _tmpPosterUrl = null;
            } else {
              _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            }
            final String _tmpGenres;
            _tmpGenres = _cursor.getString(_cursorIndexOfGenres);
            final String _tmpImdbId;
            _tmpImdbId = _cursor.getString(_cursorIndexOfImdbId);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            _item = new WatchItem(_tmpId,_tmpTitle,_tmpYear,_tmpType,_tmpIsWatched,_tmpRating,_tmpSeason,_tmpEpisode,_tmpNotes,_tmpPosterUrl,_tmpGenres,_tmpImdbId,_tmpDateAdded,_tmpLastUpdated);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<WatchItem> getItemByIdFlow(final long id) {
    final String _sql = "SELECT * FROM watch_items WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"watch_items"}, new Callable<WatchItem>() {
      @Override
      @Nullable
      public WatchItem call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIsWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "isWatched");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfSeason = CursorUtil.getColumnIndexOrThrow(_cursor, "season");
          final int _cursorIndexOfEpisode = CursorUtil.getColumnIndexOrThrow(_cursor, "episode");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfGenres = CursorUtil.getColumnIndexOrThrow(_cursor, "genres");
          final int _cursorIndexOfImdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "imdbId");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
          final WatchItem _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final MediaType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toMediaType(_tmp);
            final boolean _tmpIsWatched;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsWatched);
            _tmpIsWatched = _tmp_1 != 0;
            final float _tmpRating;
            _tmpRating = _cursor.getFloat(_cursorIndexOfRating);
            final Integer _tmpSeason;
            if (_cursor.isNull(_cursorIndexOfSeason)) {
              _tmpSeason = null;
            } else {
              _tmpSeason = _cursor.getInt(_cursorIndexOfSeason);
            }
            final Integer _tmpEpisode;
            if (_cursor.isNull(_cursorIndexOfEpisode)) {
              _tmpEpisode = null;
            } else {
              _tmpEpisode = _cursor.getInt(_cursorIndexOfEpisode);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpPosterUrl;
            if (_cursor.isNull(_cursorIndexOfPosterUrl)) {
              _tmpPosterUrl = null;
            } else {
              _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            }
            final String _tmpGenres;
            _tmpGenres = _cursor.getString(_cursorIndexOfGenres);
            final String _tmpImdbId;
            _tmpImdbId = _cursor.getString(_cursorIndexOfImdbId);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            _result = new WatchItem(_tmpId,_tmpTitle,_tmpYear,_tmpType,_tmpIsWatched,_tmpRating,_tmpSeason,_tmpEpisode,_tmpNotes,_tmpPosterUrl,_tmpGenres,_tmpImdbId,_tmpDateAdded,_tmpLastUpdated);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getItemById(final long id, final Continuation<? super WatchItem> $completion) {
    final String _sql = "SELECT * FROM watch_items WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<WatchItem>() {
      @Override
      @Nullable
      public WatchItem call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIsWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "isWatched");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfSeason = CursorUtil.getColumnIndexOrThrow(_cursor, "season");
          final int _cursorIndexOfEpisode = CursorUtil.getColumnIndexOrThrow(_cursor, "episode");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfGenres = CursorUtil.getColumnIndexOrThrow(_cursor, "genres");
          final int _cursorIndexOfImdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "imdbId");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
          final WatchItem _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final MediaType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toMediaType(_tmp);
            final boolean _tmpIsWatched;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsWatched);
            _tmpIsWatched = _tmp_1 != 0;
            final float _tmpRating;
            _tmpRating = _cursor.getFloat(_cursorIndexOfRating);
            final Integer _tmpSeason;
            if (_cursor.isNull(_cursorIndexOfSeason)) {
              _tmpSeason = null;
            } else {
              _tmpSeason = _cursor.getInt(_cursorIndexOfSeason);
            }
            final Integer _tmpEpisode;
            if (_cursor.isNull(_cursorIndexOfEpisode)) {
              _tmpEpisode = null;
            } else {
              _tmpEpisode = _cursor.getInt(_cursorIndexOfEpisode);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpPosterUrl;
            if (_cursor.isNull(_cursorIndexOfPosterUrl)) {
              _tmpPosterUrl = null;
            } else {
              _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            }
            final String _tmpGenres;
            _tmpGenres = _cursor.getString(_cursorIndexOfGenres);
            final String _tmpImdbId;
            _tmpImdbId = _cursor.getString(_cursorIndexOfImdbId);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            _result = new WatchItem(_tmpId,_tmpTitle,_tmpYear,_tmpType,_tmpIsWatched,_tmpRating,_tmpSeason,_tmpEpisode,_tmpNotes,_tmpPosterUrl,_tmpGenres,_tmpImdbId,_tmpDateAdded,_tmpLastUpdated);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<WatchItem>> searchItems(final String query) {
    final String _sql = "\n"
            + "        SELECT * FROM watch_items \n"
            + "        WHERE title LIKE '%' || ? || '%' \n"
            + "        ORDER BY dateAdded DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"watch_items"}, new Callable<List<WatchItem>>() {
      @Override
      @NonNull
      public List<WatchItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIsWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "isWatched");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfSeason = CursorUtil.getColumnIndexOrThrow(_cursor, "season");
          final int _cursorIndexOfEpisode = CursorUtil.getColumnIndexOrThrow(_cursor, "episode");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfGenres = CursorUtil.getColumnIndexOrThrow(_cursor, "genres");
          final int _cursorIndexOfImdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "imdbId");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
          final List<WatchItem> _result = new ArrayList<WatchItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WatchItem _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final MediaType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toMediaType(_tmp);
            final boolean _tmpIsWatched;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsWatched);
            _tmpIsWatched = _tmp_1 != 0;
            final float _tmpRating;
            _tmpRating = _cursor.getFloat(_cursorIndexOfRating);
            final Integer _tmpSeason;
            if (_cursor.isNull(_cursorIndexOfSeason)) {
              _tmpSeason = null;
            } else {
              _tmpSeason = _cursor.getInt(_cursorIndexOfSeason);
            }
            final Integer _tmpEpisode;
            if (_cursor.isNull(_cursorIndexOfEpisode)) {
              _tmpEpisode = null;
            } else {
              _tmpEpisode = _cursor.getInt(_cursorIndexOfEpisode);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpPosterUrl;
            if (_cursor.isNull(_cursorIndexOfPosterUrl)) {
              _tmpPosterUrl = null;
            } else {
              _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            }
            final String _tmpGenres;
            _tmpGenres = _cursor.getString(_cursorIndexOfGenres);
            final String _tmpImdbId;
            _tmpImdbId = _cursor.getString(_cursorIndexOfImdbId);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            _item = new WatchItem(_tmpId,_tmpTitle,_tmpYear,_tmpType,_tmpIsWatched,_tmpRating,_tmpSeason,_tmpEpisode,_tmpNotes,_tmpPosterUrl,_tmpGenres,_tmpImdbId,_tmpDateAdded,_tmpLastUpdated);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> getTotalCount() {
    final String _sql = "SELECT COUNT(*) FROM watch_items";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"watch_items"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> getMovieCount() {
    final String _sql = "SELECT COUNT(*) FROM watch_items WHERE type = 'MOVIE'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"watch_items"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> getSeriesCount() {
    final String _sql = "SELECT COUNT(*) FROM watch_items WHERE type = 'SERIES'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"watch_items"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> getWatchedCount() {
    final String _sql = "SELECT COUNT(*) FROM watch_items WHERE isWatched = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"watch_items"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<WatchItem>> getSeriesInProgress() {
    final String _sql = "\n"
            + "        SELECT * FROM watch_items \n"
            + "        WHERE type = 'SERIES' AND isWatched = 0 \n"
            + "        AND (season IS NOT NULL OR episode IS NOT NULL)\n"
            + "        ORDER BY lastUpdated DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"watch_items"}, new Callable<List<WatchItem>>() {
      @Override
      @NonNull
      public List<WatchItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIsWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "isWatched");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfSeason = CursorUtil.getColumnIndexOrThrow(_cursor, "season");
          final int _cursorIndexOfEpisode = CursorUtil.getColumnIndexOrThrow(_cursor, "episode");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfGenres = CursorUtil.getColumnIndexOrThrow(_cursor, "genres");
          final int _cursorIndexOfImdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "imdbId");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
          final List<WatchItem> _result = new ArrayList<WatchItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WatchItem _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final MediaType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toMediaType(_tmp);
            final boolean _tmpIsWatched;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsWatched);
            _tmpIsWatched = _tmp_1 != 0;
            final float _tmpRating;
            _tmpRating = _cursor.getFloat(_cursorIndexOfRating);
            final Integer _tmpSeason;
            if (_cursor.isNull(_cursorIndexOfSeason)) {
              _tmpSeason = null;
            } else {
              _tmpSeason = _cursor.getInt(_cursorIndexOfSeason);
            }
            final Integer _tmpEpisode;
            if (_cursor.isNull(_cursorIndexOfEpisode)) {
              _tmpEpisode = null;
            } else {
              _tmpEpisode = _cursor.getInt(_cursorIndexOfEpisode);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpPosterUrl;
            if (_cursor.isNull(_cursorIndexOfPosterUrl)) {
              _tmpPosterUrl = null;
            } else {
              _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            }
            final String _tmpGenres;
            _tmpGenres = _cursor.getString(_cursorIndexOfGenres);
            final String _tmpImdbId;
            _tmpImdbId = _cursor.getString(_cursorIndexOfImdbId);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            _item = new WatchItem(_tmpId,_tmpTitle,_tmpYear,_tmpType,_tmpIsWatched,_tmpRating,_tmpSeason,_tmpEpisode,_tmpNotes,_tmpPosterUrl,_tmpGenres,_tmpImdbId,_tmpDateAdded,_tmpLastUpdated);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<WatchItem>> getRecentlyAdded(final int limit) {
    final String _sql = "SELECT * FROM watch_items ORDER BY dateAdded DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"watch_items"}, new Callable<List<WatchItem>>() {
      @Override
      @NonNull
      public List<WatchItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIsWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "isWatched");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfSeason = CursorUtil.getColumnIndexOrThrow(_cursor, "season");
          final int _cursorIndexOfEpisode = CursorUtil.getColumnIndexOrThrow(_cursor, "episode");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfGenres = CursorUtil.getColumnIndexOrThrow(_cursor, "genres");
          final int _cursorIndexOfImdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "imdbId");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
          final List<WatchItem> _result = new ArrayList<WatchItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WatchItem _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final MediaType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toMediaType(_tmp);
            final boolean _tmpIsWatched;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsWatched);
            _tmpIsWatched = _tmp_1 != 0;
            final float _tmpRating;
            _tmpRating = _cursor.getFloat(_cursorIndexOfRating);
            final Integer _tmpSeason;
            if (_cursor.isNull(_cursorIndexOfSeason)) {
              _tmpSeason = null;
            } else {
              _tmpSeason = _cursor.getInt(_cursorIndexOfSeason);
            }
            final Integer _tmpEpisode;
            if (_cursor.isNull(_cursorIndexOfEpisode)) {
              _tmpEpisode = null;
            } else {
              _tmpEpisode = _cursor.getInt(_cursorIndexOfEpisode);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpPosterUrl;
            if (_cursor.isNull(_cursorIndexOfPosterUrl)) {
              _tmpPosterUrl = null;
            } else {
              _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            }
            final String _tmpGenres;
            _tmpGenres = _cursor.getString(_cursorIndexOfGenres);
            final String _tmpImdbId;
            _tmpImdbId = _cursor.getString(_cursorIndexOfImdbId);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            _item = new WatchItem(_tmpId,_tmpTitle,_tmpYear,_tmpType,_tmpIsWatched,_tmpRating,_tmpSeason,_tmpEpisode,_tmpNotes,_tmpPosterUrl,_tmpGenres,_tmpImdbId,_tmpDateAdded,_tmpLastUpdated);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<WatchItem>> getItemsViaQuery(final SupportSQLiteQuery query) {
    return CoroutinesRoom.createFlow(__db, false, new String[] {"watch_items"}, new Callable<List<WatchItem>>() {
      @Override
      @NonNull
      public List<WatchItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, query, false, null);
        try {
          final List<WatchItem> _result = new ArrayList<WatchItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WatchItem _item;
            _item = __entityCursorConverter_comWatchlaterModelWatchItem(_cursor);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private WatchItem __entityCursorConverter_comWatchlaterModelWatchItem(
      @NonNull final Cursor cursor) {
    final WatchItem _entity;
    final int _cursorIndexOfId = CursorUtil.getColumnIndex(cursor, "id");
    final int _cursorIndexOfTitle = CursorUtil.getColumnIndex(cursor, "title");
    final int _cursorIndexOfYear = CursorUtil.getColumnIndex(cursor, "year");
    final int _cursorIndexOfType = CursorUtil.getColumnIndex(cursor, "type");
    final int _cursorIndexOfIsWatched = CursorUtil.getColumnIndex(cursor, "isWatched");
    final int _cursorIndexOfRating = CursorUtil.getColumnIndex(cursor, "rating");
    final int _cursorIndexOfSeason = CursorUtil.getColumnIndex(cursor, "season");
    final int _cursorIndexOfEpisode = CursorUtil.getColumnIndex(cursor, "episode");
    final int _cursorIndexOfNotes = CursorUtil.getColumnIndex(cursor, "notes");
    final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndex(cursor, "posterUrl");
    final int _cursorIndexOfGenres = CursorUtil.getColumnIndex(cursor, "genres");
    final int _cursorIndexOfImdbId = CursorUtil.getColumnIndex(cursor, "imdbId");
    final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndex(cursor, "dateAdded");
    final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndex(cursor, "lastUpdated");
    final long _tmpId;
    if (_cursorIndexOfId == -1) {
      _tmpId = 0;
    } else {
      _tmpId = cursor.getLong(_cursorIndexOfId);
    }
    final String _tmpTitle;
    if (_cursorIndexOfTitle == -1) {
      _tmpTitle = null;
    } else {
      _tmpTitle = cursor.getString(_cursorIndexOfTitle);
    }
    final int _tmpYear;
    if (_cursorIndexOfYear == -1) {
      _tmpYear = 0;
    } else {
      _tmpYear = cursor.getInt(_cursorIndexOfYear);
    }
    final MediaType _tmpType;
    if (_cursorIndexOfType == -1) {
      _tmpType = null;
    } else {
      final String _tmp;
      _tmp = cursor.getString(_cursorIndexOfType);
      _tmpType = __converters.toMediaType(_tmp);
    }
    final boolean _tmpIsWatched;
    if (_cursorIndexOfIsWatched == -1) {
      _tmpIsWatched = false;
    } else {
      final int _tmp_1;
      _tmp_1 = cursor.getInt(_cursorIndexOfIsWatched);
      _tmpIsWatched = _tmp_1 != 0;
    }
    final float _tmpRating;
    if (_cursorIndexOfRating == -1) {
      _tmpRating = 0f;
    } else {
      _tmpRating = cursor.getFloat(_cursorIndexOfRating);
    }
    final Integer _tmpSeason;
    if (_cursorIndexOfSeason == -1) {
      _tmpSeason = null;
    } else {
      if (cursor.isNull(_cursorIndexOfSeason)) {
        _tmpSeason = null;
      } else {
        _tmpSeason = cursor.getInt(_cursorIndexOfSeason);
      }
    }
    final Integer _tmpEpisode;
    if (_cursorIndexOfEpisode == -1) {
      _tmpEpisode = null;
    } else {
      if (cursor.isNull(_cursorIndexOfEpisode)) {
        _tmpEpisode = null;
      } else {
        _tmpEpisode = cursor.getInt(_cursorIndexOfEpisode);
      }
    }
    final String _tmpNotes;
    if (_cursorIndexOfNotes == -1) {
      _tmpNotes = null;
    } else {
      _tmpNotes = cursor.getString(_cursorIndexOfNotes);
    }
    final String _tmpPosterUrl;
    if (_cursorIndexOfPosterUrl == -1) {
      _tmpPosterUrl = null;
    } else {
      if (cursor.isNull(_cursorIndexOfPosterUrl)) {
        _tmpPosterUrl = null;
      } else {
        _tmpPosterUrl = cursor.getString(_cursorIndexOfPosterUrl);
      }
    }
    final String _tmpGenres;
    if (_cursorIndexOfGenres == -1) {
      _tmpGenres = null;
    } else {
      _tmpGenres = cursor.getString(_cursorIndexOfGenres);
    }
    final String _tmpImdbId;
    if (_cursorIndexOfImdbId == -1) {
      _tmpImdbId = null;
    } else {
      _tmpImdbId = cursor.getString(_cursorIndexOfImdbId);
    }
    final long _tmpDateAdded;
    if (_cursorIndexOfDateAdded == -1) {
      _tmpDateAdded = 0;
    } else {
      _tmpDateAdded = cursor.getLong(_cursorIndexOfDateAdded);
    }
    final long _tmpLastUpdated;
    if (_cursorIndexOfLastUpdated == -1) {
      _tmpLastUpdated = 0;
    } else {
      _tmpLastUpdated = cursor.getLong(_cursorIndexOfLastUpdated);
    }
    _entity = new WatchItem(_tmpId,_tmpTitle,_tmpYear,_tmpType,_tmpIsWatched,_tmpRating,_tmpSeason,_tmpEpisode,_tmpNotes,_tmpPosterUrl,_tmpGenres,_tmpImdbId,_tmpDateAdded,_tmpLastUpdated);
    return _entity;
  }
}
