package ge.kakha.mynote

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast

// ---- აპის განახლება (Play Store-ის გარეშე) ----
// როგორ მუშაობს (version.json-ის გარეშე):
// 1) აპი გაშვებისას (დღეში ერთხელ) კითხულობს GitHub-ზე მდებარე module.toml-ს.
// 2) თუ იქ versionCode დაყენებულზე მეტია — დიალოგი: „განახლება ხელმისაწვდომია".
// 3) დადასტურებისას APK (ყოველთვის app-debug.apk, რეპოს ძირში) ჩუმად იწერება
//    აპის პირად საქაღალდეში და ინსტალაცია იწყება. ძველი ფაილი თავისით იშლება.
//
// ყოველ ახალ ვერსიაზე: versionCode +1 (module.toml-ში) -> Build APK ->
// APK ატვირთე GitHub-ზე (app-debug.apk-ზე გადაწერა) -> Push (module.toml თავისით მიდის).
object AppUpdate {

    // ვერსიას პირდაპირ module.toml-დან ვკითხულობთ — ცალკე version.json აღარაა საჭირო.
    // module.toml CodeAssist-ის Push-ით თავისით მიდის GitHub-ზე, ამიტომ ვერსია სულ ერთ ადგილას წერია.
    const val MODULE_TOML_URL = "https://raw.githubusercontent.com/gairlink/MyNote/main/app/module.toml"
    // APK ყოველთვის app-debug.apk-ა (CodeAssist რასაც აშენებს) — ამიტომ ბმული ფიქსირებულია
    const val APK_URL = "https://raw.githubusercontent.com/gairlink/MyNote/main/app-debug.apk"

    private const val PREF_LAST_CHECK = "lastUpdateCheck"
    private var downloadId: Long = -1L
    // APK-ის ბმული, რომელიც პარამეტრებში გაგზავნამდე დავიმახსოვრეთ —
    // უკან დაბრუნებისას (onResume) ჩამოტვირთვა თავისით გრძელდება
    private var pendingApkUrl: String? = null

