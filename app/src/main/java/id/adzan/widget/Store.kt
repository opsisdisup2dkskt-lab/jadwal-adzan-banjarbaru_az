package id.adzan.widget

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Penyimpanan lokal (SharedPreferences) agar jadwal tetap tampil saat offline. */
object Store {
    private const val FILE = "adzan_prefs"
    private const val DAY = "day_"
    const val DEFAULT_QUERY = "Banjarbaru"
    const val DEFAULT_TZ = "Asia/Makassar" // WITA

    fun sp(c: Context): SharedPreferences =
        c.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun query(c: Context): String = sp(c).getString("query", DEFAULT_QUERY) ?: DEFAULT_QUERY
    fun locId(c: Context): String? = sp(c).getString("loc_id", null)
    fun locName(c: Context): String? = sp(c).getString("loc_name", null)
    fun tzId(c: Context): String = sp(c).getString("tz", DEFAULT_TZ) ?: DEFAULT_TZ

    fun zone(c: Context): ZoneId = try {
        ZoneId.of(tzId(c))
    } catch (e: Exception) {
        ZoneId.of(DEFAULT_TZ)
    }

    fun saveLocation(c: Context, query: String, id: String, name: String, tz: String) {
        sp(c).edit()
            .putString("query", query)
            .putString("loc_id", id)
            .putString("loc_name", name)
            .putString("tz", tz)
            .commit()
    }

    fun saveDay(c: Context, date: LocalDate, times: List<String>) {
        sp(c).edit().putString(DAY + date.toString(), times.joinToString(",")).commit()
    }

    fun hasDay(c: Context, date: LocalDate): Boolean = sp(c).contains(DAY + date.toString())

    private fun parse(s: String?): List<LocalTime>? {
        if (s == null) return null
        return try {
            val list = s.split(",").map { LocalTime.parse(it.trim()) }
            if (list.size == 5) list else null
        } catch (e: Exception) {
            null
        }
    }

    private fun dayKeys(c: Context): List<LocalDate> =
        sp(c).all.keys
            .filter { it.startsWith(DAY) }
            .mapNotNull {
                try {
                    LocalDate.parse(it.removePrefix(DAY))
                } catch (e: Exception) {
                    null
                }
            }
            .sorted()

    /**
     * Waktu sholat untuk tanggal tertentu. Jika tanggal itu belum pernah diunduh
     * (misalnya offline beberapa hari), pakai data tersimpan terdekat sebagai perkiraan.
     */
    fun timesFor(c: Context, date: LocalDate): List<LocalTime>? {
        parse(sp(c).getString(DAY + date.toString(), null))?.let { return it }
        val keys = dayKeys(c)
        if (keys.isEmpty()) return null
        val pick = keys.lastOrNull { !it.isAfter(date) } ?: keys.first()
        return parse(sp(c).getString(DAY + pick.toString(), null))
    }

    fun clearDays(c: Context) {
        val e = sp(c).edit()
        sp(c).all.keys.filter { it.startsWith(DAY) }.forEach { e.remove(it) }
        e.commit()
    }

    fun prune(c: Context, before: LocalDate) {
        val e = sp(c).edit()
        dayKeys(c).filter { it.isBefore(before) }.forEach { e.remove(DAY + it.toString()) }
        e.commit()
    }
}
