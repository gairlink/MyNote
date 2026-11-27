package ge.kakha.mynote

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

// ვიჯეტის კონფიგურაცია: ვიჯეტის დამატებისას — საიდანაც არ უნდა დაამატო
// (მენიუდან თუ მთავარი ეკრანიდან) — ჯერ ნოუტის არჩევა გამოდის.
// მენიუდან დამატებისას ეს ფანჯარა არ იშვება — იქ არჩევა უკვე მენიუში ხდება.
// უკან გასვლა = ვიჯეტი არ იდება (RESULT_CANCELED).
class WidgetConfigActivity : Activity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var picked = -1

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // ნაგულისხმევი შედეგი — უკან გასვლისას ვიჯეტი არ იდება
        val cancelIntent = Intent()
        cancelIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        setResult(RESULT_CANCELED, cancelIntent)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val outer = LinearLayout(this)
        outer.orientation = LinearLayout.VERTICAL
        outer.setBackgroundColor(Color.parseColor("#E6E0FF"))
        outer.setPadding(dp(16), dp(16), dp(16), dp(16))

        val title = TextView(this)
        title.text = Lang.strCtx(this, "widget")
        title.textSize = 20f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(Color.parseColor("#2B2447"))
        outer.addView(title)

        val hint = TextView(this)
        hint.text = Lang.strCtx(this, "widgetPickNotes")
        hint.textSize = 15f
        hint.setTextColor(Color.parseColor("#444444"))
        hint.setPadding(0, dp(8), 0, dp(12))
        outer.addView(hint)

        val notes = WidgetNotes.load(this)
        if (notes.isEmpty()) {
            val empty = TextView(this)
            empty.text = Lang.strCtx(this, "widgetEmpty")
            empty.textSize = 16f
            empty.setTextColor(Color.parseColor("#2B2447"))
            outer.addView(empty)
            setContentView(outer)
            return
        }

        val noteBox = LinearLayout(this)
        noteBox.orientation = LinearLayout.VERTICAL
        val scroll = ScrollView(this)
        scroll.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
        scroll.addView(noteBox)
        outer.addView(scroll)

        lateinit var installBtn: TextView
        fun refreshBtn() {
            installBtn.alpha = if (picked >= 0) 1f else 0.4f
        }

        fun rebuildRows() {
            noteBox.removeAllViews()
            for (n in notes) {
                val on = picked == n.id
                // ნოუტი ჩვეულებრივი ბარათით, როგორც HomePage-ში და მენიუში:
                // ფერმკრთალი ფონი + კატეგორიის ზოლი მარცხნივ, შიგნით მხოლოდ სათაური.
                val cat = WidgetNotes.catColor(this, n.folder)
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                val cardBg = GradientDrawable()
                cardBg.shape = GradientDrawable.RECTANGLE
                cardBg.cornerRadius = dp(22).toFloat()
                cardBg.setColor(WidgetNotes.paleColor(cat))
                if (on) cardBg.setStroke(dp(3), Color.parseColor("#7A6FF0"))
                row.background = cardBg
                val rp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(64)
                )
                rp.topMargin = dp(6)
                rp.bottomMargin = dp(6)
                row.layoutParams = rp
                val stripe = android.view.View(this)
                val r = dp(22).toFloat()
                val stripeBg = GradientDrawable()
                stripeBg.shape = GradientDrawable.RECTANGLE
                stripeBg.cornerRadii = floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r)
                stripeBg.setColor(cat)
                stripe.background = stripeBg
                stripe.layoutParams = LinearLayout.LayoutParams(
                    dp(14),
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                row.addView(stripe)

                val titleTv = TextView(this)
                val raw = if (n.title.isNotBlank()) n.title
                    else WidgetNotes.preview(this, n).lineSequence().firstOrNull() ?: ""
                titleTv.text = raw
                titleTv.textSize = 18f
                titleTv.setTypeface(null, Typeface.BOLD)
                titleTv.setTextColor(Color.parseColor("#2B2447"))
                titleTv.maxLines = 1
                titleTv.ellipsize = android.text.TextUtils.TruncateAt.END
                val tp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                tp.leftMargin = dp(16)
                tp.rightMargin = dp(12)
                titleTv.layoutParams = tp
                row.addView(titleTv)
                row.setOnClickListener {
                    picked = n.id
                    rebuildRows()
                    refreshBtn()
                }
                noteBox.addView(row)
            }
        }
        rebuildRows()

        installBtn = TextView(this)
        installBtn.text = Lang.strCtx(this, "widgetInstall")
        installBtn.textSize = 17f
        installBtn.setTypeface(null, Typeface.BOLD)
        installBtn.setTextColor(Color.WHITE)
        installBtn.gravity = Gravity.CENTER
        val btnBg = GradientDrawable()
        btnBg.shape = GradientDrawable.RECTANGLE
        btnBg.cornerRadius = dp(24).toFloat()
        btnBg.setColor(Color.parseColor("#7A6FF0"))
        installBtn.background = btnBg
        val bp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(52)
        )
        bp.topMargin = dp(12)
        installBtn.layoutParams = bp
        installBtn.setPadding(0, dp(12), 0, dp(12))
        installBtn.setOnClickListener {
            if (picked < 0) return@setOnClickListener
            // არჩევანი პირდაპირ ამ ვიჯეტს მიებმება — pending აღარ გვჭირდება
            getSharedPreferences("mynote", MODE_PRIVATE).edit()
                .putString("widgetNotes_" + widgetId, picked.toString()).apply()
            NoteSingleWidgetProvider.updateAll(this)
            NoteSingleWideWidgetProvider.updateAll(this)
            val okIntent = Intent()
            okIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(RESULT_OK, okIntent)
            finish()
        }
        outer.addView(installBtn)
        refreshBtn()

        setContentView(outer)
    }
}
