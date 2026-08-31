package com.agnocode.minimalhomeapp.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agnocode.minimalhomeapp.data.model.AppItem
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    app: AppItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBlock: (Long?) -> Unit,
    showIcon: Boolean = false,
    iconPackPackage: String? = null
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var showMenu by remember { mutableStateOf(false) }
    var showBlockMenu by remember { mutableStateOf(false) }
    var showCustomBlockDialog by remember { mutableStateOf(false) }

    val appIcon = remember(app.packageName, showIcon, iconPackPackage) {
        if (showIcon) {
            try {
                // If iconPackPackage is set, we SHOULD load from there.
                // For now, let's just load the default system icon.
                pm.getApplicationIcon(app.packageName)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                    launchIntent?.let { context.startActivity(it) }
                },
                onLongClick = {
                    showMenu = true
                }
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = app.label,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(Color.DarkGray)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                        color = Color.White
                    )
                },
                onClick = {
                    onToggleFavorite()
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Block App", color = Color.White) },
                onClick = {
                    showBlockMenu = true
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("App Info", color = Color.White) },
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", app.packageName, null)
                    )
                    context.startActivity(intent)
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Uninstall", color = Color.White) },
                onClick = {
                    val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
                    context.startActivity(intent)
                    showMenu = false
                }
            )
        }

        DropdownMenu(
            expanded = showBlockMenu,
            onDismissRequest = { showBlockMenu = false },
            modifier = Modifier.background(Color.DarkGray)
        ) {
            val now = System.currentTimeMillis()
            listOf(
                "15 minutes" to 15 * 60 * 1000L,
                "1 hour" to 60 * 60 * 1000L,
                "8 hours" to 8 * 60 * 60 * 1000L,
                "24 hours" to 24 * 60 * 60 * 1000L
            ).forEach { (label, duration) ->
                DropdownMenuItem(
                    text = { Text(label, color = Color.White) },
                    onClick = {
                        onBlock(now + duration)
                        showBlockMenu = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Custom duration...", color = Color.White) },
                onClick = {
                    showCustomBlockDialog = true
                    showBlockMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Until unblocked", color = Color.White) },
                onClick = {
                    onBlock(null)
                    showBlockMenu = false
                }
            )
        }

        if (showCustomBlockDialog) {
            CustomBlockDialog(
                onDismiss = { showCustomBlockDialog = false },
                onConfirm = { minutes ->
                    onBlock(System.currentTimeMillis() + (minutes * 60 * 1000L))
                    showCustomBlockDialog = false
                }
            )
        }
    }
}

@Composable
fun CustomBlockDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var minutesInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.DarkGray,
        title = {
            Text(
                "Custom Block Duration",
                color = Color.White
            )
        },
        text = {
            Column {
                Text(
                    "Enter duration in minutes (max 9999):",
                    color = Color.LightGray
                )
                OutlinedTextField(
                    value = minutesInput,
                    onValueChange = { 
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            minutesInput = it 
                        }
                    },
                    placeholder = {
                        Text("9999", color = Color.Gray.copy(alpha = 0.5f))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.padding(top = 8.dp),
                    textStyle = TextStyle(color = Color.White)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val mins = minutesInput.toIntOrNull() ?: 0
                    if (mins > 0) onConfirm(mins)
                }
            ) {
                Text("Block", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}
