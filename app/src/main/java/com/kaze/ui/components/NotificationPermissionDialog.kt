package com.kaze.ui.components

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun NotificationPermissionHandler() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = LocalContext.current
        
        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            )
        }
        
        val prefs = context.getSharedPreferences("kaze_prefs", Context.MODE_PRIVATE)
        var hasAsked by remember { mutableStateOf(prefs.getBoolean("has_asked_notifications", false)) }
        var showDialog by remember { mutableStateOf(!hasPermission && !hasAsked) }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                hasPermission = isGranted
                showDialog = false
                prefs.edit().putBoolean("has_asked_notifications", true).apply()
            }
        )

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showDialog = false
                    prefs.edit().putBoolean("has_asked_notifications", true).apply()
                },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Stay Updated",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        text = "Get notified when friends interact with your watchlists or share Arcs with you. We keep it minimal and noise-free.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Allow Notifications", modifier = Modifier.padding(vertical = 4.dp))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showDialog = false
                            prefs.edit().putBoolean("has_asked_notifications", true).apply()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Not Now")
                    }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            )
        }
    }
}
