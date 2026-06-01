package com.kaze.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaze.data.repository.SupabaseUser
import com.kaze.data.repository.UserRepository
import com.kaze.ui.components.UserAvatar
import com.kaze.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FriendsViewModel(private val repository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    // Track local user id to hide self and manage follow state
    private var localUserId: String? = null

    init {
        viewModelScope.launch {
            localUserId = repository.getLocalUserId()
            // Load all users on start (excluding self)
            loadAllUsers()
        }
    }

    private suspend fun loadAllUsers() {
        _uiState.update { it.copy(isSearching = true) }
        try {
            val results = repository.searchUsers("")
                .filter { it.id != localUserId }
            val lid = localUserId
            val followedIds = if (lid != null) {
                repository.getFollowedIds(lid)
            } else emptySet()
            _uiState.update {
                it.copy(
                    allUsers = results,
                    searchResults = results,
                    followedIds = followedIds,
                    isSearching = false
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isSearching = false) }
        }
    }

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            // Show all users when no query
            _uiState.update {
                it.copy(searchResults = it.allUsers, isSearching = false)
            }
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(400)
            val results = repository.searchUsers(query)
                .filter { it.id != localUserId } // hide self

            val lid = localUserId
            val followedIds = if (lid != null) {
                val allFollowed = repository.getFollowedIds(lid)
                results.mapNotNull { user -> if (allFollowed.contains(user.id)) user.id else null }.toSet()
            } else emptySet()

            _uiState.update {
                it.copy(searchResults = results, followedIds = followedIds, isSearching = false)
            }
        }
    }

    fun toggleFollow(userId: String) {
        val lid = localUserId ?: return
        viewModelScope.launch {
            val currentlyFollowing = _uiState.value.followedIds.contains(userId)
            // Optimistic update
            _uiState.update {
                it.copy(followedIds = if (currentlyFollowing) it.followedIds - userId else it.followedIds + userId)
            }
            try {
                if (currentlyFollowing) repository.unfollowUser(lid, userId)
                else repository.followUser(lid, userId)
            } catch (e: Exception) {
                // Rollback on failure
                _uiState.update {
                    it.copy(followedIds = if (currentlyFollowing) it.followedIds + userId else it.followedIds - userId)
                }
            }
        }
    }

    class Factory(private val context: android.content.Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FriendsViewModel(UserRepository(context)) as T
        }
    }
}

data class FriendsUiState(
    val searchQuery: String = "",
    val allUsers: List<SupabaseUser> = emptyList(),
    val searchResults: List<SupabaseUser> = emptyList(),
    val followedIds: Set<String> = emptySet(),
    val isSearching: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onUserClick: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: FriendsViewModel = viewModel(factory = FriendsViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Friends",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by username…", color = TextTertiary) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = TextTertiary)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextSecondary,
                    unfocusedBorderColor = SurfaceHighlight,
                    focusedContainerColor = SurfaceElevated,
                    unfocusedContainerColor = SurfaceElevated,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            when {
                uiState.isSearching -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TextSecondary, strokeWidth = 2.dp)
                    }
                }
                uiState.searchQuery.isNotBlank() && uiState.searchResults.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No users found", color = TextTertiary)
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(uiState.searchResults, key = { it.id }) { user ->
                            UserSearchRow(
                                user = user,
                                isFollowing = uiState.followedIds.contains(user.id),
                                onFollowClick = { viewModel.toggleFollow(user.id) },
                                onClick = { onUserClick(user.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchRow(
    user: SupabaseUser,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(username = user.username, size = 44.dp, fontSize = 17.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = user.username,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        // Follow toggle button
        OutlinedButton(
            onClick = onFollowClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (isFollowing) TextTertiary else TextPrimary
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isFollowing) SurfaceHighlight else TextSecondary
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(
                text = if (isFollowing) "Following" else "Follow",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
