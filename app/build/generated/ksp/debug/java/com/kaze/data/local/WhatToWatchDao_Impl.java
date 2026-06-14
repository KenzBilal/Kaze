package com.kaze.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.RoomDatabase;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteQuery;
import com.kaze.model.MediaType;
import com.kaze.model.WatchItem;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WhatToWatchDao_Impl implements WhatToWatchDao {
  private final RoomDatabase __db;

  private final Converters __converters = new Converters();

  public WhatToWatchDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
  }

  @Override
  public Object getRandomSuggestion(final SupportSQLiteQuery query,
      final Continuation<? super WatchItem> $completion) {
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<WatchItem>() {
      @Override
      @Nullable
      public WatchItem call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, query, false, null);
        try {
          final WatchItem _result;
          if (_cursor.moveToFirst()) {
            _result = __entityCursorConverter_comKazeModelWatchItem(_cursor);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private WatchItem __entityCursorConverter_comKazeModelWatchItem(@NonNull final Cursor cursor) {
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
    final int _cursorIndexOfPlot = CursorUtil.getColumnIndex(cursor, "plot");
    final int _cursorIndexOfTrailerUrl = CursorUtil.getColumnIndex(cursor, "trailerUrl");
    final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndex(cursor, "isFavorite");
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
    final String _tmpPlot;
    if (_cursorIndexOfPlot == -1) {
      _tmpPlot = null;
    } else {
      _tmpPlot = cursor.getString(_cursorIndexOfPlot);
    }
    final String _tmpTrailerUrl;
    if (_cursorIndexOfTrailerUrl == -1) {
      _tmpTrailerUrl = null;
    } else {
      _tmpTrailerUrl = cursor.getString(_cursorIndexOfTrailerUrl);
    }
    final boolean _tmpIsFavorite;
    if (_cursorIndexOfIsFavorite == -1) {
      _tmpIsFavorite = false;
    } else {
      final int _tmp_2;
      _tmp_2 = cursor.getInt(_cursorIndexOfIsFavorite);
      _tmpIsFavorite = _tmp_2 != 0;
    }
    _entity = new WatchItem(_tmpId,_tmpTitle,_tmpYear,_tmpType,_tmpIsWatched,_tmpRating,_tmpSeason,_tmpEpisode,_tmpNotes,_tmpPosterUrl,_tmpGenres,_tmpImdbId,_tmpDateAdded,_tmpLastUpdated,_tmpPlot,_tmpTrailerUrl,_tmpIsFavorite);
    return _entity;
  }
}
