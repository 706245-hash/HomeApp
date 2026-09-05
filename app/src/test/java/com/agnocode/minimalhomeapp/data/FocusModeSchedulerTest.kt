package com.agnocode.minimalhomeapp.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.agnocode.minimalhomeapp.data.local.dao.FocusModeDao
import com.agnocode.minimalhomeapp.data.local.entities.FocusModeEntity
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class FocusModeSchedulerTest {

    private val context = mockk<Context>(relaxed = true)
    private val dao = mockk<FocusModeDao>()
    private val alarmManager = mockk<AlarmManager>(relaxed = true)
    
    // Fixed clock at 2026-09-05 10:00:00 UTC
    private val clock = Clock.fixed(Instant.parse("2026-09-05T10:00:00Z"), ZoneId.of("UTC"))

    @Before
    fun setup() {
        mockkStatic(Log::class)
        mockkStatic(PendingIntent::class)
        mockkConstructor(Intent::class)
        
        every { Log.d(any(), any()) } returns 0
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        
        val mockPendingIntent = mockk<PendingIntent>()
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns mockPendingIntent
        
        every { anyConstructed<Intent>().setAction(any()) } returns mockk()
        every { anyConstructed<Intent>().putExtra(any<String>(), any<String>()) } returns mockk()
        every { anyConstructed<Intent>().putExtra(any<String>(), any<Boolean>()) } returns mockk()
    }

    @Test
    fun `scheduleNext sets alarm for mode starting later today`() = runTest {
        val scheduler = FocusModeScheduler(context, dao, clock)
        val modes = listOf(
            FocusModeEntity("Work", 660, 1020) // 11:00 - 17:00
        )
        every { dao.getAllFocusModes() } returns flowOf(modes)

        scheduler.scheduleNext()

        // 11:00 UTC on 2026-09-05 is 1757070000000 ms roughly
        // 2026-09-05T11:00:00Z
        val expectedTime = Instant.parse("2026-09-05T11:00:00Z").toEpochMilli()
        
        verify {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                expectedTime,
                any()
            )
        }
    }

    @Test
    fun `scheduleNext sets alarm for mode ending later today`() = runTest {
        // Current time is 10:00. Mode is 09:00 - 17:00.
        // It should schedule the END at 17:00.
        val scheduler = FocusModeScheduler(context, dao, clock)
        val modes = listOf(
            FocusModeEntity("Work", 540, 1020) // 09:00 - 17:00
        )
        every { dao.getAllFocusModes() } returns flowOf(modes)

        scheduler.scheduleNext()

        val expectedTime = Instant.parse("2026-09-05T17:00:00Z").toEpochMilli()
        
        verify {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                expectedTime,
                any()
            )
        }
    }

    @Test
    fun `scheduleNext sets alarm for tomorrow if mode already passed today`() = runTest {
        // Current time is 10:00. Mode is 07:00 - 08:00.
        // It should schedule the START for tomorrow at 07:00.
        val scheduler = FocusModeScheduler(context, dao, clock)
        val modes = listOf(
            FocusModeEntity("Early", 420, 480) // 07:00 - 08:00
        )
        every { dao.getAllFocusModes() } returns flowOf(modes)

        scheduler.scheduleNext()

        val expectedTime = Instant.parse("2026-09-06T07:00:00Z").toEpochMilli()
        
        verify {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                expectedTime,
                any()
            )
        }
    }
}
