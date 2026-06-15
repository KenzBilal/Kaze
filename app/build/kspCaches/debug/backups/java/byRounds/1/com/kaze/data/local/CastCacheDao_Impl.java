package com.kaze.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class CastCacheDao_Impl implements CastCacheDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CastCacheEntity> __insertionAdapterOfCastCacheEntity;

  public CastCacheDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCastCacheEntity = new EntityInsertionAdapter<CastCacheEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cast_cache` (`imdbId`,`actorName`,`characterName`,`imageUrl`,`cachedAt`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CastCacheEntity entity) {
        statement.bindString(1, entity.getImdbId());
        statement.bindString(2, entity.getActorName());
        statement.bindString(3, entity.getCharacterName());
        if (entity.getImageUrl() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getImageUrl());
        }
        statement.bindLong(5, entity.getCachedAt());
      }
    };
  }

  @Override
  public Object insertAll(final List<CastCacheEntity> cast,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCastCacheEntity.insert(cast);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByImdbId(final String imdbId,
      final Continuation<? super List<CastCacheEntity>> $completion) {
    final String _sql = "SELECT * FROM cast_cache WHERE imdbId = ? ORDER BY actorName ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, imdbId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CastCacheEntity>>() {
      @Override
      @NonNull
      public List<CastCacheEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfImdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "imdbId");
          final int _cursorIndexOfActorName = CursorUtil.getColumnIndexOrThrow(_cursor, "actorName");
          final int _cursorIndexOfCharacterName = CursorUtil.getColumnIndexOrThrow(_cursor, "characterName");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final List<CastCacheEntity> _result = new ArrayList<CastCacheEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CastCacheEntity _item;
            final String _tmpImdbId;
            _tmpImdbId = _cursor.getString(_cursorIndexOfImdbId);
            final String _tmpActorName;
            _tmpActorName = _cursor.getString(_cursorIndexOfActorName);
            final String _tmpCharacterName;
            _tmpCharacterName = _cursor.getString(_cursorIndexOfCharacterName);
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            _item = new CastCacheEntity(_tmpImdbId,_tmpActorName,_tmpCharacterName,_tmpImageUrl,_tmpCachedAt);
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
  public Object countByImdbId(final String imdbId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM cast_cache WHERE imdbId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, imdbId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
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
