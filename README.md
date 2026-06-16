# Kaze — Watchlist & Media Tracker

> **Production-grade Android app** for tracking movies and TV series with full social features, episode-level tracking, automatic updates, and offline-first design.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Project Structure](#project-structure)
4. [Data Layer](#data-layer)
   - [Local Database (Room)](#local-database-room)
   - [Remote Database (Supabase)](#remote-database-supabase)
   - [External APIs](#external-apis)
5. [Screens & UI](#screens--ui)
   - [Splash Screen](#splash-screen)
   - [Onboarding](#onboarding)
   - [Home Screen](#home-screen)
   - [Detail Screen](#detail-screen)
   - [Search Screen](#search-screen)
   - [Add Item](#add-item)
   - [Stats Screen](#stats-screen)
   - [Discover Screen](#discover-screen)
   - [Activity Feed](#activity-feed)
   - [Friends Screen](#friends-screen)
   - [User Profile Screen](#user-profile-screen)
   - [My Profile Screen](#my-profile-screen)
   - [Settings Screen](#settings-screen)
6. [Core Features In Detail](#core-features-in-detail)
   - [Episode Tracking System](#episode-tracking-system)
   - [Rating System](#rating-system)
   - [WhatToWatch Suggestion Engine](#whattowatch-suggestion-engine)
   - [Social Graph (Follow/Unfollow)](#social-graph-followunfollow)
   - [Offline Queue & Sync Worker](#offline-queue--sync-worker)
   - [Cloud Backup & Restore](#cloud-backup--restore)
   - [Local Backup (JSON)](#local-backup-json)
7. [Navigation](#navigation)
8. [Design System & Theme](#design-system--theme)
9. [UI Components](#ui-components)
10. [Dependency Injection (Manual DI)](#dependency-injection-manual-di)
11. [Background Processing](#background-processing)
12. [OTA Auto-Updater](#ota-auto-updater)
13. [CI/CD Pipeline](#cicd-pipeline)
14. [Permissions](#permissions)
15. [Build Configuration](#build-configuration)
16. [Database Migrations History](#database-migrations-history)
17. [API Keys & Secrets](#api-keys--secrets)
18. [AI Developer Rules](#ai-developer-rules)

---

## Overview

Kaze (formerly WatchLater) is an **offline-first, social Android application** that lets users:

- Track movies and TV series in a personal watchlist
- Mark individual TV episodes as watched with auto-advancement
- Discover what to watch next via a smart suggestion engine
- Follow friends and see their watchlists in a social feed
- View rich statistics about their watch history
- Auto-update via an in-app updater connected to GitHub Releases

**Min SDK:** 24 (Android 7.0)  
**Target SDK:** 35  
**Current Version:** 2.8.6 (versionCode 72)  
**Language:** Kotlin  
**UI Toolkit:** Jetpack Compose (Material 3)

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     UI Layer                            │
│  Jetpack Compose screens + ViewModels (StateFlow/MVVM) │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                  Repository Layer                       │
│  WatchItemRepository  SeriesRepository  UserRepository  │
│  ActivityRepository   DiscoverCacheRepository           │
└──────────┬───────────────────────────────┬──────────────┘
           │                               │
┌──────────▼──────────┐       ┌────────────▼─────────────┐
│   Local (Room DB)   │       │   Remote (Supabase PG)   │
│  watch_items        │       │  public_watchlist         │
│  episode_progress   │       │  public_episode_progress  │
│  series_cache       │       │  users                    │
│  season_episodes    │       │  follows                  │
│  pending_actions    │       │  activity_feed            │
│  cast_cache         │       └──────────────────────────-┘
│  what_to_watch_*    │
└─────────────────────┘
           │
┌──────────▼──────────┐
│   External APIs     │
│  OMDB (metadata)    │
│  Trakt (trailers)   │
│  GitHub Gist (OTA)  │
└─────────────────────┘
```

**Pattern:** MVVM (Model-View-ViewModel)  
**State management:** Kotlin `StateFlow` + `collectAsState()`  
**Async:** Kotlin Coroutines + Flow  
**DI:** Manual via `AppContainer` (singleton)

---

## Project Structure

```
com.kaze/
├── MainActivity.kt              — Entry point, edge-to-edge, deep link handling
├── WatchLaterApp.kt             — Application class, initializes AppContainer
│
├── model/
│   ├── WatchItem.kt             — Core domain entity (Room @Entity)
│   └── SortFilter.kt            — SortOption, FilterOption enums
│
├── data/
│   ├── local/
│   │   ├── WatchLaterDatabase.kt    — Room DB, all DAOs, 12 migrations
│   │   ├── WatchItemDao.kt          — CRUD + search + favorites queries
│   │   ├── SeriesEntities.kt        — SeriesCache, SeasonEpisode, EpisodeProgress
│   │   ├── SeriesDaos.kt            — DAOs for series/episode entities
│   │   ├── CastCacheEntity.kt       — Cast member cache entity + DAO
│   │   ├── WhatToWatchDao.kt        — Raw SQL random suggestion query
│   │   ├── PendingAction.kt         — Offline queue action entity
│   │   ├── PendingActionDao.kt      — Queue insert/delete/list
│   │   └── Converters.kt            — Room type converters
│   │
│   ├── remote/
│   │   ├── OmdbApi.kt               — Retrofit interface for OMDB
│   │   ├── OmdbModels.kt            — OmdbResponse, OmdbEpisodeResponse, etc.
│   │   ├── OmdbRepository.kt        — OMDB fetch + rating clamp (max 5)
│   │   ├── TraktApi.kt              — Retrofit interface for Trakt v2
│   │   ├── TraktModels.kt           — TraktMovieResult, TraktShowResult, etc.
│   │   ├── TraktRepository.kt       — Trailer URL resolution via Trakt
│   │   └── SupabaseClient.kt        — Supabase client singleton
│   │
│   └── repository/
│       ├── WatchItemRepository.kt       — Local CRUD, search, snapshot ops
│       ├── SeriesRepository.kt          — Episode logic + OMDB cache layer
│       ├── UserRepository.kt            — Auth, social graph, watchlist sync
│       ├── ActivityRepository.kt        — Activity feed read/write
│       └── DiscoverCacheRepository.kt   — Discover section remote data
│
├── di/
│   └── AppContainer.kt          — Manual DI; creates all repositories + APIs
│
├── ui/
│   ├── Navigation.kt            — NavHost, routes, deep link routing
│   ├── theme/
│   │   ├── Color.kt             — All custom color tokens
│   │   ├── Theme.kt             — Material 3 dark color scheme
│   │   ├── Typography.kt        — AppTypography
│   │   └── Shape.kt             — Shapes config
│   │
│   ├── components/
│   │   ├── WatchItemCard.kt         — Reusable item card
│   │   ├── WhatToWatchBottomSheet.kt — Suggestion engine UI
│   │   ├── DiscoverFilterBottomSheet.kt — Discover filters
│   │   ├── SortFilterSheet.kt       — Sort/filter bottom sheet
│   │   ├── Components.kt            — Shared small composables
│   │   ├── EmptyState.kt            — Empty state composable
│   │   ├── NetworkStatusBanner.kt   — Offline/online banner
│   │   └── UserAvatar.kt            — Avatar with initials
│   │
│   └── screens/
│       ├── splash/SplashScreen.kt
│       ├── onboarding/SetUsernameScreen.kt
│       ├── home/HomeScreen.kt + HomeViewModel.kt + WhatToWatchViewModel.kt
│       ├── detail/DetailScreen.kt + DetailViewModel.kt
│       ├── search/SearchScreen.kt + SearchViewModel.kt
│       ├── add/AddItemSheet.kt + AddItemViewModel.kt
│       ├── stats/StatsScreen.kt + StatsViewModel.kt
│       ├── discover/DiscoverScreen.kt
│       ├── feed/FeedScreen.kt
│       ├── friends/FriendsScreen.kt + UserProfileScreen.kt
│       ├── profile/MyProfileScreen.kt
│       └── settings/SettingsScreen.kt
│
├── updater/
│   └── UpdateManager.kt         — OTA update check + download + install
│
├── worker/
│   └── SyncWorker.kt            — WorkManager: drains pending_actions queue
│
├── service/
│   └── KazeMessagingService.kt  — Firebase Cloud Messaging (push notifications)
│
└── utils/
    ├── BackupManager.kt         — Local JSON + cloud backup/restore
    ├── FeatureFlags.kt          — Runtime feature toggles
    ├── HapticUtils.kt           — Haptic feedback helpers
    ├── NetworkMonitor.kt        — Connectivity state flow
    └── UserPreferences.kt       — SharedPreferences wrappers
```

---

## Data Layer

### Local Database (Room)

**Database name:** `watch_later_db`  
**Current schema version:** 12  
**Class:** `WatchLaterDatabase`

#### Tables

| Table | Purpose |
|---|---|
| `watch_items` | Core watchlist — every movie/series added by the user |
| `episode_progress` | Per-item per-episode watched status, with timestamp |
| `series_cache` | OMDB series metadata (total seasons, isFinished) — 30 day cache |
| `season_episodes` | OMDB episode list per season — cached indefinitely once fetched |
| `cast_cache` | Trakt cast data per imdbId — cached on fetch |
| `pending_actions` | Offline queue for Supabase operations that failed due to no network |

#### `watch_items` Schema

```kotlin
@Entity(tableName = "watch_items", indices = [isWatched, type, dateAdded, imdbId])
data class WatchItem(
    id: Long,             // auto-generated PK
    title: String,
    year: Int,
    type: MediaType,      // MOVIE or SERIES
    isWatched: Boolean,
    rating: Float,        // 0–5 scale, integer (rounded)
    season: Int?,         // current watching position
    episode: Int?,        // current watching position
    notes: String,
    posterUrl: String?,
    genres: String,       // comma-separated
    imdbId: String,       // OMDB ID — used for all API lookups
    dateAdded: Long,
    lastUpdated: Long,
    plot: String,
    trailerUrl: String,   // YouTube URL from Trakt
    isFavorite: Boolean   // heart-marked by user
)
```

#### `episode_progress` Schema

```kotlin
@Entity(primaryKeys = [watchItemId, season, episodeNumber],
        foreignKeys = [FK(WatchItem.id → CASCADE DELETE)])
data class EpisodeProgress(
    watchItemId: Long,
    season: Int,
    episodeNumber: Int,
    isWatched: Boolean,
    watchedAt: Long?
)
```

#### `series_cache` Schema

```kotlin
@Entity(tableName = "series_cache")
data class SeriesCache(
    imdbId: String,       // PK
    title: String,
    totalSeasons: Int,
    isFinished: Boolean,
    cachedAt: Long        // 30-day TTL
)
```

#### `season_episodes` Schema

```kotlin
@Entity(primaryKeys = [imdbId, season, episodeNumber])
data class SeasonEpisode(
    imdbId: String,
    season: Int,
    episodeNumber: Int,
    title: String,
    released: String,
    imdbRating: String,
    episodeImdbId: String,  // for per-episode plot fetching
    plot: String,
    cachedAt: Long
)
```

#### `cast_cache` Schema

```kotlin
@Entity(primaryKeys = [imdbId, actorName])
data class CastCacheEntity(
    imdbId: String,
    actorName: String,
    characterName: String,
    imageUrl: String?,
    cachedAt: Long
)
```

#### `pending_actions` Schema (Offline Queue)

```kotlin
@Entity(tableName = "pending_actions")
data class PendingAction(
    id: Long,
    actionType: ActionType,  // FOLLOW, UNFOLLOW, SYNC_WATCHLIST, DELETE_WATCHLIST,
                             // UPDATE_PROFILE, SYNC_EPISODE_PROGRESS, POST_ACTIVITY
    userId: String,
    targetId: String,
    payload: String,         // JSON-serialized data
    createdAt: Long
)
```

---

### Remote Database (Supabase)

**Project:** Supabase PostgreSQL  
**Client library:** `io.github.jan.supabase:postgrest-kt`

#### Tables

| Table | Purpose |
|---|---|
| `users` | User accounts: `id` (UUID), `username`, `fav_movie`, `fav_series`, `fav_genre` |
| `public_watchlist` | Mirror of local watchlist, synced on every add/update/delete |
| `public_episode_progress` | Mirror of episode progress per imdbId |
| `follows` | Follow graph: `follower_id → following_id` |
| `activity_feed` | Social events: added item, marked watched, followed someone |

#### Sync Strategy

- **Write:** Local-first. Every mutation hits Room first, then Supabase. On network failure, the action is queued to `pending_actions` and `SyncWorker` is enqueued.
- **Conflict resolution:** `last_updated` timestamp wins. Items with older timestamps are skipped during upsert.
- **Upsert conflict key:** `(user_id, title, year, type)` for watchlist; `(user_id, imdb_id, season, episode_number)` for episode progress.
- **Cloud restore:** Fetches remote items → merges with local (newer wins) → restores episode progress.

---

### External APIs

#### OMDB API

- **Base URL:** `https://www.omdbapi.com/`
- **Used for:** Poster URLs, genres, release year, total seasons for series, episode lists per season, per-episode metadata
- **Caching:** Season data cached for 30 days in `series_cache`. Episode lists stored permanently in `season_episodes`. Rating responses are clamped: `round(rating / 2)` to convert 10-point to 5-point scale.
- **Key injection:** `buildConfigField("OMDB_API_KEY")` from `gradle.properties`

#### Trakt API v2

- **Base URL:** `https://api.trakt.tv/`
- **Used for:** Trailer YouTube URLs and series/movie plot fetches
- **Authentication:** Client-ID header (`TRAKT_CLIENT_ID` BuildConfig field)
- **Flow:** `TraktRepository.getTrailerUrl(imdbId)` → fetches Trakt movie/show → extracts YouTube key → stores as `https://youtube.com/embed/{key}` in `WatchItem.trailerUrl`

#### GitHub Gist (OTA)

- **URL:** `https://gist.githubusercontent.com/KenzBilal/7c19255da1430800f0030ba3c6e99765/raw/update.json`
- **Used by:** `UpdateManager` on app launch to compare `versionCode`

---

## Screens & UI

### Splash Screen

- Uses `androidx.core.splashscreen` API
- Shows `ic_splash_logo` drawable with `windowSplashScreenAnimatedIcon`
- Parent theme: `Theme.SplashScreen`, post-splash: `Theme.WatchLater`
- Navigates to Onboarding if no local `user_id` in SharedPreferences, otherwise to Home

---

### Onboarding

**File:** `SetUsernameScreen.kt`

**Flow:**
1. User enters a username (4–12 letters only, letters-only regex enforced client-side)
2. `SetUsernameViewModel.submit()` calls `userRepository.createUser(username)`
3. On success: syncs entire local watchlist to cloud, attempts cloud restore, navigates to Home
4. On failure: shows inline `apiError` text

**UI:** Dark background `#0D0D0D`, "Wotchy" brand label, large heading, `OutlinedTextField` with live character counter (suffix "X/12"), animated error text with `AnimatedVisibility`, Continue button.

---

### Home Screen

**Files:** `HomeScreen.kt`, `HomeViewModel.kt`

**State:** `HomeUiState`
- `items`: list of all `WatchItem`s filtered + sorted
- `tab`: 0 = To Watch, 1 = Watched
- `sortOption`, `filterOption`
- `updateAvailable`, `updateVersionName` — from OTA check
- `pendingUpdate` — download/install state

**Logic (HomeViewModel):**

| Function | Description |
|---|---|
| `setTab(Int)` | Switches between To Watch / Watched tabs |
| `updateSort(SortOption)` | Changes sort: DATE_ADDED, TITLE, YEAR, RATING |
| `updateFilter(FilterOption)` | Filters by: ALL, MOVIES, SERIES |
| `toggleWatched(item)` | Toggles `isWatched`, updates `lastUpdated`, syncs to cloud |
| `toggleFavorite(item)` | Toggles `isFavorite` flag on watched items |
| `saveRating(item, rating)` | Saves rating (0–5), syncs to cloud |
| `deleteItem(item)` | Deletes locally + removes from Supabase watchlist |
| `downloadUpdate()` | Triggers `UpdateManager.downloadUpdate()` |
| `installUpdate()` | Triggers `UpdateManager.installApk()` |

**Tabs:**
- **To Watch:** Unwatched items, default sorted by date added descending
- **Watched:** Watched items with optional star rating

**Extras:**
- `WhatToWatch` FAB → opens `WhatToWatchBottomSheet`
- Sort/filter via `SortFilterSheet` bottom sheet
- `NetworkStatusBanner` shown at top when offline
- `UpdateDialog` shown when `updateAvailable == true`

---

### Detail Screen

**Files:** `DetailScreen.kt`, `DetailViewModel.kt`

This is the most complex screen in the app. It shows full metadata for a single `WatchItem` and handles all episode tracking for series.

**State:** `DetailUiState`
- `item`: the current `WatchItem`
- `isWatched`, `rating`, `notes`
- `totalSeasons`, `selectedSeason`, `seasonEpisodes: List<EpisodeUiItem>`
- `isLoadingEpisodes`, `isPreview` (viewing another user's item — read-only)
- `cast: List<CastMember>`
- `showRatingPrompt`, `showDeleteDialog`, `showMarkAllSeriesDialog`
- `isMarkingAllWatched`, `markAllProgress`
- `toastMessage`
- `episodePlotDialog: EpisodeUiItem?`

**Logic (DetailViewModel):**

| Function | Description |
|---|---|
| `toggleWatched()` | Toggle item watched status. If marking watched → shows rating prompt |
| `selectSeason(Int)` | Changes active season, triggers episode load |
| `toggleEpisode(season, ep)` | Mark/unmark single episode. Auto-advances position |
| `markSeasonWatched()` | Marks all episodes in current season as watched, validates previous seasons done |
| `unmarkSeasonWatched()` | Unmarks all episodes in current season |
| `markAllPreviousWatched(season, ep)` | Marks all episodes before this one (including earlier seasons) as watched |
| `unmarkAllPreviousWatched(season, ep)` | Unmarks all episodes before this one (including earlier seasons) |
| `markAllSeriesWatched()` | Marks every episode in every season, updates `isWatched` on item |
| `loadCast()` | Fetches cast from Trakt via `TraktRepository`, caches in `cast_cache` |
| `fetchEpisodePlot(episode)` | Fetches single episode plot from OMDB by `episodeImdbId`, caches in `season_episodes` |
| `dismissEpisodePlotDialog()` | Closes episode plot dialog |
| `saveItem()` | Persists rating + notes changes |
| `deleteItem()` | Deletes item from local DB + Supabase |

**Episode auto-advance logic:**
After any episode toggle, `autoAdvancePosition()` scans all seasons for the first unwatched episode and updates `item.season` + `item.episode`. Syncs to cloud.

**Sequential enforcement:**
`SeriesRepository.validateEpisodeMarkable()` checks that all previous seasons are fully watched before marking a later season. Returns `EpisodeValidationResult.Blocked` if not.

**Series progress bar:** Displayed per-item in `WatchItemCard` using an asymptotic formula for visual "fake" progress before actual episode data loads.

**Sections in Detail Screen (top to bottom):**
1. Poster (full-width hero, async loaded by Coil)
2. Title, year, type badge, genres
3. Plot text (expandable)
4. Trailer (YouTube embed in `AndroidView`/`WebView`, lazy-loaded on tap)
5. Cast row (horizontal scrollable)
6. Episode section (series only, when not fully watched)
   - Season selector chips (LazyRow)
   - Season progress bar + episode count
   - "Mark season watched" / "Unmark season" button
   - Episode rows with individual toggle, "mark previous" button
7. Rating selector (shown when watched)
8. Notes field
9. Delete button

---

### Search Screen

**Files:** `SearchScreen.kt`, `SearchViewModel.kt`

**Logic:**
- `SearchViewModel` maintains a `_query: MutableStateFlow<String>`
- `results` is derived via `.debounce(200ms).distinctUntilChanged().flatMapLatest { repo.searchItems(it) }`
- Local full-text search across `title` using Room `LIKE` query
- Three UI states: empty query → hint, no results → empty state, results → `LazyColumn` of `WatchItemCard`

---

### Add Item

**Files:** `AddItemSheet.kt`, `AddItemViewModel.kt`

Presented as a **bottom sheet modal** over any screen.

**Flow:**
1. User types title → OMDB search triggered after 700ms debounce
2. OMDB suggestions shown as `OmdbSuggestionRow` (poster + title + year + type)
3. User taps a suggestion → form auto-fills title, year, type, genres, imdbId, posterUrl
4. User taps Save → `AddItemViewModel.saveItem()`
   - Checks for duplicates (same imdbId or same title+year+type)
   - Inserts to Room
   - If user logged in: pushes to Supabase + posts activity feed entry
   - Fetches full genres async from OMDB in background

**Fields:** Title (required), Year (required, 4-digit), Type (Movie/Series chips)

---

### Stats Screen

**Files:** `StatsScreen.kt`, `StatsViewModel.kt`

Displays rich watch statistics derived from local Room data.

**Stats shown:**
| Stat | Description |
|---|---|
| Total items | All items in watchlist |
| Watched | Count of `isWatched == true` |
| Movies / Series | Breakdown by type |
| Watch percentage | Animated progress bar: `watched / total` |
| Estimated watch time | Based on avg durations (movies: 110 min, series ep: 45 min) |
| Top genres | Horizontal segmented bar + legend, derived from `genreList` |
| In-progress series | Series where `season != null && !isWatched` |
| Recently added | Last 5 items added |

---

### Discover Screen

**File:** `DiscoverScreen.kt`

Shows a global feed of public watchlist items from all Supabase users.

**Sections:**
- **Trending** — top-rated public items globally (sorted by rating)
- **Recently Added** — newest items across all users

**Cards:** Poster + title + year + rating (out of 5). Tapping opens `DetailScreen` in preview mode (read-only).

**Filter:** `DiscoverFilterBottomSheet` allows filtering by type (Movie/Series) and genre.

---

### Activity Feed

**File:** `FeedScreen.kt`

Shows social activity from users the current user follows.

**Logic:**
1. `FeedViewModel` loads the followed user IDs
2. Fetches `activity_feed` entries from Supabase for those IDs
3. Supports pagination via `loadMore()`
4. Pull-to-refresh

**Event types displayed:**
- "added X to watchlist"
- "marked X as watched"
- "followed @username"

**Card:** Poster thumbnail (or icon placeholder), username, action string, item title, timestamp.

---

### Friends Screen

**File:** `FriendsScreen.kt`

- Search bar to find users by username (calls `userRepository.searchUsers()`)
- Results list with username + follow status
- Tap → opens `UserProfileScreen`

---

### User Profile Screen

**File:** `UserProfileScreen.kt`

Public profile of another user.

**Header:**
- Avatar (initials-based `UserAvatar`)
- Username
- Followers / Following counts (tappable → `UserListDialog`)
- Fav Movie, Fav Series, Genre (if set)
- Follow / Unfollow button (optimistic update)

**Content:** Two tabs — **Watched** / **To Watch**  
- Staggered 2-column Pinterest-style grid via `LazyVerticalStaggeredGrid`
- Items already in own watchlist shown at 50% opacity and non-clickable
- Tapping an item the user doesn't own → opens `DetailScreen` in preview mode

**`UserListDialog`:** Modal showing list of followers or following with avatar + username.

---

### My Profile Screen

**File:** `MyProfileScreen.kt`

Current user's own profile.

**Header:**
- Avatar + username
- Movies watched · Series watched subtitle

**Stats bar:** Followers | Following | Watched (total)

**Favorites section:**
- Staggered grid of items where `isFavorite == true`
- Heart icon overlay on each card
- Empty state if no favorites

**Top bar actions:**
- Settings icon (navigates to SettingsScreen)
- Share icon → generates share text with profile deep-link URL + APK download link, opens Android share sheet

**Deep link URL format:** `https://kenzbilal.github.io/Kaze/u/{username}`

---

### Settings Screen

**File:** `SettingsScreen.kt`

**Sections:**

| Setting | Description |
|---|---|
| Profile (Fav Movie/Series/Genre) | Editable fields, saved via `userRepository.updateProfile()` |
| Local Backup | Export all items to JSON file |
| Local Restore | Import from JSON file, merges with current data |
| Cloud Backup | Upload all local items to Supabase for the logged-in user |
| Cloud Restore | Download and merge items from Supabase |
| Sign Out | Clears local `user_id` and `username` from SharedPreferences |

---

## Core Features In Detail

### Episode Tracking System

Series episodes are tracked at three levels:

1. **Series level** (`WatchItem`): `season` and `episode` fields store the current watching position (next unwatched episode).
2. **Season level** (`SeriesCache`): total seasons count from OMDB.
3. **Episode level** (`EpisodeProgress`): one row per episode per item; `isWatched` + `watchedAt`.

**Season caching:** `SeriesRepository` checks if the season is already in `season_episodes` with a `cachedAt` within 30 days. If not, fetches from OMDB and stores. This conserves OMDB API quota.

**Auto-advance:** After marking an episode watched, `autoAdvancePosition()` scans all seasons in order for the first unwatched episode and updates `WatchItem.season` + `WatchItem.episode`.

**Sequential validation:** `validateEpisodeMarkable()` in `SeriesRepository` prevents marking a season as watched if earlier seasons have unwatched episodes. Returns `EpisodeValidationResult.Blocked` with an error message.

**Bulk operations:**
- `markSeasonWatched()` — marks every episode in the current season
- `unmarkSeasonWatched()` — unmarks every episode in the current season
- `markAllPreviousWatched(season, episode)` — marks all episodes in all previous seasons + all episodes before the target episode in the same season
- `unmarkAllPreviousWatched(season, episode)` — opposite of above
- `markAllSeriesWatched()` — marks all episodes in all seasons, marks item as watched

---

### Rating System

- **Scale:** 0–5, integer (rounded)
- **Display:** Star icons (1–5), gold filled = rated
- **OMDB ratings:** Fetched as 0–10 strings, divided by 2 and rounded: `round(omdbRating.toFloat() / 2)`
- **Legacy migration:** DB migration v11→v12 runs `UPDATE watch_items SET rating = ROUND(rating / 2.0) WHERE rating > 5` to fix any stored 10-point ratings

---

### WhatToWatch Suggestion Engine

**Files:** `WhatToWatchViewModel.kt`, `WhatToWatchBottomSheet.kt`

A smart random picker that finds the next thing to watch based on user preferences.

**Filters:**
- **Type:** Movie, Series, or Both
- **Genre:** Any genre present in the user's watchlist (auto-populated)
- **Series Length:** Short (1–2 seasons), Medium (3–4 seasons), Long (5+ seasons)

**Query logic:**
- Builds a raw SQL query against `watch_items` + `series_cache` JOIN
- Applies `WHERE isWatched = 0` (unwatched only)
- Applies type, genre (`LIKE '%genre%'`), and series length filters
- `ORDER BY RANDOM() LIMIT 1`

**UI Flow:**
1. Bottom sheet opens with filter chips (FlowRow for genres)
2. "Suggest Something" → shows result card (WatchItemCard)
3. "Re-roll" → picks again
4. "Change Filters" → goes back to filter view

---

### Social Graph (Follow/Unfollow)

**Tables:** `follows` (Supabase): `follower_id`, `following_id`

**Operations:**
| Function | Behavior |
|---|---|
| `followUser(followerId, followingId)` | Inserts row to Supabase. On failure → queues to `pending_actions` |
| `unfollowUser(followerId, followingId)` | Deletes row from Supabase. On failure → queues |
| `getFollowersCount(userId)` | COUNT from `follows WHERE following_id = userId` |
| `getFollowingCount(userId)` | COUNT from `follows WHERE follower_id = userId` |
| `isFollowing(f, t)` | SELECT EXISTS check |
| `getFollowedIds(userId)` | All following_id set for this user |

**Optimistic UI:** `UserProfileViewModel.toggleFollow()` immediately updates `isFollowing` and `followersCount` in state, then makes the network call. Rolls back on exception.

---

### Offline Queue & Sync Worker

**File:** `SyncWorker.kt` (extends `CoroutineWorker`)

When any Supabase write fails (network unavailable), the failing action is written to `pending_actions` in Room. `SyncWorker.enqueue(context)` is then called which schedules a one-time `WorkManager` task with `NETWORK_CONNECTED` constraint.

**When the device comes back online**, `SyncWorker` runs and drains the queue:
1. Reads all pending actions from `PendingActionDao`
2. For each action, performs the appropriate Supabase operation:
   - `FOLLOW` → `followUser(..., fromSyncWorker = true)`
   - `UNFOLLOW` → `unfollowUser(..., fromSyncWorker = true)`
   - `SYNC_WATCHLIST` → `syncWatchlist(...)`
   - `DELETE_WATCHLIST` → `deleteFromWatchlist(...)`
   - `UPDATE_PROFILE` → `updateProfile(...)`
   - `SYNC_EPISODE_PROGRESS` → `syncEpisodeProgress(...)`
   - `POST_ACTIVITY` → `postActivityFromPayload(...)`
3. On success: deletes the action from the queue
4. On failure: leaves in queue for next run

---

### Cloud Backup & Restore

**File:** `BackupManager.kt`

#### Upload (`uploadToCloud`)
- Fetches all local `WatchItem`s via `repository.getAllItemsSnapshot()`
- Calls `userRepository.syncWatchlist(userId, items)` → upserts to `public_watchlist`
- Also syncs all `EpisodeProgress` rows to `public_episode_progress`

#### Restore (`restoreFromCloud`)
- Downloads all items from `public_watchlist WHERE user_id = userId`
- Merges with local: new items inserted, existing items updated only if remote `last_updated` is newer
- Downloads episode progress from `public_episode_progress`
- Maps progress to local item IDs via imdbId, upserts to `episode_progress`

---

### Local Backup (JSON)

**File:** `BackupManager.kt`

#### Export
- Serializes all `WatchItem`s + `EpisodeProgress` rows to a custom `WatchLaterBackup` JSON structure
- Writes to user-selected file via `ActivityResultLauncher` (Android Storage Access Framework)

#### Import
- Parses JSON, validates version/items
- Calls `repository.restoreItems(watchItems)` — atomic replace (clears old, inserts new)
- Remaps episode progress IDs to new Room-generated IDs via imdbId or fallback key

---

## Navigation

**File:** `Navigation.kt`  
**Library:** `androidx.navigation:navigation-compose`

### Routes

| Route | Description |
|---|---|
| `home` | Home screen (To Watch / Watched tabs) |
| `detail/{itemId}` | Full detail for a local item |
| `detail_preview/{imdbId}` | Read-only preview via discover (fetches from Supabase) |
| `search` | Local search screen |
| `add` | Add item bottom sheet |
| `stats` | Statistics screen |
| `discover` | Global discover feed |
| `feed` | Social activity feed |
| `friends` | Friend search screen |
| `user_profile/{userId}` | Public profile of a user |
| `profile` | Own profile screen |
| `settings` | Settings screen |
| `onboarding` | Username setup (first launch) |

### Deep Links

Deep links of the form `https://kenzbilal.github.io/Kaze/u/{username}` are handled by `MainActivity`. The username is resolved via `userRepository.searchUsers(username)` → navigates to `user_profile/{userId}`.

---

## Design System & Theme

**File:** `Color.kt`, `Theme.kt`, `Typography.kt`, `Shape.kt`

### Color Tokens

| Token | Hex | Usage |
|---|---|---|
| `Background` | `#0D0D0D` | App background |
| `SurfaceContainer` | `#141414` | Cards, dialogs |
| `SurfaceElevated` | `#1A1A1A` | Elevated surfaces |
| `SurfaceHighlight` | `#2A2A2A` | Borders, dividers |
| `SurfaceBorder` | `#333333` | Outline borders |
| `TextPrimary` | `#FFFFFF` | Primary text |
| `TextSecondary` | `#A0A0A0` | Secondary text |
| `TextTertiary` | `#606060` | Hints, metadata |
| `AccentBlue` | `#4A90E2` | Genre chips, accent actions |
| `WatchedGreen` | `#4CAF50` | Watched checkmark |
| `FavoriteRed` | `#E53935` | Heart icon |

**Theme:** Pure dark mode only. Material 3 `darkColorScheme` built on top of the custom tokens. No light theme.

**Typography:** System default (Roboto) with custom `letterSpacing` tweaks.

---

## UI Components

### `WatchItemCard`

Reusable card for displaying a `WatchItem` in lists.

**Layout:**
- Row: 60dp-wide poster (Coil `AsyncImage`) + content column
- Content: Checked/unchecked icon, title, year + type badge + genres (up to 3, AccentBlue chips)
- Series: shows `S{season}E{episode}` progress chip
- Rating: star display if `rating > 0`
- Favorite heart: shown only when `isWatched == true` and `onToggleFavorite` is provided
- Series progress bar: asymptotic formula based on `season/totalSeasons`

### `UserAvatar`

Circular avatar showing the first letter of the username on a tinted background (color derived from username hash).

### `NetworkStatusBanner`

Animated banner at top of Home showing "No internet connection" when `NetworkMonitor` emits `false`. Disappears when connectivity restored.

### `SortFilterSheet`

Bottom sheet with:
- Sort options: Date Added, Title, Year, Rating
- Filter options: All, Movies, Series

### `DiscoverFilterBottomSheet`

Bottom sheet with type (Movie/Series/All) and genre filter chips.

### `EmptyState`

Centered icon + title + subtitle for zero-result states.

---

## Dependency Injection (Manual DI)

**File:** `AppContainer.kt`

All dependencies created in `WatchLaterApp.onCreate()` and accessed via `app.container.*`.

```kotlin
class AppContainer(context: Context) {
    val database: WatchLaterDatabase
    val omdbApi: OmdbApi                      // Retrofit instance
    val traktApi: TraktApi                    // Retrofit instance
    val omdbRepository: OmdbRepository
    val traktRepository: TraktRepository
    val repository: WatchItemRepository       // local CRUD
    val seriesRepository: SeriesRepository    // episodes + OMDB cache
    val userRepository: UserRepository        // social + auth
    val activityRepository: ActivityRepository
    val discoverCacheRepository: DiscoverCacheRepository
    val backupManager: BackupManager
    val updateManager: UpdateManager
    val networkMonitor: NetworkMonitor
}
```

Each ViewModel has an inner `Factory` class that accesses `app.container` via `context.applicationContext`.

---

## Background Processing

### WorkManager: `SyncWorker`

- **Class:** `SyncWorker` (extends `CoroutineWorker`)
- **Trigger:** `SyncWorker.enqueue(context)` — called whenever a Supabase write fails
- **Constraint:** `NetworkType.CONNECTED`
- **Behavior:** Runs once, drains entire `pending_actions` queue. Re-schedules itself if any actions remain or fail again.

### Firebase Cloud Messaging: `KazeMessagingService`

- **Class:** `KazeMessagingService` (extends `FirebaseMessagingService`)
- **Used for:** Push notifications (e.g., new follower, update available)
- **Permissions:** `POST_NOTIFICATIONS` (Android 13+)

---

## OTA Auto-Updater

**File:** `UpdateManager.kt`

### How It Works

1. **On every app launch**, `HomeViewModel.init` calls `updateManager.checkForUpdate()`
2. `UpdateManager` fetches the Gist JSON: `{ "versionCode": N, "versionName": "X.Y.Z", "apkUrl": "https://..." }`
3. If `gist.versionCode > BuildConfig.VERSION_CODE` → sets `updateAvailable = true` in HomeUiState
4. User sees `UpdateDialog` with version name
5. User taps "Download" → `UpdateManager.downloadUpdate()` uses Android `DownloadManager` to fetch APK to Downloads folder
6. When download completes → `UpdateManager.installApk()` uses `FileProvider` to trigger native package installer

### Gist JSON Format

```json
{
  "versionCode": 72,
  "versionName": "2.8.6",
  "apkUrl": "https://github.com/KenzBilal/Kaze/releases/download/v2.8.6/app-release.apk"
}
```

---

## CI/CD Pipeline

**File:** `.github/workflows/release.yml`  
**Trigger:** Push of any tag matching `v*` (e.g., `v2.8.6`)

### Steps

1. **Checkout** code at the tag ref
2. **Setup JDK 17**
3. **Grant execute permission** to `gradlew`
4. **Decode Keystore** from base64 GitHub Secret → writes to temp file
5. **Inject API Keys** — sets environment variables for OMDB key, Supabase URL/key, Trakt ID
6. **Build Release APK** — `./gradlew assembleRelease`
7. **Get Version** — reads `versionName` from `build.gradle.kts`
8. **Create GitHub Release** — uploads `app-release.apk` as release asset
9. **Update Gist** — authenticates with `GIST_TOKEN` secret → patches the update JSON with new `versionCode`, `versionName`, and direct APK URL

### Required GitHub Secrets

| Secret | Purpose |
|---|---|
| `KEYSTORE_BASE64` | Release keystore (base64 encoded) |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |
| `GIST_TOKEN` | GitHub PAT with gist write scope |
| `OMDB_API_KEY` | OMDB API key |

### To Release a New Version

```bash
# 1. Bump versionCode and versionName in app/build.gradle.kts
# 2. Commit
git add -A && git commit -m "chore: bump version to X.Y.Z"
git push origin master

# 3. Tag and push
git tag vX.Y.Z
git push origin vX.Y.Z

# Automation handles the rest: build → GitHub Release → APK → Gist update
```

---

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | All network operations (OMDB, Trakt, Supabase, OTA) |
| `REQUEST_INSTALL_PACKAGES` | OTA installer — triggers native APK install |
| `POST_NOTIFICATIONS` | Firebase push notifications (Android 13+) |
| `VIBRATE` | Haptic feedback on interactions |

---

## Build Configuration

**File:** `app/build.gradle.kts`

```
applicationId       = "com.kaze"
minSdk              = 24
targetSdk           = 35
compileSdk          = 35
jvmTarget           = "17"
isMinifyEnabled     = true   (release only)
isShrinkResources   = true   (release only)
```

**Key BuildConfig fields:**

| Field | Source |
|---|---|
| `OMDB_API_KEY` | `gradle.properties` `omdb.api.key` |
| `UPDATE_JSON_URL` | Hard-coded Gist URL |
| `TRAKT_CLIENT_ID` | Hard-coded in build file |
| `SUPABASE_URL` | Hard-coded in build file |
| `SUPABASE_KEY` | Hard-coded in build file |

**Key dependencies:**

| Library | Version/Catalog Alias | Purpose |
|---|---|---|
| Jetpack Compose BOM | `libs.androidx.compose.bom` | All Compose libs |
| Material 3 | `libs.androidx.material3` | UI components |
| Navigation Compose | `libs.androidx.navigation.compose` | Screen routing |
| Room | `libs.androidx.room.*` | Local database |
| WorkManager | `libs.androidx.work.runtime.ktx` | Background sync |
| Retrofit2 | `libs.retrofit` | OMDB + Trakt HTTP |
| OkHttp Logging | `libs.okhttp.logging` | Network debugging |
| Coil Compose | `libs.coil.compose` | Image loading |
| Supabase PostgREST | `libs.supabase.postgrest` | Remote database |
| Ktor Android | `libs.ktor.client.android` | Supabase HTTP engine |
| kotlinx.serialization | `libs.kotlinx.serialization.json` | Supabase models |
| Kotlin Coroutines | `libs.kotlinx.coroutines.android` | Async |
| Gson | `libs.gson` | JSON serialization |
| SplashScreen | `libs.androidx.splashscreen` | Splash API |
| Firebase BOM 33.1.2 | — | Push notifications |
| firebase-messaging-ktx | — | FCM |

---

## Database Migrations History

| Migration | Change |
|---|---|
| 1 → 2 | Added `series_cache` + `season_episodes` tables |
| 2 → 3 | Added `episode_progress` table |
| 3 → 4 | Added indexes on `isWatched`, `type`, `dateAdded` |
| 4 → 5 | Added `pending_actions` (offline queue) table |
| 5 → 6 | Added `imdbId` index on `watch_items`; recreated `episode_progress` with FK + CASCADE DELETE + watchItemId index |
| 6 → 7 | Added `isFinished` column to `series_cache` |
| 7 → 8 | Added `plot` + `trailerUrl` to `watch_items`; `plot` to `season_episodes` |
| 8 → 9 | Added `episodeImdbId` to `season_episodes` |
| 9 → 10 | Added `isFavorite` to `watch_items` |
| 10 → 11 | Added `cast_cache` table with index |
| 11 → 12 | Data migration: `UPDATE watch_items SET rating = ROUND(rating / 2.0) WHERE rating > 5` (10-point → 5-point scale fix) |

---

## API Keys & Secrets

### Local Development

Create/edit `gradle.properties` in the project root:

```properties
omdb.api.key=YOUR_OMDB_KEY_HERE
```

Get a free OMDB key at [omdbapi.com/apikey.aspx](https://www.omdbapi.com/apikey.aspx).

Supabase and Trakt keys are hard-coded in `build.gradle.kts` for this project. Do not rotate them without updating the build file.

### CI/CD

All secrets are stored in GitHub repository secrets. See [CI/CD Pipeline](#cicd-pipeline) for the full list.

---

## AI Developer Rules

These rules **MUST** be followed by any AI agent working on this repository:

1. **Verify Deployments:** After every git push, you MUST monitor the GitHub Actions pipeline. If it fails, fix the build immediately using professional, standard solutions — no temporary hacks. Repeat until the build passes.

2. **Maintain Documentation:** After every feature edit or architectural change, you MUST update this `README.md` accordingly. This ensures project knowledge remains accurate regardless of which AI agent works on it.

3. **Efficient Workflows:** Do NOT use live visual browser tools to watch pipelines. Use efficient CLI tools: `curl` against the GitHub API, or `gh run list` / `gh run view`.

4. **Professional Grade:** Never treat this as a toy app. It is a real, production-ready product. All code quality, error handling, and architecture must meet professional engineering standards.

5. **Rating Scale:** All ratings are stored and displayed on a **0–5 scale, integer only** (rounded). Never introduce float ratings or 10-point scale values.

6. **Theme:** App is **dark-only**. Do not add any light mode code or dynamic theming.

7. **No Chat Features:** The global chat feature has been permanently removed. Do not re-add any chat-related code.

8. **Episode Logic:** When marking episodes as watched, always respect the sequential validation (`validateEpisodeMarkable`). Do not bypass it.

9. **Offline First:** Every Supabase write must have a fallback that queues the action to `pending_actions` and enqueues `SyncWorker`. Never assume network is available.

10. **Gradle Cleanup:** When removing features, remove their dependencies from `build.gradle.kts` and `libs.versions.toml` to keep the dependency tree clean.
