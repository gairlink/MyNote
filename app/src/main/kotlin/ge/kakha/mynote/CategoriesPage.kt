package ge.kakha.mynote

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

object CategoriesPage {

    private val COLORS = listOf(
        "#B9AFFF",
        "#7A6FD0",
        "#3D3566",
        "#D8C2FF",
        "#EDE9FF",
        "#FFB3B3",
        "#FF7A7A",
        "#C0392B",
        "#FFD88A",
        "#FFA726",
        "#FFE27A",
        "#A8E6A1",
        "#4CAF50",
        "#2E7D32",
        "#8AD6FF",
        "#2196F3",
        "#0D47A1",
        "#FF9ED6",
        "#E91E63",
        "#9C27B0",
        "#8D6E63",
        "#78909C",
        "#111111",
        "#FFFFFF"
    )

    private fun getCatColor(a: MainActivity, cat: String): String {
        val p = a.getSharedPreferences("mynote", 0)
        return p.getString("catColor_" + cat, "#B9AFFF") ?: "#B9AFFF"
    }

    private fun setCatColor(a: MainActivity, cat: String, color: String) {
        a.getSharedPreferences("mynote", 0).edit().putString("catColor_" + cat, color).apply()
    }

    private fun removeCatColor(a: MainActivity, cat: String) {
        a.getSharedPreferences("mynote", 0).edit().remove("catColor_" + cat).apply()
    }

