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
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusModeScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val focusModeDao: FocusModeDao
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleNext() {
        val modes = focusModeDao.getAllFocusModes().first()
        val scheduledModes = modes.map { entity ->
            // Note: We'd ideally have the packages here too, but for scheduling we just need times
            FocusMode(entity.name, emptySet(), entity.startTime, entity.endTime)
        }.filter { it.startTime != null && it.endTime != null }

        if (scheduledModes.isEmpty()) {
            cancelCurrentAlarm()
            return
        }

        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        var nextTriggerTime: Calendar? = null
        var nextModeName: String? = null
        var isStarting = true

        scheduledModes.forEach { mode ->
            val start = mode.startTime!!
            val end = mode.endTime!!

            // Calculate when this mode starts today
            val startCal = getCalendarForMinutes(start)
            if (startCal.before(now)) startCal.add(Calendar.DAY_OF_YEAR, 1)

            // Calculate when this mode ends today
            val endCal = getCalendarForMinutes(end)
            if (endCal.before(now)) endCal.add(Calendar.DAY_OF_YEAR, 1)

            // Check start
            if (nextTriggerTime == null || startCal.before(nextTriggerTime)) {
                nextTriggerTime = startCal
                nextModeName = mode.name
                isStarting = true
            }

            // Check end
            if (nextTriggerTime == null || endCal.before(nextTriggerTime)) {
                nextTriggerTime = endCal
                nextModeName = mode.name
                isStarting = false
            }
        }

        nextTriggerTime?.let { triggerTime ->
            Log.d("FocusModeScheduler", "Scheduling next alarm for $nextModeName at ${triggerTime.time}, starting: $isStarting")
            
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

            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime.timeInMillis,
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

    private fun getCalendarForMinutes(minutes: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
