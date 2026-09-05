package com.agnocode.minimalhomeapp.data.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.agnocode.minimalhomeapp.data.AppRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DailyBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: AppRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val enabled = repository.autoSyncEnabledFlow.first()
        val uriString = repository.autoSyncUriFlow.first()
        
        if (!enabled || uriString == null) {
            Log.d("DailyBackupWorker", "Auto-sync skipped: enabled=$enabled, uri=$uriString")
            return Result.success()
        }
        
        Log.d("DailyBackupWorker", "Starting auto-sync backup to $uriString")
        return try {
            val uri = Uri.parse(uriString)
            val json = repository.generateBackupJson()
            
            applicationContext.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(json.toByteArray())
            }
            
            Log.d("DailyBackupWorker", "Auto-sync successful")
            Result.success()
        } catch (e: Exception) {
            Log.e("DailyBackupWorker", "Auto-sync failed", e)
            // If it's a security exception, retrying might not help without user action
            if (e is SecurityException) Result.failure() else Result.retry()
        }
    }
}
