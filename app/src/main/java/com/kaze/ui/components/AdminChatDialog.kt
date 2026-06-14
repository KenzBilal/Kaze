package com.kaze.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.postgrest
import com.kaze.data.repository.SupabaseUser
import com.kaze.data.repository.UserRepository
import com.kaze.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminChatDialog(
    userRepository: UserRepository,
    onDismiss: () -> Unit
) {
    var users by remember { mutableStateOf<List<SupabaseUser>>(emptyList()) }
    var chatMembers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val allUsers = userRepository.searchUsers("") // Returns all limited users
        val membersList = com.kaze.data.remote.SupabaseApi.client.postgrest["global_chat_members"]
            .select().decodeList<com.kaze.data.repository.GlobalChatMember>()
        val memberIds = membersList.map { it.user_id }.toSet()
        users = allUsers
        chatMembers = memberIds
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = { Text("Manage Chat Access", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            if (isLoading) {
                CircularProgressIndicator(color = AccentBlue)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    items(users, key = { it.id }) { u ->
                        val hasAccess = chatMembers.contains(u.id)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(u.username, color = TextPrimary)
                            Switch(
                                checked = hasAccess,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        if (checked) {
                                            userRepository.inviteToGlobalChat(u.id)
                                            chatMembers = chatMembers + u.id
                                        } else {
                                            userRepository.removeFromGlobalChat(u.id)
                                            chatMembers = chatMembers - u.id
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Background,
                                    checkedTrackColor = AccentBlue,
                                    uncheckedThumbColor = TextTertiary,
                                    uncheckedTrackColor = SurfaceContainer
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = AccentBlue)
            }
        }
    )
}
