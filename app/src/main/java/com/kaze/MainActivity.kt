package com.kaze

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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kaze.ui.Screen
import com.kaze.ui.WatchLaterNavGraph
import com.kaze.ui.theme.*

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isCenter: Boolean = false  // Discover tab is slightly larger
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as WatchLaterApp
        setContent {
            WatchLaterTheme {
                AppContent(app = app)
            }
        }
    }
}

@Composable
fun AppContent(app: WatchLaterApp) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(
            screen = Screen.Home,
            label = "Watchlist",
            selectedIcon = Icons.Filled.Bookmark,
            unselectedIcon = Icons.Outlined.Bookmark
        ),
        BottomNavItem(
            screen = Screen.Friends,
            label = "Friends",
            selectedIcon = Icons.Filled.People,
            unselectedIcon = Icons.Outlined.People
        ),
        BottomNavItem(
            screen = Screen.Discover,
            label = "Discover",
            selectedIcon = Icons.Filled.Explore,
            unselectedIcon = Icons.Outlined.Explore,
            isCenter = true   // Slightly larger center icon
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
        Screen.Friends.route,
        Screen.Discover.route,
        Screen.Stats.route,
        Screen.MyProfile.route
    )

    val isOnline by app.container.networkMonitor.isOnline.collectAsState()
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
                    bottomNavItems.forEach { item ->
                        val selected = currentEntry?.destination?.hierarchy
                            ?.any { it.route == item.screen.route } == true
                        val iconSize: Dp = if (item.isCenter) 30.dp else 24.dp

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
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
            WatchLaterNavGraph(navController = navController, app = app)
            
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