    fun installedVersion(a: MainActivity): Long {
        return try {
            @Suppress("DEPRECATION")
            val info = a.packageManager.getPackageInfo(a.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        } catch (e: Exception) { 0L }
    }

    private fun canInstall(a: MainActivity): Boolean {
        return android.os.Build.VERSION.SDK_INT < 26 || a.packageManager.canRequestPackageInstalls()
    }

    // პარამეტრებიდან („უცნობი წყაროები") დაბრუნებისას იძახება MainActivity.onResume-იდან:
    // თუ ნებართვა უკვე დაშვებულია — ჩამოტვირთვა თავისით იწყება
    fun onResume(a: MainActivity) {
        val url = pendingApkUrl ?: return
        if (canInstall(a)) {
            pendingApkUrl = null
            startDownload(a, url)
        }
    }

    // silent=true  -> დიალოგი მხოლოდ თუ განახლებაა (გაშვებისას, დღეში ერთხელ)
    // silent=false -> მენიუდან ხელით შემოწმება — პასუხი ყოველთვის (Toast)
    fun check(a: MainActivity, silent: Boolean = true) {
        if (silent) {
            val last = a.getSharedPreferences("mynote", 0).getLong(PREF_LAST_CHECK, 0L)
            if (System.currentTimeMillis() - last < 24L * 60 * 60 * 1000) return
        }
        // ძველი განახლების ფაილი არ რჩება — ყოველი შემოწმებისას ვშლით,
        // რომ ტელეფონი არ გაივსოს (ახალი ჩამოტვირთვა ისევ იმავე სახელით მოდის)
        try {
            val old = java.io.File(a.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "mynote-update.apk")
            if (old.exists()) old.delete()
        } catch (e: Exception) { }
        Thread {
            try {
                val conn = java.net.URL(MODULE_TOML_URL + "?t=" + System.currentTimeMillis()).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val text = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                // module.toml-ში: versionCode = 4  (შეიძლება იყოს ბრჭყალებითაც: "4")
                val remoteCode = Regex("versionCode\\s*=\\s*\"?([0-9]+)\"?").find(text)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                val apkUrl = APK_URL
                val notes = ""
                a.runOnUiThread {
                    a.getSharedPreferences("mynote", 0).edit().putLong(PREF_LAST_CHECK, System.currentTimeMillis()).apply()
                    if (remoteCode > installedVersion(a) && apkUrl.isNotEmpty()) {
                        showDialog(a, apkUrl, notes)
                    } else if (!silent) {
                        Toast.makeText(a, Lang.str(a, "upToDate"), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // ხელით შემოწმებისას შეცდომა აღარ იმალება „უახლესია"-ს უკან —
                // რეალურ მიზეზს ვაჩვენებთ, თორემ ვერასდროს გავიგებთ, რა გაფუჭდა
                if (!silent) {
                    a.runOnUiThread {
                        Toast.makeText(a, Lang.str(a, "updateCheckError") + " " + (e.message ?: "?"), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }.start()
    }

    private fun showDialog(a: MainActivity, apkUrl: String, notes: String) {
        try {
        var msg = Lang.str(a, "updateMsg")
        if (notes.isNotEmpty()) msg += "\n\n" + notes
        AlertDialog.Builder(a)
            .setTitle("🌿  MyNote — " + Lang.str(a, "updateAvailable"))
            .setMessage(msg)
            .setPositiveButton(Lang.str(a, "updateNow")) { _, _ -> startDownload(a, apkUrl) }
            .setNegativeButton(Lang.str(a, "later"), null)
            .show()
        } catch (e: Exception) { }
    }

    private fun startDownload(a: MainActivity, apkUrl: String) {
        // Android 8+: APK-ის დაყენებას „უცნობი წყაროები" სჭირდება.
        // თუ არაა დაშვებული — ბმულს ვიმახსოვრებთ, პარამეტრები იხსნება,
        // უკან დაბრუნებისას onResume-ი ჩამოტვირთვას თავისით იწყებს.
        // ⚠️ ადრე ეს ადგილი try-catch-ის გარეშე იყო — პარამეტრების გახსნისას
        // ნებისმიერი შეცდომა აპს იქრეშავდა. ახლა ყველაფერი დაცულია.
        try {
        if (!canInstall(a)) {
            pendingApkUrl = apkUrl
            Toast.makeText(a, Lang.str(a, "allowUnknownSources"), Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.packageName))
                a.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(a, Lang.str(a, "updateFailed") + " " + (e.message ?: "?"), Toast.LENGTH_LONG).show()
            }
            return
        }
            val dm = a.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val req = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("MyNote")
                .setDescription(Lang.str(a, "downloadingUpdate"))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(a, Environment.DIRECTORY_DOWNLOADS, "mynote-update.apk")
                .setMimeType("application/vnd.android.package-archive")
                .setAllowedOverMetered(true)
            downloadId = dm.enqueue(req)
            Toast.makeText(a, Lang.str(a, "downloadingUpdate"), Toast.LENGTH_SHORT).show()
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    // ⚠️ completedId ჯერ ვიჭერთ და მერე ვანულებთ downloadId-ს —
                    // ადრე downloadId ჯერ ნულდებოდა და install() ცარიელ მისამართს იღებდა
                    val completedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                    if (completedId != downloadId) return
                    try { a.applicationContext.unregisterReceiver(this) } catch (e: Exception) { }
                    downloadId = -1L
                    // ჩამოტვირთვა მართლა წარმატებულია?
                    try {
                        val dm2 = a.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        val q = DownloadManager.Query().setFilterById(completedId)
                        val cur = dm2.query(q)
                        var ok = false
                        if (cur != null) {
                            if (cur.moveToFirst()) {
                                ok = cur.getInt(cur.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
                            }
                            cur.close()
                        }
                        if (!ok) {
                            Toast.makeText(a, Lang.str(a, "updateFailed"), Toast.LENGTH_LONG).show()
                            return
                        }
                    } catch (e: Exception) { }
                    install(a, completedId)
                }
            }
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                a.applicationContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                a.applicationContext.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            Toast.makeText(a, Lang.str(a, "updateFailed") + " " + (e.message ?: "?"), Toast.LENGTH_LONG).show()
        }
    }

    private fun install(a: MainActivity, id: Long) {
        try {
            val dm = a.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = dm.getUriForDownloadedFile(id)
            if (uri == null) {
                Toast.makeText(a, Lang.str(a, "updateFailed"), Toast.LENGTH_LONG).show()
                return
            }
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // ჩამოტვირთვის დასრულებისას Activity შეიძლება უკვე დახურული იყოს —
            // applicationContext-ით ვიწყებთ, რომ არ იქრეშოს
            a.applicationContext.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(a, Lang.str(a, "updateFailed") + " " + (e.message ?: "?"), Toast.LENGTH_LONG).show()
        }
    }
}
