# Graph Report - app/src/main/java  (2026-06-18)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 1075 nodes · 1816 edges · 71 communities (66 shown, 5 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 84 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `f4a12e44`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 21|Community 21]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 23|Community 23]]
- [[_COMMUNITY_Community 24|Community 24]]
- [[_COMMUNITY_Community 25|Community 25]]
- [[_COMMUNITY_Community 26|Community 26]]
- [[_COMMUNITY_Community 27|Community 27]]
- [[_COMMUNITY_Community 28|Community 28]]
- [[_COMMUNITY_Community 29|Community 29]]
- [[_COMMUNITY_Community 30|Community 30]]
- [[_COMMUNITY_Community 31|Community 31]]
- [[_COMMUNITY_Community 32|Community 32]]
- [[_COMMUNITY_Community 33|Community 33]]
- [[_COMMUNITY_Community 34|Community 34]]
- [[_COMMUNITY_Community 35|Community 35]]
- [[_COMMUNITY_Community 36|Community 36]]
- [[_COMMUNITY_Community 37|Community 37]]
- [[_COMMUNITY_Community 38|Community 38]]
- [[_COMMUNITY_Community 39|Community 39]]
- [[_COMMUNITY_Community 40|Community 40]]
- [[_COMMUNITY_Community 41|Community 41]]
- [[_COMMUNITY_Community 42|Community 42]]
- [[_COMMUNITY_Community 43|Community 43]]
- [[_COMMUNITY_Community 44|Community 44]]
- [[_COMMUNITY_Community 45|Community 45]]
- [[_COMMUNITY_Community 46|Community 46]]
- [[_COMMUNITY_Community 47|Community 47]]
- [[_COMMUNITY_Community 48|Community 48]]
- [[_COMMUNITY_Community 49|Community 49]]
- [[_COMMUNITY_Community 50|Community 50]]
- [[_COMMUNITY_Community 51|Community 51]]
- [[_COMMUNITY_Community 52|Community 52]]
- [[_COMMUNITY_Community 53|Community 53]]
- [[_COMMUNITY_Community 54|Community 54]]
- [[_COMMUNITY_Community 55|Community 55]]
- [[_COMMUNITY_Community 56|Community 56]]
- [[_COMMUNITY_Community 57|Community 57]]
- [[_COMMUNITY_Community 58|Community 58]]
- [[_COMMUNITY_Community 59|Community 59]]
- [[_COMMUNITY_Community 60|Community 60]]
- [[_COMMUNITY_Community 61|Community 61]]
- [[_COMMUNITY_Community 62|Community 62]]
- [[_COMMUNITY_Community 63|Community 63]]
- [[_COMMUNITY_Community 64|Community 64]]
- [[_COMMUNITY_Community 66|Community 66]]
- [[_COMMUNITY_Community 67|Community 67]]

## God Nodes (most connected - your core abstractions)
1. `DetailViewModel` - 33 edges
2. `ArcRepository` - 32 edges
3. `UserRepository` - 27 edges
4. `WatchItemRepository` - 27 edges
5. `WatchItemDao` - 25 edges
6. `AdminArcEditorViewModel` - 25 edges
7. `String` - 24 edges
8. `WatchLaterNavGraph()` - 24 edges
9. `Screen` - 19 edges
10. `AdminArcEditorScreen()` - 19 edges

## Surprising Connections (you probably didn't know these)
- `WatchLaterNavGraph()` --calls--> `Composable`  [INFERRED]
  com/kaze/ui/Navigation.kt → com/kaze/ui/components/EmptyState.kt
- `AppContent()` --calls--> `WatchLaterNavGraph()`  [INFERRED]
  com/kaze/MainActivity.kt → com/kaze/ui/Navigation.kt
- `MyProfileScreen()` --calls--> `Intent`  [INFERRED]
  com/kaze/ui/screens/profile/MyProfileScreen.kt → com/kaze/MainActivity.kt
- `WatchLaterNavGraph()` --calls--> `AddItemSheet()`  [INFERRED]
  com/kaze/ui/Navigation.kt → com/kaze/ui/screens/add/AddItemSheet.kt
- `WatchLaterNavGraph()` --calls--> `AdminArcEditorScreen()`  [INFERRED]
  com/kaze/ui/Navigation.kt → com/kaze/ui/screens/arcs/admin/AdminArcEditorScreen.kt

