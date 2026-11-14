package id.adzan.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri

object Notifier {
    /** Suara adzan kustom: taruh file di app/src/main/res/raw/adzan.mp3 */
    fun hasCustomSound(c: Context): Boolean =
        c.resources.getIdentifier("adzan", "raw", c.packageName) != 0

    // Suara channel tidak bisa diubah setelah dibuat, jadi id dibedakan menurut jenis suara.
    private fun channelId(c: Context) = if (hasCustomSound(c)) "adzan_custom" else "adzan_default"

    private fun soundUri(c: Context): Uri =
        if (hasCustomSound(c)) {
            val resId = c.resources.getIdentifier("adzan", "raw", c.packageName)
            Uri.parse("android.resource://${c.packageName}/$resId")
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

    private fun ensureChannel(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        val id = channelId(c)
        if (nm.getNotificationChannel(id) != null) return
        val ch = NotificationChannel(id, "Waktu Sholat", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Pengingat tepat waktu untuk lima waktu sholat"
            setSound(
                soundUri(c),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(ch)
    }

    /** Mengembalikan false jika notifikasi tidak diizinkan. */
    fun show(c: Context, title: String, text: String, id: Int): Boolean {
        val nm = c.getSystemService(NotificationManager::class.java)
        if (!nm.areNotificationsEnabled()) return false
        ensureChannel(c)
        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(c, channelId(c))
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(id, n)
        return true
    }
}
