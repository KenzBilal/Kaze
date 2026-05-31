package com.watchlater.ui

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
import com.watchlater.WatchLaterApp
import com.watchlater.ui.screens.add.AddItemSheet
import com.watchlater.ui.screens.add.AddItemViewModel
import com.watchlater.ui.screens.detail.DetailScreen
import com.watchlater.ui.screens.detail.DetailViewModel
import com.watchlater.ui.screens.home.HomeScreen
import com.watchlater.ui.screens.home.HomeViewModel
import com.watchlater.ui.screens.search.SearchScreen
import com.watchlater.ui.screens.search.SearchViewModel
import com.watchlater.ui.screens.stats.StatsScreen
import com.watchlater.ui.screens.stats.StatsViewModel
import com.watchlater.ui.screens.splash.SplashScreen
import com.watchlater.ui.screens.friends.FriendsScreen
import com.watchlater.ui.screens.friends.UserProfileScreen
import com.watchlater.ui.screens.onboarding.SetUsernameScreen
import com.watchlater.ui.theme.SurfaceContainer

sealed class Screen(val route: String) {
    object Splash       : Screen("splash")
    object SetUsername  : Screen("setUsername")
    object Home         : Screen("home")
    object Friends      : Screen("friends")
    object Stats        : Screen("stats")
    object Search       : Screen("search")
    object Detail : Screen("detail/{itemId}") {
        fun createRoute(id: Long) = "detail/$id"
    }
    object UserProfile : Screen("userProfile/{userId}") {
        fun createRoute(id: String) = "userProfile/$id"
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
            val homeVm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repo, app.container.updateManager))
            val addVm:  AddItemViewModel = viewModel(factory = AddItemViewModel.Factory(repo, app.container.omdbRepository))
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
                    onDismissRequest = { showAddSheet = false },
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

        // ── User Profile ──────────────────────────────────────────────────
        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStack ->
            val userId = backStack.arguments!!.getString("userId") ?: ""
            UserProfileScreen(
                userId = userId,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Detail ────────────────────────────────────────────────────────
        composable(
            route     = Screen.Detail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { backStack ->
            val itemId = backStack.arguments!!.getLong("itemId")
            val detailVm: DetailViewModel = viewModel(
                factory = DetailViewModel.Factory(repo, app.container.seriesRepository, itemId)
            )
            DetailScreen(
                viewModel = detailVm,
                onBack    = { navController.popBackStack() }
            )
        }
    }
}