## Import Cycles
- None detected.

## Communities (71 total, 5 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.06
Nodes (19): Int, List, String, Class, EpisodeUiItem, Float, Int, SharedFlow (+11 more)

### Community 1 - "Community 1"
Cohesion: 0.07
Nodes (39): Float, Int, Long, String, WatchLaterApp, OmdbRepository, String, Boolean (+31 more)

### Community 2 - "Community 2"
Cohesion: 0.12
Nodes (16): Collection, Boolean, com, Int, List, Set, SharedPreferences, String (+8 more)

### Community 3 - "Community 3"
Cohesion: 0.12
Nodes (16): Boolean, List, Long, Map, Pair, Result, String, Double (+8 more)

### Community 4 - "Community 4"
Cohesion: 0.12
Nodes (26): AdminAddItemSheet(), AdminArcEditorScreen(), AdminArcEditorViewModel, AdminArcItemRow(), AdminMoviePickerSheet(), AdminRangePickerSheet(), Factory, MetaEditorDialog() (+18 more)

### Community 5 - "Community 5"
Cohesion: 0.14
Nodes (11): Boolean, EpisodeProgress, Flow, Int, List, Long, MediaType, SortFilterState (+3 more)

### Community 6 - "Community 6"
Cohesion: 0.08
Nodes (21): AddItemUiState, AddItemViewModel, Factory, Boolean, Int, List, Set, String (+13 more)

### Community 7 - "Community 7"
Cohesion: 0.14
Nodes (10): androidx, Boolean, com, Flow, Int, List, Long, String (+2 more)

### Community 8 - "Community 8"
Cohesion: 0.09
Nodes (23): Modifier, String, Boolean, Class, Int, List, PublicWatchlistItem, StateFlow (+15 more)

### Community 9 - "Community 9"
Cohesion: 0.09
Nodes (24): Boolean, Class, DiscoverItem, List, Modifier, OmdbRepository, PublicWatchlistItem, StateFlow (+16 more)

### Community 10 - "Community 10"
Cohesion: 0.14
Nodes (11): EpisodeProgress, Flow, Int, List, Long, SeasonEpisode, String, EpisodeProgressDao (+3 more)

### Community 11 - "Community 11"
Cohesion: 0.13
Nodes (21): ActivityRepository, ArcItemUiState, ArcRowState, ArcDetailScreen(), ArcDetailViewModel, ArcItemRow(), Factory, Arc (+13 more)

### Community 12 - "Community 12"
Cohesion: 0.19
Nodes (14): Boolean, EpisodeProgress, Flow, Int, List, Long, Pair, SeasonEpisode (+6 more)

### Community 13 - "Community 13"
Cohesion: 0.13
Nodes (17): Class, StateFlow, String, SupabaseUser, T, WatchItem, MyProfileUiState, Factory (+9 more)

### Community 14 - "Community 14"
Cohesion: 0.16
Nodes (15): AdminAIArcSheet(), AdminArcRow(), AdminArcsScreen(), AdminArcsViewModel, CreateArcDialog(), Factory, Boolean, String (+7 more)

### Community 15 - "Community 15"
Cohesion: 0.09
Nodes (14): ArcProgressDao, CastCacheDao, Context, EpisodeProgressDao, getInstance(), migrate(), WatchLaterDatabase, PendingActionDao (+6 more)

### Community 16 - "Community 16"
Cohesion: 0.17
Nodes (16): ArcCard(), ArcsScreen(), ArcsViewModel, CreatePersonalArcDialog(), Factory, ShareInboxRow(), ArcShare, Arc (+8 more)

### Community 17 - "Community 17"
Cohesion: 0.17
Nodes (14): android, Int, String, Gson, BackupManager, BackupPayload, BackupResult, CloudEpisodeProgress (+6 more)

### Community 18 - "Community 18"
Cohesion: 0.12
Nodes (11): Float, Int, List, SharedFlow, StateFlow, WatchItem, FilterOption, HomeViewModel (+3 more)

### Community 19 - "Community 19"
Cohesion: 0.16
Nodes (12): Class, List, Set, StateFlow, String, T, WatchItem, Factory (+4 more)

