package com.kaze.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kaze.WatchLaterApp
import com.kaze.ui.screens.add.AddItemSheet
import com.kaze.ui.screens.add.AddItemViewModel
import com.kaze.ui.screens.detail.DetailScreen
import com.kaze.ui.screens.detail.DetailViewModel
import com.kaze.ui.screens.discover.DiscoverScreen
import com.kaze.ui.screens.feed.FeedScreen
import com.kaze.ui.screens.home.HomeScreen
import com.kaze.ui.screens.home.HomeViewModel
import com.kaze.ui.screens.search.SearchScreen
import com.kaze.ui.screens.search.SearchViewModel
import com.kaze.ui.screens.settings.SettingsScreen
import com.kaze.ui.screens.stats.StatsScreen
import com.kaze.ui.screens.stats.StatsViewModel
import com.kaze.ui.screens.splash.SplashScreen
import com.kaze.ui.screens.friends.FriendsScreen
import com.kaze.ui.screens.friends.UserProfileScreen
import com.kaze.ui.screens.onboarding.SetUsernameScreen
import com.kaze.ui.screens.profile.MyProfileScreen
import com.kaze.ui.theme.SurfaceContainer

sealed class Screen(val route: String) {
    object Splash       : Screen("splash")
    object SetUsername  : Screen("setUsername")
    object Home         : Screen("home")
    object Discover     : Screen("discover")
    object Feed         : Screen("feed")
    object Friends      : Screen("friends")
    object Stats        : Screen("stats")
    object MyProfile    : Screen("myProfile")
    object Settings     : Screen("settings")
    object Search       : Screen("search")
    object Detail : Screen("detail/{itemId}") {
        fun createRoute(id: Long) = "detail/$id"
    }
    object UserProfile : Screen("userProfile/{userId}") {
        fun createRoute(id: String) = "userProfile/$id"
    }
    object DetailPreview : Screen("detail_preview/{imdbId}?title={title}&type={type}&poster={poster}&rating={rating}&notes={notes}&genres={genres}&year={year}&season={season}&episode={episode}") {
        fun createRoute(imdbId: String, title: String, type: String, poster: String?, rating: Float = 0f, notes: String = "", genres: String = "", year: Int = 0, season: Int = 1, episode: Int = 1) =
            "detail_preview/$imdbId?title=${android.net.Uri.encode(title)}&type=$type&poster=${android.net.Uri.encode(poster ?: "")}&rating=$rating&notes=${android.net.Uri.encode(notes)}&genres=${android.net.Uri.encode(genres)}&year=$year&season=$season&episode=$episode"
    }
}

