package ge.kakha.mynote

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object CalendarPage {

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

    private fun locale(a: MainActivity): Locale {
        return when (Lang.code(a)) {
            "en" -> Locale.ENGLISH
            "it" -> Locale.ITALIAN
            "ru" -> Locale("ru")
            else -> Locale("ka")
        }
    }

    private fun iso(y: Int, m: Int, d: Int): String {
        return String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
    }

    // ნოუტის თარიღის ნორმალიზაცია: "დღეს" / "today" / "Oggi" / "Сегодня" = დღევანდელი დღე,
    // "ხვალ" = ხვალ, "გუშინ" = გუშინ, დანარჩენი — yyyy-MM-dd, სხვა ფორმატი ვერ მიებმება დღეს
    private fun noteIso(date: String): String? {
        val today = Calendar.getInstance()
        fun shift(days: Int): String {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_MONTH, days)
            return String.format(Locale.getDefault(), "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
        }
        val todayIso = shift(0)
        return when (date) {
            "დღეს", "today", "Today", "Oggi", "Сегодня", todayIso -> todayIso
            "ხვალ", "tomorrow", "Tomorrow", "Domani", "Завтра" -> shift(1)
            "გუშინ", "yesterday", "Yesterday", "Ieri", "Вчера" -> shift(-1)
            else -> if (date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) date else null
        }
    }

    // ამ დღეს ჩანაწერები: ნოუტის თარიღით ან შეხსენებით
    private fun notesForDay(a: MainActivity, dayIso: String): List<Note> {
        val ids = mutableSetOf<Int>()
        val prefs = a.getSharedPreferences("mynote", 0)
        for ((k, v) in prefs.all) {
            if (k.startsWith("reminder_") && k.indexOf("_", 9) < 0 && v is Long) {
                val c = Calendar.getInstance()
                c.timeInMillis = v
                val rIso = String.format(Locale.getDefault(), "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
                if (rIso == dayIso) {
                    k.removePrefix("reminder_").toIntOrNull()?.let { ids.add(it) }
                }
            }
        }
        val out = mutableListOf<Note>()
        for (n in a.notes) {
            // წაშლილი და არქივში გადატანილი ნოუტი კალენდარში არ ჩანს
            if (n.isArchived || n.isDeleted) continue
            if (noteIso(n.date) == dayIso || ids.contains(n.id)) out.add(n)
        }
        out.sortWith(compareByDescending<Note> { it.isFav }.thenByDescending { it.id })
        return out
    }

    private fun hasNotes(a: MainActivity, dayIso: String): Boolean {
        // წერტილი დღეზე მხოლოდ აქტიურ ნოუტებზე ჩანს
        if (a.notes.any { !it.isArchived && !it.isDeleted && noteIso(it.date) == dayIso }) return true
        val prefs = a.getSharedPreferences("mynote", 0)
        for ((k, v) in prefs.all) {
            if (k.startsWith("reminder_") && k.indexOf("_", 9) < 0 && v is Long) {
                val c = Calendar.getInstance()
                c.timeInMillis = v
                val rIso = String.format(Locale.getDefault(), "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
                if (rIso == dayIso) {
                    // შეხსენება წაშლილ/არქივის ნოუტზე წერტილს არ ანთებს
                    val nid = k.removePrefix("reminder_").toIntOrNull()
                    val n = a.notes.firstOrNull { it.id == nid }
                    if (n != null && !n.isArchived && !n.isDeleted) return true
                }
            }
        }
        return false
    }

    fun show(a: MainActivity, year: Int = -1, month: Int = -1, selDay: Int = -1) {
        val now = Calendar.getInstance()
        val y = if (year < 0) now.get(Calendar.YEAR) else year
        val m = if (month < 0) now.get(Calendar.MONTH) else month
        val sel = if (selDay < 0) now.get(Calendar.DAY_OF_MONTH) else selDay
        val loc = locale(a)

        val frame = FrameLayout(a)
        frame.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        frame.setBackgroundColor(Color.parseColor(Theme.bg(a)))

        val outer = LinearLayout(a)
        outer.orientation = LinearLayout.VERTICAL
        outer.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

        // ზედა ზოლი: უკან | თვე წელი | < >
        val topBar = LinearLayout(a)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 16), UiHelpers.dp(a, 20), UiHelpers.dp(a, 14))
        val back = Icons.backButton(a) { a.handleBack() }
        topBar.addView(back)
        val title = TextView(a)
        val monthCal = Calendar.getInstance()
        monthCal.set(y, m, 1)
        title.text = SimpleDateFormat("LLLL yyyy", loc).format(monthCal.time).replaceFirstChar { it.uppercaseChar() }
        title.textSize = 19f
        title.setTypeface(null, Typeface.BOLD)
        title.gravity = Gravity.CENTER
        title.setTextColor(Color.parseColor(Theme.title(a)))
        title.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        topBar.addView(title)
        val prev = TextView(a)
        prev.text = "  " + Pictograms.chevronLeft + "  "
        prev.textSize = 26f
        prev.setTypeface(null, Typeface.BOLD)
        prev.setTextColor(Color.parseColor("#7A6FF0"))
        prev.setOnClickListener {
            val nm = if (m == 0) 11 else m - 1
            val ny = if (m == 0) y - 1 else y
            show(a, ny, nm, 1)
        }
        topBar.addView(prev)
        val next = TextView(a)
        next.text = "  " + Pictograms.chevronRight + "  "
        next.textSize = 26f
        next.setTypeface(null, Typeface.BOLD)
        next.setTextColor(Color.parseColor("#7A6FF0"))
        next.setOnClickListener {
            val nm = if (m == 11) 0 else m + 1
            val ny = if (m == 11) y + 1 else y
            show(a, ny, nm, 1)
        }
        topBar.addView(next)
        outer.addView(topBar)

        // კვირის დღეები: ორშაბათიდან
        val weekRow = LinearLayout(a)
        weekRow.orientation = LinearLayout.HORIZONTAL
        weekRow.setPadding(UiHelpers.dp(a, 8), UiHelpers.dp(a, 4), UiHelpers.dp(a, 8), UiHelpers.dp(a, 4))
        val wdCal = Calendar.getInstance()
        wdCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        for (i in 0 until 7) {
            val wd = TextView(a)
            wd.text = SimpleDateFormat("EEE", loc).format(wdCal.time).replaceFirstChar { it.uppercaseChar() }
            wd.textSize = 13f
            wd.setTypeface(null, Typeface.BOLD)
            wd.gravity = Gravity.CENTER
            wd.setTextColor(Color.parseColor("#888888"))
            wd.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            weekRow.addView(wd)
            wdCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        outer.addView(weekRow)

        // თვის ბადე
        val first = Calendar.getInstance()
        first.set(y, m, 1)
        val offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        var day = 1
        for (rowIdx in 0 until 6) {
            if (day > daysInMonth && rowIdx > 0) break
            val row = LinearLayout(a)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(UiHelpers.dp(a, 8), 0, UiHelpers.dp(a, 8), 0)
            for (col in 0 until 7) {
                val cell = LinearLayout(a)
                cell.orientation = LinearLayout.VERTICAL
                cell.gravity = Gravity.CENTER
                cell.setPadding(0, UiHelpers.dp(a, 6), 0, UiHelpers.dp(a, 6))
                cell.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                val cellDay = if (rowIdx == 0 && col < offset) 0 else if (day <= daysInMonth) day++ else 0
                if (cellDay > 0) {
                    val dayIso = iso(y, m, cellDay)
                    val isSel = cellDay == sel
                    val isToday = y == now.get(Calendar.YEAR) && m == now.get(Calendar.MONTH) && cellDay == now.get(Calendar.DAY_OF_MONTH)
                    val bg = GradientDrawable()
                    bg.shape = GradientDrawable.OVAL
                    if (isSel) bg.setColor(Color.parseColor("#7A6FF0"))
                    else if (isToday) bg.setColor(Color.parseColor(Theme.pill(a)))
                    cell.background = bg
                    val num = TextView(a)
                    num.text = cellDay.toString()
                    num.textSize = 15f
                    num.gravity = Gravity.CENTER
                    num.setTextColor(if (isSel) Color.WHITE else Color.parseColor(Theme.title(a)))
                    if (isToday || isSel) num.setTypeface(null, Typeface.BOLD)
                    cell.addView(num)
                    val dot = TextView(a)
                    dot.text = if (hasNotes(a, dayIso)) Pictograms.dot else ""
                    dot.textSize = 7f
                    dot.gravity = Gravity.CENTER
                    dot.setTextColor(Color.parseColor("#E8A020"))
                    cell.addView(dot)
                    cell.setOnClickListener { show(a, y, m, cellDay) }
                }
                row.addView(cell)
            }
            outer.addView(row)
        }

        // არჩეული დღის ჩანაწერები
        val dayTitle = TextView(a)
        val selCal = Calendar.getInstance()
        selCal.set(y, m, sel)
        dayTitle.text = SimpleDateFormat("d LLLL", loc).format(selCal.time)
        dayTitle.textSize = 17f
        dayTitle.setTypeface(null, Typeface.BOLD)
        dayTitle.setTextColor(Color.parseColor(Theme.title(a)))
        dayTitle.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 16), UiHelpers.dp(a, 20), UiHelpers.dp(a, 6))
        outer.addView(dayTitle)

        val scroll = ScrollView(a)
        scroll.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        val listBox = LinearLayout(a)
        listBox.orientation = LinearLayout.VERTICAL
        listBox.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 4), UiHelpers.dp(a, 20), UiHelpers.dp(a, 8))
        val dayNotes = notesForDay(a, iso(y, m, sel))
        if (dayNotes.isEmpty()) {
            val empty = TextView(a)
            empty.text = Lang.str(a, "noNotesDay")
            empty.textSize = 15f
            empty.gravity = Gravity.CENTER
            empty.setTextColor(Color.parseColor(Theme.sub(a)))
            empty.setPadding(0, UiHelpers.dp(a, 24), 0, 0)
            listBox.addView(empty)
        } else {
            for (n in dayNotes) {
                // ნოუტი თავისი სტილით — თავისი კატეგორიის ფერის ბარათი,
                // შიგნით მარტო სათაური, როგორც აქამდე იყო
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
                outerRow.setOnClickListener { a.showView(n) }

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

                val titleTv = TextView(a)
                titleTv.text = (if (a.isNoteLocked(n.id)) Pictograms.lock + "  " else "") + n.title
                titleTv.textSize = 16f
                titleTv.setTypeface(null, Typeface.BOLD)
                titleTv.setTextColor(Color.BLACK)
                row.addView(titleTv)

                outerRow.addView(row)
                listBox.addView(outerRow)
            }
        }
        scroll.addView(listBox)
        outer.addView(scroll)

        outer.addView(HomePage.bottomNav(a, "calendar"))
        // + ღილაკი კალენდარში არ ჩანს
        frame.addView(outer)
        a.setContentView(frame)
        a.currentScreen = "calendar"
    }
}
