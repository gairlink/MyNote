package ge.kakha.mynote

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Google Drive სინქრონიზაცია (Production):
 *
 * - მომხმარებელი შედის თავისი Google ანგარიშით (ტელეფონში არსებული მეილით).
 * - ნოუტები ინახება მის Drive-ში, დამალულ appDataFolder-ში — ფაილი
 *   "mynote-sync.json" ჩანს მხოლოდ ამ აპისთვის, Drive-ის ფაილების სიაში არ ჩანს.
 * - ახალ ტელეფონზე შესვლისას ნოუტები თავისით ჩამოიტვირთება.
 * - კონფლიქტი: იმარჯვებს ის, ვინც უფრო გვიან დასინქრონდა (syncTime).
 *
 * არასდროს იგზავნება: პაროლი (PIN), ჩაკეტილი ნოუტების სია (lockedNotes).
 * ისინი მხოლოდ ტელეფონში რჩება.
 *
 * Cloud Console-ში საჭიროა (ერთხელ):
 * 1) პროექტი + ჩართული "Google Drive API"
 * 2) OAuth consent screen -> External -> Production (გამოქვეყნებული)
 * 3) OAuth client ID -> Android -> package ge.kakha.mynote + SHA-1
 * 4) Scope: https://www.googleapis.com/auth/drive.appdata
 */
object DriveSync {

    const val RC_SIGN_IN = 9001
    const val RC_AUTH = 9002

    private const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    private const val FILE_NAME = "mynote-sync.json"
    private const val PREF_LAST_SYNC = "lastSyncTime"

    @Volatile
    var syncing = false

    private var lastUploadAttempt = 0L
    private var pendingRetry = false

    // ---------- შესვლა / გამოსვლა ----------

