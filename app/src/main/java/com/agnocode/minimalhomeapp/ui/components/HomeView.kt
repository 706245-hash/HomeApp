package com.agnocode.minimalhomeapp.ui.components

import android.content.Intent
import android.provider.AlarmClock
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agnocode.minimalhomeapp.data.model.AppItem
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
private val dateFmt = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())

@Composable
fun HomeView(
    favorites: List<AppItem>,
    onRemoveFavorite: (String) -> Unit,
    onBlock: (String, Long?) -> Unit
) {
    var time by remember { mutableStateOf(currentTime()) }
    val context = LocalContext.current
    
    fun expandNotifications() {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val method = statusBarManager.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
        } catch (e: Exception) {
            Log.e("HomeApp", "Failed to expand notifications panel", e)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            time = currentTime()
            val nextSecond = 1000 - (System.currentTimeMillis() % 1000)
            delay(nextSecond.milliseconds)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 20f) {
                        expandNotifications()
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(80.dp))

            Column(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    try {
                        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("HomeApp", "Could not open clock app", e)
                    }
                }
            ) {
                Text(
                    text = time.first,
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = time.second,
                    color = Color.Gray,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.height(64.dp))

            if (favorites.isNotEmpty()) {
                Text(
                    text = "Favorites",
                    color = Color.DarkGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                favorites.forEach { app ->
                    AppListItem(
                        app = app,
                        isFavorite = true,
                        onToggleFavorite = { onRemoveFavorite(app.packageName) },
                        onBlock = { duration -> onBlock(app.packageName, duration) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                val intent = Intent(Intent.ACTION_DIAL)
                context.startActivity(intent)
            }) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Phone",
                    tint = Color.DarkGray
                )
            }
            IconButton(onClick = {
                val intent = Intent("android.media.action.STILL_IMAGE_CAMERA")
                context.startActivity(intent)
            }) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Camera",
                    tint = Color.DarkGray
                )
            }
        }

        YearProgressBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        )
    }
}

@Composable
fun YearProgressBar(modifier: Modifier = Modifier) {
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(12) { month ->
            val passedProgress = when {
                month < currentMonth -> 1f
                month == currentMonth -> currentDay.toFloat() / daysInMonth
                else -> 0f
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.White)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(passedProgress)
                        .fillMaxHeight()
                        .background(Color.Gray)
                )
            }
        }
    }
}

fun currentTime(): Pair<String, String> {
    val now = Date()
    return timeFmt.format(now) to dateFmt.format(now)
}
