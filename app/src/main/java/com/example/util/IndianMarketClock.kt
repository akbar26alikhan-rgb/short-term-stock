package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class MarketStatusInfo(
    val isOpen: Boolean,
    val isPreOpen: Boolean,
    val isWeekend: Boolean,
    val statusText: String,
    val istTimeFormatted: String,
    val adviceText: String
)

object IndianMarketClock {

    private val kolkataTz = TimeZone.getTimeZone("Asia/Kolkata")

    fun getMarketStatus(): MarketStatusInfo {
        val calendar = Calendar.getInstance(kolkataTz)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val totalMinutes = hour * 60 + minute

        val timeFormat = SimpleDateFormat("dd MMM, hh:mm a 'IST'", Locale.ENGLISH)
        timeFormat.timeZone = kolkataTz
        val istTimeStr = timeFormat.format(calendar.time)

        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

        if (isWeekend) {
            return MarketStatusInfo(
                isOpen = false,
                isPreOpen = false,
                isWeekend = true,
                statusText = "Market Closed (Weekend)",
                istTimeFormatted = istTimeStr,
                adviceText = "NSE is closed for the weekend. Exchange Bhavcopy closing data is ready for weekend swing analysis."
            )
        }

        // Trading hours: 9:15 AM (555 min) to 3:30 PM (930 min)
        // Pre-open: 9:00 AM (540 min) to 9:15 AM (555 min)
        return when {
            totalMinutes in 540..554 -> {
                MarketStatusInfo(
                    isOpen = false,
                    isPreOpen = true,
                    isWeekend = false,
                    statusText = "Market Pre-Open Session",
                    istTimeFormatted = istTimeStr,
                    adviceText = "NSE pre-open session active (09:00 - 09:15 IST). Collecting overnight news and opening orders."
                )
            }
            totalMinutes in 555..930 -> {
                MarketStatusInfo(
                    isOpen = true,
                    isPreOpen = false,
                    isWeekend = false,
                    statusText = "Market Open (Live Session)",
                    istTimeFormatted = istTimeStr,
                    adviceText = "Live NSE market session in progress (09:15 - 15:30 IST). Collecting live prices."
                )
            }
            else -> {
                MarketStatusInfo(
                    isOpen = false,
                    isPreOpen = false,
                    isWeekend = false,
                    statusText = "Market Closed for Today",
                    istTimeFormatted = istTimeStr,
                    adviceText = "NSE is closed for today. Official closing data & Bhavcopy settlement prices are available from the exchange."
                )
            }
        }
    }
}
