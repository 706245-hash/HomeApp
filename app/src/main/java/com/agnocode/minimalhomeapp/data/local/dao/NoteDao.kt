package com.agnocode.minimalhomeapp.data.local.dao

import androidx.room.*
import com.agnocode.minimalhomeapp.data.local.entities.NoteEntity
import com.agnocode.minimalhomeapp.data.local.entities.NoteWithTasks
import com.agnocode.minimalhomeapp.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Transaction
    @Query("SELECT * FROM notes ORDER BY date DESC")
    fun getAllNotesWithTasks(): Flow<List<NoteWithTasks>>

    @Query("SELECT * FROM notes ORDER BY date DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE date = :date")
    suspend fun getNoteByDate(date: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Query("SELECT * FROM tasks WHERE noteDate = :date ORDER BY `order` ASC")
    fun getTasksForNote(date: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Transaction
    suspend fun saveNoteWithTasks(note: NoteEntity, tasks: List<TaskEntity>) {
        insertNote(note)
        // For simplicity in this migration, we replace tasks for the date
        // In a real app, you might want to sync instead of wipe/re-insert
        // But since the current DataStore logic is "save all", this is equivalent.
        deleteTasksForNote(note.date)
        tasks.forEach { insertTask(it) }
    }

    @Query("DELETE FROM tasks WHERE noteDate = :date")
    suspend fun deleteTasksForNote(date: String)
}
