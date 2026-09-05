package com.agnocode.minimalhomeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agnocode.minimalhomeapp.data.model.DailyNote
import com.agnocode.minimalhomeapp.data.model.NoteTask
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesView(
    date: String,
    noteText: String,
    tasks: List<NoteTask>,
    weeklyProductivity: List<Float>,
    allDailyNotes: Map<String, DailyNote>,
    isEditingPastNote: Boolean,
    onDateSelect: (String) -> Unit,
    onNoteTextChange: (String) -> Unit,
    onAddTask: () -> Unit,
    onUpdateTaskText: (String, String) -> Unit,
    onToggleTask: (String, Boolean) -> Unit,
    onDeleteTask: (String) -> Unit,
    onToggleEditPastNote: () -> Unit,
    onSavePastNote: () -> Unit,
    onUndoPastNote: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val utcDateFmt = remember { 
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { 
            timeZone = TimeZone.getTimeZone("UTC") 
        } 
    }
    val today = remember { dateFmt.format(Date()) }
    val isPast = date < today
    val canEdit = !isPast || isEditingPastNote
    var showUnlockConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showWeeklyDashboard by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
    ) {
        // Top Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (date != today) {
                    IconButton(
                        onClick = { onDateSelect(today) },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Today", tint = Color.Gray)
                    }
                }
                Text(
                    text = if (date == today) "Today's Note" else date,
                    color = if (isPast && !isEditingPastNote) Color.Gray else MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPast) {
                    if (isEditingPastNote) {
                        TextButton(onClick = onSavePastNote) {
                            Text("SAVE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onUndoPastNote) {
                            Text("UNDO", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(onClick = { showUnlockConfirm = true }) {
                            Text(
                                text = "EDIT",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.History, contentDescription = "History", tint = Color.Gray)
                }
                
                if (canEdit) {
                    IconButton(onClick = onAddTask) {
                        Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color.White)
                    }
                }
            }
        }

        WeeklyProductivitySparkline(
            weeklyProductivity = weeklyProductivity,
            onLongClick = { showWeeklyDashboard = true }
        )
        
        val linkedDates = remember(noteText) {
            val regex = Regex("\\[\\[(\\d{4}-\\d{2}-\\d{2})]]")
            regex.findAll(noteText).map { it.groupValues[1] }.distinct().toList()
        }
        
        if (linkedDates.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                linkedDates.forEach { linkedDate ->
                    Surface(
                        modifier = Modifier.clickable { onDateSelect(linkedDate) },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = linkedDate,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        DayProgressBar()
        
        Spacer(Modifier.height(16.dp))

        // Main Note Area
        BasicTextField(
            value = noteText,
            onValueChange = onNoteTextChange,
            readOnly = !canEdit,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(
                color = if (canEdit) Color.White else Color.Gray,
                fontSize = 18.sp
            ),
            cursorBrush = SolidColor(Color.White),
            decorationBox = { innerTextField ->
                Box {
                    if (noteText.isEmpty()) {
                        Text("Type your thoughts here...", color = Color.DarkGray, fontSize = 18.sp)
                    }
                    innerTextField()
                }
            }
        )

        // Bottom Tasks Area
        if (tasks.isNotEmpty()) {
            HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(tasks) { task ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isChecked,
                            onCheckedChange = { if (canEdit) onToggleTask(task.id, it) },
                            enabled = canEdit,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = Color.DarkGray,
                                checkmarkColor = Color.Black
                            )
                        )
                        BasicTextField(
                            value = task.text,
                            onValueChange = { onUpdateTaskText(task.id, it) },
                            readOnly = !canEdit,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            textStyle = LocalTextStyle.current.copy(
                                color = when {
                                    task.isChecked -> Color.Gray
                                    !canEdit -> Color.Gray.copy(alpha = 0.6f)
                                    else -> Color.White
                                },
                                fontSize = 16.sp
                            ),
                            cursorBrush = SolidColor(Color.White),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (task.text.isEmpty()) {
                                        Text("Type your task here...", color = Color.DarkGray, fontSize = 16.sp)
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        if (canEdit) {
                            IconButton(onClick = { taskToDelete = task.id }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Task",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUnlockConfirm) {
        AlertDialog(
            onDismissRequest = { showUnlockConfirm = false },
            title = { Text("Edit History?", color = Color.White) },
            text = { Text("Modifying the past can change your insights. Proceed?", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    onToggleEditPastNote()
                    showUnlockConfirm = false
                }) {
                    Text("UNLOCK", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockConfirm = false }) {
                    Text("CANCEL", color = Color.Gray)
                }
            },
            containerColor = Color.Black
        )
    }

    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task?", color = Color.White) },
            text = { Text("This action cannot be undone.", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    taskToDelete?.let { onDeleteTask(it) }
                    taskToDelete = null
                }) {
                    Text("DELETE", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("CANCEL", color = Color.Gray)
                }
            },
            containerColor = Color.Black
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateFmt.parse(date)?.let {
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val localCal = Calendar.getInstance().apply { time = it }
                cal.set(
                    localCal.get(Calendar.YEAR),
                    localCal.get(Calendar.MONTH),
                    localCal.get(Calendar.DAY_OF_MONTH),
                    0, 0, 0
                )
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            },
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    cal.timeInMillis = utcTimeMillis
                    val d = utcDateFmt.format(cal.time)
                    return d == today || allDailyNotes.containsKey(d)
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        cal.timeInMillis = it
                        onDateSelect(utcDateFmt.format(cal.time))
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("CANCEL", color = Color.Gray)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color.Black)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    headlineContentColor = Color.White,
                    weekdayContentColor = Color.Gray,
                    subheadContentColor = Color.Gray,
                    yearContentColor = Color.Gray,
                    currentYearContentColor = Color.White,
                    selectedYearContentColor = Color.Black,
                    selectedYearContainerColor = Color.White,
                    dayContentColor = Color.White,
                    disabledDayContentColor = Color.DarkGray,
                    selectedDayContentColor = Color.Black,
                    selectedDayContainerColor = Color.White,
                    todayContentColor = Color.White,
                    todayDateBorderColor = Color.White
                )
            )
        }
    }

    if (showWeeklyDashboard) {
        WeeklyDashboard(
            weeklyProductivity = weeklyProductivity,
            allDailyNotes = allDailyNotes,
            onDismiss = { showWeeklyDashboard = false }
        )
    }
}

@Composable
fun WeeklyProductivitySparkline(
    weeklyProductivity: List<Float>,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(vertical = 8.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onLongClick
            ),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        weeklyProductivity.forEach { progress ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(progress.coerceIn(0.05f, 1f))
                    .background(if (progress >= 1f) MaterialTheme.colorScheme.primary else Color.DarkGray)
            )
        }
    }
}

