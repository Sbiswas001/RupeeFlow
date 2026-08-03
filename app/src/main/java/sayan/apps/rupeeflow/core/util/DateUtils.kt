package sayan.apps.rupeeflow.core.util

import java.util.*

object DateUtils {
    private val indianLocale = Locale("en", "IN")
    
    /**
     * Returns the start and end timestamps for the current Indian Financial Year (Apr 1 - Mar 31).
     */
    fun getCurrentFinancialYearRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        val startYear = if (currentMonth >= Calendar.APRIL) currentYear else currentYear - 1
        
        val startCalendar = Calendar.getInstance().apply {
            set(startYear, Calendar.APRIL, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val endCalendar = Calendar.getInstance().apply {
            set(startYear + 1, Calendar.MARCH, 31, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        
        return Pair(startCalendar.timeInMillis, endCalendar.timeInMillis)
    }

    /**
     * Returns a string representing the Financial Year (e.g., FY 2024-25)
     */
    fun getFinancialYearLabel(): String {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        val startYear = if (currentMonth >= Calendar.APRIL) currentYear else currentYear - 1
        val endYearShort = (startYear + 1) % 100
        
        return "FY $startYear-${String.format(indianLocale, "%02d", endYearShort)}"
    }

    /**
     * Returns the number of days remaining in the current month, including today.
     */
    fun getDaysRemainingInMonth(): Int {
        val calendar = Calendar.getInstance()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        return daysInMonth - currentDay + 1
    }

    fun getStartOfMonth(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getEndOfMonth(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}
