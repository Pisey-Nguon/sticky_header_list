package com.dsi.sticky_header_list

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    /**
     * Normalizes a Date to midnight (00:00:00) of that day.
     * This ensures all dates on the same day are grouped together.
     */
    fun normalizeToDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    /**
     * Converts a Date object to a display format:
     * - "Today" for current date
     * - "Yesterday" for previous date
     * - "dd/MM/yyyy" for older dates
     */
    fun formatDateForHeader(outputPattern:String, date: Date,titleToday:String,titleYesterday: String): String {
        val outputFormat = SimpleDateFormat(outputPattern, Locale.getDefault())
        val calendar = Calendar.getInstance().apply { time = date }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }

        return when {
            isSameDay(calendar, today) -> titleToday
            isSameDay(calendar, yesterday) -> titleYesterday
            else -> outputFormat.format(date)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
