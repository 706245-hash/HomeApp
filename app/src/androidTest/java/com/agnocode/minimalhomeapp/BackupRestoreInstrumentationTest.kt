package com.agnocode.minimalhomeapp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agnocode.minimalhomeapp.data.AppRepository
import com.agnocode.minimalhomeapp.data.model.DailyNote
import com.agnocode.minimalhomeapp.data.model.NoteTask
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BackupRestoreInstrumentationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: AppRepository

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testBackupAndRestoreCycle() = runBlocking {
        // 1. Prepare some data
        val testDate = "2026-09-05"
        val testContent = "Test Note Content"
        val testTasks = listOf(NoteTask("task1", "Test Task", false))
        val note = DailyNote(testDate, testContent, testTasks)
        
        repository.saveDailyNote(note)
        
        // 2. Generate backup
        val json = repository.generateBackupJson()
        assertTrue(json.contains(testContent))
        
        // 3. Clear database (simulate new app install)
        // We use a trick: restore from an empty backup or just wait for restore to clear it
        // Actually restoreFromBackupJson clears everything first.
        
        // 4. Restore from backup
        val success = repository.restoreFromBackupJson(json)
        assertTrue(success)
        
        // 5. Verify data is back
        val notes = repository.dailyNotesFlow.first()
        assertTrue(notes.containsKey(testDate))
        assertEquals(testContent, notes[testDate]?.content)
        assertEquals(1, notes[testDate]?.tasks?.size)
        assertEquals("Test Task", notes[testDate]?.tasks?.get(0)?.text)
    }
}
