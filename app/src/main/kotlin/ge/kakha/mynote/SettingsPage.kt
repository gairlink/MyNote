package ge.kakha.mynote

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

object SettingsPage {

    private fun prefs(a: MainActivity) = a.getSharedPreferences("mynote", 0)

    private fun pill(a: MainActivity, color: String): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = UiHelpers.dp(a, 24).toFloat()
        d.setColor(Color.parseColor(color))
        return d
    }

    private fun card(a: MainActivity): LinearLayout {
        val c = LinearLayout(a)
        c.orientation = LinearLayout.VERTICAL
        c.setBackgroundColor(Color.parseColor(if (Theme.isDark(a)) "#23232F" else "#FFFFFF"))
        val p = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        p.leftMargin = UiHelpers.dp(a, 16)
        p.rightMargin = UiHelpers.dp(a, 16)
        p.bottomMargin = UiHelpers.dp(a, 16)
        c.layoutParams = p
        c.setPadding(UiHelpers.dp(a, 8), UiHelpers.dp(a, 8), UiHelpers.dp(a, 8), UiHelpers.dp(a, 8))
        return c
    }

    private fun sectionTitle(a: MainActivity, title: String): TextView {
        val t = TextView(a)
        t.text = title
        t.textSize = 16f
        t.setTypeface(null, Typeface.BOLD)
        t.setTextColor(Color.parseColor(if (Theme.isDark(a)) "#B8B2E8" else "#8B7FE8"))
        t.setPadding(UiHelpers.dp(a, 8), UiHelpers.dp(a, 12), UiHelpers.dp(a, 8), UiHelpers.dp(a, 12))
        return t
    }

    private fun divider(a: MainActivity): TextView {
        val v = TextView(a)
        v.setBackgroundColor(Color.parseColor(Theme.line(a)))
        val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, UiHelpers.dp(a, 1))
        p.leftMargin = UiHelpers.dp(a, 8)
        p.rightMargin = UiHelpers.dp(a, 8)
        v.layoutParams = p
        return v
    }

    private fun toggleView(a: MainActivity, isOn: Boolean, onToggle: () -> Unit): LinearLayout {
        val wrap = LinearLayout(a)
        wrap.orientation = LinearLayout.HORIZONTAL
        wrap.gravity = Gravity.CENTER_VERTICAL
        val track = LinearLayout(a)
        track.orientation = LinearLayout.HORIZONTAL
        track.gravity = if (isOn) Gravity.END else Gravity.START
        track.background = pill(a, if (isOn) "#8B7FE8" else "#D8D8D8")
        track.setPadding(UiHelpers.dp(a, 3), UiHelpers.dp(a, 3), UiHelpers.dp(a, 3), UiHelpers.dp(a, 3))
        val trackParams = LinearLayout.LayoutParams(UiHelpers.dp(a, 48), UiHelpers.dp(a, 28))
        track.layoutParams = trackParams
        val thumb = TextView(a)
        thumb.setBackgroundColor(Color.WHITE)
        val thumbParams = LinearLayout.LayoutParams(UiHelpers.dp(a, 22), UiHelpers.dp(a, 22))
        thumb.layoutParams = thumbParams
        val thumbBg = GradientDrawable()
        thumbBg.shape = GradientDrawable.OVAL
        thumbBg.setColor(Color.WHITE)
        thumb.background = thumbBg
        track.addView(thumb)
        wrap.addView(track)
        wrap.setOnClickListener { onToggle() }
        return wrap
    }

    private fun row(a: MainActivity, icon: String, title: String, right: View?, onClick: (() -> Unit)?): LinearLayout {
        val r = LinearLayout(a)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.setPadding(UiHelpers.dp(a, 8), UiHelpers.dp(a, 14), UiHelpers.dp(a, 8), UiHelpers.dp(a, 14))
        val iconTv = TextView(a)
        iconTv.text = icon
        iconTv.textSize = 22f
        iconTv.setPadding(0, 0, UiHelpers.dp(a, 12), 0)
        r.addView(iconTv)
        val titleTv = TextView(a)
        titleTv.text = title
        titleTv.textSize = 17f
        titleTv.setTextColor(Color.parseColor(Theme.text(a)))
        val titleParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        titleTv.layoutParams = titleParams
        r.addView(titleTv)
        if (right != null) r.addView(right)
        if (onClick != null) r.setOnClickListener { onClick() }
        return r
    }

    private fun valueView(a: MainActivity, value: String): LinearLayout {
        val w = LinearLayout(a)
        w.orientation = LinearLayout.HORIZONTAL
        w.gravity = Gravity.CENTER_VERTICAL
        val v = TextView(a)
        v.text = value + "  ▼"
        v.textSize = 16f
        v.setTypeface(null, Typeface.BOLD)
        v.setTextColor(Color.parseColor(if (Theme.isDark(a)) "#B8B2E8" else "#8B7FE8"))
        w.addView(v)
        return w
    }

    private fun chevronView(a: MainActivity): TextView {
        val v = TextView(a)
        v.text = ""
        v.textSize = 18f
        v.setPadding(UiHelpers.dp(a, 8), 0, 0, 0)
        return v
    }

    private fun showSimpleDialog(a: MainActivity, title: String, message: String) {
        UiDialog.show(a, title) { box, dialog ->
            val msg = TextView(a)
            msg.text = message
            msg.textSize = 16f
            msg.setTextColor(Color.parseColor(Theme.text(a)))
            box.addView(msg)
        }
    }

    // არჩევის სია — წერტილების გარეშე, არჩეული ღია ლავანდის ფონით + ✓
    private fun showPicker(a: MainActivity, title: String, items: Array<String>, current: String, onPick: (String) -> Unit) {
        UiDialog.show(a, title) { box, dialog ->
            for (i in items.indices) {
                val isSelected = items[i] == current
                val itemRow = LinearLayout(a)
                itemRow.orientation = LinearLayout.HORIZONTAL
                itemRow.gravity = Gravity.CENTER_VERTICAL
                val bg = GradientDrawable()
                bg.shape = GradientDrawable.RECTANGLE
                bg.cornerRadius = UiHelpers.dp(a, 14).toFloat()
                if (isSelected) {
                    bg.setColor(Color.parseColor(if (Theme.isDark(a)) "#3D3566" else "#EDE9FF"))
                } else {
                    bg.setColor(Color.TRANSPARENT)
                }
                itemRow.background = bg
                itemRow.setPadding(UiHelpers.dp(a, 12), UiHelpers.dp(a, 12), UiHelpers.dp(a, 12), UiHelpers.dp(a, 12))
                val rowParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                rowParams.bottomMargin = UiHelpers.dp(a, 4)
                itemRow.layoutParams = rowParams

                val itemTv = TextView(a)
                itemTv.text = items[i]
                itemTv.textSize = 17f
                if (isSelected) {
                    itemTv.setTypeface(null, Typeface.BOLD)
                    itemTv.setTextColor(Color.parseColor(if (Theme.isDark(a)) "#D8D2FF" else "#5A4DB8"))
                } else {
                    itemTv.setTypeface(null, Typeface.NORMAL)
                    itemTv.setTextColor(Color.parseColor(Theme.text(a)))
                }
                val tvParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                itemTv.layoutParams = tvParams
                itemRow.addView(itemTv)

                if (isSelected) {
                    val check = TextView(a)
                    check.text = "✓"
                    check.textSize = 18f
                    check.setTypeface(null, Typeface.BOLD)
                    check.setTextColor(Color.parseColor(if (Theme.isDark(a)) "#D8D2FF" else "#5A4DB8"))
                    itemRow.addView(check)
                }

                itemRow.setOnClickListener {
                    onPick(items[i])
                    dialog.dismiss()
                    show(a)
                }
                box.addView(itemRow)
            }
        }
    }

    private fun showLanguagePicker(a: MainActivity) {
        val names = arrayOf("ქართული", "English", "Italiano", "Русский")
        val codes = arrayOf("ka", "en", "it", "ru")
        val currentCode = Lang.code(a)
        val currentIndex = codes.indexOf(currentCode).coerceAtLeast(0)
        showPicker(a, Lang.str(a, "pickLang"), names, names[currentIndex]) { picked ->
            val index = names.indexOf(picked)
            if (index >= 0) prefs(a).edit().putString("languageCode", codes[index]).apply()
        }
    }

    private fun askPin(a: MainActivity, title: String, isChange: Boolean) {
        val p = prefs(a)
        val hasPin = p.getString("pin", null) != null
        if (isChange && !hasPin) {
            Toast.makeText(a, Lang.str(a, "pinFirst"), Toast.LENGTH_SHORT).show()
            return
        }
        if (isChange) {
            UiDialog.show(a, title) { box2, dialog2 ->
                val etOld = EditText(a); etOld.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD; etOld.hint = Lang.str(a, "oldPin"); box2.addView(etOld)
                box2.addView(UiDialog.actionButton(a, Lang.str(a, "save")) {
                    if (!PinGate.checkHash(a, etOld.text.toString().trim())) { Toast.makeText(a, Lang.str(a, "wrongOldPin"), Toast.LENGTH_SHORT).show(); return@actionButton }
                    dialog2.dismiss(); askPin(a, title, false)
                })
            }
            return
        }
        UiDialog.show(a, title) { box, dialog ->
            val et = EditText(a)
            et.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            et.hint = Lang.str(a, "enterPin")
            et.setPadding(UiHelpers.dp(a, 4), UiHelpers.dp(a, 12), UiHelpers.dp(a, 4), UiHelpers.dp(a, 12))
            box.addView(et)
            box.addView(UiDialog.actionButton(a, Lang.str(a, "save")) {
                val pin = et.text.toString().trim()
                if (pin.length < 4) {
                    Toast.makeText(a, Lang.str(a, "pinShort"), Toast.LENGTH_SHORT).show()
                    return@actionButton
                }
                p.edit().putString("pin", PinGate.hash(pin)).apply()
                Toast.makeText(a, if (isChange) Lang.str(a, "pinChanged") else Lang.str(a, "pinSaved"), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                show(a)
            })
        }
    }

    fun show(a: MainActivity) {
        val outer = LinearLayout(a)
        outer.orientation = LinearLayout.VERTICAL
        outer.setBackgroundColor(Color.parseColor(Theme.bg(a)))

        val topBar = LinearLayout(a)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(UiHelpers.dp(a, 16), UiHelpers.dp(a, 16), UiHelpers.dp(a, 16), UiHelpers.dp(a, 16))
        val back = Icons.backButton(a) { a.showHomePage() }
        topBar.addView(back)
        val head = TextView(a)
        head.text = Lang.str(a, "settings")
        head.textSize = 24f
        head.setTypeface(null, Typeface.BOLD)
        head.setTextColor(Color.parseColor(Theme.title(a)))
        topBar.addView(head)
        outer.addView(topBar)

        val scroll = ScrollView(a)
        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )
        scroll.layoutParams = scrollParams
        val box = LinearLayout(a)
        box.orientation = LinearLayout.VERTICAL

        val card1 = card(a)
        card1.addView(sectionTitle(a, Lang.str(a, "general")))

        val dark = Theme.isDark(a)
        val darkRow = row(a, "🌙", Lang.str(a, "dark"), toggleView(a, dark) {
            Theme.setDark(a, !dark)
            show(a)
        }, null)
        card1.addView(darkRow)
        card1.addView(divider(a))

        val catRow = row(a, "📁", Lang.str(a, "categories"), chevronView(a)) { a.showCategories() }
        card1.addView(catRow)
        card1.addView(divider(a))

        val defCat = prefs(a).getString("defaultFolder", "პირადი") ?: "პირადი"
        val defRow = row(a, "📁", Lang.str(a, "defaultCat"), valueView(a, when (defCat) { "ყველა" -> Lang.str(a, "all")
            "პირადი" -> Lang.str(a, "personal")
            else -> defCat })) {
            val cats = a.cats.filter { it != "ყველა" }.toTypedArray()
            showPicker(a, Lang.str(a, "pickDefault"), cats, defCat) { picked ->
                prefs(a).edit().putString("defaultFolder", picked).apply()
            }
        }
        card1.addView(defRow)
        card1.addView(divider(a))

        val langCode = Lang.code(a)
        val langName = when (langCode) {
            "en" -> "English"
            "it" -> "Italiano"
            "ru" -> "Русский"
            else -> "ქართული"
        }
        val langRow = row(a, "🌐", Lang.str(a, "language"), valueView(a, langName)) { showLanguagePicker(a) }
        card1.addView(langRow)
        card1.addView(divider(a))

        val fontRow = row(a, "🔤", Lang.str(a, "fontSize"), chevronView(a)) { a.showFontSize() }
        card1.addView(fontRow)
        card1.addView(divider(a))

        val weekStart = prefs(a).getString("weekStart", "monday") ?: "monday"
        val weekName = if (weekStart == "sunday") Lang.str(a, "sunday") else Lang.str(a, "monday")
        val weekRow = row(a, "📅", Lang.str(a, "weekStart"), valueView(a, weekName)) {
            val items = arrayOf(Lang.str(a, "monday"), Lang.str(a, "sunday"))
            showPicker(a, Lang.str(a, "pickWeek"), items, weekName) { picked ->
                val code = if (picked == Lang.str(a, "sunday")) "sunday" else "monday"
                prefs(a).edit().putString("weekStart", code).apply()
            }
        }
        card1.addView(weekRow)
        card1.addView(divider(a))

        val notif = prefs(a).getBoolean("notif", true)
        val notifRow = row(a, "🔔", Lang.str(a, "notif"), toggleView(a, notif) {
            prefs(a).edit().putBoolean("notif", !notif).apply()
            show(a)
        }, null)
        card1.addView(notifRow)
        box.addView(card1)

        val card2 = card(a)
        card2.addView(sectionTitle(a, Lang.str(a, "security")))

        val hasPin = prefs(a).getString("pin", null) != null
        val pinRow = row(a, "🔒", Lang.str(a, "setPass"), toggleView(a, hasPin) {
            if (hasPin) {
                prefs(a).edit().remove("pin").apply()
                Toast.makeText(a, Lang.str(a, "pinOff"), Toast.LENGTH_SHORT).show()
                show(a)
            } else {
                askPin(a, Lang.str(a, "setPass"), false)
            }
        }, null)
        card2.addView(pinRow)
        card2.addView(divider(a))

        val changeRow = row(a, "🔄", Lang.str(a, "changePass"), chevronView(a)) {
            askPin(a, Lang.str(a, "changePass"), true)
        }
        card2.addView(changeRow)
        card2.addView(divider(a))

        val accRow = row(a, "👤", Lang.str(a, "account"), chevronView(a)) {
            showSimpleDialog(a, Lang.str(a, "account"), Lang.str(a, "accountMsg"))
        }
        card2.addView(accRow)
        box.addView(card2)

        val card3 = card(a)
        card3.addView(sectionTitle(a, Lang.str(a, "others")))

        val shareRow = row(a, "🔗", Lang.str(a, "share"), chevronView(a)) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, Lang.str(a, "shareText"))
            a.startActivity(Intent.createChooser(intent, Lang.str(a, "share")))
        }
        card3.addView(shareRow)
        card3.addView(divider(a))

        val privRow = row(a, "👁", Lang.str(a, "privacy"), chevronView(a)) {
            showSimpleDialog(a, Lang.str(a, "privacy"), Lang.str(a, "privacyMsg"))
        }
        card3.addView(privRow)
        box.addView(card3)

        scroll.addView(box)
        outer.addView(scroll)

        a.setContentView(outer)
        a.currentScreen = "settings"
    }
}