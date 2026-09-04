package com.agnocode.minimalhomeapp.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.agnocode.minimalhomeapp.data.model.AppItem
import com.agnocode.minimalhomeapp.util.SmartAction
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
private val dateFmt = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())

@Composable
fun HomeView(
    favorites: List<AppItem>,
    searchResults: List<AppItem>,
    isSearchActive: Boolean,
    searchQuery: String,
    tasksCount: Int,
    isVisible: Boolean = true,
    showFavorites: Boolean = true,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onBlock: (String, Long?) -> Unit,
    onTasksClick: () -> Unit = {},
    protectedPackages: Set<String> = emptySet(),
    onToggleProtected: (String) -> Unit = {},
    onProtectedLaunch: (() -> Unit) -> Unit = { it() },
    getIcon: (String) -> android.graphics.drawable.Drawable? = { null },
    usageStats: Map<String, Long> = emptyMap(),
    usageMode: String = "none",
    isMonochrome: Boolean = false,
    smartAction: SmartAction? = null,
    noteResults: List<com.agnocode.minimalhomeapp.data.local.entities.NoteEntity> = emptyList(),
    onNoteClick: (String) -> Unit = {},
    onSearch: () -> Unit = {},
    showIcons: Boolean = false,
    iconPackPackage: String? = null
) {
    var time by remember { mutableStateOf(currentTime()) }
    var showDashboard by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    val nestedScrollConnection = remember(isSearchActive) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // When search is active and we pull down at the top of the list, exit search
                if (isSearchActive && available.y > 10f && source == NestedScrollSource.UserInput) {
                    onSearchToggle(false)
                    onSearchQueryChange("")
                    return available
                }
                return Offset.Zero
            }
        }
    }
    
    if (isSearchActive) {
        BackHandler {
            onSearchToggle(false)
            onSearchQueryChange("")
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }
    
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

    LaunchedEffect(isVisible) {
        if (isVisible) {
            while (true) {
                time = currentTime()
                val nextSecond = 1000 - (System.currentTimeMillis() % 1000)
                delay(nextSecond.milliseconds)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .nestedScroll(nestedScrollConnection)
            .pointerInput(isSearchActive) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 10f) {
                        if (isSearchActive) {
                            onSearchToggle(false)
                            onSearchQueryChange("")
                        } else {
                            expandNotifications()
                        }
                    } else if (dragAmount < -10f) {
                        if (!isSearchActive) {
                            onSearchToggle(true)
                        }
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
            if (!isSearchActive) {
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

                if (tasksCount > 0) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "$tasksCount task(s) remaining",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onTasksClick() }
                    )
                }

                Spacer(Modifier.height(32.dp))

                if (showFavorites && favorites.isNotEmpty()) {
                    Text(
                        text = "Favorites",
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    favorites.forEach { app ->
                        val usageMillis = usageStats[app.packageName] ?: 0L
                        val usageSubtitle = when (usageMode) {
                            "time" -> if (usageMillis > 0) formatUsageTime(usageMillis) else null
                            "percentage" -> if (usageMillis > 0) formatUsagePercentage(usageMillis) else null
                            else -> null
                        }
                        
                        AppListItem(
                            app = app,
                            isFavorite = true,
                            onToggleFavorite = { onRemoveFavorite(app.packageName) },
                            onBlock = { duration -> onBlock(app.packageName, duration) },
                            isProtected = protectedPackages.contains(app.packageName),
                            onToggleProtected = { onToggleProtected(app.packageName) },
                            onProtectedLaunch = onProtectedLaunch,
                            iconOverride = getIcon(app.packageName),
                            usageSubtitle = usageSubtitle,
                            isMonochrome = isMonochrome,
                            showIcon = showIcons,
                            iconPackPackage = iconPackPackage
                        )
                    }
                }
            } else {
                // Universal Search UI
                Spacer(Modifier.height(32.dp))
                
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = LocalTextStyle.current.copy(
                        color = Color.White,
                        fontSize = 24.sp
                    ),
                    cursorBrush = SolidColor(Color.White),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    decorationBox = { innerTextField ->
                        Box {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Search anything...",
                                    color = Color.DarkGray,
                                    fontSize = 24.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                
                Spacer(Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (smartAction != null) {
                        item {
                            SmartResultItem(
                                action = smartAction,
                                onClick = { 
                                    onSearchToggle(false)
                                    onSearchQueryChange("")
                                }
                            )
                        }
                    }

                    items(searchResults) { app ->
                        val usageMillis = usageStats[app.packageName] ?: 0L
                        val usageSubtitle = when (usageMode) {
                            "time" -> if (usageMillis > 0) formatUsageTime(usageMillis) else null
                            "percentage" -> if (usageMillis > 0) formatUsagePercentage(usageMillis) else null
                            else -> null
                        }
                        
                        AppListItem(
                            app = app,
                            isFavorite = false,
                            onToggleFavorite = {}, // Not needed here
                            onBlock = { duration -> onBlock(app.packageName, duration) },
                            isProtected = protectedPackages.contains(app.packageName),
                            onToggleProtected = { onToggleProtected(app.packageName) },
                            onProtectedLaunch = onProtectedLaunch,
                            iconOverride = getIcon(app.packageName),
                            usageSubtitle = usageSubtitle,
                            isMonochrome = isMonochrome,
                            showIcon = showIcons,
                            iconPackPackage = iconPackPackage
                        )
                    }

                    if (noteResults.isNotEmpty()) {
                        item {
                            Text(
                                "Notes",
                                color = Color.DarkGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(noteResults) { note ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        onNoteClick(note.date)
                                        onSearchToggle(false)
                                        onSearchQueryChange("")
                                    }
                                    .padding(vertical = 8.dp),
                                color = Color.Transparent
                            ) {
                                Column {
                                    Text(note.date, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        note.content.take(50).replace("\n", " ") + "...",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    
                    if (searchQuery.isNotEmpty()) {
                        item {
                            Text(
                                text = "Search web for \"$searchQuery\"",
                                color = Color.LightGray,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com/search?q=$searchQuery"))
                                            context.startActivity(intent)
                                            onSearchToggle(false)
                                            onSearchQueryChange("")
                                        } catch (e: Exception) {
                                            Log.e("HomeApp", "Could not open browser", e)
                                        }
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }

        if (!isSearchActive) {
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
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("HomeApp", "Could not open calendar app", e)
                            }
                        },
                        onLongClick = {
                            showDashboard = true
                        }
                    )
            )

            if (showDashboard) {
                YearDashboard(onDismiss = { showDashboard = false })
            }
        }
    }
}

@Composable
fun YearDashboard(onDismiss: () -> Unit) {
    val calendar = Calendar.getInstance()
    val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
    val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
    val daysLeft = totalDays - dayOfYear
    val progress = (dayOfYear.toFloat() / totalDays * 100).toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        title = {
            Text(
                text = "${calendar.get(Calendar.YEAR)} Overview",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "You have lived $progress% of this year.",
                    color = Color.LightGray,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "$daysLeft days remaining to make it count.",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Gray)
            }
        }
    )
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

private fun formatUsageTime(millis: Long): String {
    val mins = (millis / 60000)
    val hrs = mins / 60
    return if (hrs > 0) "${hrs}h ${mins % 60}m today" else "${mins}m today"
}

private fun formatUsagePercentage(millis: Long): String {
    val awakeMillis = 16 * 60 * 60 * 1000L // Assume 16 hours awake
    val pct = (millis.toFloat() / awakeMillis * 100).toInt()
    return if (pct > 0) "$pct% of your day" else "< 1% of your day"
}
