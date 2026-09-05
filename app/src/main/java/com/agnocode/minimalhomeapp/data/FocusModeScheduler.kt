package com.agnocode.minimalhomeapp.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.agnocode.minimalhomeapp.data.local.dao.FocusModeDao
import com.agnocode.minimalhomeapp.data.model.FocusMode
import com.agnocode.minimalhomeapp.data.receiver.FocusModeReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusModeScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val focusModeDao: FocusModeDao,
    private val clock: Clock
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleNext() {
        val modes = focusModeDao.getAllFocusModes().first()
        val scheduledModes = modes.map { entity ->
            FocusMode(entity.name, emptySet(), entity.startTime, entity.endTime)
        }.filter { it.startTime != null && it.endTime != null }

        if (scheduledModes.isEmpty()) {
            cancelCurrentAlarm()
            return
        }

        val now = LocalDateTime.now(clock)
        var nextTriggerTime: LocalDateTime? = null
        var nextModeName: String? = null
        var isStarting = true

        scheduledModes.forEach { mode ->
            val start = mode.startTime!!
            val end = mode.endTime!!

            var startDt = now.withHour(start / 60).withMinute(start % 60).withSecond(0).withNano(0)
            if (startDt.isBefore(now)) startDt = startDt.plusDays(1)

            var endDt = now.withHour(end / 60).withMinute(end % 60).withSecond(0).withNano(0)
            if (endDt.isBefore(now)) endDt = endDt.plusDays(1)

            // Check start
            if (nextTriggerTime == null || startDt.isBefore(nextTriggerTime)) {
                nextTriggerTime = startDt
                nextModeName = mode.name
                isStarting = true
            }

            // Check end
            if (nextTriggerTime == null || endDt.isBefore(nextTriggerTime)) {
                nextTriggerTime = endDt
                nextModeName = mode.name
                isStarting = false
            }
        }

        nextTriggerTime?.let { triggerTime ->
            Log.d("FocusModeScheduler", "Scheduling next alarm for $nextModeName at $triggerTime, starting: $isStarting")
            
            val intent = Intent(context, FocusModeReceiver::class.java).apply {
                action = FocusModeReceiver.ACTION_FOCUS_MODE_TRIGGER
                putExtra(FocusModeReceiver.EXTRA_MODE_NAME, nextModeName)
                putExtra(FocusModeReceiver.EXTRA_IS_STARTING, isStarting)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerMillis = triggerTime.atZone(clock.zone).toInstant().toEpochMilli()

            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    private fun cancelCurrentAlarm() {
        val intent = Intent(context, FocusModeReceiver::class.java).apply {
            action = FocusModeReceiver.ACTION_FOCUS_MODE_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}
