package com.agnocode.minimalhomeapp.ui

import com.agnocode.minimalhomeapp.data.AppRepository
import com.agnocode.minimalhomeapp.data.model.DailyNote
import com.agnocode.minimalhomeapp.data.model.NoteTask
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

    private val repository = mockk<AppRepository>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    
    // Fixed clock at 2026-09-05 10:00:00 UTC
    private val clock = Clock.fixed(Instant.parse("2026-09-05T10:00:00Z"), ZoneId.of("UTC"))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.dailyNotesFlow } returns flowOf(emptyMap())
    }

    @Test
    fun `weeklyProductivity calculates correctly when no notes exist`() {
        val viewModel = NotesViewModel(repository, clock)
        
        val productivity = viewModel.weeklyProductivity.value
        assertEquals(7, productivity.size)
        productivity.forEach { assertEquals(0f, it) }
    }

    @Test
    fun `weeklyProductivity calculates correctly with mixed completion`() {
        val notes = mapOf(
            "2026-09-05" to DailyNote("2026-09-05", "", listOf(
                NoteTask("1", "T1", true),
                NoteTask("2", "T2", false)
            )), // 50%
            "2026-09-04" to DailyNote("2026-09-04", "", listOf(
                NoteTask("3", "T3", true)
            )) // 100%
        )
        every { repository.dailyNotesFlow } returns flowOf(notes)
        
        val viewModel = NotesViewModel(repository, clock)
        
        val productivity = viewModel.weeklyProductivity.value
        // Today is 09-05 (last element in reversed list)
        assertEquals(0.5f, productivity.last())
        assertEquals(1.0f, productivity[ productivity.size - 2 ])
        assertEquals(0f, productivity[0])
    }

    @Test
    fun `hasIncompleteTasks is true when today has unchecked tasks`() {
        val viewModel = NotesViewModel(repository, clock)
        viewModel.currentTasks.add(NoteTask("1", "Task", false))
        
        assertEquals(true, viewModel.hasIncompleteTasks.value)
    }

    @Test
    fun `hasIncompleteTasks is false when today has only checked tasks`() {
        val viewModel = NotesViewModel(repository, clock)
        viewModel.currentTasks.add(NoteTask("1", "Task", true))
        
        assertEquals(false, viewModel.hasIncompleteTasks.value)
    }
}