### Community 20 - "Community 20"
Cohesion: 0.15
Nodes (13): Color, Boolean, Class, StateFlow, String, T, NetworkStatusBanner(), Factory (+5 more)

### Community 21 - "Community 21"
Cohesion: 0.15
Nodes (12): ActivityFeedItem, Class, List, StateFlow, String, T, Factory, FeedEventCard() (+4 more)

### Community 22 - "Community 22"
Cohesion: 0.20
Nodes (9): Boolean, Int, List, OmdbResult, OmdbSeasonResponse, String, OmdbApi, OmdbRepository (+1 more)

### Community 23 - "Community 23"
Cohesion: 0.19
Nodes (9): Int, List, String, TraktApi, TraktMovieSummary, TraktPeopleResponse, TraktShowSummary, TraktTrendingMovieResponse (+1 more)

### Community 24 - "Community 24"
Cohesion: 0.27
Nodes (14): Float, Int, MediaType, Modifier, String, ConfirmDeleteDialog(), NumberStepper(), ProgressChip() (+6 more)

### Community 25 - "Community 25"
Cohesion: 0.19
Nodes (6): AppSearchSession, Context, List, WatchItem, AppSearchManager, WatchItemDocument

### Community 26 - "Community 26"
Cohesion: 0.21
Nodes (9): Boolean, Int, List, String, TraktMovie, TraktShow, TraktRepository, TraktApi (+1 more)

### Community 27 - "Community 27"
Cohesion: 0.15
Nodes (12): DiscoverItem, TraktCastMember, TraktIds, TraktMovie, TraktMovieSummary, TraktPeopleResponse, TraktPerson, TraktPersonImages (+4 more)

### Community 28 - "Community 28"
Cohesion: 0.21
Nodes (11): Boolean, ImageVector, List, String, Unit, WatchItem, WhatToWatchViewModel, HomeScreen() (+3 more)

### Community 29 - "Community 29"
Cohesion: 0.20
Nodes (8): Class, List, StateFlow, String, T, WatchItem, Factory, SearchViewModel

### Community 30 - "Community 30"
Cohesion: 0.21
Nodes (7): Long, StateFlow, String, File, UpdateInfo, UpdateManager, UpdateState

### Community 31 - "Community 31"
Cohesion: 0.22
Nodes (5): Int, List, Long, PendingActionDao, PendingAction

### Community 32 - "Community 32"
Cohesion: 0.25
Nodes (6): Int, OmdbSeasonResponse, String, OmdbDetailResponse, OmdbSearchResponse, OmdbApi

### Community 33 - "Community 33"
Cohesion: 0.18
Nodes (9): Boolean, DiscoverItem, List, Modifier, Unit, WatchItem, WatchedPill(), DiscoverFilterBottomSheet() (+1 more)

### Community 34 - "Community 34"
Cohesion: 0.25
Nodes (10): Boolean, EpisodeUiItem, String, SubtleDivider(), DetailScreen(), EpisodeRow(), SeriesEpisodeSection(), TapToPlayTrailer() (+2 more)

### Community 35 - "Community 35"
Cohesion: 0.29
Nodes (10): Int, Map, String, WatchItem, InProgressCard(), RecentlyAddedRow(), StatsScreen(), TopGenresCard() (+2 more)

### Community 36 - "Community 36"
Cohesion: 0.24
Nodes (7): Bundle, WatchLaterApp, ComponentActivity, AppContent(), BottomNavItem, MainActivity, WatchLaterTheme()

### Community 37 - "Community 37"
Cohesion: 0.20
Nodes (8): ImageVector, Modifier, String, Unit, WhatToWatchViewModel, EmptyState(), WhatToWatchBottomSheet(), Composable

### Community 38 - "Community 38"
Cohesion: 0.24
Nodes (8): Boolean, Class, SupabaseUser, T, Factory, FriendsScreen(), FriendsUiState, UserSearchRow()

### Community 39 - "Community 39"
Cohesion: 0.24
Nodes (8): Class, StateFlow, T, Factory, StatsUiState, StatsViewModel, ViewModel, ViewModelProvider

### Community 40 - "Community 40"
Cohesion: 0.38
Nodes (4): Context, Long, UserPreferences, HapticUtils

