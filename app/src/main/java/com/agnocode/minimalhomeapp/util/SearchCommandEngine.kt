package com.agnocode.minimalhomeapp.util

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class SmartAction(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onAction: (Context) -> Unit
)

object SearchCommandEngine {
    private val quotes = listOf(
        "Focus on being productive instead of busy.",
        "Your mind is for having ideas, not holding them.",
        "Simplicity is the ultimate sophistication.",
        "Done is better than perfect.",
        "The best way to predict the future is to create it.",
        "Minimalism is not subtraction, it is focus."
    )

    fun parse(query: String): SmartAction? {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return null

        // 1. Math Evaluation (Simple: 1 + 1)
        val mathRegex = Regex("^(\\d+(\\.\\d+)?)\\s*([+\\-*/])\\s*(\\d+(\\.\\d+)?)$")
        mathRegex.find(trimmed)?.let { match ->
            val v1 = match.groupValues[1].toDouble()
            val op = match.groupValues[3]
            val v2 = match.groupValues[4].toDouble()
            val result = when (op) {
                "+" -> v1 + v2
                "-" -> v1 - v2
                "*" -> v1 * v2
                "/" -> if (v2 != 0.0) v1 / v2 else Double.NaN
                else -> null
            }
            if (result != null) {
                val formattedResult = if (result % 1.0 == 0.0) result.toInt().toString() else "%.2f".format(result)
                return SmartAction(
                    icon = Icons.Default.Calculate,
                    title = "Result: $formattedResult",
                    subtitle = "Tap to copy",
                    onAction = { context ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Calculation", formattedResult))
                    }
                )
            }
        }

        // 2. Timer (timer 5, t 5)
        val timerRegex = Regex("^(timer|t)\\s+(\\d+)$")
        timerRegex.find(trimmed)?.let { match ->
            val mins = match.groupValues[2].toInt()
            return SmartAction(
                icon = Icons.Default.Timer,
                title = "Set $mins-minute timer",
                subtitle = "Will open system clock",
                onAction = { context ->
                    val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                        putExtra(AlarmClock.EXTRA_LENGTH, mins * 60)
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    }
                    context.startActivity(intent)
                }
            )
        }

        // 3. Alarm (alarm 07:30, a 07:30)
        val alarmRegex = Regex("^(alarm|a)\\s+(\\d{1,2})[:.](\\d{2})$")
        alarmRegex.find(trimmed)?.let { match ->
            val hour = match.groupValues[2].toInt()
            val min = match.groupValues[3].toInt()
            if (hour in 0..23 && min in 0..59) {
                return SmartAction(
                    icon = Icons.Default.Alarm,
                    title = "Set alarm for %02d:%02d".format(hour, min),
                    subtitle = "Will open system clock",
                    onAction = { context ->
                        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                            putExtra(AlarmClock.EXTRA_HOUR, hour)
                            putExtra(AlarmClock.EXTRA_MINUTES, min)
                            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }

        // 4. Greetings / Quotes
        if (trimmed == "hi" || trimmed == "hello") {
            return SmartAction(
                icon = Icons.Default.WavingHand,
                title = "Hello there!",
                subtitle = "Stay focused today.",
                onAction = {}
            )
        }
        
        if (trimmed == "quote" || trimmed == "inspire") {
            return SmartAction(
                icon = Icons.Default.FormatQuote,
                title = quotes.random(),
                subtitle = "Today's Inspiration",
                onAction = {}
            )
        }

        return null
    }
}
