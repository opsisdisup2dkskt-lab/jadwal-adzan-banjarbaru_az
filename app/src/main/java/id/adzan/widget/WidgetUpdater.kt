package id.adzan.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object WidgetUpdater {
    private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val AMBER = Color.parseColor("#FFB300")
    private val DARK = Color.parseColor("#1A1A1A")
    private val DIM = Color.parseColor("#B0BEC5")

    private val COLS = intArrayOf(R.id.col_subuh, R.id.col_dzuhur, R.id.col_ashar, R.id.col_maghrib, R.id.col_isya)
    private val NAMES = intArrayOf(R.id.nm_subuh, R.id.nm_dzuhur, R.id.nm_ashar, R.id.nm_maghrib, R.id.nm_isya)
    private val TIMES = intArrayOf(R.id.tm_subuh, R.id.tm_dzuhur, R.id.tm_ashar, R.id.tm_maghrib, R.id.tm_isya)

    fun updateAll(c: Context) {
        val mgr = AppWidgetManager.getInstance(c)
        val ids = mgr.getAppWidgetIds(ComponentName(c, PrayerWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val rv = build(c)
        for (id in ids) mgr.updateAppWidget(id, rv)
    }

    fun build(c: Context): RemoteViews {
        val rv = RemoteViews(c.packageName, R.layout.widget_prayer)
        val zone = Store.zone(c)
        val now = ZonedDateTime.now(zone)
        val next = PrayerLogic.next(c, now)

        rv.setTextViewText(R.id.tv_location, Store.locName(c) ?: "Jadwal Adzan")

        if (next == null) {
            rv.setTextViewText(R.id.tv_next, "Buka aplikasi untuk memuat jadwal")
            for (i in COLS.indices) {
                rv.setTextViewText(TIMES[i], "--:--")
                rv.setInt(COLS[i], "setBackgroundResource", 0)
                rv.setTextColor(NAMES[i], DIM)
                rv.setTextColor(TIMES[i], Color.WHITE)
            }
        } else {
            // Tampilkan waktu untuk tanggal yang sama dengan waktu berikutnya (besok jika sudah lewat Isya).
            val showDate = next.time.toLocalDate()
            val times = Store.timesFor(c, showDate)
            val hhmm = next.time.format(HHMM)
            val label = if (next.tomorrow) "${next.prayer.label} $hhmm · besok" else "${next.prayer.label} $hhmm"
            rv.setTextViewText(R.id.tv_next, label)
            for (i in COLS.indices) {
                rv.setTextViewText(TIMES[i], times?.get(i)?.format(HHMM) ?: "--:--")
                if (i == next.prayer.ordinal) {
                    rv.setInt(COLS[i], "setBackgroundResource", R.drawable.bg_next)
                    rv.setTextColor(NAMES[i], DARK)
                    rv.setTextColor(TIMES[i], DARK)
                } else {
                    rv.setInt(COLS[i], "setBackgroundResource", 0)
                    rv.setTextColor(NAMES[i], DIM)
                    rv.setTextColor(TIMES[i], Color.WHITE)
                }
            }
        }

        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        rv.setOnClickPendingIntent(R.id.widget_root, open)
        return rv
    }
}
