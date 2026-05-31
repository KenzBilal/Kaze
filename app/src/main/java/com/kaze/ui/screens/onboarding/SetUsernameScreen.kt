package com.kaze.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.kaze.data.local.WatchLaterDatabase
import com.kaze.data.repository.UserRepository
import com.kaze.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── Validation ────────────────────────────────────────────────────────────────

private val USERNAME_REGEX = Regex("^[A-Za-z]*$")

fun validateUsername(name: String): String? = when {
    name.isBlank() -> null
    name.length < 4 -> "Must be at least 4 letters"
    name.length > 12 -> "Must be at most 12 letters"
    !USERNAME_REGEX.matches(name) -> "Only letters allowed"
    else -> null
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class SetUsernameViewModel(
    private val userRepository: UserRepository,
    private val dao: com.kaze.data.local.WatchItemDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetUsernameUiState())
    val uiState: StateFlow<SetUsernameUiState> = _uiState.asStateFlow()

    fun onUsernameChange(raw: String) {
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

            val result = userRepository.createUser(name)
            if (!result.success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        apiError = result.errorMessage ?: "Failed to create account."
                    )
                }
                return@launch
            }

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
            return SetUsernameViewModel(UserRepository(context), db.watchItemDao()) as T
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
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // App name header
            Text(
                text = "Kaze",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF888888),
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(40.dp))

            // Heading
            Text(
                text = "Choose a\nusername",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEEEEEE),
                lineHeight = 40.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "4–12 letters. No numbers or symbols.",
                fontSize = 14.sp,
                color = Color(0xFF777777)
            )
            Spacer(modifier = Modifier.height(40.dp))

            // Input field
            val isError = uiState.validationError != null
            OutlinedTextField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        "e.g. alex",
                        color = Color(0xFF444444),
                        fontSize = 16.sp
                    )
                },
                isError = isError,
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
                    focusedBorderColor = Color(0xFFAAAAAA),
                    unfocusedBorderColor = Color(0xFF333333),
                    errorBorderColor = Color(0xFFCF6679),
                    focusedContainerColor = Color(0xFF161616),
                    unfocusedContainerColor = Color(0xFF161616),
                    errorContainerColor = Color(0xFF161616),
                    focusedTextColor = Color(0xFFEEEEEE),
                    unfocusedTextColor = Color(0xFFEEEEEE),
                    cursorColor = Color(0xFFEEEEEE)
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                suffix = {
                    Text(
                        text = "${uiState.username.length}/12",
                        color = if (uiState.username.length >= 12) Color(0xFFCF6679) else Color(0xFF555555),
                        fontSize = 12.sp
                    )
                }
            )

            // Validation error
            AnimatedVisibility(visible = isError, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = uiState.validationError ?: "",
                    color = Color(0xFFCF6679),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // API error
            AnimatedVisibility(visible = uiState.apiError != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = uiState.apiError ?: "",
                    color = Color(0xFFCF6679),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Submit button — minimal white/grey
            Button(
                onClick = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                    viewModel.submit(onAccountCreated)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = uiState.username.length >= 4 && uiState.validationError == null && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEEEEEE),
                    contentColor = Color(0xFF0D0D0D),
                    disabledContainerColor = Color(0xFF252525),
                    disabledContentColor = Color(0xFF555555)
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color(0xFF0D0D0D),
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Continue",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
