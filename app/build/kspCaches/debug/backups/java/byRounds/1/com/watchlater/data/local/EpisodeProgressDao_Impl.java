package com.watchlater.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
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
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class EpisodeProgressDao_Impl implements EpisodeProgressDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<EpisodeProgress> __insertionAdapterOfEpisodeProgress;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public EpisodeProgressDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfEpisodeProgress = new EntityInsertionAdapter<EpisodeProgress>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `episode_progress` (`watchItemId`,`season`,`episodeNumber`,`isWatched`,`watchedAt`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EpisodeProgress entity) {
        statement.bindLong(1, entity.getWatchItemId());
        statement.bindLong(2, entity.getSeason());
        statement.bindLong(3, entity.getEpisodeNumber());
        final int _tmp = entity.isWatched() ? 1 : 0;
        statement.bindLong(4, _tmp);
        if (entity.getWatchedAt() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getWatchedAt());
        }
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM episode_progress WHERE watchItemId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final EpisodeProgress progress,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfEpisodeProgress.insert(progress);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<EpisodeProgress> list,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfEpisodeProgress.insert(list);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<EpisodeProgress> list,
      final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> EpisodeProgressDao.DefaultImpls.upsertAll(EpisodeProgressDao_Impl.this, list, __cont), $completion);
  }

  @Override
  public Object deleteAll(final long watchItemId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, watchItemId);
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
  public Flow<List<EpisodeProgress>> observeAll(final long watchItemId) {
    final String _sql = "SELECT * FROM episode_progress WHERE watchItemId = ? ORDER BY season ASC, episodeNumber ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, watchItemId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"episode_progress"}, new Callable<List<EpisodeProgress>>() {
      @Override
      @NonNull
      public List<EpisodeProgress> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfWatchItemId = CursorUtil.getColumnIndexOrThrow(_cursor, "watchItemId");
          final int _cursorIndexOfSeason = CursorUtil.getColumnIndexOrThrow(_cursor, "season");
          final int _cursorIndexOfEpisodeNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeNumber");
          final int _cursorIndexOfIsWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "isWatched");
          final int _cursorIndexOfWatchedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "watchedAt");
          final List<EpisodeProgress> _result = new ArrayList<EpisodeProgress>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EpisodeProgress _item;
            final long _tmpWatchItemId;
            _tmpWatchItemId = _cursor.getLong(_cursorIndexOfWatchItemId);
            final int _tmpSeason;
            _tmpSeason = _cursor.getInt(_cursorIndexOfSeason);
            final int _tmpEpisodeNumber;
            _tmpEpisodeNumber = _cursor.getInt(_cursorIndexOfEpisodeNumber);
            final boolean _tmpIsWatched;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWatched);
            _tmpIsWatched = _tmp != 0;
            final Long _tmpWatchedAt;
            if (_cursor.isNull(_cursorIndexOfWatchedAt)) {
              _tmpWatchedAt = null;
            } else {
              _tmpWatchedAt = _cursor.getLong(_cursorIndexOfWatchedAt);
            }
            _item = new EpisodeProgress(_tmpWatchItemId,_tmpSeason,_tmpEpisodeNumber,_tmpIsWatched,_tmpWatchedAt);
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
  public Object getSeason(final long watchItemId, final int season,
      final Continuation<? super List<EpisodeProgress>> $completion) {
    final String _sql = "SELECT * FROM episode_progress WHERE watchItemId = ? AND season = ? ORDER BY episodeNumber ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, watchItemId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, season);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<EpisodeProgress>>() {
      @Override
      @NonNull
      public List<EpisodeProgress> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfWatchItemId = CursorUtil.getColumnIndexOrThrow(_cursor, "watchItemId");
          final int _cursorIndexOfSeason = CursorUtil.getColumnIndexOrThrow(_cursor, "season");
          final int _cursorIndexOfEpisodeNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeNumber");
          final int _cursorIndexOfIsWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "isWatched");
          final int _cursorIndexOfWatchedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "watchedAt");
          final List<EpisodeProgress> _result = new ArrayList<EpisodeProgress>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EpisodeProgress _item;
            final long _tmpWatchItemId;
            _tmpWatchItemId = _cursor.getLong(_cursorIndexOfWatchItemId);
            final int _tmpSeason;
            _tmpSeason = _cursor.getInt(_cursorIndexOfSeason);
            final int _tmpEpisodeNumber;
            _tmpEpisodeNumber = _cursor.getInt(_cursorIndexOfEpisodeNumber);
            final boolean _tmpIsWatched;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWatched);
            _tmpIsWatched = _tmp != 0;
            final Long _tmpWatchedAt;
            if (_cursor.isNull(_cursorIndexOfWatchedAt)) {
              _tmpWatchedAt = null;
            } else {
              _tmpWatchedAt = _cursor.getLong(_cursorIndexOfWatchedAt);
            }
            _item = new EpisodeProgress(_tmpWatchItemId,_tmpSeason,_tmpEpisodeNumber,_tmpIsWatched,_tmpWatchedAt);
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
  public Object get(final long watchItemId, final int season, final int ep,
      final Continuation<? super EpisodeProgress> $completion) {
    final String _sql = "SELECT * FROM episode_progress WHERE watchItemId = ? AND season = ? AND episodeNumber = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, watchItemId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, season);
    _argIndex = 3;
    _statement.bindLong(_argIndex, ep);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<EpisodeProgress>() {
      @Override
      @Nullable
      public EpisodeProgress call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfWatchItemId = CursorUtil.getColumnIndexOrThrow(_cursor, "watchItemId");
          final int _cursorIndexOfSeason = CursorUtil.getColumnIndexOrThrow(_cursor, "season");
          final int _cursorIndexOfEpisodeNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeNumber");
          final int _cursorIndexOfIsWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "isWatched");
          final int _cursorIndexOfWatchedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "watchedAt");
          final EpisodeProgress _result;
          if (_cursor.moveToFirst()) {
            final long _tmpWatchItemId;
            _tmpWatchItemId = _cursor.getLong(_cursorIndexOfWatchItemId);
            final int _tmpSeason;
            _tmpSeason = _cursor.getInt(_cursorIndexOfSeason);
            final int _tmpEpisodeNumber;
            _tmpEpisodeNumber = _cursor.getInt(_cursorIndexOfEpisodeNumber);
            final boolean _tmpIsWatched;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWatched);
            _tmpIsWatched = _tmp != 0;
            final Long _tmpWatchedAt;
            if (_cursor.isNull(_cursorIndexOfWatchedAt)) {
              _tmpWatchedAt = null;
            } else {
              _tmpWatchedAt = _cursor.getLong(_cursorIndexOfWatchedAt);
            }
            _result = new EpisodeProgress(_tmpWatchItemId,_tmpSeason,_tmpEpisodeNumber,_tmpIsWatched,_tmpWatchedAt);
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
  public Object getAll(final long watchItemId,
      final Continuation<? super List<EpisodeProgress>> $completion) {
    final String _sql = "SELECT * FROM episode_progress WHERE watchItemId = ? ORDER BY season ASC, episodeNumber ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, watchItemId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<EpisodeProgress>>() {
      @Override
      @NonNull
      public List<EpisodeProgress> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfWatchItemId = CursorUtil.getColumnIndexOrThrow(_cursor, "watchItemId");
          final int _cursorIndexOfSeason = CursorUtil.getColumnIndexOrThrow(_cursor, "season");
          final int _cursorIndexOfEpisodeNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeNumber");
          final int _cursorIndexOfIsWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "isWatched");
          final int _cursorIndexOfWatchedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "watchedAt");
          final List<EpisodeProgress> _result = new ArrayList<EpisodeProgress>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EpisodeProgress _item;
            final long _tmpWatchItemId;
            _tmpWatchItemId = _cursor.getLong(_cursorIndexOfWatchItemId);
            final int _tmpSeason;
            _tmpSeason = _cursor.getInt(_cursorIndexOfSeason);
            final int _tmpEpisodeNumber;
            _tmpEpisodeNumber = _cursor.getInt(_cursorIndexOfEpisodeNumber);
            final boolean _tmpIsWatched;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWatched);
            _tmpIsWatched = _tmp != 0;
            final Long _tmpWatchedAt;
            if (_cursor.isNull(_cursorIndexOfWatchedAt)) {
              _tmpWatchedAt = null;
            } else {
              _tmpWatchedAt = _cursor.getLong(_cursorIndexOfWatchedAt);
            }
            _item = new EpisodeProgress(_tmpWatchItemId,_tmpSeason,_tmpEpisodeNumber,_tmpIsWatched,_tmpWatchedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
