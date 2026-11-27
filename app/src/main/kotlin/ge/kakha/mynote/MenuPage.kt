package ge.kakha.mynote

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

object MenuPage {

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
        title.text = Lang.str(a, "menu")
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

        val head = LinearLayout(a)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.setPadding(UiHelpers.dp(a, 16), UiHelpers.dp(a, 16), UiHelpers.dp(a, 16), UiHelpers.dp(a, 16))
        val headBg = GradientDrawable()
        headBg.shape = GradientDrawable.RECTANGLE
        headBg.cornerRadius = UiHelpers.dp(a, 22).toFloat()
        headBg.setColor(Color.parseColor(Theme.pill(a)))
        head.background = headBg
        val leaf = TextView(a)
        leaf.text = Pictograms.leaf
        leaf.textSize = 28f
        leaf.setPadding(0, 0, UiHelpers.dp(a, 12), 0)
        head.addView(leaf)
        val headTexts = LinearLayout(a)
        headTexts.orientation = LinearLayout.VERTICAL
        val appName = TextView(a)
        appName.text = Lang.str(a, "app")
        appName.textSize = 20f
        appName.setTypeface(null, Typeface.BOLD)
        appName.setTextColor(Color.parseColor(Theme.title(a)))
        headTexts.addView(appName)
        val count = TextView(a)
        val activeCount = a.notes.count { !it.isArchived && !it.isDeleted }
        count.text = activeCount.toString() + " " + Lang.str(a, "notesCount")
        count.textSize = 13f
        count.setTextColor(Color.parseColor(Theme.sub(a)))
        headTexts.addView(count)
        head.addView(headTexts)
        // გამყოფი ხაზი ოდნავ მეტი დაშორებით — ანგარიშის ბარათის გარშემო ხაზებს
        fun spacedLine(): android.view.View {
            val v = UiHelpers.lineView(a)
            val lp = v.layoutParams as? LinearLayout.LayoutParams
            if (lp != null) {
                lp.topMargin = UiHelpers.dp(a, 10)
                lp.bottomMargin = UiHelpers.dp(a, 10)
                v.layoutParams = lp
            }
            return v
        }
        content.addView(head)
        content.addView(spacedLine())

        // Google ანგარიში — ავატარი + მეილი + ბოლო სინქრონიზაცია + ↻, სქრინშოთის მსგავსად
        val accCard = LinearLayout(a)
        accCard.orientation = LinearLayout.VERTICAL
        accCard.setPadding(UiHelpers.dp(a, 16), UiHelpers.dp(a, 14), UiHelpers.dp(a, 16), UiHelpers.dp(a, 12))
        val accBg = GradientDrawable()
        accBg.shape = GradientDrawable.RECTANGLE
        accBg.cornerRadius = UiHelpers.dp(a, 22).toFloat()
        accBg.setColor(Color.parseColor(Theme.pill(a)))
        accCard.background = accBg

        val accRow = LinearLayout(a)
        accRow.orientation = LinearLayout.HORIZONTAL
        accRow.gravity = Gravity.CENTER_VERTICAL

        val avatar = TextView(a)
        val avatarBg = GradientDrawable()
        avatarBg.shape = GradientDrawable.OVAL
        avatarBg.setColor(Color.parseColor("#7A6FF0"))
        avatar.background = avatarBg
        avatar.layoutParams = LinearLayout.LayoutParams(UiHelpers.dp(a, 48), UiHelpers.dp(a, 48))
        avatar.gravity = Gravity.CENTER
        avatar.textSize = 22f
        avatar.setTypeface(null, Typeface.BOLD)
        avatar.setTextColor(Color.WHITE)

        val accTexts = LinearLayout(a)
        accTexts.orientation = LinearLayout.VERTICAL
        accTexts.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        accTexts.setPadding(UiHelpers.dp(a, 12), 0, UiHelpers.dp(a, 8), 0)

