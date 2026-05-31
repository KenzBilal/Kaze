package com.watchlater.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchlater.data.repository.SupabaseUser
import com.watchlater.data.repository.UserRepository

import com.watchlater.ui.theme.*
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

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(500)
            val results = repository.searchUsers(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
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
    val searchResults: List<SupabaseUser> = emptyList(),
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
                title = { Text("Friends", color = TextPrimary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        FriendsSearchSection(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            uiState = uiState,
            onSearchChange = viewModel::onSearchChange,
            onUserClick = onUserClick
        )
    }
}



@Composable
fun FriendsSearchSection(
    modifier: Modifier = Modifier,
    uiState: FriendsUiState,
    onSearchChange: (String) -> Unit,
    onUserClick: (String) -> Unit
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search users...", color = TextTertiary) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = TextTertiary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = SurfaceHighlight,
                focusedContainerColor = SurfaceElevated,
                unfocusedContainerColor = SurfaceElevated,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (uiState.isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else if (uiState.searchQuery.isNotBlank() && uiState.searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No users found", color = TextTertiary)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(uiState.searchResults) { user ->
                    UserRow(user = user, onClick = { onUserClick(user.id) })
                }
            }
        }
    }
}

@Composable
fun UserRow(user: SupabaseUser, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(AccentBlue.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = "Profile", tint = AccentBlue)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "@${user.username}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}
