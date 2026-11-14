package id.adzan.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Jadwalkan ulang alarm setelah reboot, update aplikasi, perubahan waktu/zona, atau izin alarm berubah. */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Updater.handle(context.applicationContext, goAsync())
    }
}
