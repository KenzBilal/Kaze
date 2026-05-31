package com.watchlater.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchlater.R
import com.watchlater.data.local.WatchLaterDatabase
import com.watchlater.data.repository.UserRepository
import com.watchlater.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── Validation ────────────────────────────────────────────────────────────────

private val USERNAME_REGEX = Regex("^[A-Za-z]*$")

fun validateUsername(name: String): String? = when {
    name.isBlank() -> null // No error shown for empty field
    name.length < 4 -> "Must be at least 4 letters"
    name.length > 12 -> "Must be at most 12 letters"
    !USERNAME_REGEX.matches(name) -> "Only letters allowed (a-z, A-Z)"
    else -> null
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class SetUsernameViewModel(
    private val userRepository: UserRepository,
    private val dao: com.watchlater.data.local.WatchItemDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetUsernameUiState())
    val uiState: StateFlow<SetUsernameUiState> = _uiState.asStateFlow()

    fun onUsernameChange(raw: String) {
        // Reject non-letter chars immediately; allow up to 12
        val filtered = raw.filter { it.isLetter() }.take(12)
        val error = validateUsername(filtered)
        _uiState.update { it.copy(username = filtered, validationError = error) }
    }

    fun submit(onSuccess: () -> Unit) {
        val name = _uiState.value.username.trim()
        val error = validateUsername(name)
        if (error != null) {
            _uiState.update { it.copy(validationError = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, apiError = null) }

            // 1. Create the user account in Supabase
            val result = userRepository.createUser(name)
            if (!result.success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        apiError = result.errorMessage ?: "Failed to create account. Try a different username."
                    )
                }
                return@launch
            }

            // 2. Sync existing local watchlist to Supabase
            val userId = userRepository.getLocalUserId()
            if (userId != null) {
                val localItems = dao.getAllItemsOnce()
                userRepository.syncWatchlist(userId, localItems)
            }

            _uiState.update { it.copy(isLoading = false) }
            onSuccess()
        }
    }

    class Factory(private val context: android.content.Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = WatchLaterDatabase.getInstance(context)
            val userRepo = UserRepository(context)
            return SetUsernameViewModel(userRepo, db.watchItemDao()) as T
        }
    }
}

data class SetUsernameUiState(
    val username: String = "",
    val validationError: String? = null,
    val apiError: String? = null,
    val isLoading: Boolean = false
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetUsernameScreen(onAccountCreated: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SetUsernameViewModel = viewModel(factory = SetUsernameViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier.fillMaxSize().background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.ic_splash_logo),
                contentDescription = "Kaze Logo",
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Kaze",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(36.dp))

            // Title
            Text(
                text = "Choose your username",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "4–12 letters only. No numbers or symbols.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))

            // Input
            OutlinedTextField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("e.g. kenzwatches", color = TextTertiary) },
                isError = uiState.validationError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = SurfaceHighlight,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    focusedContainerColor = SurfaceElevated,
                    unfocusedContainerColor = SurfaceElevated,
                    errorContainerColor = SurfaceElevated,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                suffix = {
                    Text(
                        text = "${uiState.username.length}/12",
                        color = if (uiState.username.length >= 12) MaterialTheme.colorScheme.error else TextTertiary,
                        fontSize = 12.sp
                    )
                }
            )

            // Validation error
            AnimatedVisibility(
                visible = uiState.validationError != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = uiState.validationError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }

            // API error
            AnimatedVisibility(
                visible = uiState.apiError != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = uiState.apiError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit button
            Button(
                onClick = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                    viewModel.submit(onAccountCreated)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = uiState.username.length >= 4 && uiState.validationError == null && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Background,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Let's go →", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
