package id.adzan.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val rv = WidgetUpdater.build(context)
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, rv)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            // Pastikan alarm tetap terjadwal dan data hari ini/besok tersedia.
            Updater.handle(context.applicationContext, goAsync())
        }
    }
}
