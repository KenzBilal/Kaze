package com.kaze.ui.screens.arcs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kaze.data.repository.Arc
import com.kaze.data.repository.ArcRepository
import com.kaze.data.repository.ArcShare
import com.kaze.data.repository.UserRepository
import com.kaze.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ArcsViewModel(
    private val arcRepository: ArcRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _arcs = MutableStateFlow<List<Arc>>(emptyList())
    private val _query = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(true)
    private val _pendingShares = MutableStateFlow<List<ArcShare>>(emptyList())
    private val _userId = MutableStateFlow<String?>(null)

    val query: StateFlow<String> = _query.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val pendingShares: StateFlow<List<ArcShare>> = _pendingShares.asStateFlow()

    /** Arcs filtered by current query — search by name and aliases only (no N+1 item fetch) */
    private val _filteredArcs: StateFlow<List<Arc>> = combine(_arcs, _query) { arcs, q ->
        if (q.isBlank()) arcs
        else {
            val qLow = q.trim().lowercase()
            arcs.filter { arc ->
                arc.name.lowercase().contains(qLow) ||
                arc.aliases.lowercase().split(",").any { it.trim().contains(qLow) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val officialArcs: StateFlow<List<Arc>> = combine(_filteredArcs, _userId) { arcs, _ ->
        arcs.filter { it.owner_id == null }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalArcs: StateFlow<List<Arc>> = combine(_filteredArcs, _userId) { arcs, uid ->
        arcs.filter { it.owner_id != null && it.owner_id == uid }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        load()
    }

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            val uid = userRepository.getLocalUserId()
            _userId.value = uid
            _arcs.value = arcRepository.getPublishedArcs(forceRefresh)
            if (uid != null) {
                _pendingShares.value = arcRepository.getPendingShares(uid)
            }
            _isLoading.value = false
        }
    }

    fun createPersonalArc(name: String, description: String, aliases: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val uid = userRepository.getLocalUserId() ?: return@launch
            val newId = java.util.UUID.randomUUID().toString()
            val arc = Arc(
                id = newId,
                name = name.trim(),
                description = description.trim(),
                aliases = aliases.trim(),
                is_published = false,
                owner_id = uid
            )
            arcRepository.createArc(arc)
            load(forceRefresh = true)
            onDone(newId)
        }
    }

    fun acceptShare(share: ArcShare) {
        viewModelScope.launch {
            arcRepository.acceptShare(share)
            load(forceRefresh = true)
        }
    }

    fun rejectShare(shareId: String) {
        viewModelScope.launch {
            arcRepository.rejectShare(shareId)
            load(forceRefresh = true)
        }
    }

    fun setQuery(q: String) { _query.value = q }

    class Factory(
        private val arcRepository: ArcRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            ArcsViewModel(arcRepository, userRepository) as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArcsScreen(
    arcRepository: ArcRepository,
    userRepository: UserRepository,
    onArcClick: (String) -> Unit
) {
    val vm: ArcsViewModel = viewModel(factory = ArcsViewModel.Factory(arcRepository, userRepository))
    val officialArcs by vm.officialArcs.collectAsStateWithLifecycle()
    val personalArcs by vm.personalArcs.collectAsStateWithLifecycle()
    val pendingShares by vm.pendingShares.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showInboxSheet by remember { mutableStateOf(false) }

    val currentArcs = if (selectedTabIndex == 0) officialArcs else personalArcs

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Arcs", style = MaterialTheme.typography.headlineSmall, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
                actions = {
                    BadgedBox(
                        badge = {
                            if (pendingShares.isNotEmpty()) {
                                Badge(containerColor = AccentBlue) {
                                    Text(pendingShares.size.toString(), color = Background)
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        IconButton(onClick = { showInboxSheet = true }) {
                            Icon(Icons.Default.Inbox, contentDescription = "Inbox", tint = TextPrimary)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 1) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = AccentBlue,
                    contentColor = Background
                ) {
                    Icon(Icons.Default.Add, "New Arc")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Background,
                contentColor = AccentBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = AccentBlue
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Official", color = if (selectedTabIndex == 0) AccentBlue else TextTertiary) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Personal", color = if (selectedTabIndex == 1) AccentBlue else TextTertiary) }
                )
            }

            // Search bar
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search franchises, titles...", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = SurfaceHighlight,
                    focusedBorderColor = AccentBlue,
                    unfocusedContainerColor = SurfaceContainer,
                    focusedContainerColor = SurfaceContainer,
                    cursorColor = AccentBlue,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            var isRefreshing by remember { mutableStateOf(false) }
            LaunchedEffect(isLoading) {
                if (!isLoading) isRefreshing = false
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    vm.load(forceRefresh = true)
                },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    isLoading && !isRefreshing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentBlue)
                    }
                    currentArcs.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Collections, contentDescription = null,
                                tint = TextTertiary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (query.isBlank()) "No arcs available yet" else "No results for \"$query\"",
                                color = TextTertiary, style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(currentArcs, key = { it.id }) { arc ->
                            ArcCard(arc = arc, onClick = { onArcClick(arc.id) })
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePersonalArcDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, aliases ->
                vm.createPersonalArc(name, desc, aliases) { newId ->
                    showCreateDialog = false
                    onArcClick(newId)
                }
            }
        )
    }

    if (showInboxSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInboxSheet = false },
            containerColor = SurfaceContainer
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Arc Inbox", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                if (pendingShares.isEmpty()) {
                    Text("No pending arcs.", color = TextTertiary, modifier = Modifier.padding(vertical = 24.dp))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(pendingShares) { share ->
                            ShareInboxRow(
                                share = share,
                                onAccept = { vm.acceptShare(share) },
                                onReject = { vm.rejectShare(share.id) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ArcCard(arc: Arc, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainer)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover image
        if (!arc.cover_url.isNullOrBlank()) {
            AsyncImage(
                model = arc.cover_url,
                contentDescription = arc.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 60.dp, height = 80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceElevated)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = arc.name.take(2).uppercase(),
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                arc.name,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (arc.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    arc.description,
                    color = TextTertiary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (arc.owner_id != null) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Person, null, tint = AccentBlue.copy(alpha = 0.7f), modifier = Modifier.size(11.dp))
                    Text("Personal", color = AccentBlue.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun CreatePersonalArcDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String, aliases: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        title = { Text("New Personal Arc", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = TextTertiary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = SurfaceHighlight
                    )
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (Optional)", color = TextTertiary) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = SurfaceHighlight
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, desc, "") },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, disabledContainerColor = SurfaceHighlight)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextTertiary) }
        }
    )
}

@Composable
private fun ShareInboxRow(
    share: ArcShare,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceHighlight)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(share.arc_name.ifBlank { "Shared Arc" }, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("From: ${share.sender_username.ifBlank { share.sender_id.take(8) }}", color = TextTertiary, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onAccept,
                modifier = Modifier.background(AccentBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Check, "Accept", tint = AccentBlue)
            }
            IconButton(
                onClick = onReject,
                modifier = Modifier.background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Close, "Reject", tint = Color.Red)
            }
        }
    }
}
