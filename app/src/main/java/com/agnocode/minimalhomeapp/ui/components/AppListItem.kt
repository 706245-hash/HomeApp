package com.agnocode.minimalhomeapp.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.agnocode.minimalhomeapp.data.model.AppItem

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
            modifier = Modifier
                .background(Color.Black)
                .border(1.dp, Color.White, MaterialTheme.shapes.extraSmall)
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
            modifier = Modifier
                .background(Color.Black)
                .border(1.dp, Color.White, MaterialTheme.shapes.extraSmall)
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
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(1.dp, Color.White, RoundedCornerShape(8.dp)),
            color = Color.Black,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    "Block Duration",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    "Minutes",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                
                OutlinedTextField(
                    value = minutesInput,
                    onValueChange = { 
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            minutesInput = it 
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(color = Color.White, fontSize = 24.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.DarkGray,
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 30, 60).forEach { mins ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val current = minutesInput.toIntOrNull() ?: 0
                                    minutesInput = (current + mins).toString()
                                },
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, Color.Gray)
                        ) {
                            Text(
                                "+${mins}m",
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = Color.Gray)
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val mins = minutesInput.toIntOrNull() ?: 0
                            if (mins > 0) onConfirm(mins)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        Text("BLOCK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
