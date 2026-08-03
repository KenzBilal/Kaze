package com.kaze
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.kaze.utils.NetworkMonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kaze.ui.Screen
import com.kaze.ui.WatchLaterNavGraph
import com.kaze.ui.theme.*
import android.content.Intent
import android.net.Uri
import android.content.Context
import com.kaze.util.DeepLinkHandler

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isCenter: Boolean = false  // Discover tab is slightly larger
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var networkMonitor: NetworkMonitor
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        com.kaze.worker.BingeTracker.initChannel(this)
        enableEdgeToEdge()

        val pendingUpdateUrl = getSharedPreferences("kaze_pending_update", Context.MODE_PRIVATE)
            .getString("update_url", null)
        getSharedPreferences("kaze_pending_update", Context.MODE_PRIVATE)
            .edit().remove("update_url").apply()

        setContent {
            WatchLaterTheme {
                com.kaze.ui.components.NotificationPermissionHandler()

                if (pendingUpdateUrl != null) {
                    UpdateAvailableDialog(
                        url = pendingUpdateUrl,
                        onDismiss = {},
                        onDownload = {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pendingUpdateUrl)))
                        }
                    )
                }

                AppContent(networkMonitor = networkMonitor, initialIntent = intent)
            }
        }
    }
}

@Composable
fun UpdateAvailableDialog(url: String, onDismiss: () -> Unit, onDownload: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = com.kaze.ui.theme.SurfaceContainer,
        titleContentColor = com.kaze.ui.theme.TextPrimary,
        textContentColor = com.kaze.ui.theme.TextSecondary,
        title = { Text("Update Available") },
        text = { Text("A new version of Kaze is available. Would you like to download and install it?") },
        confirmButton = {
            TextButton(onClick = onDownload) {
                Text("Download", color = com.kaze.ui.theme.AccentBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = com.kaze.ui.theme.TextTertiary)
            }
        }
    )
}

@Composable
fun AppContent(networkMonitor: com.kaze.utils.NetworkMonitor, initialIntent: Intent?) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(
            screen = Screen.Home,
            label = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        BottomNavItem(
            screen = Screen.Arcs,
            label = "Arcs",
            selectedIcon = Icons.Filled.Collections,
            unselectedIcon = Icons.Outlined.Collections
        ),
        BottomNavItem(
            screen = Screen.Discover,
            label = "Discover",
            selectedIcon = Icons.Filled.Explore,
            unselectedIcon = Icons.Outlined.Explore,
            isCenter = true
        ),
        BottomNavItem(
            screen = Screen.Stats,
            label = "Stats",
            selectedIcon = Icons.Filled.BarChart,
            unselectedIcon = Icons.Filled.BarChart
        ),
        BottomNavItem(
            screen = Screen.MyProfile,
            label = "Profile",
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person
        )
    )

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Arcs.route,
        Screen.Discover.route,
        Screen.Stats.route,
        Screen.MyProfile.route
    )

    val isOnline by networkMonitor.isOnline.collectAsState()
    var showNetworkBanner by remember { mutableStateOf(false) }
    var wasOnline by remember { mutableStateOf(true) }
    var bannerIsOnline by remember { mutableStateOf(true) }

    LaunchedEffect(isOnline) {
        if (!isOnline && wasOnline) {
            bannerIsOnline = false
            showNetworkBanner = true
            kotlinx.coroutines.delay(3000)
            showNetworkBanner = false
        } else if (isOnline && !wasOnline) {
            bannerIsOnline = true
            showNetworkBanner = true
            kotlinx.coroutines.delay(3000)
            showNetworkBanner = false
        }
        wasOnline = isOnline
    }

    LaunchedEffect(initialIntent) {
        initialIntent?.let { intent ->
            // Handle Global AppSearch
            if (intent.action == android.content.Intent.ACTION_SEARCH) {
                val query = intent.getStringExtra(android.app.SearchManager.QUERY)
                if (!query.isNullOrBlank()) {
                    navController.navigate(Screen.Search.route)
                    // We don't have a direct way to pass initial query yet, 
                    // but landing on the search screen is correct.
                }
            } 
            // Handle Verified Deep Links
            else if (DeepLinkHandler.isWatchLaterDeepLink(intent.data)) {
                val imdbId = DeepLinkHandler.extractImdbId(intent.data)
                if (imdbId != null) {
                    navController.navigate(Screen.DeepLinkLoading.createRoute(imdbId))
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                NavigationBar(
                    containerColor = SurfaceContainer
                ) {
                    val haptic = LocalHapticFeedback.current
                    bottomNavItems.forEach { item ->
                        val selected = currentEntry?.destination?.hierarchy
                            ?.any { it.route == item.screen.route } == true
                        val iconSize: Dp = if (item.isCenter) 30.dp else 24.dp

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                navController.navigate(item.screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(iconSize)
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentBlue,
                                selectedTextColor = AccentBlue,
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary,
                                indicatorColor = AccentBlue.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            WatchLaterNavGraph(navController = navController)
            
            AnimatedVisibility(
                visible = showNetworkBanner,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.TopCenter)
                    .padding(top = padding.calculateTopPadding())
            ) {
                com.kaze.ui.components.NetworkStatusBanner(isOnline = bannerIsOnline)
            }
        }
    }
}
