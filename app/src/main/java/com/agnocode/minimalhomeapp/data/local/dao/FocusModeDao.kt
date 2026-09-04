package com.agnocode.minimalhomeapp.data.local.dao

import androidx.room.*
import com.agnocode.minimalhomeapp.data.local.entities.FocusModeEntity
import com.agnocode.minimalhomeapp.data.local.entities.FocusModePackageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusModeDao {
    @Query("SELECT * FROM focus_modes")
    fun getAllFocusModes(): Flow<List<FocusModeEntity>>

    @Query("SELECT packageName FROM focus_mode_packages WHERE modeName = :modeName")
    fun getPackagesForMode(modeName: String): Flow<List<String>>

    @Query("SELECT * FROM focus_mode_packages")
    suspend fun getAllPackagesRaw(): List<FocusModePackageEntity>

    @Query("SELECT * FROM focus_modes")
    suspend fun getAllFocusModesRaw(): List<FocusModeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusMode(mode: FocusModeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(pkg: FocusModePackageEntity)

    @Query("DELETE FROM focus_modes WHERE name = :name")
    suspend fun deleteFocusMode(name: String)

    @Query("DELETE FROM focus_mode_packages WHERE modeName = :modeName")
    suspend fun deletePackagesForMode(modeName: String)

    @Query("DELETE FROM focus_modes")
    suspend fun deleteAllFocusModes()

    @Query("DELETE FROM focus_mode_packages")
    suspend fun deleteAllPackages()

    @Transaction
    suspend fun saveFocusMode(mode: FocusModeEntity, packages: Set<String>) {
        insertFocusMode(mode)
        deletePackagesForMode(mode.name)
        packages.forEach { 
            insertPackage(FocusModePackageEntity(mode.name, it))
        }
    }
}
