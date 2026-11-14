package id.adzan.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Dipanggil AlarmManager tepat pada waktu sholat. */
class AdzanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL)
        val time = intent.getStringExtra(AlarmScheduler.EXTRA_TIME) ?: ""
        val epoch = intent.getLongExtra(AlarmScheduler.EXTRA_EPOCH, 0L)
        val nid = intent.getIntExtra(AlarmScheduler.EXTRA_ID, 100)

        // Cegah alarm yang sama terjadwal ulang jika bunyi sedikit lebih awal.
        val now = ZonedDateTime.now()
        val fired = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault()).plusSeconds(1)
        val after = if (fired.isAfter(now)) fired else now

        Updater.handle(app, goAsync(), after) {
            if (label != null) {
                val loc = Store.locName(app) ?: ""
                Notifier.show(app, "Waktu $label", "Saatnya sholat $label · $time $loc".trim(), nid)
            }
        }
    }
}
