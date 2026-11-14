package id.adzan.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object AlarmScheduler {
    private const val REQUEST_CODE = 1001
    const val ACTION_ADZAN = "id.adzan.widget.ACTION_ADZAN"
    const val EXTRA_LABEL = "label"
    const val EXTRA_TIME = "time"
    const val EXTRA_EPOCH = "epoch"
    const val EXTRA_ID = "nid"

    private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun canExact(c: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    /** Jadwalkan satu alarm untuk waktu sholat berikutnya setelah [after]. */
    fun scheduleNext(c: Context, after: ZonedDateTime = ZonedDateTime.now()) {
        val next = PrayerLogic.next(c, after) ?: return
        val am = c.getSystemService(AlarmManager::class.java)
        val at = next.time.toInstant().toEpochMilli()

        val intent = Intent(c, AdzanReceiver::class.java).apply {
            action = ACTION_ADZAN
            putExtra(EXTRA_LABEL, next.prayer.label)
            putExtra(EXTRA_TIME, next.time.format(HHMM))
            putExtra(EXTRA_EPOCH, at)
            putExtra(EXTRA_ID, 100 + next.prayer.ordinal)
        }
        val pi = PendingIntent.getBroadcast(
            c, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canExact(c)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                // Fallback: izin alarm tepat waktu belum diberikan → bisa meleset beberapa menit.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }
}
