package com.agnocode.minimalhomeapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.agnocode.minimalhomeapp.data.local.dao.FocusModeDao
import com.agnocode.minimalhomeapp.data.local.dao.NoteDao
import com.agnocode.minimalhomeapp.data.local.entities.FocusModeEntity
import com.agnocode.minimalhomeapp.data.local.entities.FocusModePackageEntity
import com.agnocode.minimalhomeapp.data.local.entities.NoteEntity
import com.agnocode.minimalhomeapp.data.local.entities.TaskEntity

@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        FocusModeEntity::class,
        FocusModePackageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun focusModeDao(): FocusModeDao
}
