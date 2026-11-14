package id.adzan.widget

import android.content.BroadcastReceiver
import android.content.Context
import java.time.ZonedDateTime

/** Pekerjaan latar yang dipakai bersama oleh semua receiver. */
object Updater {
    fun quick(c: Context, after: ZonedDateTime = ZonedDateTime.now()) {
        AlarmScheduler.scheduleNext(c, after)
        WidgetUpdater.updateAll(c)
    }

    fun fetchIfNeeded(c: Context) {
        if (Repo.needsFetch(c)) {
            val r = Repo.refresh(c)
            if (r.ok) quick(c)
        }
    }

    /**
     * Jalankan di thread: [notify] (opsional) → jadwalkan alarm berikutnya + segarkan widget →
     * selesaikan broadcast → baru unduh data jika perlu (jaringan bisa lambat).
     */
    fun handle(
        c: Context,
        pending: BroadcastReceiver.PendingResult?,
        after: ZonedDateTime = ZonedDateTime.now(),
        notify: (() -> Unit)? = null
    ) {
        val app = c.applicationContext
        Thread {
            try {
                notify?.invoke()
            } catch (e: Exception) {
                // abaikan
            }
            try {
                quick(app, after)
            } catch (e: Exception) {
                // abaikan
            } finally {
                pending?.finish()
            }
            try {
                fetchIfNeeded(app)
            } catch (e: Exception) {
                // abaikan
            }
        }.start()
    }
}