@Composable
fun WatchLaterNavGraph(
    navController: NavHostController,
    app: WatchLaterApp
) {
    val repo   = app.container.repository

    NavHost(
        navController  = navController,
        startDestination = Screen.Splash.route,
        enterTransition  = { fadeIn(tween(200)) + slideInHorizontally { it / 12 } },
        exitTransition   = { fadeOut(tween(160)) },
        popEnterTransition  = { fadeIn(tween(200)) },
        popExitTransition   = { fadeOut(tween(160)) + slideOutHorizontally { it / 12 } }
    ) {
        // ── Splash ────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                userRepository = app.container.userRepository,
                onGoHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onGoSetUsername = {
                    navController.navigate(Screen.SetUsername.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Set Username (Onboarding) ─────────────────────────────────────
        composable(Screen.SetUsername.route) {
            SetUsernameScreen(
                onAccountCreated = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SetUsername.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Home ──────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            val homeVm: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(
                    repository = repo,
                    seriesRepository = app.container.seriesRepository,
                    userRepository = app.container.userRepository,
                    userPreferences = app.container.userPreferences,
                    updateManager = app.container.updateManager,
                    backupManager = app.container.backupManager
                )
            )
            val addVm:  AddItemViewModel = viewModel(factory = AddItemViewModel.Factory(repo, app.container.omdbRepository, app.container.activityRepository, app.container.userRepository))
            var showAddSheet by remember { mutableStateOf(false) }
            val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            HomeScreen(
                viewModel    = homeVm,
                onItemClick  = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                onAddClick   = { showAddSheet = true },
                onSearchClick = { navController.navigate(Screen.Search.route) }
            )

            if (showAddSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAddSheet = false; addVm.reset() },
                    sheetState       = addSheetState,
                    containerColor   = SurfaceContainer,
                    dragHandle       = null
                ) {
                    AddItemSheet(
                        viewModel = addVm,
                        onDismiss = { showAddSheet = false }
                    )
                }
            }
        }

        composable(Screen.Discover.route) {
            DiscoverScreen(
                repository = repo,
                onItemClick = { item ->
                    if (item.imdb_id.isNotBlank()) {
                        navController.navigate(Screen.DetailPreview.createRoute(
                            imdbId = item.imdb_id,
                            title = item.title,
                            type = item.type,
                            poster = item.poster_url,
                            rating = item.rating,
                            notes = item.notes,
                            genres = item.genres,
                            year = item.year,
                            season = item.season ?: 1,
                            episode = item.episode ?: 1
                        ))
                    }
                }
            )
        }

        // ── Feed ──────────────────────────────────────────────────────────
        composable(Screen.Feed.route) {
            FeedScreen()
        }

        // ── Stats ─────────────────────────────────────────────────────────
        composable(Screen.Stats.route) {
            val statsVm: StatsViewModel = viewModel(
                factory = StatsViewModel.Factory(repo)
            )
            StatsScreen(viewModel = statsVm)
        }

        // ── Search ────────────────────────────────────────────────────────
        composable(Screen.Search.route) {
            val searchVm: SearchViewModel = viewModel(factory = SearchViewModel.Factory(repo))
            SearchScreen(
                viewModel   = searchVm,
                onItemClick = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                onBack      = { navController.popBackStack() }
            )
        }

        // ── Friends ───────────────────────────────────────────────────────
        composable(Screen.Friends.route) {
            FriendsScreen(
                onUserClick = { id -> navController.navigate(Screen.UserProfile.createRoute(id)) }
            )
        }

        // ── My Profile ────────────────────────────────────────────────────
        composable(Screen.MyProfile.route) {
            MyProfileScreen(
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        // ── Settings ──────────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        // ── User Profile ──────────────────────────────────────────────────
        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStack ->
            val userId = backStack.arguments!!.getString("userId") ?: ""
            UserProfileScreen(
                profileUserId = userId,
                onBack = { navController.popBackStack() },
                onUserClick = { id -> navController.navigate(Screen.UserProfile.createRoute(id)) },
                onItemClick = { item ->
                    if (item.imdb_id.isNotBlank()) {
                        navController.navigate(Screen.DetailPreview.createRoute(
                            imdbId = item.imdb_id,
                            title = item.title,
                            type = item.type,
                            poster = item.poster_url,
                            rating = item.rating,
                            notes = item.notes,
                            genres = item.genres,
                            year = item.year,
                            season = item.season ?: 1,
                            episode = item.episode ?: 1
                        ))
                    }
                }
            )
        }


        // ── Detail ────────────────────────────────────────────────────────
        composable(
            route     = Screen.Detail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { backStack ->
            val itemId = backStack.arguments!!.getLong("itemId")
            val detailVm: DetailViewModel = viewModel(
                factory = DetailViewModel.Factory(repo, app.container.seriesRepository, app.container.userRepository, itemId)
            )
            DetailScreen(
                viewModel = detailVm,
                onBack    = { navController.popBackStack() }
            )
        }
        
        // ── Detail Preview ────────────────────────────────────────────────
        composable(
            route = Screen.DetailPreview.route,
            arguments = listOf(
                navArgument("imdbId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType },
                navArgument("poster") { type = NavType.StringType; nullable = true },
                navArgument("rating") { type = NavType.FloatType; defaultValue = 0f },
                navArgument("notes") { type = NavType.StringType; defaultValue = "" },
                navArgument("genres") { type = NavType.StringType; defaultValue = "" },
                navArgument("year") { type = NavType.IntType; defaultValue = 0 },
                navArgument("season") { type = NavType.IntType; defaultValue = 1 },
                navArgument("episode") { type = NavType.IntType; defaultValue = 1 }
            )
        ) { backStack ->
            val imdbId = backStack.arguments!!.getString("imdbId") ?: ""
            val title  = backStack.arguments!!.getString("title") ?: ""
            val type   = backStack.arguments!!.getString("type") ?: "MOVIE"
            val poster = backStack.arguments!!.getString("poster")
            val rating = backStack.arguments!!.getFloat("rating")
            val notes  = backStack.arguments!!.getString("notes") ?: ""
            val genres = backStack.arguments!!.getString("genres") ?: ""
            val year   = backStack.arguments!!.getInt("year")
            val season = backStack.arguments!!.getInt("season")
            val episode = backStack.arguments!!.getInt("episode")

            val detailVm: DetailViewModel = viewModel(
                factory = DetailViewModel.Factory(
                    repository = repo,
                    seriesRepository = app.container.seriesRepository,
                    userRepository = app.container.userRepository,
                    itemId = -1L,
                    previewImdbId = imdbId,
                    previewTitle = title,
                    previewType = type,
                    previewPoster = poster,
                    previewRating = rating,
                    previewNotes = notes,
                    previewGenres = genres,
                    previewYear = year,
                    previewSeason = season,
                    previewEpisode = episode
                )
            )
            DetailScreen(
                viewModel = detailVm,
                onBack    = { navController.popBackStack() }
            )
        }
    }
}
