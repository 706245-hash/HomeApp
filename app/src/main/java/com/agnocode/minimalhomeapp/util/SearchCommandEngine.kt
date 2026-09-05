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

        // 1. Specific Commands (Timer/Alarm) take precedence
        // Timer (timer 5, t 5)
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

        // Alarm (alarm 07:30, a 07:30)
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

        // 2. Math Evaluation (Robust Parser)
        if (isPotentialMath(trimmed)) {
            try {
                val result = evaluateMath(trimmed)
                if (!result.isNaN() && !result.isInfinite()) {
                    val formattedResult = if (result % 1.0 == 0.0) {
                        result.toLong().toString()
                    } else {
                        "%.4f".format(result).trimEnd('0').trimEnd('.')
                    }
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
            } catch (_: Exception) {}
        }

        // 3. Greetings / Quotes
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

    private fun isPotentialMath(s: String): Boolean {
        // Must contain at least one digit
        if (!s.any { it.isDigit() }) return false
        // Must contain at least one operator or parenthesis
        val operators = setOf('+', '-', '*', '/', '^', '(', ')')
        if (!s.any { it in operators }) return false
        // Only allow math-related characters
        return s.all { it.isDigit() || it == '.' || it in operators || it.isWhitespace() || it.isLetter() }
    }

    private fun evaluateMath(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = str.substring(startPos, pos).toDouble()
                } else if (ch >= 'a'.code && ch <= 'z'.code) {
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    val func = str.substring(startPos, pos)
                    x = parseFactor()
                    x = when (func) {
                        "sqrt" -> kotlin.math.sqrt(x)
                        "sin" -> kotlin.math.sin(x * kotlin.math.PI / 180.0)
                        "cos" -> kotlin.math.cos(x * kotlin.math.PI / 180.0)
                        "tan" -> kotlin.math.tan(x * kotlin.math.PI / 180.0)
                        else -> throw RuntimeException("Unknown function: $func")
                    }
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }

                if (eat('^'.code)) {
                    val exponent = parseFactor()
                    x = Math.pow(x, exponent)
                }

                return x
            }
        }.parse()
    }
}
