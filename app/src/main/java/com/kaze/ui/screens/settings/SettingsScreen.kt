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
import com.kaze.ui.theme.*
import com.kaze.utils.HapticUtils
import com.kaze.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import io.github.jan.supabase.postgrest.from

@Serializable
data class RemoteWatchItem(
    val imdb_id: String,
    val title: String,
    val year: Int,
    val type: String,
    val is_watched: Boolean,
    val rating: Int,
    val season: Int?,
    val episode: Int?,
    val notes: String?,
    val poster_url: String?,
    val genres: String?,
    val date_added: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()

    var hapticEnabled by remember { mutableStateOf(prefs.hapticEnabled) }
    var soundEnabled by remember { mutableStateOf(prefs.soundEnabled) }
    var isSyncing by remember { mutableStateOf(false) }

    Scaffold(
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
                        scope.launch(Dispatchers.IO) {
                            try {
                                val db = com.kaze.data.local.WatchLaterDatabase.getInstance(context)
                                val userRepo = com.kaze.data.repository.UserRepository(context)
                                val userId = userRepo.getLocalUserId() ?: return@launch
                                
                                val remoteItems = com.kaze.data.remote.SupabaseApi.client.from("public_watchlist")
                                    .select { filter { eq("user_id", userId) } }
                                    .decodeList<RemoteWatchItem>()
                                    
                                val dao = db.watchItemDao()
                                for (r in remoteItems) {
                                    val existing = dao.getItemByTitleYearType(r.title, r.year, com.kaze.model.MediaType.valueOf(r.type))
                                    if (existing == null) {
                                        dao.insertItem(
                                            com.kaze.model.WatchItem(
                                                imdbId = r.imdb_id,
                                                title = r.title,
                                                year = r.year,
                                                type = com.kaze.model.MediaType.valueOf(r.type),
                                                isWatched = r.is_watched,
                                                rating = r.rating,
                                                season = r.season,
                                                episode = r.episode,
                                                notes = r.notes ?: "",
                                                posterUrl = r.poster_url ?: "",
                                                genres = r.genres ?: "",
                                                dateAdded = r.date_added
                                            )
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isSyncing = false
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
                    Text("v1.6.0", color = TextTertiary, fontSize = 12.sp)
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