@Composable
fun WeeklyDashboard(
    weeklyProductivity: List<Float>,
    allDailyNotes: Map<String, DailyNote>,
    onDismiss: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dayFmt = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    
    val weekData = remember {
        (0..6).map { i ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateStr = dateFmt.format(cal.time)
            val dayLabel = dayFmt.format(cal.time)
            val note = allDailyNotes[dateStr]
            Triple(dayLabel, note?.tasks?.count { it.isChecked } ?: 0, note?.tasks?.size ?: 0)
        }.reversed()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        title = {
            Text(
                text = "Weekly Performance",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val avgProgress = (weeklyProductivity.average() * 100).toInt()
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "Average Completion: ",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        "$avgProgress%",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                weekData.forEach { (day, done, total) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(day, color = Color.Gray, modifier = Modifier.width(40.dp))
                        LinearProgressIndicator(
                            progress = { if (total > 0) done.toFloat() / total else 0f },
                            modifier = Modifier.weight(1f).height(4.dp).padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.DarkGray,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Text(
                            text = if (total > 0) "$done/$total" else "-",
                            color = Color.White,
                            modifier = Modifier.width(40.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Color.White)
            }
        },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun DayProgressBar() {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val totalMinutesInDay = 24 * 60
    val elapsedMinutes = hour * 60 + minute
    val progress = elapsedMinutes.toFloat() / totalMinutesInDay

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(MaterialTheme.colorScheme.primary) // Remaining time (subtle)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress) // Passed time
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.2f))
        )
    }
}
