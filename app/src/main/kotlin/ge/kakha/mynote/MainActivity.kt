package ge.kakha.mynote

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.widget.Toast
import org.json.JSONObject
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout

class MainActivity : Activity() {

    val notes = mutableListOf<Note>()
    lateinit var paperBox: android.widget.LinearLayout
    var filter = "ყველა"
    var query = ""
    var nextId = 1
    var cats = mutableListOf<String>()
    var viewFontSize = 16

    val LAV = "#C9BEFF"
    val LAV_LIGHT = "#EDE9FF"
    val LAV_BG = "#E6E0FF"
    val LINE = "#D8D0FF"

    var currentScreen = "home"
    private val backStack = ArrayDeque<Pair<String, Note?>>()
    private var navigatingBack = false
    var currentNote: Note? = null
    // ერთჯერადი საშვი: წარმატებული პაროლის მერე მხოლოდ ერთი შესვლა უშვებს, შემდეგ გახსნაზე ისევ ითხოვს
    val justUnlockedNotes = mutableSetOf<Int>()
    fun isNoteLocked(id: Int): Boolean {
        return getSharedPreferences("mynote", 0).getStringSet("lockedNotes", emptySet())?.contains(id.toString()) == true
    }
    fun setNoteLocked(id: Int, locked: Boolean) {
        val prefs = getSharedPreferences("mynote", 0)
        val set = prefs.getStringSet("lockedNotes", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (locked) set.add(id.toString()) else set.remove(id.toString())
        prefs.edit().putStringSet("lockedNotes", set).apply()
    }
    var exitDialogShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // არასდროს გავიდეს სისტემური ზოლების ქვეშ
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        loadCats()
        loadFontSize()
        loadNotes()
        // Android 33+ (predictive back): onBackPressed() აღარ იძახება, სისტემა პირდაპირ ხურავს.
        // ამიტომ ვარეგისტრირებთ OnBackInvokedCallback-ს — ეს არის ის, რასაც სისტემა ნამდვილად იძახებს.
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            try {
                onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT
                ) { handleBack() }
            } catch (e: Exception) { /* ძველ მოწყობილობაზე onBackPressed() იმუშავებს */ }
        }
        // პაროლი აღარ გამოიყენება პროგრამაში შესასვლელად — აპი იხსნება პირდაპირ
        showHomePage()
        // ვიჯეტიდან გახსნა: კონკრეტული ნოუტი ან ახალი ნოუტის შექმნა
        when (intent.getStringExtra("widgetAction")) {
            "open" -> {
                val nid = intent.getIntExtra("noteId", -1)
                val n = notes.find { it.id == nid }
                if (n != null) showView(n)
            }
            "create" -> showCreatePicker()
        }
        // Google Drive: თუ შესულია, გახსნაზე თავისით სინქრონიზაცია
        // ჩუმი სინქრონი — ეკრანს არ ვეხებით: თუ Drive-ში უფრო ახალია,
        // applySyncedData თავისით განაახლებს იმ გვერდს, სადაც ხარ
        if (DriveSync.isSignedIn(this)) {
            DriveSync.sync(this) { }
        }
        // შეტყობინების ნებართვა (Android 13+) — პირველ გახსნაზე ვითხოვთ
        askNotifPermission()
        // განახლების შემოწმება (დღეში ერთხელ, ჩუმად — დიალოგი მხოლოდ თუ განახლებაა)
        AppUpdate.check(this)
    }

    override fun onResume() {
        super.onResume()
        // პარამეტრებიდან („უცნობი წყაროები") დაბრუნებისას განახლების ჩამოტვირთვა გრძელდება
        AppUpdate.onResume(this)
    }

    // შეტყობინების სისტემური ნებართვა (Android 13+).
    // მანიფესტში დეკლარირება საკმარისი არაა — დიალოგს კოდმა უნდა ითხოვოს,
    // თორემ შეხსენებები ჩუმად იკარგება: მაღვიძარა გაისვრის, შეტყობინება კი არ გამოჩნდება.
    // force = true — მომხმარებლის აშკარა მოქმედებისას (პარამეტრებში ჩართვა, შეხსენების დაყენება)
    fun askNotifPermission(force: Boolean = false) {
        if (android.os.Build.VERSION.SDK_INT < 33) return
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val p = getSharedPreferences("mynote", MODE_PRIVATE)
        if (!force && p.getBoolean("notifPermAsked", false)) return
        // ორჯერ უარი = სამუდამო უარი: Android დიალოგს აღარ აჩვენებს,
        // ამიტომ ვხსნით აპის სისტემურ პარამეტრებს, სადაც ხელით ირთვება
        if (p.getBoolean("notifPermAsked", false) && !shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
            try {
                val i = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                i.data = android.net.Uri.parse("package:" + packageName)
                startActivity(i)
            } catch (e: Exception) { }
            return
        }
        p.edit().putBoolean("notifPermAsked", true).apply()
        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
    }

    // Google-ში შესვლის / ნებართვის დადასტურების შედეგი
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DriveSync.RC_SIGN_IN || requestCode == DriveSync.RC_AUTH) {
            if (requestCode == DriveSync.RC_SIGN_IN && resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, Lang.str(this, "signInFailed"), Toast.LENGTH_SHORT).show()
                return
            }
            DriveSync.handleActivityResult(this, requestCode, resultCode, data) { ok ->
                Toast.makeText(this, Lang.str(this, if (ok) "syncDone" else "syncFailed"), Toast.LENGTH_SHORT).show()
                refreshCurrentScreen()
            }
        }
    }

    // Drive-დან ჩამოტვირთული პაკეტის გამოყენება.
    // განგებ არ ვეხებით: lockedNotes, PIN — ისინი მხოლოდ ამ ტელეფონში რჩება.
    fun applySyncedData(payload: JSONObject) {
        try {
            val p = getSharedPreferences("mynote", MODE_PRIVATE)
            val ed = p.edit()
            ed.putString("notes", payload.optString("notes", ""))
            ed.putInt("nextId", payload.optInt("nextId", 1))
            ed.putString("cats", payload.optString("cats", ""))
            ed.putInt("viewFontSize", payload.optInt("viewFontSize", 16))
            ed.putString("languageCode", payload.optString("languageCode", "en"))
            val colors = payload.optJSONObject("catColors")
            if (colors != null) {
                val keys = colors.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    ed.putString(k, colors.optString(k, ""))
                }
            }
            val reminders = payload.optJSONObject("reminders")
            if (reminders != null) {
                val keys = reminders.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    ed.putLong(k, reminders.optLong(k, 0L))
                }
            }
            ed.commit()
            loadCats()
            loadFontSize()
            notes.clear()
            loadNotes()
            NoteSingleWidgetProvider.updateAll(this)
            NoteSingleWideWidgetProvider.updateAll(this)
            NoteSingleWidgetProvider.updateAll(this)
            refreshCurrentScreen()
        } catch (e: Exception) {
            // დაზიანებული პაკეტი — ადგილობრივი მონაცემები არ იშლება
        }
    }

    // ყველა გვერდი ამის გავლით იხსნება,
    // ამიტომ ყველა გვერდი ავტომატურად დადგება
    // სტატუს-ზოლის ქვემოთ და სისტემური ღილაკების ზემოთ
    override fun setContentView(view: View) {
        val wrapper = FrameLayout(this)
        wrapper.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        wrapper.addView(view)
        val topPad = getBarHeight("status_bar_height")
        var bottomPad = getBarHeight("navigation_bar_height")
        if (bottomPad < UiHelpers.dp(this, 16)) {
            bottomPad = UiHelpers.dp(this, 16)
        }
        wrapper.setPadding(0, topPad, 0, bottomPad)
        wrapper.fitsSystemWindows = true
        super.setContentView(wrapper)
    }

    private fun getBarHeight(name: String): Int {
        val id = resources.getIdentifier(name, "dimen", "android")
        if (id > 0) {
            return resources.getDimensionPixelSize(id)
        }
        return 0
    }

    // onKeyDown მოვხსენი — ის + onBackPressed ორჯერ იძახებდა handleBack()-ს და პირდაპირ ხურავდა აპს

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        handleBack()
    }

    private fun pushCurrent() {
        if (navigatingBack) return
        if (backStack.isEmpty() || backStack.last().first != currentScreen || backStack.last().second?.id != currentNote?.id) {
            backStack.addLast(Pair(currentScreen, currentNote))
        }
    }

    fun handleBack() {
        if (exitDialogShowing) return
        if (backStack.isEmpty()) {
            if (currentScreen == "home") showExitDialog() else showHomePage()
            return
        }
        val (prevScreen, prevNote) = backStack.removeLast()
        navigatingBack = true
        when (prevScreen) {
            "home" -> showHomePage()
            "settings" -> showSettings()
            "categories" -> showCategories()
            "fontsize" -> showFontSize()
            "calendar" -> showCalendar()
            "menu" -> showMenu()
            "trash" -> showTrash()
            "archivePage" -> showArchive()
            "view" -> if (prevNote != null) showView(prevNote) else showHomePage()
            "editor" -> showEditor(prevNote, prevNote?.noteType ?: "text")
            else -> showHomePage()
        }
        navigatingBack = false
    }

    fun showExitDialog() {
        exitDialogShowing = true
        val dialog = AlertDialog.Builder(this)
            .setTitle("🌿  MyNote")
            .setMessage(Lang.str(this, "exitMsg"))
            .setPositiveButton(Lang.str(this, "exitYes")) { _, _ ->
                exitDialogShowing = false
                finish()
            }
            .setNegativeButton(Lang.str(this, "exitNo")) { _, _ ->
                exitDialogShowing = false
            }
            .setOnCancelListener {
                exitDialogShowing = false
            }
            .create()
        dialog.show()
    }

    fun showHomePage() {
        pushCurrent()
        currentScreen = "home"
        currentNote = null
        HomePage.show(this)
    }

    // მიმდინარე გვერდის განახლება უკან-დაბრუნების სტეკის გარეშე
    // (სინქრონიზაციის მერე მომხმარებელი იმავე გვერდზე რჩება)
    fun refreshCurrentScreen() {
        when (currentScreen) {
            "menu" -> MenuPage.show(this)
            "settings" -> SettingsPage.show(this)
            "calendar" -> CalendarPage.show(this)
            "trash" -> TrashPage.show(this)
            "archivePage" -> ArchivePage.show(this)
            "categories" -> CategoriesPage.show(this)
            "fontsize" -> FontSizePage.show(this)
            "view" -> {
                val n = currentNote
                // სინქრონის მერე ჩატვირთული სიიდან ვიღებთ ახალ ვერსიას,
                // რომ ნანახი ჩანაწერი განახლებული ჩანდეს
                val fresh = n?.let { note -> notes.find { it.id == note.id } }
                if (fresh != null) {
                    currentNote = fresh
                    ViewPage.show(this, fresh)
                } else HomePage.show(this)
            }
            // ჩუმი სინქრონი რედაქტირებას არ წყვეტს — მომხმარებელი რედაქტორში რჩება
            "editor" -> { }
            else -> HomePage.show(this)
        }
    }

    fun showSettings() {
        pushCurrent()
        currentScreen = "settings"
        SettingsPage.show(this)
    }

    fun showCategories() {
        pushCurrent()
        currentScreen = "categories"
        CategoriesPage.show(this)
    }

    fun showFontSize() {
        pushCurrent()
        currentScreen = "fontsize"
        FontSizePage.show(this)
    }

    fun showCalendar() {
        pushCurrent()
        currentScreen = "calendar"
        currentNote = null
        CalendarPage.show(this)
    }

    fun showMenu() {
        pushCurrent()
        currentScreen = "menu"
        currentNote = null
        MenuPage.show(this)
    }

    fun showTrash() {
        pushCurrent()
        currentScreen = "trash"
        currentNote = null
        TrashPage.show(this)
    }

    fun showArchive() {
        pushCurrent()
        currentScreen = "archivePage"
        currentNote = null
        ArchivePage.show(this)
    }

    fun showView(note: Note) {
        if (isNoteLocked(note.id)) {
            // ერთჯერადი საშვი — წარმატებული პაროლის მერე ეს ერთი შესვლა უშვებს, შემდეგი გახსნა ისევ ითხოვს
            if (justUnlockedNotes.remove(note.id)) {
                // საშვი დაიხარჯა, ვაგრძელებთ გახსნას
            } else {
                PinGate.promptForNote(this, onUnlock = {
                    justUnlockedNotes.add(note.id)
                    showView(note)
                }, onCancel = { showHomePage() })
                return
            }
        }
        pushCurrent()
        currentScreen = "view"
        currentNote = note
        ViewPage.show(this, note)
    }

    fun showEditor(note: Note?, startType: String) {
        if (note != null && isNoteLocked(note.id)) {
            if (justUnlockedNotes.remove(note.id)) {
                // საშვი დაიხარჯა, ვაგრძელებთ რედაქტორში გახსნას
            } else {
                PinGate.promptForNote(this, onUnlock = {
                    justUnlockedNotes.add(note.id)
                    showEditor(note, startType)
                }, onCancel = { showHomePage() })
                return
            }
        }
        pushCurrent()
        currentScreen = "editor"
        currentNote = note
        EditorPage.show(this, note, startType)
    }

    fun showCreatePicker() {
        CreatePicker.show(this)
    }

    fun refresh() {
        HomePage.refresh(this)
    }

    fun getFiltered(): List<Note> {
        val out = mutableListOf<Note>()
        for (n in notes) {
            if (n.isArchived || n.isDeleted) continue
            // დაბლოკილი ნოუტის ტექსტი ძიებაშიც არ უნდა გაჟონოს — მხოლოდ სათაური იძებნება
            val hay = if (isNoteLocked(n.id)) {
                n.title
            } else if (n.noteType == "checklist") {
                n.title + " " + n.checklist.joinToString(" ") { it.text }
            } else {
                n.title + " " + n.text
            }
            if ((filter == "ყველა" || n.folder == filter) && hay.contains(query, true)) {
                out.add(n)
            }
        }
        out.sortWith(compareByDescending<Note> { it.isFav }.thenByDescending { it.id })
        return out
    }

    fun saveCats() {
        val ed = getSharedPreferences("mynote", MODE_PRIVATE).edit()
        ed.putString("cats", cats.joinToString(";;"))
        ed.commit()
    }

    fun loadCats() {
        val p = getSharedPreferences("mynote", MODE_PRIVATE)
        val saved = p.getString("cats", "")
        if (saved != null && saved.isNotEmpty()) {
            val list = saved.split(";;")
            val clean = mutableListOf<String>()
            for (c in list) {
                if (c.isNotEmpty()) {
                    clean.add(c)
                }
            }
            if (!clean.contains("ყველა")) {
                clean.add(0, "ყველა")
            }
            cats = clean
        }
    }

    fun saveFontSize() {
        val ed = getSharedPreferences("mynote", MODE_PRIVATE).edit()
        ed.putInt("viewFontSize", viewFontSize)
        ed.commit()
    }

    fun loadFontSize() {
        val p = getSharedPreferences("mynote", MODE_PRIVATE)
        viewFontSize = p.getInt("viewFontSize", 16)
    }

    // შენახვის კოდირება: ტექსტი Base64-ში ინახება, რომ \n, წინა/უკანა სფეისები, | და ;; არ დაიკარგოს.
    // ძველი ჩანაწერები (clean-ით შენახული) ავტომატურად იკითხება fallback-ით.
    private fun encField(s: String): String {
        return try { android.util.Base64.encodeToString(s.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP) } catch (e: Exception) { s }
    }
    private fun decField(s: String): String {
        return try {
            val decoded = android.util.Base64.decode(s, android.util.Base64.NO_WRAP)
            String(decoded, Charsets.UTF_8)
        } catch (e: Exception) { s }
    }
    private fun isB64(s: String): Boolean {
        if (s.isEmpty()) return true
        if (s.length % 4 != 0) return false
        return s.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '+' || it == '/' || it == '=' }
    }
    private fun decFieldSafe(s: String): String { return if (isB64(s)) decField(s) else s }

    fun saveNotes() {
        val lines = mutableListOf<String>()
        for (n in notes) {
            val parts = mutableListOf<String>()
            for (ci in n.checklist) {
                val flag = if (ci.done) "1" else "0"
                parts.add(flag + "~" + encField(ci.text))
            }
            val check = parts.joinToString(";;")
            val line = n.id.toString() +
                "|" + encField(n.title) +
                "|" + encField(n.text) +
                "|" + encField(n.folder) +
                "|" + n.isFav.toString() +
                "|" + encField(n.date) +
                "|" + n.noteType +
                "|" + check +
                "|" + n.createdAt.toString() +
                "|" + n.updatedAt.toString() +
                "|" + n.isArchived.toString() +
                "|" + n.isDeleted.toString()
            lines.add(line)
        }
        val s = lines.joinToString("\n")
        val ed = getSharedPreferences("mynote", MODE_PRIVATE).edit()
        ed.putString("notes", s)
        ed.putInt("nextId", nextId)
        ed.commit()
        NoteSingleWidgetProvider.updateAll(this)
        NoteSingleWideWidgetProvider.updateAll(this)
        NoteSingleWidgetProvider.updateAll(this)
        // Google Drive: ცვლილების მერე ჩუმი ატვირთვა (თუ შესულია)
        DriveSync.uploadIfNeeded(this)
    }

    // "yyyy-MM-dd" -> millis (00:00). ვერ გაიშიფრა -> 0L
    private fun dateStringToMillis(date: String): Long {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(date)?.time ?: 0L
        } catch (e: Exception) { 0L }
    }

    private fun loadNotes() {
        val p = getSharedPreferences("mynote", MODE_PRIVATE)
        nextId = p.getInt("nextId", 1)
        val saved = p.getString("notes", "")
        if (saved != null) {
            for (l in saved.lines()) {
              try {
                val a = l.split("|")
                if (a.size >= 6) {
                    val type = if (a.size >= 7) a[6] else "text"
                    val list = mutableListOf<CheckItem>()
                    if (a.size >= 8 && a[7].isNotEmpty()) {
                        val checkParts = a[7].split(";;")
                        for (part in checkParts) {
                            val b = part.split("~")
                            if (b.size >= 2) {
                                list.add(CheckItem(decFieldSafe(b[1]), b[0] == "1"))
                            }
                        }
                    }
                    val note = Note(
                        a[0].toIntOrNull() ?: continue,
                        decFieldSafe(a[1]),
                        decFieldSafe(a[2]),
                        decFieldSafe(a[3]),
                        a[4].toBoolean(),
                        decFieldSafe(a[5]),
                        type,
                        list,
                        if (a.size >= 9) a[8].toLongOrNull() ?: 0L else 0L,
                        if (a.size >= 10) a[9].toLongOrNull() ?: 0L else 0L,
                        if (a.size >= 11) a[10].toBoolean() else false,
                        if (a.size >= 12) a[11].toBoolean() else false
                    )
                    // ძველი ჩანაწერები, რომლებსაც შექმნის/რედაქტირების დრო არ აქვთ:
                    // ვაღდგენთ ჩანაწერის საკუთარი კალენდრის თარიღიდან, რომ სინქრონის
                    // მერე „დღევანდელი" (ჩამოტვირთვის) თარიღი არ გამოჩნდეს
                    if (note.createdAt == 0L) {
                        note.createdAt = dateStringToMillis(note.date)
                    }
                    if (note.updatedAt == 0L) {
                        note.updatedAt = note.createdAt
                    }
                    notes.add(note)
                }
              } catch (e: Exception) { /* დაზიანებული ხაზი გამოტოვებულია, აპი არ ეცემა */ }
            }
        }
        // კატეგორიები არსად არ ინახებოდა (მხოლოდ ნაგულისხმევი სია იყო კოდში) —
        // ამიტომ აღვადგენთ ნოუტების კატეგორიებიდან.
        // ახალი აპი (ნოუტების გარეშე) ცარიელი რჩება.
        // "ყველა"-ს წაშლა შეუძლებელია, ამიტომ cats ცარიელი მხოლოდ
        // ძველი მონაცემების / ცარიელი Drive-ბექაფის შემთხვევაშია.
        if (cats.isEmpty() && notes.isNotEmpty()) {
            val folders = mutableListOf<String>()
            for (n in notes) {
                if (n.folder.isNotEmpty() && !folders.contains(n.folder)) folders.add(n.folder)
            }
            if (folders.isNotEmpty()) {
                cats = mutableListOf("ყველა")
                cats.addAll(folders)
                saveCats()
            }
        }
    }
}