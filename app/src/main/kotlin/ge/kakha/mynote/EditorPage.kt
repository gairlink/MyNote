package ge.kakha.mynote

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

object EditorPage {

    private fun pill(a: MainActivity, color: String): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = UiHelpers.dp(a, 24).toFloat()
        d.setColor(Color.parseColor(color))
        return d
    }

    private fun line(a: MainActivity): TextView {
        val v = TextView(a)
        v.setBackgroundColor(Color.parseColor("#DDDDDD"))
        val p = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            UiHelpers.dp(a, 1)
        )
        v.layoutParams = p
        return v
    }

    fun show(a: MainActivity, note: Note?, startType: String) {
        val isNew = note == null
        var type = startType
        if (!isNew && note != null) type = note.noteType
        var folder = if (note != null) note.folder else a.getSharedPreferences("mynote", 0).getString("defaultFolder", "პირადი") ?: "პირადი"
        // ჩვენება თარგმნილი, შენახვა ისევ შიდა სახელით (პირადი / ყველა)
        fun displayFolder(f: String) = when (f) { "ყველა" -> Lang.str(a, "all")
            "პირადი" -> Lang.str(a, "personal")
            else -> f }
        val items = mutableListOf<CheckItem>()
        if (note != null) {
            for (ci in note.checklist) items.add(CheckItem(ci.text, ci.done))
        }
        if (items.isEmpty() && type == "checklist") items.add(CheckItem("", false))

        val outer = LinearLayout(a)
        outer.orientation = LinearLayout.VERTICAL
        outer.setBackgroundColor(Color.parseColor(a.LAV_BG))
        outer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        // ზედა: ← უკან ფურცელზე
        val topBar = LinearLayout(a)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(UiHelpers.dp(a, 16), UiHelpers.dp(a, 14), UiHelpers.dp(a, 16), UiHelpers.dp(a, 8))
        val btnBack = Icons.backButton(a) { a.handleBack() }
        topBar.addView(btnBack)
        outer.addView(topBar)

        // სათაური: 🌿 ახალი ჩანაწერი
        val head = TextView(a)
        head.text = "🌿  " + if (isNew) Lang.str(a, "newNote") else Lang.str(a, "edit")
        head.textSize = 22f
        head.setTypeface(null, Typeface.BOLD)
        head.setTextColor(Color.parseColor("#3D3566"))
        head.setPadding(UiHelpers.dp(a, 20), 0, UiHelpers.dp(a, 20), UiHelpers.dp(a, 12))
        outer.addView(head)

        // თეთრი ფურცელი
        val scroll = ScrollView(a)
        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
        scrollParams.leftMargin = UiHelpers.dp(a, 16)
        scrollParams.rightMargin = UiHelpers.dp(a, 16)
        scroll.layoutParams = scrollParams
        val paper = LinearLayout(a)
        paper.orientation = LinearLayout.VERTICAL
        paper.setBackgroundColor(Color.WHITE)
        paper.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 20), UiHelpers.dp(a, 20), UiHelpers.dp(a, 20))

        val titleEt = EditText(a)
        titleEt.hint = Lang.str(a, "titleHint")
        titleEt.textSize = 20f
        titleEt.setTypeface(null, Typeface.BOLD)
        titleEt.setBackgroundColor(Color.TRANSPARENT)
        if (note != null) titleEt.setText(note.title)
        paper.addView(titleEt)
        // შექმნის / ბოლო რედაქტირების დრო — რედაქტირებისას ჩანს
        if (note != null && (note.createdAt > 0L || note.updatedAt > 0L)) {
            val timeTv = TextView(a)
            val fmt = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            val createdStr = if (note.createdAt > 0L) fmt.format(java.util.Date(note.createdAt)) else note.date
            val updatedStr = if (note.updatedAt > 0L) fmt.format(java.util.Date(note.updatedAt)) else note.date
            timeTv.text = Lang.str(a, "createdTime") + ": " + createdStr + "  •  " + Lang.str(a, "lastModified") + ": " + updatedStr
            timeTv.textSize = 12f
            timeTv.setTextColor(Color.parseColor("#888888"))
            timeTv.setPadding(0, UiHelpers.dp(a, 2), 0, UiHelpers.dp(a, 6))
            paper.addView(timeTv)
        }
        paper.addView(line(a))

        val checkBox = LinearLayout(a)
        checkBox.orientation = LinearLayout.VERTICAL
        checkBox.setPadding(0, UiHelpers.dp(a, 12), 0, 0)
        paper.addView(checkBox)

        var textEt: EditText? = null
        if (type == "text") {
            textEt = EditText(a)
            textEt.hint = Lang.str(a, "textHint")
            textEt.textSize = 16f
            textEt.minLines = 8
            textEt.setBackgroundColor(Color.TRANSPARENT)
            // ფორმატის შენარჩუნება: მრავალხაზიანი, Enter = ახალი ხაზი, სიტყვები არ იჭრება, სფეისები არ იშლება
            textEt.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            textEt.setSingleLine(false)
            textEt.setHorizontallyScrolling(false)
            textEt.gravity = Gravity.TOP or Gravity.START
            if (note != null) textEt.setText(note.text)
            paper.addView(textEt)
        }
        paper.addView(line(a))

        // კატეგორია — ჩამოსაშლელი
        val folderTv = TextView(a)
        folderTv.text = displayFolder(folder) + "    ▼"
        folderTv.textSize = 17f
        folderTv.setTextColor(Color.parseColor("#111111"))
        folderTv.setPadding(0, UiHelpers.dp(a, 14), 0, UiHelpers.dp(a, 6))
        folderTv.setOnClickListener {
            val catList = a.cats.filter { it != "ყველა" }
            if (catList.isEmpty()) { Toast.makeText(a, Lang.str(a, "enterCatName"), Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            AlertDialog.Builder(a)
                .setItems(catList.toTypedArray()) { _, which ->
                    val list = a.cats.filter { it != "ყველა" }
                    folder = list[which]
                    folderTv.text = displayFolder(folder) + "    ▼"
                }
                .show()
        }
        paper.addView(folderTv)

        fun rebuildChecklist() {
            checkBox.removeAllViews()
            for (index in items.indices) {
                val row = LinearLayout(a)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                val cb = CheckBox(a)
                cb.isChecked = items[index].done
                cb.setOnCheckedChangeListener { _, checked ->
                    items[index].done = checked
                }
                row.addView(cb)
                val et = EditText(a)
                et.hint = Lang.str(a, "itemHint")
                et.setSingleLine(false)
                et.setHorizontallyScrolling(false)
                et.setText(items[index].text)
                val etParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                et.layoutParams = etParams
                et.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        items[index].text = s.toString()
                    }
                })
                row.addView(et)
                val upBtn = TextView(a)
                upBtn.text = "↑"
                upBtn.textSize = 20f
                upBtn.setPadding(UiHelpers.dp(a, 8), 0, UiHelpers.dp(a, 4), 0)
                upBtn.setOnClickListener {
                    if (index > 0) {
                        val tmp = items[index]
                        items[index] = items[index - 1]
                        items[index - 1] = tmp
                        rebuildChecklist()
                    }
                }
                row.addView(upBtn)
                val downBtn = TextView(a)
                downBtn.text = "↓"
                downBtn.textSize = 20f
                downBtn.setPadding(UiHelpers.dp(a, 4), 0, UiHelpers.dp(a, 4), 0)
                downBtn.setOnClickListener {
                    if (index < items.size - 1) {
                        val tmp = items[index]
                        items[index] = items[index + 1]
                        items[index + 1] = tmp
                        rebuildChecklist()
                    }
                }
                row.addView(downBtn)
                val delBtn = TextView(a)
                delBtn.text = "✕"
                delBtn.textSize = 18f
                delBtn.setPadding(UiHelpers.dp(a, 4), 0, 0, 0)
                delBtn.setOnClickListener {
                    items.removeAt(index)
                    rebuildChecklist()
                }
                row.addView(delBtn)
                checkBox.addView(row)
            }
        }
        if (type == "checklist") {
            rebuildChecklist()
            val addBtn = TextView(a)
            addBtn.text = Lang.str(a, "addItem")
            addBtn.textSize = 16f
            addBtn.setTypeface(null, Typeface.BOLD)
            addBtn.setTextColor(Color.parseColor("#7A6FD0"))
            addBtn.setPadding(0, UiHelpers.dp(a, 12), 0, 0)
            addBtn.setOnClickListener {
                items.add(CheckItem("", false))
                rebuildChecklist()
            }
            paper.addView(addBtn)
        }

        scroll.addView(paper)
        outer.addView(scroll)

        // ქვემოთ განიერი ოვალური შენახვა
        val saveBar = LinearLayout(a)
        saveBar.orientation = LinearLayout.HORIZONTAL
        saveBar.gravity = Gravity.CENTER
        saveBar.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 12), UiHelpers.dp(a, 20), UiHelpers.dp(a, 16))
        val saveBtn = TextView(a)
        saveBtn.text = "💜  " + Lang.str(a, "saveToPaper")
        saveBtn.textSize = 17f
        saveBtn.setTypeface(null, Typeface.BOLD)
        saveBtn.gravity = Gravity.CENTER
        saveBtn.setTextColor(Color.WHITE)
        saveBtn.background = pill(a, "#B9AFFF")
        saveBtn.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 16), UiHelpers.dp(a, 20), UiHelpers.dp(a, 16))
        val saveParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        saveBtn.layoutParams = saveParams
        saveBtn.setOnClickListener {
            val title = titleEt.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(a, Lang.str(a, "emptyTitle"), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isNew) {
                val realDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val nowMs = System.currentTimeMillis()
                val newNote = Note(a.nextId++, title, textEt?.text?.toString() ?: "", folder, false, realDate, type, items, nowMs, nowMs)
                a.notes.add(newNote)
            } else if (note != null) {
                note.title = title
                // createdAt-ს აქ არ ვიგონებთ — თუ 0-ია, ჩატვირთვისას უკვე აღდგა
                // ჩანაწერის საკუთარი თარიღიდან; თუ ვერ აღდგა, ჩვენება იმ თარიღს აჩვენებს
                note.updatedAt = System.currentTimeMillis()
                if (type == "text") note.text = textEt?.text?.toString() ?: note.text
                note.noteType = type
                note.checklist = items
                note.folder = folder
            }
            a.saveNotes()
            a.showHomePage()
        }
        saveBar.addView(saveBtn)
        outer.addView(saveBar)

        a.setContentView(outer)
        a.currentScreen = "editor"
    }
}