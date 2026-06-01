package com.kaze.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaze.WatchLaterApp
import com.kaze.ui.theme.*
import com.kaze.utils.HapticUtils
import com.kaze.utils.UserPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as com.kaze.WatchLaterApp
    val prefs = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var hapticEnabled by remember { mutableStateOf(prefs.hapticEnabled) }
    var soundEnabled by remember { mutableStateOf(prefs.soundEnabled) }
    var isSyncing by remember { mutableStateOf(false) }
    var isBackingUp by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticUtils.tick(context)
                        onBack()
                    }) {
                        Icon(Icons.Filled.ArrowBackIosNew, "Back", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            // ── Section: System ───────────────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                SettingsSectionLabel("SYSTEM")
                Spacer(Modifier.height(10.dp))
            }

            item {
                SettingsToggleRow(
                    icon = Icons.Filled.Vibration,
                    title = "Haptic Feedback",
                    subtitle = "Vibrations on interactions",
                    checked = hapticEnabled,
                    onCheckedChange = { enabled ->
                        hapticEnabled = enabled
                        prefs.hapticEnabled = enabled
                        if (enabled) HapticUtils.tick(context)
                    }
                )
            }

            item { Spacer(Modifier.height(10.dp)) }

            item {
                SettingsToggleRow(
                    icon = Icons.Filled.GraphicEq,
                    title = "Sound Effects",
                    subtitle = "Audio feedback on actions",
                    checked = soundEnabled,
                    onCheckedChange = { enabled ->
                        soundEnabled = enabled
                        prefs.soundEnabled = enabled
                        HapticUtils.tick(context)
                    }
                )
            }

            item { Spacer(Modifier.height(10.dp)) }

            item {
                SettingsActionRow(
                    icon = Icons.Filled.CloudDownload,
                    title = "Cloud Restore",
                    subtitle = if (isSyncing) "Restoring..." else "Recover data from cloud",
                    onClick = {
                        if (isSyncing) return@SettingsActionRow
                        isSyncing = true
                        scope.launch {
                            try {
                                val userId = app.container.userRepository.getLocalUserId()
                                    ?: throw Exception("Not signed in")
                                val count = app.container.backupManager.restoreFromCloud(userId)
                                snackbarHostState.showSnackbar("Restored $count new item(s) from cloud")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Restore failed: ${e.message}")
                            } finally {
                                isSyncing = false
                            }
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(10.dp)) }

            item {
                SettingsActionRow(
                    icon = Icons.Filled.CloudUpload,
                    title = "Cloud Backup",
                    subtitle = if (isBackingUp) "Uploading..." else "Upload local data to cloud",
                    onClick = {
                        if (isBackingUp) return@SettingsActionRow
                        isBackingUp = true
                        scope.launch {
                            try {
                                val userId = app.container.userRepository.getLocalUserId()
                                    ?: throw Exception("Not signed in")
                                val count = app.container.backupManager.uploadToCloud(userId)
                                snackbarHostState.showSnackbar("Backed up $count item(s) to cloud")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Backup failed: ${e.message}")
                            } finally {
                                isBackingUp = false
                            }
                        }
                    }
                )
            }

            // ── Section: About ────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(28.dp))
                SettingsSectionLabel("ABOUT")
                Spacer(Modifier.height(10.dp))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kaze", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Track what you watch", color = TextTertiary, fontSize = 12.sp)
                    }
                    Text("v${com.kaze.BuildConfig.VERSION_NAME}", color = TextTertiary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text,
        fontSize = 10.sp,
        color = TextTertiary,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, color = TextTertiary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Background,
                checkedTrackColor = TextPrimary,
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = SurfaceHighlight
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, color = TextTertiary, fontSize = 12.sp)
        }
    }
}
