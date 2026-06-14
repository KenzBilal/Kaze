package com.kaze.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaze.data.repository.GlobalChatMessage
import com.kaze.data.repository.UserRepository
import com.kaze.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class ChatUiState(
    val messages: List<GlobalChatMessage> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val currentUserId: String = "",
    val currentUsername: String = "",
    val isKenzbilal: Boolean = false
)

class GlobalChatViewModel(
    private val userRepo: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Emoji-block regex
    private val emojiRegex = Regex(
        "[\uD83C-\uDBFF\uDC00-\uDFFF]|[\u2600-\u26FF]|[\u2700-\u27BF]|[\\x{1F000}-\\x{1FFFF}]",
        RegexOption.IGNORE_CASE
    )

    init {
        viewModelScope.launch {
            val userId = userRepo.getLocalUserId() ?: return@launch
            val username = userRepo.getLocalUsername() ?: return@launch
            _uiState.update {
                it.copy(
                    currentUserId = userId,
                    currentUsername = username,
                    isKenzbilal = username.equals("kenzbilal", ignoreCase = true)
                )
            }
            loadMessages()
            // Poll every 8 seconds
            while (true) {
                delay(8_000)
                refreshMessages()
            }
        }
    }

    private suspend fun loadMessages() {
        val msgs = userRepo.fetchChatMessages()
        _uiState.update { it.copy(messages = msgs, isLoading = false) }
    }

    private suspend fun refreshMessages() {
        val msgs = userRepo.fetchChatMessages()
        _uiState.update { it.copy(messages = msgs) }
    }

    fun sendMessage(raw: String) {
        val text = raw.trim()
        if (text.isBlank()) return
        // Block emojis
        if (emojiRegex.containsMatchIn(text)) return
        if (text.length > 400) return

        val state = _uiState.value
        if (state.isSending) return

        _uiState.update { it.copy(isSending = true) }
        viewModelScope.launch {
            userRepo.sendChatMessage(state.currentUserId, state.currentUsername, text)
            refreshMessages()
            _uiState.update { it.copy(isSending = false) }
        }
    }

    fun inviteUser(userId: String) {
        viewModelScope.launch { userRepo.inviteToGlobalChat(userId) }
    }

    class Factory(private val context: android.content.Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GlobalChatViewModel(UserRepository(context)) as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: GlobalChatViewModel = viewModel(factory = GlobalChatViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var draftText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to latest message
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Global Chat", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Text only · No emojis", color = TextTertiary, fontSize = 10.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        bottomBar = {
            Surface(color = Background, tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { if (it.length <= 400) draftText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message…", color = TextTertiary, fontSize = 14.sp) },
                        singleLine = true,
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
                    IconButton(
                        onClick = {
                            viewModel.sendMessage(draftText)
                            draftText = ""
                        },
                        enabled = draftText.isNotBlank() && !uiState.isSending
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = AccentBlue)
                    }
                }
            }
        },
        containerColor = Background
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue, strokeWidth = 2.dp)
            }
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (uiState.messages.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxWidth().padding(40.dp), Alignment.Center) {
                        Text("No messages yet. Say something!", color = TextTertiary, fontSize = 14.sp)
                    }
                }
            } else {
                items(uiState.messages, key = { it.id }) { msg ->
                    ChatBubble(
                        msg = msg,
                        isOwn = msg.user_id == uiState.currentUserId
                    )
                }
            }
        }
    }
}

// ── Chat Bubble ───────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(msg: GlobalChatMessage, isOwn: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isOwn) {
                Text(
                    msg.username,
                    color = AccentBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isOwn) 14.dp else 4.dp,
                            topEnd = if (isOwn) 4.dp else 14.dp,
                            bottomStart = 14.dp,
                            bottomEnd = 14.dp
                        )
                    )
                    .background(if (isOwn) AccentBlue else SurfaceElevated)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    msg.message,
                    color = if (isOwn) Background else TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
