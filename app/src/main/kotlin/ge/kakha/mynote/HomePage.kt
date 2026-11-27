package ge.kakha.mynote

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

object HomePage {

    private fun pill(a: MainActivity, color: String): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = UiHelpers.dp(a, 24).toFloat()
        d.setColor(Color.parseColor(color))
        return d
    }

    private fun line(a: MainActivity): TextView {
        val v = TextView(a)
        v.setBackgroundColor(Color.parseColor(Theme.line(a)))
        val p = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            UiHelpers.dp(a, 1)
        )
        v.layoutParams = p
        return v
    }

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

    private fun grayFilter(): ColorMatrixColorFilter {
        val m = ColorMatrix()
        m.setSaturation(0f)
        return ColorMatrixColorFilter(m)
    }

    // ქვედა ნავიგაცია: სახლი | კალენდარი | მენიუ | პარამეტრები — ყველა გვერდზე ერთნაირი
    fun bottomNav(a: MainActivity, active: String): LinearLayout {
        val bar = LinearLayout(a)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 10), UiHelpers.dp(a, 20), UiHelpers.dp(a, 16))
        fun navBtn(icon: String, activeNow: Boolean, onClick: () -> Unit): TextView {
            val b = TextView(a)
            b.text = icon
            b.textSize = 22f
            b.gravity = Gravity.CENTER
            b.background = pill(a, if (activeNow) Theme.pillDark(a) else Theme.pill(a))
            if (activeNow) b.setTextColor(Color.parseColor("#EDE9FF")) else b.setTextColor(Color.parseColor(Theme.title(a)))
            b.setPadding(UiHelpers.dp(a, 16), UiHelpers.dp(a, 14), UiHelpers.dp(a, 16), UiHelpers.dp(a, 14))
            val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            p.rightMargin = UiHelpers.dp(a, 8)
            b.layoutParams = p
            b.setOnClickListener { onClick() }
            return b
        }
        bar.addView(navBtn("" + Pictograms.home + "", active == "home") { if (active != "home") a.showHomePage() })
        bar.addView(navBtn("" + Pictograms.calendar + "", active == "calendar") { if (active != "calendar") a.showCalendar() })
        bar.addView(navBtn("" + Pictograms.menu + "", active == "menu") { if (active != "menu") a.showMenu() })
        // სეთინგები ქვედა პანელიდან მოვხსენით — მენიუშია
        return bar
    }

    // მცურავი მრგვალი ლურჯი + ღილაკი, ქვემოთ მარჯვნივ
    fun fabView(a: MainActivity): TextView {
        val fab = TextView(a)
        fab.text = "+"
        fab.textSize = 32f
        fab.setTypeface(null, Typeface.BOLD)
        fab.gravity = Gravity.CENTER
        fab.setTextColor(Color.WHITE)
        val fabBg = GradientDrawable()
        fabBg.shape = GradientDrawable.OVAL
        fabBg.setColor(Color.parseColor("#2196F3"))
        fab.background = fabBg
        fab.elevation = UiHelpers.dp(a, 8).toFloat()
        val fabParams = FrameLayout.LayoutParams(UiHelpers.dp(a, 64), UiHelpers.dp(a, 64))
        fabParams.gravity = Gravity.BOTTOM or Gravity.END
        fabParams.rightMargin = UiHelpers.dp(a, 20)
        fabParams.bottomMargin = UiHelpers.dp(a, 96)
        fab.layoutParams = fabParams
        fab.setOnClickListener { a.showCreatePicker() }
        return fab
    }

    fun show(a: MainActivity) {
        val frame = FrameLayout(a)
        frame.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        frame.setBackgroundColor(Color.parseColor(Theme.bg(a)))

        val outer = LinearLayout(a)
        outer.orientation = LinearLayout.VERTICAL
        outer.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        val topBar = LinearLayout(a)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 16), UiHelpers.dp(a, 20), UiHelpers.dp(a, 14))

        val leafL = TextView(a)
        leafL.text = "" + Pictograms.leaf + ""
        leafL.textSize = 28f
        topBar.addView(leafL)

        val centerWrap = LinearLayout(a)
        centerWrap.gravity = Gravity.CENTER
        val centerParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        centerWrap.layoutParams = centerParams

        val centerPill = TextView(a)
        centerPill.text = Lang.str(a, "app")
        centerPill.textSize = 19f
        centerPill.setTypeface(null, Typeface.BOLD)
        centerPill.gravity = Gravity.CENTER
        centerPill.setTextColor(Color.parseColor(Theme.title(a)))
        centerPill.background = pill(a, Theme.pill(a))
        centerPill.setPadding(UiHelpers.dp(a, 36), UiHelpers.dp(a, 12), UiHelpers.dp(a, 36), UiHelpers.dp(a, 12))
        centerWrap.addView(centerPill)
        topBar.addView(centerWrap)

        val leafR = TextView(a)
        leafR.text = "" + Pictograms.leaf + ""
        leafR.textSize = 28f
        leafR.setOnClickListener { a.showSettings() }
        topBar.addView(leafR)

        outer.addView(topBar)
        outer.addView(line(a))

        val search = EditText(a)
        search.hint = "" + Pictograms.search + "  " + Lang.str(a, "search")
        search.textSize = 17f
        search.setTextColor(Color.parseColor(Theme.text(a)))
        search.setHintTextColor(Color.parseColor(Theme.hint(a)))
        search.setBackgroundColor(Color.TRANSPARENT)
        val searchParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        searchParams.leftMargin = UiHelpers.dp(a, 24)
        searchParams.rightMargin = UiHelpers.dp(a, 24)
        search.layoutParams = searchParams
        search.setPadding(UiHelpers.dp(a, 4), UiHelpers.dp(a, 14), UiHelpers.dp(a, 4), UiHelpers.dp(a, 14))
        search.setText(a.query)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                a.query = s.toString()
                refresh(a)
            }
        })
        outer.addView(search)

        val searchLineWrap = LinearLayout(a)
        searchLineWrap.setPadding(UiHelpers.dp(a, 20), 0, UiHelpers.dp(a, 20), 0)
        searchLineWrap.addView(line(a))
        outer.addView(searchLineWrap)

        val catScroll = HorizontalScrollView(a)
        catScroll.isHorizontalScrollBarEnabled = false
        val catRow = LinearLayout(a)
        catRow.orientation = LinearLayout.HORIZONTAL
        catRow.gravity = Gravity.CENTER_VERTICAL
        catRow.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 4), UiHelpers.dp(a, 20), UiHelpers.dp(a, 4))
        for (cat in a.cats) {
            val chip = TextView(a)
            val catName = if (cat == "ყველა") Lang.str(a, "all") else cat
            val dotText = SpannableString("" + Pictograms.dot + "  " + catName)
            dotText.setSpan(
                ForegroundColorSpan(Color.parseColor(getCatColor(a, cat))),
                0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            dotText.setSpan(
                RelativeSizeSpan(1.5f),
                0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            chip.text = dotText
            chip.textSize = 14f
            chip.setTypeface(null, Typeface.BOLD)
            chip.setTextColor(Color.parseColor(Theme.title(a)))
            chip.isSingleLine = true
            if (a.filter == cat) {
                chip.background = pill(a, Theme.pill(a))
                chip.setPadding(UiHelpers.dp(a, 14), UiHelpers.dp(a, 5), UiHelpers.dp(a, 14), UiHelpers.dp(a, 5))
            } else {
                chip.setPadding(UiHelpers.dp(a, 10), UiHelpers.dp(a, 5), UiHelpers.dp(a, 10), UiHelpers.dp(a, 5))
            }
            val chipParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            chipParams.rightMargin = UiHelpers.dp(a, 8)
            chip.layoutParams = chipParams
            chip.setOnClickListener {
                a.filter = cat
                show(a)
            }
            catRow.addView(chip)
        }
        catScroll.addView(catRow)
        outer.addView(catScroll)

        val catLineWrap = LinearLayout(a)
        catLineWrap.setPadding(UiHelpers.dp(a, 20), 0, UiHelpers.dp(a, 20), 0)
        catLineWrap.addView(line(a))
        outer.addView(catLineWrap)

        val scroll = ScrollView(a)
        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
        scroll.layoutParams = scrollParams
        a.paperBox = LinearLayout(a)
        a.paperBox.orientation = LinearLayout.VERTICAL
        a.paperBox.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 8), UiHelpers.dp(a, 20), UiHelpers.dp(a, 8))
        scroll.addView(a.paperBox)
        outer.addView(scroll)

        outer.addView(bottomNav(a, "home"))

        frame.addView(outer)

        frame.addView(fabView(a))

        a.setContentView(frame)
        a.currentScreen = "home"
        refresh(a)
    }

    fun refresh(a: MainActivity) {
        a.paperBox.removeAllViews()
        val list = a.getFiltered()
        if (list.isEmpty()) {
            val empty = TextView(a)
            empty.text = Lang.str(a, "noNotes")
            empty.textSize = 16f
            empty.gravity = Gravity.CENTER
            empty.setTextColor(Color.parseColor(Theme.sub(a)))
            empty.setPadding(0, UiHelpers.dp(a, 40), 0, 0)
            a.paperBox.addView(empty)
            return
        }
        for (n in list) {
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
            val barParams = LinearLayout.LayoutParams(
                UiHelpers.dp(a, 14),
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            bar.layoutParams = barParams
            outerRow.addView(bar)

            val row = LinearLayout(a)
            row.orientation = LinearLayout.VERTICAL
            row.setPadding(UiHelpers.dp(a, 14), UiHelpers.dp(a, 12), UiHelpers.dp(a, 14), UiHelpers.dp(a, 12))
            val rowParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            row.layoutParams = rowParams

            val topRow = LinearLayout(a)
            topRow.orientation = LinearLayout.HORIZONTAL
            topRow.gravity = Gravity.CENTER_VERTICAL
            val titleTv = TextView(a)
            titleTv.text = if (a.isNoteLocked(n.id)) n.title + "  " + Pictograms.lock + "" else n.title
            titleTv.textSize = 19f
            titleTv.setTypeface(null, Typeface.BOLD)
            titleTv.setTextColor(Color.BLACK)
            val titleParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            titleTv.layoutParams = titleParams
            topRow.addView(titleTv)
            val favBtn = TextView(a)
            // ჩართული — ძველი Pictograms.star, გამორთული — იგივე Pictograms.star ოღონდ ნაცრისფერი ფილტრით
            favBtn.text = "" + Pictograms.star + ""
            favBtn.textSize = 22f
            favBtn.paint.colorFilter = if (n.isFav) null else grayFilter()
            favBtn.setPadding(UiHelpers.dp(a, 8), 0, 0, 0)
            favBtn.setOnClickListener {
                n.isFav = !n.isFav
                a.saveNotes()
                refresh(a)
            }
            topRow.addView(favBtn)
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
            preview.text = if (a.isNoteLocked(n.id)) {
                "••••••••"
            } else if (n.noteType == "checklist") {
                val total = n.checklist.size
                val done = n.checklist.count { it.done }
                Lang.str(a, "checklist") + " • " + done.toString() + "/" + total.toString()
            } else {
                n.text
            }
            preview.textSize = 15f
            preview.setTextColor(Color.BLACK)
            preview.maxLines = 3
            row.addView(preview)

            outerRow.addView(row)
            outerRow.setOnClickListener { a.showView(n) }
            a.paperBox.addView(outerRow)
        }
    }
}