package com.kaze.data.local;

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
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
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

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SeasonEpisodeDao_Impl implements SeasonEpisodeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SeasonEpisode> __insertionAdapterOfSeasonEpisode;

  private final EntityDeletionOrUpdateAdapter<SeasonEpisode> __updateAdapterOfSeasonEpisode;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public SeasonEpisodeDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSeasonEpisode = new EntityInsertionAdapter<SeasonEpisode>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `season_episodes` (`imdbId`,`season`,`episodeNumber`,`title`,`released`,`imdbRating`,`episodeImdbId`,`plot`,`cachedAt`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SeasonEpisode entity) {
        statement.bindString(1, entity.getImdbId());
        statement.bindLong(2, entity.getSeason());
        statement.bindLong(3, entity.getEpisodeNumber());
        statement.bindString(4, entity.getTitle());
        statement.bindString(5, entity.getReleased());
        statement.bindString(6, entity.getImdbRating());
        statement.bindString(7, entity.getEpisodeImdbId());
        statement.bindString(8, entity.getPlot());
        statement.bindLong(9, entity.getCachedAt());
      }
    };
    this.__updateAdapterOfSeasonEpisode = new EntityDeletionOrUpdateAdapter<SeasonEpisode>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `season_episodes` SET `imdbId` = ?,`season` = ?,`episodeNumber` = ?,`title` = ?,`released` = ?,`imdbRating` = ?,`episodeImdbId` = ?,`plot` = ?,`cachedAt` = ? WHERE `imdbId` = ? AND `season` = ? AND `episodeNumber` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SeasonEpisode entity) {
        statement.bindString(1, entity.getImdbId());
        statement.bindLong(2, entity.getSeason());
        statement.bindLong(3, entity.getEpisodeNumber());
        statement.bindString(4, entity.getTitle());
        statement.bindString(5, entity.getReleased());
        statement.bindString(6, entity.getImdbRating());
        statement.bindString(7, entity.getEpisodeImdbId());
        statement.bindString(8, entity.getPlot());
        statement.bindLong(9, entity.getCachedAt());
        statement.bindString(10, entity.getImdbId());
        statement.bindLong(11, entity.getSeason());
        statement.bindLong(12, entity.getEpisodeNumber());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM season_episodes WHERE imdbId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<SeasonEpisode> episodes,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSeasonEpisode.insert(episodes);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final SeasonEpisode episode, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSeasonEpisode.handle(episode);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final String imdbId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, imdbId);
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
  public Object getSeason(final String imdbId, final int season,
      final Continuation<? super List<SeasonEpisode>> $completion) {
    final String _sql = "SELECT * FROM season_episodes WHERE imdbId = ? AND season = ? ORDER BY episodeNumber ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, imdbId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, season);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SeasonEpisode>>() {
      @Override
      @NonNull
      public List<SeasonEpisode> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfImdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "imdbId");
          final int _cursorIndexOfSeason = CursorUtil.getColumnIndexOrThrow(_cursor, "season");
          final int _cursorIndexOfEpisodeNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeNumber");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfReleased = CursorUtil.getColumnIndexOrThrow(_cursor, "released");
          final int _cursorIndexOfImdbRating = CursorUtil.getColumnIndexOrThrow(_cursor, "imdbRating");
          final int _cursorIndexOfEpisodeImdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeImdbId");
          final int _cursorIndexOfPlot = CursorUtil.getColumnIndexOrThrow(_cursor, "plot");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final List<SeasonEpisode> _result = new ArrayList<SeasonEpisode>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SeasonEpisode _item;
            final String _tmpImdbId;
            _tmpImdbId = _cursor.getString(_cursorIndexOfImdbId);
            final int _tmpSeason;
            _tmpSeason = _cursor.getInt(_cursorIndexOfSeason);
            final int _tmpEpisodeNumber;
            _tmpEpisodeNumber = _cursor.getInt(_cursorIndexOfEpisodeNumber);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpReleased;
            _tmpReleased = _cursor.getString(_cursorIndexOfReleased);
            final String _tmpImdbRating;
            _tmpImdbRating = _cursor.getString(_cursorIndexOfImdbRating);
            final String _tmpEpisodeImdbId;
            _tmpEpisodeImdbId = _cursor.getString(_cursorIndexOfEpisodeImdbId);
            final String _tmpPlot;
            _tmpPlot = _cursor.getString(_cursorIndexOfPlot);
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            _item = new SeasonEpisode(_tmpImdbId,_tmpSeason,_tmpEpisodeNumber,_tmpTitle,_tmpReleased,_tmpImdbRating,_tmpEpisodeImdbId,_tmpPlot,_tmpCachedAt);
            _result.add(_item);
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
  public Object getOne(final String imdbId, final int season, final int episodeNumber,
      final Continuation<? super SeasonEpisode> $completion) {
    final String _sql = "SELECT * FROM season_episodes WHERE imdbId = ? AND season = ? AND episodeNumber = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, imdbId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, season);
    _argIndex = 3;
    _statement.bindLong(_argIndex, episodeNumber);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SeasonEpisode>() {
      @Override
      @Nullable
      public SeasonEpisode call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfImdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "imdbId");
          final int _cursorIndexOfSeason = CursorUtil.getColumnIndexOrThrow(_cursor, "season");
          final int _cursorIndexOfEpisodeNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeNumber");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfReleased = CursorUtil.getColumnIndexOrThrow(_cursor, "released");
          final int _cursorIndexOfImdbRating = CursorUtil.getColumnIndexOrThrow(_cursor, "imdbRating");
          final int _cursorIndexOfEpisodeImdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeImdbId");
          final int _cursorIndexOfPlot = CursorUtil.getColumnIndexOrThrow(_cursor, "plot");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final SeasonEpisode _result;
          if (_cursor.moveToFirst()) {
            final String _tmpImdbId;
            _tmpImdbId = _cursor.getString(_cursorIndexOfImdbId);
            final int _tmpSeason;
            _tmpSeason = _cursor.getInt(_cursorIndexOfSeason);
            final int _tmpEpisodeNumber;
            _tmpEpisodeNumber = _cursor.getInt(_cursorIndexOfEpisodeNumber);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpReleased;
            _tmpReleased = _cursor.getString(_cursorIndexOfReleased);
            final String _tmpImdbRating;
            _tmpImdbRating = _cursor.getString(_cursorIndexOfImdbRating);
            final String _tmpEpisodeImdbId;
            _tmpEpisodeImdbId = _cursor.getString(_cursorIndexOfEpisodeImdbId);
            final String _tmpPlot;
            _tmpPlot = _cursor.getString(_cursorIndexOfPlot);
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            _result = new SeasonEpisode(_tmpImdbId,_tmpSeason,_tmpEpisodeNumber,_tmpTitle,_tmpReleased,_tmpImdbRating,_tmpEpisodeImdbId,_tmpPlot,_tmpCachedAt);
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
  public Object getCachedAt(final String imdbId, final int season,
      final Continuation<? super Long> $completion) {
    final String _sql = "SELECT cachedAt FROM season_episodes WHERE imdbId = ? AND season = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, imdbId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, season);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @Nullable
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            if (_cursor.isNull(0)) {
              _result = null;
            } else {
              _result = _cursor.getLong(0);
            }
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
