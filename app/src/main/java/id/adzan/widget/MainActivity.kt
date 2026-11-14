package id.adzan.widget

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var etCity: EditText
    private lateinit var rg: RadioGroup
    private val buttons = mutableListOf<Button>()

    private val tzIds = listOf("Asia/Jakarta", "Asia/Makassar", "Asia/Jayapura")
    private val tzLabels = listOf("WIB", "WITA", "WIT")
    private val radioIds = IntArray(3) { View.generateViewId() }
    private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

    private companion object {
        const val REQ_NOTIF = 11
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        refreshStatus()
        // Pertama kali dibuka: muat jadwal untuk kota default.
        if (Store.locId(this) == null) {
            runTask { Repo.saveLocation(applicationContext, etCity.text.toString().trim(), selectedTz()) }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        Thread {
            try {
                Updater.quick(applicationContext)
            } catch (e: Exception) {
                // abaikan
            }
        }.start()
    }

    // ---------------------------------------------------------------- UI

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun label(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(18), 0, dp(4))
    }

    private fun btn(text: String, onClick: () -> Unit): Button = Button(this).apply {
        this.text = text
        isAllCaps = false
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(6) }
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
        }

        root.addView(TextView(this).apply {
            text = "Jadwal Adzan"
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Gratis, tanpa iklan. Sumber data: Kemenag RI melalui MyQuran."
            textSize = 13f
            setPadding(0, dp(4), 0, dp(12))
        })

        status = TextView(this).apply {
            textSize = 14f
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(0x14000000)
            setLineSpacing(0f, 1.15f)
        }
        root.addView(status)

        root.addView(label("Kota / Kabupaten"))
        etCity = EditText(this).apply {
            hint = "mis. Banjarbaru"
            setSingleLine(true)
            setText(Store.query(this@MainActivity))
        }
        root.addView(etCity)

        root.addView(label("Zona waktu kota"))
        rg = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val currentTz = Store.tzId(this)
        for (i in tzIds.indices) {
            rg.addView(RadioButton(this).apply {
                id = radioIds[i]
                text = tzLabels[i]
                isChecked = tzIds[i] == currentTz
            })
        }
        if (rg.checkedRadioButtonId == -1) rg.check(radioIds[1])
        root.addView(rg)

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val save = Button(this).apply {
            text = "Simpan"
            isAllCaps = false
            setOnClickListener { onSave() }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val refresh = Button(this).apply {
            text = "Perbarui"
            isAllCaps = false
            setOnClickListener { onRefresh() }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        buttons.add(save)
        buttons.add(refresh)
        row.addView(save)
        row.addView(refresh)
        root.addView(row)

        root.addView(label("Izin & pengaturan"))
        root.addView(btn("Izin notifikasi") { onNotifPermission() })
        root.addView(btn("Izin alarm tepat waktu") { onExactAlarm() })
        root.addView(btn("Pengaturan aplikasi (Autostart & Baterai)") { onAppSettings() })

        root.addView(label("Uji coba"))
        root.addView(btn("Tes notifikasi") { onTest() })

        root.addView(TextView(this).apply {
            text = "Tambahkan widget: tahan layar utama → Widget → Jadwal Adzan.\n" +
                "Di OriginOS, aktifkan Autostart dan Baterai \"Tanpa batasan\" agar adzan tidak terlewat."
            textSize = 12f
            setPadding(0, dp(20), 0, 0)
        })

        return ScrollView(this).apply { addView(root) }
    }

    // ---------------------------------------------------------------- Aksi

    private fun selectedTz(): String {
        val idx = radioIds.indexOf(rg.checkedRadioButtonId)
        return tzIds[if (idx >= 0) idx else 1]
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun runTask(work: () -> Outcome) {
        buttons.forEach { it.isEnabled = false }
        val app = applicationContext
        Thread {
            val result = try {
                work()
            } catch (e: Exception) {
                Outcome(false, "Terjadi kesalahan: ${e.message ?: e.javaClass.simpleName}")
            }
            try {
                Updater.quick(app)
            } catch (e: Exception) {
                // abaikan
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                buttons.forEach { it.isEnabled = true }
                toast(result.msg)
                refreshStatus()
            }
        }.start()
    }

    private fun onSave() {
        val q = etCity.text.toString().trim()
        if (q.isEmpty()) {
            toast("Isi nama kota terlebih dahulu")
            return
        }
        val tz = selectedTz()
        runTask { Repo.saveLocation(applicationContext, q, tz) }
    }

    private fun onRefresh() {
        runTask { Repo.refresh(applicationContext) }
    }

    private fun hasNotifPermission(): Boolean {
        val granted = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    }

    private fun openNotifSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        )
    }

    private fun onNotifPermission() {
        when {
            hasNotifPermission() -> toast("Izin notifikasi sudah aktif")
            Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ->
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
            else -> openNotifSettings()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NOTIF) {
            if (grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
                toast("Izin ditolak. Aktifkan notifikasi lewat pengaturan.")
                openNotifSettings()
            }
            refreshStatus()
        }
    }

    private fun onExactAlarm() {
        if (AlarmScheduler.canExact(this)) {
            toast("Izin alarm tepat waktu sudah aktif")
            return
        }
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
            )
        } catch (e: Exception) {
            onAppSettings()
        }
    }

    private fun onAppSettings() {
        toast("Aktifkan Autostart/Mulai otomatis, dan Baterai → Tanpa batasan")
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        )
    }

    private fun onTest() {
        val ok = Notifier.show(this, "Tes Adzan", "Notifikasi dan suara berfungsi dengan baik", 99)
        if (!ok) toast("Notifikasi belum diizinkan. Tekan \"Izin notifikasi\".")
    }

    // ---------------------------------------------------------------- Status

    private fun refreshStatus() {
        val zone = Store.zone(this)
        val today = LocalDate.now(zone)
        val tzLabel = tzLabels[tzIds.indexOf(Store.tzId(this)).let { if (it >= 0) it else 1 }]
        val sb = StringBuilder()
        sb.append("Lokasi: ").append(Store.locName(this) ?: "belum dipilih").append('\n')
        sb.append("Zona waktu: ").append(tzLabel).append('\n')

        val times = Store.timesFor(this, today)
        if (times != null) {
            val exact = if (Store.hasDay(this, today)) "" else " (perkiraan, data lama)"
            sb.append("Hari ini$exact:\n")
            for (p in Prayer.values()) {
                sb.append("  ").append(p.label.padEnd(8)).append(times[p.ordinal].format(hhmm)).append('\n')
            }
        } else {
            sb.append("Jadwal belum dimuat. Tekan Simpan/Perbarui.\n")
        }

        val next = PrayerLogic.next(this, java.time.ZonedDateTime.now(zone))
        if (next != null) {
            sb.append("Berikutnya: ").append(next.prayer.label).append(' ')
                .append(next.time.format(hhmm))
            if (next.tomorrow) sb.append(" (besok)")
            sb.append('\n')
        }

        sb.append("Notifikasi: ").append(if (hasNotifPermission()) "diizinkan" else "belum diizinkan").append('\n')
        sb.append("Alarm tepat waktu: ")
            .append(if (AlarmScheduler.canExact(this)) "aktif" else "belum aktif (alarm bisa meleset)").append('\n')
        sb.append("Suara adzan: ")
            .append(if (Notifier.hasCustomSound(this)) "adzan.mp3 (kustom)" else "nada bawaan")
        status.text = sb.toString()
    }
}