### Community 41 - "Community 41"
Cohesion: 0.36
Nodes (4): ActionType, MediaType, String, Converters

### Community 42 - "Community 42"
Cohesion: 0.31
Nodes (8): AddItemSheet(), GenreChip(), OmdbSuggestionRow(), outlinedTextFieldColors(), AddItemViewModel, com, Modifier, String

### Community 43 - "Community 43"
Cohesion: 0.31
Nodes (4): AppContainer, Application, Int, WatchLaterApp

### Community 44 - "Community 44"
Cohesion: 0.39
Nodes (4): List, String, ArcItemProgress, ArcProgressDao

### Community 45 - "Community 45"
Cohesion: 0.28
Nodes (6): DiscoverItem, List, Map, String, DiscoverCacheDto, DiscoverCacheRepository

### Community 46 - "Community 46"
Cohesion: 0.28
Nodes (4): String, FirebaseMessagingService, RemoteMessage, KazeMessagingService

### Community 47 - "Community 47"
Cohesion: 0.31
Nodes (5): Job, StateFlow, String, FriendsViewModel, FriendsUiState

### Community 48 - "Community 48"
Cohesion: 0.29
Nodes (4): Context, Result, Intent, GrassWorker

### Community 49 - "Community 49"
Cohesion: 0.43
Nodes (6): Boolean, SortFilterState, String, SheetRow(), SheetSection(), SortFilterSheet()

### Community 50 - "Community 50"
Cohesion: 0.33
Nodes (4): Boolean, String, Uri, DeepLinkHandler

### Community 52 - "Community 52"
Cohesion: 0.29
Nodes (5): Context, Result, CoroutineWorker, enqueue(), SyncWorker

### Community 53 - "Community 53"
Cohesion: 0.29
Nodes (6): OmdbDetailResponse, OmdbEpisodeItem, OmdbResult, OmdbSearchItem, OmdbSearchResponse, OmdbSeasonResponse

### Community 54 - "Community 54"
Cohesion: 0.33
Nodes (4): Class, T, Factory, HomeUiState

### Community 55 - "Community 55"
Cohesion: 0.47
Nodes (5): String, FocusRequester, SearchScreen(), SearchTopBar(), SearchViewModel

### Community 56 - "Community 56"
Cohesion: 0.33
Nodes (4): Boolean, Flow, UserRepository, FeatureFlags

### Community 58 - "Community 58"
Cohesion: 0.40
Nodes (3): WatchItem, WhatToWatchDao, SupportSQLiteQuery

### Community 59 - "Community 59"
Cohesion: 0.40
Nodes (4): List, String, MediaType, WatchItem

### Community 60 - "Community 60"
Cohesion: 0.50
Nodes (3): Boolean, StateFlow, NetworkMonitor

### Community 61 - "Community 61"
Cohesion: 0.40
Nodes (4): Boolean, SharedPreferences, String, UserPreferences

### Community 62 - "Community 62"
Cohesion: 0.50
Nodes (3): EpisodeProgress, SeasonEpisode, SeriesCache

### Community 63 - "Community 63"
Cohesion: 0.50
Nodes (3): FilterOption, SortFilterState, SortOption

## Knowledge Gaps
- **286 isolated node(s):** `Bundle`, `WatchLaterApp`, `Int`, `Int`, `PendingAction` (+281 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **5 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `WatchLaterNavGraph()` connect `Community 1` to `Community 34`, `Community 35`, `Community 36`, `Community 4`, `Community 37`, `Community 38`, `Community 8`, `Community 9`, `Community 42`, `Community 11`, `Community 13`, `Community 14`, `Community 16`, `Community 20`, `Community 21`, `Community 55`, `Community 28`?**
  _High betweenness centrality (0.159) - this node is a cross-community bridge._
- **Why does `ActivityFeedEntry` connect `Community 6` to `Community 3`, `Community 11`?**
  _High betweenness centrality (0.064) - this node is a cross-community bridge._
- **Why does `DetailViewModel` connect `Community 0` to `Community 39`?**
  _High betweenness centrality (0.051) - this node is a cross-community bridge._
- **What connects `Bundle`, `WatchLaterApp`, `Int` to the rest of the system?**
  _286 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.06196078431372549 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.06547619047619048 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.1173054587688734 - nodes in this community are weakly interconnected._