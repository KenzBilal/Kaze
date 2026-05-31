package com.kaze.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaze.model.FilterOption
import com.kaze.model.SortFilterState
import com.kaze.model.SortOption
import com.kaze.ui.theme.*

@Composable
fun SortFilterSheet(
    state: SortFilterState,
    onSortChange: (SortOption) -> Unit,
    onFilterChange: (FilterOption) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // Sheet handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.width(40.dp).height(4.dp),
                shape = MaterialTheme.shapes.extraSmall,
                color = SurfaceBorder
            ) {}
        }

        SheetSection(title = "SORT")

        SortOption.entries.forEach { option ->
            SheetRow(
                label = option.label,
                selected = state.sort == option,
                onClick = { onSortChange(option); onDismiss() }
            )
        }

        Spacer(Modifier.height(16.dp))
        SubtleDivider(modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(16.dp))

        SheetSection(title = "FILTER")

        FilterOption.entries.forEach { option ->
            SheetRow(
                label = option.label,
                selected = state.filter == option,
                onClick = { onFilterChange(option); onDismiss() }
            )
        }
    }
}

@Composable
private fun SheetSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = TextTertiary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
    )
}

@Composable
private fun SheetRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) TextPrimary else TextSecondary
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
