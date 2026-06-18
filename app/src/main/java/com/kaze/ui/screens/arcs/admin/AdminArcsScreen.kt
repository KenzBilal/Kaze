package com.kaze.ui.screens.arcs.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.kaze.data.repository.Arc
import com.kaze.data.repository.ArcRepository
import com.kaze.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AdminArcsViewModel @Inject constructor(private val arcRepository: ArcRepository) : ViewModel() {

    private val _arcs = MutableStateFlow<List<Arc>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    val arcs: StateFlow<List<Arc>> = _arcs.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isAIGenerating = MutableStateFlow(false)
    val isAIGenerating: StateFlow<Boolean> = _isAIGenerating.asStateFlow()
    
    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _arcs.value = arcRepository.getAllArcsAdmin()
            _isLoading.value = false
        }
    }

    fun createArc(name: String, description: String, aliases: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val arc = Arc(
                id = name.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "") + "_" + UUID.randomUUID().toString().take(4),
                name = name.trim(),
                description = description.trim(),
                aliases = aliases.trim(),
                is_published = false
            )
            arcRepository.createArc(arc)
            load()
        }
    }

    fun deleteArc(arcId: String) {
        viewModelScope.launch {
            arcRepository.deleteArc(arcId)
            load()
        }
    }

    fun togglePublish(arc: Arc) {
        viewModelScope.launch {
            arcRepository.publishArc(arc.id, !arc.is_published)
            load()
        }
    }

    fun generateArcWithAI(prompt: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            _isAIGenerating.value = true
            _aiError.value = null
            val result = arcRepository.generateArcWithAI(prompt, "kenzbilal")
            _isAIGenerating.value = false
            result.onSuccess { arcId ->
                load()
                onDone(arcId)
            }.onFailure {
                _aiError.value = it.message ?: "Unknown error"
            }
        }
    }

    
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminArcsScreen(
    arcRepository: ArcRepository,
    onBack: () -> Unit,
    onEditArc: (String) -> Unit
) {
    val vm: AdminArcsViewModel = hiltViewModel()
    val arcs by vm.arcs.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val isAIGenerating by vm.isAIGenerating.collectAsStateWithLifecycle()
    val aiError by vm.aiError.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showAISheet by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Arc?>(null) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Manage Arcs", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBackIosNew, "Back", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New Arc", tint = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAISheet = true },
                containerColor = AccentBlue,
                contentColor = Background,
                icon = { Icon(Icons.Filled.AutoAwesome, null) },
                text = { Text("AI Auto-Gen") }
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(arcs, key = { it.id }) { arc ->
                    AdminArcRow(
                        arc = arc,
                        onEdit = { onEditArc(arc.id) },
                        onDelete = { deleteTarget = arc },
                        onTogglePublish = { vm.togglePublish(arc) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // Create dialog
    if (showCreateDialog) {
        CreateArcDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, aliases ->
                vm.createArc(name, desc, aliases)
                showCreateDialog = false
            }
        )
    }

    if (showAISheet) {
        AdminAIArcSheet(
            onDismiss = { showAISheet = false },
            onGenerate = { prompt ->
                vm.generateArcWithAI(prompt) { newId ->
                    showAISheet = false
                    onEditArc(newId)
                }
            },
            isLoading = isAIGenerating,
            errorMsg = aiError
        )
    }

    // Delete confirmation
    deleteTarget?.let { arc ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = SurfaceContainer,
            title = { Text("Delete \"${arc.name}\"?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text = { Text("This will delete the arc and all its items. Cannot be undone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { vm.deleteArc(arc.id); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.7f))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun AdminArcRow(
    arc: Arc,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePublish: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(arc.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                // Published switch
                Switch(
                    checked = arc.is_published,
                    onCheckedChange = { onTogglePublish() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Background,
                        checkedTrackColor = WatchedGreen,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }
            if (arc.description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(arc.description, color = TextTertiary, fontSize = 12.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AccentBlue, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CreateArcDialog(onDismiss: () -> Unit, onCreate: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var aliases by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        title = { Text("New Arc", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ArcTextField(value = name, onValueChange = { name = it }, label = "Name *")
                ArcTextField(value = description, onValueChange = { description = it }, label = "Description")
                ArcTextField(value = aliases, onValueChange = { aliases = it }, label = "Aliases (comma separated)")
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, description, aliases) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Background),
                enabled = name.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun ArcTextField(value: String, onValueChange: (String) -> Unit, label: String, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextTertiary, fontSize = 12.sp) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = SurfaceHighlight,
            focusedBorderColor = AccentBlue,
            unfocusedContainerColor = SurfaceElevated,
            focusedContainerColor = SurfaceElevated,
            cursorColor = AccentBlue,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(10.dp)
    )
}
