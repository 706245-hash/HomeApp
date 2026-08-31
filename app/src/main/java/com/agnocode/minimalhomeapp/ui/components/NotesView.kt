package com.agnocode.minimalhomeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agnocode.minimalhomeapp.data.model.NoteTask
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NotesView(
    date: String,
    availableDates: List<String>,
    noteText: String,
    tasks: List<NoteTask>,
    onDateSelect: (String) -> Unit,
    onNoteTextChange: (String) -> Unit,
    onAddTask: () -> Unit,
    onUpdateTaskText: (String, String) -> Unit,
    onToggleTask: (String, Boolean) -> Unit,
    onDeleteTask: (String) -> Unit
) {
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
            val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
            Text(
                text = if (date == today) "Daily Note" else date,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                var showHistoryMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showHistoryMenu = true }) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = showHistoryMenu,
                        onDismissRequest = { showHistoryMenu = false },
                        modifier = Modifier
                            .background(Color.Black)
                            .border(1.dp, Color.White, MaterialTheme.shapes.extraSmall)
                    ) {
                        availableDates.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d, color = Color.White) },
                                onClick = {
                                    onDateSelect(d)
                                    showHistoryMenu = false
                                }
                            )
                        }
                    }
                }
                
                IconButton(onClick = onAddTask) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color.White)
                }
            }
        }

        DayProgressBar()
        
        Spacer(Modifier.height(16.dp))

        // Main Note Area
        BasicTextField(
            value = noteText,
            onValueChange = onNoteTextChange,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
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
                            onCheckedChange = { onToggleTask(task.id, it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color.Gray,
                                uncheckedColor = Color.DarkGray,
                                checkmarkColor = Color.Black
                            )
                        )
                        BasicTextField(
                            value = task.text,
                            onValueChange = { onUpdateTaskText(task.id, it) },
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            textStyle = TextStyle(
                                color = if (task.isChecked) Color.Gray else Color.White,
                                fontSize = 16.sp
                            ),
                            cursorBrush = SolidColor(Color.White),
                            singleLine = true
                        )
                        IconButton(onClick = { onDeleteTask(task.id) }) {
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
            .background(Color.White) // Remaining time (lighter)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress) // Passed time
                .fillMaxHeight()
                .background(Color.DarkGray)
        )
    }
}
