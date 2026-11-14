package id.adzan.widget

import android.content.Context
import java.time.ZonedDateTime

enum class Prayer(val label: String) {
    SUBUH("Subuh"),
    DZUHUR("Dzuhur"),
    ASHAR("Ashar"),
    MAGHRIB("Maghrib"),
    ISYA("Isya")
}

data class PrayerTime(val prayer: Prayer, val time: ZonedDateTime, val tomorrow: Boolean)

object PrayerLogic {
    /** Waktu sholat pertama yang jatuh setelah [after] (hari ini atau besok). */
    fun next(c: Context, after: ZonedDateTime): PrayerTime? {
        val zone = Store.zone(c)
        val now = after.withZoneSameInstant(zone)
        val today = now.toLocalDate()
        for (d in 0..1) {
            val date = today.plusDays(d.toLong())
            val times = Store.timesFor(c, date) ?: continue
            for (p in Prayer.values()) {
                val t = ZonedDateTime.of(date, times[p.ordinal], zone)
                if (t.isAfter(now)) return PrayerTime(p, t, d == 1)
            }
        }
        return null
    }
}