    private fun circle(a: MainActivity, color: String, size: Int): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.OVAL
        d.setColor(Color.parseColor(color))
        d.setSize(size, size)
        return d
    }

    private fun colorDialog(a: MainActivity, cat: String) {
        UiDialog.show(a, cat) { box, dialog ->
            val size = UiHelpers.dp(a, 48)
            var row: LinearLayout? = null
            for (i in COLORS.indices) {
                if (i % 4 == 0) {
                    row = LinearLayout(a)
                    row.orientation = LinearLayout.HORIZONTAL
                    row.gravity = Gravity.CENTER
                    row.setPadding(0, UiHelpers.dp(a, 6), 0, UiHelpers.dp(a, 6))
                    box.addView(row)
                }
                val color = COLORS[i]
                val dot = TextView(a)
                dot.text = ""
                dot.background = circle(a, color, size)
                val params = LinearLayout.LayoutParams(size, size)
                params.leftMargin = UiHelpers.dp(a, 8)
                params.rightMargin = UiHelpers.dp(a, 8)
                dot.layoutParams = params
                dot.setOnClickListener {
                    setCatColor(a, cat, color)
                    dialog.dismiss()
                    show(a)
                }
                row?.addView(dot)
            }
        }
    }

    fun show(a: MainActivity) {
        val outer = LinearLayout(a)
        outer.orientation = LinearLayout.VERTICAL
        outer.setBackgroundColor(Color.parseColor(Theme.bg(a)))

        outer.addView(UiHelpers.titleBar(a, Lang.str(a, "catTitle")) { a.showSettings() })

        val scroll = ScrollView(a)
        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )
        scroll.layoutParams = scrollParams
        val box = LinearLayout(a)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(UiHelpers.dp(a, 16), UiHelpers.dp(a, 8), UiHelpers.dp(a, 16), UiHelpers.dp(a, 16))

        for (cat in a.cats) {
            val row = LinearLayout(a)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setBackgroundColor(Color.parseColor(if (Theme.isDark(a)) "#23232F" else "#FFFFFF"))
            row.setPadding(UiHelpers.dp(a, 16), UiHelpers.dp(a, 14), UiHelpers.dp(a, 16), UiHelpers.dp(a, 14))
            val rowParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            rowParams.bottomMargin = UiHelpers.dp(a, 8)
            row.layoutParams = rowParams

            val name = TextView(a)
            name.text = if (cat == "ყველა") Lang.str(a, "all") else cat
            name.textSize = 17f
            name.setTextColor(Color.parseColor(Theme.text(a)))
            val nameParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            name.layoutParams = nameParams
            row.addView(name)

            if (cat != "ყველა") {
                val colorBtn = TextView(a)
                colorBtn.text = ""
                colorBtn.background = circle(a, getCatColor(a, cat), UiHelpers.dp(a, 28))
                val colorParams = LinearLayout.LayoutParams(UiHelpers.dp(a, 28), UiHelpers.dp(a, 28))
                colorParams.rightMargin = UiHelpers.dp(a, 12)
                colorBtn.layoutParams = colorParams
                colorBtn.setOnClickListener { colorDialog(a, cat) }
                row.addView(colorBtn)

                val renameBtn = TextView(a)
                renameBtn.text = Lang.str(a, "rename")
                renameBtn.textSize = 14f
                renameBtn.setTypeface(null, Typeface.BOLD)
                renameBtn.setTextColor(Color.parseColor(Theme.sub(a)))
                renameBtn.setPadding(UiHelpers.dp(a, 8), 0, UiHelpers.dp(a, 12), 0)
                renameBtn.setOnClickListener { renameDialog(a, cat) }
                row.addView(renameBtn)

                val delBtn = TextView(a)
                delBtn.text = "" + Pictograms.trash + ""
                delBtn.textSize = 18f
                delBtn.setOnClickListener {
                    UiDialog.show(a, Lang.str(a, "delete")) { dBox, dDialog ->
                        val dMsg = TextView(a)
                        dMsg.text = Lang.str(a, "catDeleteConfirm")
                        dMsg.textSize = 16f
                        dMsg.setTextColor(Color.parseColor(Theme.text(a)))
                        dBox.addView(dMsg)
                        val dYes = TextView(a)
                        dYes.text = Lang.str(a, "yesDelete")
                        dYes.textSize = 16f
                        dYes.setTypeface(null, Typeface.BOLD)
                        dYes.gravity = Gravity.CENTER
                        dYes.setTextColor(Color.WHITE)
                        dYes.setBackgroundColor(Color.parseColor("#C0392B"))
                        dYes.setPadding(UiHelpers.dp(a, 12), UiHelpers.dp(a, 12), UiHelpers.dp(a, 12), UiHelpers.dp(a, 12))
                        val dYesParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        dYesParams.topMargin = UiHelpers.dp(a, 12)
                        dYes.layoutParams = dYesParams
                        dYes.setOnClickListener {
                            for (n in a.notes) { if (n.folder == cat) n.folder = "პირადი" }
                            a.cats.remove(cat)
                            a.saveCats()
                            a.saveNotes()
                            removeCatColor(a, cat)
                            dDialog.dismiss()
                            show(a)
                        }
                        dBox.addView(dYes)
                    }
                }
                row.addView(delBtn)
            }
            box.addView(row)
        }

        val addBtn = TextView(a)
        addBtn.text = Lang.str(a, "addCat")
        addBtn.textSize = 16f
        addBtn.setTypeface(null, Typeface.BOLD)
        addBtn.gravity = Gravity.CENTER
        addBtn.setTextColor(Color.parseColor("#FFFFFF"))
        addBtn.setBackgroundColor(Color.parseColor(Theme.pillDark(a)))
        addBtn.setPadding(UiHelpers.dp(a, 16), UiHelpers.dp(a, 14), UiHelpers.dp(a, 16), UiHelpers.dp(a, 14))
        val addParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        addParams.topMargin = UiHelpers.dp(a, 8)
        addBtn.layoutParams = addParams
        addBtn.setOnClickListener { addDialog(a) }
        box.addView(addBtn)

        scroll.addView(box)
        outer.addView(scroll)

        a.setContentView(outer)
        a.currentScreen = "categories"
    }

    private fun addDialog(a: MainActivity) {
        UiDialog.show(a, Lang.str(a, "addCat")) { box, dialog ->
            val et = EditText(a)
            et.hint = Lang.str(a, "catNameHint")
            et.setPadding(UiHelpers.dp(a, 4), UiHelpers.dp(a, 12), UiHelpers.dp(a, 4), UiHelpers.dp(a, 12))
            box.addView(et)
            val saveBtn = TextView(a)
            saveBtn.text = Lang.str(a, "save")
            saveBtn.textSize = 16f
            saveBtn.setTypeface(null, Typeface.BOLD)
            saveBtn.gravity = Gravity.CENTER
            saveBtn.setTextColor(Color.WHITE)
            saveBtn.setBackgroundColor(Color.parseColor(Theme.pillDark(a)))
            saveBtn.setPadding(UiHelpers.dp(a, 12), UiHelpers.dp(a, 12), UiHelpers.dp(a, 12), UiHelpers.dp(a, 12))
            val saveParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            saveParams.topMargin = UiHelpers.dp(a, 12)
            saveBtn.layoutParams = saveParams
            saveBtn.setOnClickListener {
                val name = et.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(a, Lang.str(a, "enterCatName"), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (name == "ყველა" || a.cats.any { it.equals(name, true) }) {
                    Toast.makeText(a, Lang.str(a, "catExists"), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                a.cats.add(name)
                a.saveCats()
                dialog.dismiss()
                show(a)
            }
            box.addView(saveBtn)
        }
    }

    private fun renameDialog(a: MainActivity, oldName: String) {
        UiDialog.show(a, Lang.str(a, "renameCat")) { box, dialog ->
            val et = EditText(a)
            et.setText(oldName)
            et.setPadding(UiHelpers.dp(a, 4), UiHelpers.dp(a, 12), UiHelpers.dp(a, 4), UiHelpers.dp(a, 12))
            box.addView(et)
            val saveBtn = TextView(a)
            saveBtn.text = Lang.str(a, "save")
            saveBtn.textSize = 16f
            saveBtn.setTypeface(null, Typeface.BOLD)
            saveBtn.gravity = Gravity.CENTER
            saveBtn.setTextColor(Color.WHITE)
            saveBtn.setBackgroundColor(Color.parseColor(Theme.pillDark(a)))
            saveBtn.setPadding(UiHelpers.dp(a, 12), UiHelpers.dp(a, 12), UiHelpers.dp(a, 12), UiHelpers.dp(a, 12))
            val saveParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            saveParams.topMargin = UiHelpers.dp(a, 12)
            saveBtn.layoutParams = saveParams
            saveBtn.setOnClickListener {
                val name = et.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(a, Lang.str(a, "enterCatName"), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (name == "ყველა") { Toast.makeText(a, Lang.str(a, "catExists"), Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                if (a.cats.any { it.equals(name, true) && it != oldName }) { Toast.makeText(a, Lang.str(a, "catExists"), Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                val idx = a.cats.indexOf(oldName)
                if (idx >= 0) {
                    val oldColor = getCatColor(a, oldName)
                    a.cats[idx] = name
                    for (n in a.notes) {
                        if (n.folder == oldName) n.folder = name
                    }
                    a.saveCats()
                    a.saveNotes()
                    removeCatColor(a, oldName)
                    setCatColor(a, name, oldColor)
                    dialog.dismiss()
                    show(a)
                }
            }
            box.addView(saveBtn)
        }
    }
}