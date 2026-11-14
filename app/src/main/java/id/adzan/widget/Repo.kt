package id.adzan.widget

import android.content.Context
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate

data class Outcome(val ok: Boolean, val msg: String)

/** Akses API MyQuran v2 (sumber: Kemenag RI). Semua fungsi di sini BLOKIR — panggil dari thread latar. */
object Repo {
    private const val BASE = "https://api.myquran.com/v2/sholat"
    private val KEYS = listOf("subuh", "dzuhur", "ashar", "maghrib", "isya")
    private val HHMM = Regex("^\\d{2}:\\d{2}$")

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    fun pretty(s: String): String =
        s.lowercase().split(" ").joinToString(" ") { w -> w.replaceFirstChar { ch -> ch.uppercase() } }

    /** Cari kota lewat kota/cari/{nama}. Mengembalikan pasangan (id, nama) atau null jika tidak ada. */
    fun resolveCity(query: String): Pair<String, String>? {
        val q = query.trim()
        val enc = URLEncoder.encode(q, "UTF-8").replace("+", "%20")
        val json = JSONObject(httpGet("$BASE/kota/cari/$enc"))
        val arr = json.optJSONArray("data") ?: return null
        if (arr.length() == 0) return null
        var best: JSONObject? = null
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val name = o.optString("lokasi")
            if (name.startsWith("KOTA", ignoreCase = true) && name.contains(q, ignoreCase = true)) {
                best = o
                break
            }
        }
        val o = best ?: arr.getJSONObject(0)
        val id = o.optString("id")
        val name = o.optString("lokasi")
        if (id.isEmpty()) return null
        return Pair(id, name)
    }

    /** Ambil jadwal satu hari lewat jadwal/{id}/{tahun}/{bulan}/{tanggal}. */
    fun fetchDay(id: String, date: LocalDate): List<String> {
        val url = String.format("%s/jadwal/%s/%d/%02d/%02d", BASE, id, date.year, date.monthValue, date.dayOfMonth)
        val json = JSONObject(httpGet(url))
        if (!json.optBoolean("status", false)) throw IOException("Data tidak tersedia")
        val jadwal = json.getJSONObject("data").getJSONObject("jadwal")
        val times = KEYS.map { jadwal.getString(it).trim() }
        if (times.any { !HHMM.matches(it) }) throw IOException("Format waktu tidak dikenali")
        return times
    }

    fun needsFetch(c: Context): Boolean {
        if (Store.locId(c) == null) return true
        val today = LocalDate.now(Store.zone(c))
        return !Store.hasDay(c, today) || !Store.hasDay(c, today.plusDays(1))
    }

    /** Unduh jadwal hari ini dan besok untuk lokasi tersimpan. */
    fun refresh(c: Context): Outcome {
        return try {
            var id = Store.locId(c)
            if (id == null) {
                val q = Store.query(c)
                val city = resolveCity(q) ?: return Outcome(false, "Kota \"$q\" tidak ditemukan")
                Store.saveLocation(c, q, city.first, pretty(city.second), Store.tzId(c))
                id = city.first
            }
            val today = LocalDate.now(Store.zone(c))
            var okCount = 0
            var lastError = ""
            for (d in 0..1) {
                val date = today.plusDays(d.toLong())
                try {
                    Store.saveDay(c, date, fetchDay(id, date))
                    okCount++
                } catch (e: Exception) {
                    lastError = e.message ?: e.javaClass.simpleName
                }
            }
            Store.prune(c, today.minusDays(7))
            when (okCount) {
                2 -> Outcome(true, "Jadwal hari ini dan besok diperbarui")
                1 -> Outcome(true, "Sebagian jadwal diperbarui ($lastError)")
                else -> Outcome(false, "Gagal memuat jadwal: $lastError. Memakai data tersimpan.")
            }
        } catch (e: Exception) {
            Outcome(false, "Gagal memuat jadwal: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /** Simpan lokasi baru (cari kota dulu), lalu unduh jadwalnya. */
    fun saveLocation(c: Context, query: String, tzId: String): Outcome {
        return try {
            val city = resolveCity(query) ?: return Outcome(false, "Kota \"$query\" tidak ditemukan")
            Store.saveLocation(c, query.trim(), city.first, pretty(city.second), tzId)
            Store.clearDays(c)
            refresh(c)
        } catch (e: Exception) {
            Outcome(false, "Gagal mencari kota: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}
