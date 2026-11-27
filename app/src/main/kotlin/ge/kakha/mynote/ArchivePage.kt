package ge.kakha.mynote

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

object ArchivePage {


    private fun getCatColor(a: MainActivity, cat: String): String {
        val p = a.getSharedPreferences("mynote", 0)
        return p.getString("catColor_" + cat, "#B9AFFF") ?: "#B9AFFF"
    }

    private fun paleColor(hex: String): Int {
        val c = Color.parseColor(hex)
        val r = Color.red(c)
        val g = Color.green(c)
        val b = Color.blue(c)
        val nr = (r * 0.07f + 255 * 0.93f).toInt()
        val ng = (g * 0.07f + 255 * 0.93f).toInt()
        val nb = (b * 0.07f + 255 * 0.93f).toInt()
        return Color.rgb(nr, ng, nb)
    }

    fun show(a: MainActivity) {
        val frame = FrameLayout(a)
        frame.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        frame.setBackgroundColor(Color.parseColor(Theme.bg(a)))

        val outer = LinearLayout(a)
        outer.orientation = LinearLayout.VERTICAL
        outer.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

        val topBar = LinearLayout(a)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 16), UiHelpers.dp(a, 20), UiHelpers.dp(a, 14))
        val back = Icons.backButton(a) { a.handleBack() }
        topBar.addView(back)
        val title = TextView(a)
        title.text = Lang.str(a, "archive")
        title.textSize = 19f
        title.setTypeface(null, Typeface.BOLD)
        title.gravity = Gravity.CENTER
        title.setTextColor(Color.parseColor(Theme.title(a)))
        title.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        topBar.addView(title)
        outer.addView(topBar)

        val scroll = ScrollView(a)
        scroll.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        val content = LinearLayout(a)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 4), UiHelpers.dp(a, 20), UiHelpers.dp(a, 8))

        val archived = a.notes.filter { it.isArchived && !it.isDeleted }
        if (archived.isEmpty()) {
            val empty = TextView(a)
            empty.text = Lang.str(a, "archiveEmpty")
            empty.textSize = 16f
            empty.gravity = Gravity.CENTER
            empty.setTextColor(Color.parseColor(Theme.sub(a)))
            empty.setPadding(0, UiHelpers.dp(a, 40), 0, 0)
            content.addView(empty)
        }
        for (n in archived) {
            // იგივე ბარათი, რაც მთავარზე — უბრალოდ ტექსტის გარეშე (••••••••)
            val catColor = getCatColor(a, n.folder)
            val outerRow = LinearLayout(a)
            outerRow.orientation = LinearLayout.HORIZONTAL
            val outerBg = GradientDrawable()
            outerBg.shape = GradientDrawable.RECTANGLE
            outerBg.cornerRadius = UiHelpers.dp(a, 22).toFloat()
            outerBg.setColor(paleColor(catColor))
            outerRow.background = outerBg
            outerRow.clipToOutline = true
            val outerParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            outerParams.bottomMargin = UiHelpers.dp(a, 10)
            outerRow.layoutParams = outerParams

            val bar = View(a)
            val barBg = GradientDrawable()
            barBg.shape = GradientDrawable.RECTANGLE
            val r = UiHelpers.dp(a, 22).toFloat()
            barBg.cornerRadii = floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r)
            barBg.setColor(Color.parseColor(catColor))
            bar.background = barBg
            bar.layoutParams = LinearLayout.LayoutParams(UiHelpers.dp(a, 14), LinearLayout.LayoutParams.MATCH_PARENT)
            outerRow.addView(bar)

            val row = LinearLayout(a)
            row.orientation = LinearLayout.VERTICAL
            row.setPadding(UiHelpers.dp(a, 14), UiHelpers.dp(a, 12), UiHelpers.dp(a, 14), UiHelpers.dp(a, 12))
            row.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val topRow = LinearLayout(a)
            topRow.orientation = LinearLayout.HORIZONTAL
            topRow.gravity = Gravity.CENTER_VERTICAL
            val t = TextView(a)
            t.text = if (a.isNoteLocked(n.id)) n.title + "  " + Pictograms.lock + "" else n.title
            t.textSize = 19f
            t.setTypeface(null, Typeface.BOLD)
            t.setTextColor(Color.BLACK)
            t.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            topRow.addView(t)
            val unBtn = Icons.restoreButton(a) {
                n.isArchived = false
                a.saveNotes()
                Toast.makeText(a, Lang.str(a, "unarchivedMsg"), Toast.LENGTH_SHORT).show()
                show(a)
            }
            topRow.addView(unBtn)
            row.addView(topRow)

            val meta = TextView(a)
            val folderName = when (n.folder) { "ყველა" -> Lang.str(a, "all")
                "პირადი" -> Lang.str(a, "personal")
                else -> n.folder }
            meta.text = "" + Pictograms.tag + "  " + folderName + "  •  " + n.date
            meta.textSize = 13f
            meta.setTextColor(Color.parseColor("#444444"))
            meta.setPadding(0, UiHelpers.dp(a, 4), 0, UiHelpers.dp(a, 4))
            row.addView(meta)

            val preview = TextView(a)
            preview.text = "••••••••"
            preview.textSize = 15f
            preview.setTextColor(Color.BLACK)
            preview.maxLines = 3
            row.addView(preview)

            // მთელ ბარათზე დაჭერა — ნოუტის გახსნა (🔁 ღილაკი თავისას აკეთებს)
            outerRow.setOnClickListener { a.showView(n) }
            outerRow.addView(row)
            content.addView(outerRow)
        }

        scroll.addView(content)
        outer.addView(scroll)
        // ქვედა ღილაკები აქ არ ჩანს — სეთინგებში როგორცაა
        frame.addView(outer)
        a.setContentView(frame)
        a.currentScreen = "archivePage"
    }
}