        if (DriveSync.isSignedIn(a)) {
            val email = DriveSync.accountEmail(a) ?: ""
            avatar.text = email.firstOrNull()?.uppercaseChar()?.toString() ?: "G"
            val emailTv = TextView(a)
            emailTv.text = email
            emailTv.textSize = 16f
            emailTv.setTypeface(null, Typeface.BOLD)
            emailTv.setTextColor(Color.parseColor(Theme.title(a)))
            accTexts.addView(emailTv)
            val syncTv = TextView(a)
            val lastSync = DriveSync.lastSyncDateText(a)
            syncTv.text = Lang.str(a, "lastSync") + ": " + if (lastSync.isEmpty()) Lang.str(a, "neverSynced") else lastSync
            syncTv.textSize = 13f
            syncTv.setTextColor(Color.parseColor(Theme.sub(a)))
            accTexts.addView(syncTv)
            accRow.addView(avatar)
            accRow.addView(accTexts)
            val syncBtn = TextView(a)
            syncBtn.text = Pictograms.refresh
            syncBtn.textSize = 26f
            syncBtn.setPadding(UiHelpers.dp(a, 8), UiHelpers.dp(a, 8), UiHelpers.dp(a, 8), UiHelpers.dp(a, 8))
            syncBtn.setOnClickListener {
                if (DriveSync.syncing) {
                    Toast.makeText(a, Lang.str(a, "syncing"), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                Toast.makeText(a, Lang.str(a, "syncing"), Toast.LENGTH_SHORT).show()
                DriveSync.sync(a) { ok ->
                    Toast.makeText(a, Lang.str(a, if (ok) "syncDone" else "syncFailed"), Toast.LENGTH_SHORT).show()
                    show(a)
                }
            }
            accRow.addView(syncBtn)
            accCard.addView(accRow)
        } else {
            avatar.text = "G"
            val signTv = TextView(a)
            signTv.text = Lang.str(a, "signInGoogle")
            signTv.textSize = 16f
            signTv.setTypeface(null, Typeface.BOLD)
            signTv.setTextColor(Color.parseColor(Theme.title(a)))
            accTexts.addView(signTv)
            accRow.addView(avatar)
            accRow.addView(accTexts)
            accRow.setOnClickListener { DriveSync.signIn(a) }
            accCard.addView(accRow)
        }
        content.addView(accCard)
        content.addView(spacedLine())

        fun menuRow(icon: String, titleText: String, sub: String? = null, onClick: () -> Unit) {
            val row = LinearLayout(a)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(UiHelpers.dp(a, 8), UiHelpers.dp(a, 14), UiHelpers.dp(a, 8), UiHelpers.dp(a, 14))
            val iconTv = TextView(a)
            iconTv.text = icon
            iconTv.textSize = 22f
            iconTv.setPadding(0, 0, UiHelpers.dp(a, 14), 0)
            row.addView(iconTv)
            val texts = LinearLayout(a)
            texts.orientation = LinearLayout.VERTICAL
            texts.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val t = TextView(a)
            t.text = titleText
            t.textSize = 17f
            t.setTypeface(null, Typeface.BOLD)
            t.setTextColor(Color.parseColor(Theme.title(a)))
            texts.addView(t)
            if (sub != null) {
                val s = TextView(a)
                s.text = sub
                s.textSize = 13f
                s.setTextColor(Color.parseColor(Theme.sub(a)))
                texts.addView(s)
            }
            row.addView(texts)
            row.setOnClickListener { onClick() }
            content.addView(row)
            content.addView(UiHelpers.lineView(a))
        }

        // სანაგვე და არქივი — გვერდიგვერდ, ორი ბარათი, სქრინზე როგორცაა
        val trashCount = a.notes.count { it.isDeleted }
        val archCount = a.notes.count { it.isArchived && !it.isDeleted }
        val twinRow = LinearLayout(a)
        twinRow.orientation = LinearLayout.HORIZONTAL
        twinRow.gravity = Gravity.CENTER_VERTICAL
        twinRow.setPadding(0, UiHelpers.dp(a, 4), 0, UiHelpers.dp(a, 4))

        fun twinCard(icon: String, titleText: String, countText: String, onClick: () -> Unit): LinearLayout {
            val card = LinearLayout(a)
            card.orientation = LinearLayout.VERTICAL
            card.gravity = Gravity.CENTER_HORIZONTAL
            card.setPadding(UiHelpers.dp(a, 12), UiHelpers.dp(a, 18), UiHelpers.dp(a, 12), UiHelpers.dp(a, 18))
            card.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val cardBg = GradientDrawable()
            cardBg.shape = GradientDrawable.RECTANGLE
            cardBg.cornerRadius = UiHelpers.dp(a, 18).toFloat()
            cardBg.setColor(Color.parseColor(Theme.pill(a)))
            card.background = cardBg
            val iconTv = TextView(a)
            iconTv.text = icon
            iconTv.textSize = 26f
            iconTv.gravity = Gravity.CENTER
            card.addView(iconTv)
            val t = TextView(a)
            t.text = titleText
            t.textSize = 15f
            t.setTypeface(null, Typeface.BOLD)
            t.gravity = Gravity.CENTER
            t.setTextColor(Color.parseColor(Theme.title(a)))
            t.setPadding(0, UiHelpers.dp(a, 8), 0, 0)
            card.addView(t)
            val c = TextView(a)
            c.text = countText
            c.textSize = 13f
            c.gravity = Gravity.CENTER
            c.setTextColor(Color.parseColor(Theme.sub(a)))
            c.setPadding(0, UiHelpers.dp(a, 2), 0, 0)
            card.addView(c)
            card.setOnClickListener { onClick() }
            return card
        }

        twinRow.addView(twinCard(Pictograms.trash, Lang.str(a, "trash"), trashCount.toString()) { a.showTrash() })
        val spacer = LinearLayout(a)
        spacer.layoutParams = LinearLayout.LayoutParams(UiHelpers.dp(a, 12), LinearLayout.LayoutParams.WRAP_CONTENT)
        twinRow.addView(spacer)
        twinRow.addView(twinCard(Pictograms.archive, Lang.str(a, "archive"), archCount.toString()) { a.showArchive() })
        content.addView(twinRow)
        content.addView(UiHelpers.lineView(a))
        menuRow(Pictograms.feedback, Lang.str(a, "feedback")) {
            UiDialog.show(a, Lang.str(a, "feedback")) { box, dialog ->
                val et = EditText(a)
                et.hint = Lang.str(a, "feedbackHint")
                et.setPadding(UiHelpers.dp(a, 4), UiHelpers.dp(a, 12), UiHelpers.dp(a, 4), UiHelpers.dp(a, 12))
                box.addView(et)
                box.addView(UiDialog.actionButton(a, Lang.str(a, "send")) {
                    if (et.text.toString().trim().isEmpty()) {
                        Toast.makeText(a, Lang.str(a, "feedbackHint"), Toast.LENGTH_SHORT).show()
                        return@actionButton
                    }
                    dialog.dismiss()
                    Toast.makeText(a, Lang.str(a, "feedbackSent"), Toast.LENGTH_SHORT).show()
                })
            }
        }
        menuRow(Pictograms.settings, Lang.str(a, "settings")) { a.showSettings() }
        menuRow(Pictograms.widget, Lang.str(a, "widget"), Lang.str(a, "widgetInfo")) {
            UiDialog.show(a, Lang.str(a, "widget")) { box, _ ->
                val msg = TextView(a)
                msg.text = Lang.str(a, "widgetInfo")
                msg.textSize = 16f
                msg.setTextColor(Color.BLACK)
                box.addView(msg)
            }
        }
        // განახლების შემოწმება (ხელით)
        menuRow(Pictograms.refresh, Lang.str(a, "checkUpdate")) {
            AppUpdate.check(a, silent = false)
        }
        // გასვლა = Google-დან გამოსვლა, აპი არ იხურება.
        // Google-დან გამოსულზე ღილაკი საერთოდ არ ჩანს.
        if (DriveSync.isSignedIn(a)) {
            menuRow(Pictograms.exit, Lang.str(a, "exitApp")) {
                DriveSync.signOut(a) { show(a) }
            }
        }

        scroll.addView(content)
        outer.addView(scroll)
        outer.addView(HomePage.bottomNav(a, "menu"))
        // + ღილაკი მენიუში არ ჩანს
        frame.addView(outer)
        a.setContentView(frame)
        a.currentScreen = "menu"
    }
}