    private fun signInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SCOPE))
            .build()
    }

    fun signIn(a: MainActivity) {
        val client = GoogleSignIn.getClient(a, signInOptions())
        a.startActivityForResult(client.signInIntent, RC_SIGN_IN)
    }

    fun signedInAccount(a: MainActivity): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(a)
    }

    fun isSignedIn(a: MainActivity): Boolean {
        return signedInAccount(a) != null
    }

    fun accountEmail(a: MainActivity): String? {
        return signedInAccount(a)?.email
    }

    fun signOut(a: MainActivity, onDone: () -> Unit) {
        val client = GoogleSignIn.getClient(a, signInOptions())
        client.signOut().addOnCompleteListener {
            a.getSharedPreferences("mynote", 0).edit().putLong(PREF_LAST_SYNC, 0L).apply()
            a.runOnUiThread { onDone() }
        }
    }

    // ---------- ბოლო სინქრონიზაციის დრო ----------

    fun lastSyncTime(a: MainActivity): Long {
        return a.getSharedPreferences("mynote", 0).getLong(PREF_LAST_SYNC, 0L)
    }

    fun lastSyncDateText(a: MainActivity): String {
        val t = lastSyncTime(a)
        if (t <= 0L) return ""
        return SimpleDateFormat("MM-dd h:mm a", Locale.US).format(Date(t))
    }

    // ---------- სრული სინქრონიზაცია ----------

    fun sync(a: MainActivity, onDone: (Boolean) -> Unit) {
        if (syncing) {
            a.runOnUiThread { onDone(false) }
            return
        }
        syncing = true
        Thread {
            var ok = false
            try {
                val account = signedInAccount(a)
                if (account != null) {
                    val token = token(a, account)
                    if (token != null) {
                        val fileId = findFile(token)
                        if (fileId == null) {
                            // Drive-ში ჯერ არაფერია — ვტვირთავთ ადგილობრივს
                            val payload = buildPayload(a)
                            if (upload(token, null, payload)) {
                                saveLastSync(a, payload.optLong("syncTime", System.currentTimeMillis()))
                                ok = true
                            }
                        } else {
                            val remoteJson = download(token, fileId)
                            val remote = JSONObject(remoteJson)
                            val remoteSync = remote.optLong("syncTime", 0L)
                            if (remoteSync > lastSyncTime(a)) {
                                // Drive-ში უფრო ახალია — ჩამოტვირთვა და ჩანაცვლება
                                val copy = JSONObject(remoteJson)
                                a.runOnUiThread { a.applySyncedData(copy) }
                                saveLastSync(a, remoteSync)
                                ok = true
                            } else {
                                // ადგილობრივი უფრო ახალია (ან ტოლია) — ატვირთვა
                                val payload = buildPayload(a)
                                if (upload(token, fileId, payload)) {
                                    saveLastSync(a, payload.optLong("syncTime", System.currentTimeMillis()))
                                    ok = true
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                ok = false
            }
            syncing = false
            val result = ok
            a.runOnUiThread { onDone(result) }
        }.start()
    }

    // ---------- ჩუმი ატვირთვა ცვლილების მერე (debounce 60 წამი) ----------

    fun uploadIfNeeded(a: MainActivity) {
        if (!isSignedIn(a)) return
        if (syncing) return
        val now = System.currentTimeMillis()
        if (now - lastUploadAttempt < 60_000L) return
        lastUploadAttempt = now
        Thread {
            try {
                val account = signedInAccount(a) ?: return@Thread
                val token = token(a, account) ?: return@Thread
                val fileId = findFile(token)
                if (fileId != null) {
                    // ჯერ ვამოწმებთ: Drive-ში ხომ არ არის უფრო ახალი ბექაფი?
                    // ცარიელმა ადგილობრივმა მდგომარეობამ ძველი ბექაფი არ უნდა გადაფაროს
                    val remoteJson = download(token, fileId)
                    val remoteSync = JSONObject(remoteJson).optLong("syncTime", 0L)
                    if (remoteSync > lastSyncTime(a)) {
                        // Drive-ში უფრო ახალია — ჩამოტვირთვა, ატვირთვა არა
                        a.runOnUiThread { a.applySyncedData(JSONObject(remoteJson)) }
                        saveLastSync(a, remoteSync)
                        return@Thread
                    }
                }
                val payload = buildPayload(a)
                if (upload(token, fileId, payload)) {
                    saveLastSync(a, payload.optLong("syncTime", now))
                }
            } catch (e: Exception) {
                // ჩუმი ატვირთვა არასდროს აგდებს აპს
            }
        }.start()
    }

    // ---------- შესვლის შედეგი (MainActivity.onActivityResult-იდან) ----------

    fun handleActivityResult(a: MainActivity, requestCode: Int, resultCode: Int, data: Intent?, onDone: (Boolean) -> Unit) {
        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                if (account != null) {
                    // შევედით — მაშინვე სინქრონიზაცია
                    sync(a, onDone)
                } else {
                    onDone(false)
                }
            } catch (e: Exception) {
                onDone(false)
            }
            return
        }
        if (requestCode == RC_AUTH) {
            if (resultCode == Activity.RESULT_OK && pendingRetry) {
                pendingRetry = false
                sync(a, onDone)
            } else {
                onDone(false)
            }
        }
    }

    // ---------- ტოკენი ----------

    private fun token(a: MainActivity, account: GoogleSignInAccount): String? {
        return try {
            val acc = account.account ?: return null
            GoogleAuthUtil.getToken(a, acc, "oauth2:$SCOPE")
        } catch (e: UserRecoverableAuthException) {
            // Google-ს ერთხელ სჭირდება დადასტურება — ვხსნით მის ფანჯარას,
            // დადასტურების მერე სინქრონიზაციას თავიდან გავუშვებთ
            pendingRetry = true
            a.runOnUiThread { a.startActivityForResult(e.intent, RC_AUTH) }
            null
        } catch (e: Exception) {
            null
        }
    }

    // ---------- Drive REST (HttpURLConnection, დამატებითი ბიბლიოთეკის გარეშე) ----------

    private fun http(method: String, urlStr: String, token: String, body: ByteArray?, contentType: String?): String? {
        var conn: HttpURLConnection? = null
        return try {
            val conn2 = URL(urlStr).openConnection() as HttpURLConnection
            conn = conn2
            conn2.requestMethod = method
            conn2.connectTimeout = 15000
            conn2.readTimeout = 30000
            conn2.setRequestProperty("Authorization", "Bearer $token")
            if (contentType != null) conn2.setRequestProperty("Content-Type", contentType)
            if (body != null) {
                conn2.doOutput = true
                conn2.outputStream.use { it.write(body) }
            }
            val code = conn2.responseCode
            val stream = if (code in 200..299) conn2.inputStream else conn2.errorStream
            if (stream == null) return null
            val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            if (code in 200..299) text else null
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun findFile(token: String): String? {
        val q = URLEncoder.encode("name = '$FILE_NAME'", "UTF-8")
        val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$q&fields=files(id)"
        val resp = http("GET", url, token, null, null) ?: return null
        val files = JSONObject(resp).optJSONArray("files") ?: return null
        if (files.length() == 0) return null
        return files.getJSONObject(0).optString("id", "")
    }

    private fun download(token: String, fileId: String): String {
        val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
        return http("GET", url, token, null, null) ?: ""
    }

    private fun upload(token: String, fileId: String?, payload: JSONObject): Boolean {
        val boundary = "mynote_boundary"
        val meta = if (fileId == null) {
            "{\"name\":\"$FILE_NAME\",\"parents\":[\"appDataFolder\"]}"
        } else {
            "{\"name\":\"$FILE_NAME\"}"
        }
        val sb = StringBuilder()
        sb.append("--").append(boundary).append("\r\n")
        sb.append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
        sb.append(meta).append("\r\n")
        sb.append("--").append(boundary).append("\r\n")
        sb.append("Content-Type: application/json\r\n\r\n")
        sb.append(payload.toString()).append("\r\n")
        sb.append("--").append(boundary).append("--\r\n")
        val body = sb.toString().toByteArray(Charsets.UTF_8)
        val url = if (fileId == null) {
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        } else {
            "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=multipart"
        }
        val method = if (fileId == null) "POST" else "PATCH"
        val resp = http(method, url, token, body, "multipart/related; boundary=$boundary")
        return resp != null
    }

    // ---------- პაკეტი: რა იგზავნება / რა არა ----------

    private fun buildPayload(a: MainActivity): JSONObject {
        val p = a.getSharedPreferences("mynote", 0)
        val json = JSONObject()
        json.put("syncTime", System.currentTimeMillis())
        json.put("notes", p.getString("notes", "") ?: "")
        json.put("nextId", p.getInt("nextId", 1))
        json.put("cats", p.getString("cats", "") ?: "")
        json.put("viewFontSize", p.getInt("viewFontSize", 16))
        json.put("languageCode", p.getString("languageCode", "en") ?: "en")
        val colors = JSONObject()
        val reminders = JSONObject()
        for ((k, v) in p.all) {
            if (k.startsWith("catColor_") && v is String) {
                colors.put(k, v)
            }
            if (k.startsWith("reminder_") && v is Long) {
                reminders.put(k, v)
            }
        }
        json.put("catColors", colors)
        json.put("reminders", reminders)
        // განგებ არ იწერება: lockedNotes, PIN — ისინი მხოლოდ ტელეფონში რჩება
        return json
    }

    private fun saveLastSync(a: MainActivity, t: Long) {
        a.getSharedPreferences("mynote", 0).edit().putLong(PREF_LAST_SYNC, t).apply()
    }
}
