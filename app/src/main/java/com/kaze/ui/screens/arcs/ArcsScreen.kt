package com.kaze.ui.screens.arcs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import com.kaze.data.repository.ArcItem
import com.kaze.data.repository.ArcRepository
import com.kaze.data.repository.UserRepository
import com.kaze.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ArcsViewModel(
    private val arcRepository: ArcRepository
) : ViewModel() {

    private val _arcs = MutableStateFlow<List<Arc>>(emptyList())
    private val _arcItems = MutableStateFlow<Map<String, List<ArcItem>>>(emptyMap())
    private val _query = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(true)

    val query: StateFlow<String> = _query.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val filteredArcs: StateFlow<List<Arc>> = MutableStateFlow<List<Arc>>(emptyList()).also { flow ->
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(_arcs, _query, _arcItems) { arcs, q, items ->
                arcRepository.searchArcs(q, arcs, items)
            }.collect { flow.value = it }
        }
    }

    // Make filteredArcs derivable
    private val _filteredArcs = MutableStateFlow<List<Arc>>(emptyList())
    val visibleArcs: StateFlow<List<Arc>> = _filteredArcs.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(_arcs, _query, _arcItems) { arcs, q, items ->
                arcRepository.searchArcs(q, arcs, items)
            }.collect { _filteredArcs.value = it }
        }
    }

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            val arcs = arcRepository.getPublishedArcs(forceRefresh)
            _arcs.value = arcs
            // Pre-load items for each arc for search
            val itemsMap = mutableMapOf<String, List<ArcItem>>()
            arcs.forEach { arc ->
                val (_, items) = arcRepository.getArcWithItems(arc.id, forceRefresh)
                itemsMap[arc.id] = items
            }
            _arcItems.value = itemsMap
            _isLoading.value = false
        }
    }

    fun setQuery(q: String) { _query.value = q }

    class Factory(private val arcRepository: ArcRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            ArcsViewModel(arcRepository) as T
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
    val vm: ArcsViewModel = viewModel(factory = ArcsViewModel.Factory(arcRepository))
    val arcs by vm.visibleArcs.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Arcs", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
                arcs.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(arcs, key = { it.id }) { arc ->
                        ArcCard(arc = arc, onClick = { onArcClick(arc.id) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
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
        }
    }
}
