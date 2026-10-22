package com.example.moneymanager.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object FormatUtils {
    private val formatters = ConcurrentHashMap<Pair<String, Locale>, DateTimeFormatter>()

    private fun getFormatter(pattern: String, locale: Locale = Locale.getDefault()): DateTimeFormatter {
        return formatters.getOrPut(pattern to locale) {
            DateTimeFormatter.ofPattern(pattern, locale)
        }
    }

    fun formatTime(timestamp: Long): String = format(timestamp, getFormatter("HH:mm"))
    fun formatDay(timestamp: Long): String = format(timestamp, getFormatter("dd"))
    fun formatDayOfWeek(timestamp: Long): String = format(timestamp, getFormatter("EEE"))
    fun formatMonthYear(timestamp: Long): String = format(timestamp, getFormatter("MM.yyyy"))
    fun formatFullMonthYear(timestamp: Long): String = format(timestamp, getFormatter("MMMM yyyy"))
    fun formatShortMonthYear(timestamp: Long): String = format(timestamp, getFormatter("MMM yyyy"))

    private fun format(timestamp: Long, formatter: DateTimeFormatter): String {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(formatter)
    }
}
