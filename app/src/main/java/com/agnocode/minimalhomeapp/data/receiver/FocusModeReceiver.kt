package com.agnocode.minimalhomeapp.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.agnocode.minimalhomeapp.data.AppRepository
import com.agnocode.minimalhomeapp.data.FocusModeScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FocusModeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: AppRepository

    @Inject
    lateinit var scheduler: FocusModeScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val modeName = intent.getStringExtra(EXTRA_MODE_NAME)
        val isStarting = intent.getBooleanEffect(EXTRA_IS_STARTING, true)
        
        Log.d("FocusModeReceiver", "Received trigger for mode: $modeName, starting: $isStarting")

        CoroutineScope(Dispatchers.IO).launch {
            if (isStarting) {
                repository.setActiveFocusMode(modeName)
            } else {
                // If ending, only deactivate if the current active mode matches
                // This prevents a late "end" alarm for an old mode from killing a new active mode
                repository.toggleActiveModeIfMatches(modeName)
            }
            // Schedule the next one
            scheduler.scheduleNext()
        }
    }

    private fun Intent.getBooleanEffect(key: String, defaultValue: Boolean): Boolean {
        return getBooleanExtra(key, defaultValue)
    }

    companion object {
        const val ACTION_FOCUS_MODE_TRIGGER = "com.agnocode.minimalhomeapp.ACTION_FOCUS_MODE_TRIGGER"
        const val EXTRA_MODE_NAME = "extra_mode_name"
        const val EXTRA_IS_STARTING = "extra_is_starting"
    }
}
