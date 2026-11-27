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
// როგორ მუშაობს:
// 1) აპი გაშვებისას (დღეში ერთხელ) კითხულობს VERSION_URL-ზე მდებარე version.json-ს.
// 2) თუ იქ versionCode დაყენებულზე მეტია — დიალოგი: „განახლება ხელმისაწვდომია".
// 3) დადასტურებისას APK იტვირთება DownloadManager-ით და ინსტალაცია იწყება.
//
// შენი მხრიდან საჭიროა (ერთხელ):
// - GitHub-ზე რეპო, სადაც დევს version.json და APK (Release-ში).
// - ქვემოთ VERSION_URL-ში ჩასვი შენი version.json-ის raw ბმული.
// version.json-ის სახე:
//   { "versionCode": 13, "versionName": "1.3",
//     "apkUrl": "https://github.com/შენი-სახელი/MyNote/releases/download/v1.3/mynote.apk",
//     "notes": "ახალი: ..." }
// ყოველ ახალ ვერსიაზე: versionCode +1 (module.toml-ში) -> Build APK ->
// APK ატვირთე GitHub Release-ში -> version.json განაახლე (versionCode + apkUrl).
object AppUpdate {

    // 👇 შეცვალე შენი version.json-ის raw ბმულით
    const val VERSION_URL = "https://raw.githubusercontent.com/gairlink/MyNote/main/app/version.json"

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
        Thread {
            try {
                val conn = java.net.URL(VERSION_URL).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val text = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val remoteCode = Regex("\"versionCode\"\\s*:\\s*(\\d+)").find(text)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                val apkUrl = Regex("\"apkUrl\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1) ?: ""
                val notes = Regex("\"notes\"\\s*:\\s*\"([^\"]*)\"").find(text)?.groupValues?.get(1) ?: ""
                a.runOnUiThread {
                    a.getSharedPreferences("mynote", 0).edit().putLong(PREF_LAST_CHECK, System.currentTimeMillis()).apply()
                    if (remoteCode > installedVersion(a) && apkUrl.isNotEmpty()) {
                        showDialog(a, apkUrl, notes)
                    } else if (!silent) {
                        Toast.makeText(a, Lang.str(a, "upToDate"), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // ინტერნეტი არაა / ბმული არასწორია — ჩუმად ვტოვებთ, აპი არ ეცემა
                if (!silent) {
                    a.runOnUiThread {
                        Toast.makeText(a, Lang.str(a, "upToDate"), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }

    private fun showDialog(a: MainActivity, apkUrl: String, notes: String) {
        var msg = Lang.str(a, "updateMsg")
        if (notes.isNotEmpty()) msg += "\n\n" + notes
        AlertDialog.Builder(a)
            .setTitle("🌿  MyNote — " + Lang.str(a, "updateAvailable"))
            .setMessage(msg)
            .setPositiveButton(Lang.str(a, "updateNow")) { _, _ -> startDownload(a, apkUrl) }
            .setNegativeButton(Lang.str(a, "later"), null)
            .show()
    }

    private fun startDownload(a: MainActivity, apkUrl: String) {
        // Android 8+: APK-ის დაყენებას „უცნობი წყაროები" სჭირდება.
        // თუ არაა დაშვებული — ბმულს ვიმახსოვრებთ, პარამეტრები იხსნება,
        // უკან დაბრუნებისას onResume-ი ჩამოტვირთვას თავისით იწყებს.
        if (!canInstall(a)) {
            pendingApkUrl = apkUrl
            Toast.makeText(a, Lang.str(a, "allowUnknownSources"), Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.packageName))
            a.startActivity(intent)
            return
        }
        try {
            val dm = a.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val req = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("MyNote")
                .setDescription(Lang.str(a, "downloadingUpdate"))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MyNote/mynote-update.apk")
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
            // ჩამოტვირთვა ვერ დაიწყო — არაფერს ვაკეთებთ, მომხმარებელი მენიუდან თავიდან სცდის
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
            a.startActivity(intent)
        } catch (e: Exception) {
            // ინსტალაცია ვერ გაიხსნა — APK Downloads/MyNote-შია, ხელითაც დააყენებს
        }
    }
}
