package ge.kakha.mynote

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView

object UiDialog {

    // ფუნქციური კნოფკა — გრადიენტი, შემცირებული სიგანე, ცენტრში
    fun actionButton(a: MainActivity, text: String, onClick: () -> Unit): LinearLayout {
        val wrap = LinearLayout(a)
        wrap.orientation = LinearLayout.HORIZONTAL
        wrap.gravity = Gravity.CENTER
        wrap.setPadding(0, 0, 0, UiHelpers.dp(a, 10))
        val btn = TextView(a)
        btn.text = text
        btn.textSize = 16f
        btn.setTypeface(null, Typeface.BOLD)
        btn.gravity = Gravity.CENTER
        btn.setTextColor(Color.WHITE)
        btn.setShadowLayer(4f, 0f, 2f, Color.parseColor("#40000000"))
        val d = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.parseColor("#B9AFFF"),
                Color.parseColor("#8B7FE8"),
                Color.parseColor("#3D3566")
            )
        )
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = UiHelpers.dp(a, 16).toFloat()
        btn.background = d
        btn.elevation = UiHelpers.dp(a, 4).toFloat()
        btn.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 12), UiHelpers.dp(a, 20), UiHelpers.dp(a, 12))
        val p = LinearLayout.LayoutParams(
            (a.resources.displayMetrics.widthPixels * 0.55).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        p.topMargin = UiHelpers.dp(a, 12)
        p.bottomMargin = UiHelpers.dp(a, 5)
        btn.layoutParams = p
        btn.setOnClickListener { onClick() }
        wrap.addView(btn)
        return wrap
    }

    fun show(a: MainActivity, title: String, build: (LinearLayout, Dialog) -> Unit) {
        val dialog = Dialog(a)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        val isDark = Theme.isDark(a)
        val cardColor = if (isDark) "#23232F" else "#FFFFFF"
        val cardEdge = if (isDark) "#3D3566" else "#EDE9FF"

        val outer = LinearLayout(a)
        outer.orientation = LinearLayout.VERTICAL
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.RECTANGLE
        bg.cornerRadius = UiHelpers.dp(a, 24).toFloat()
        bg.setColor(Color.parseColor(cardColor))
        bg.setStroke(UiHelpers.dp(a, 1), Color.parseColor(cardEdge))
        outer.background = bg
        outer.elevation = UiHelpers.dp(a, 16).toFloat()

        val header = LinearLayout(a)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        val headerBg = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.parseColor("#B9AFFF"),
                Color.parseColor("#8B7FE8"),
                Color.parseColor("#3D3566")
            )
        )
        headerBg.shape = GradientDrawable.RECTANGLE
        val r = UiHelpers.dp(a, 24).toFloat()
        headerBg.cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        header.background = headerBg
        header.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 18), UiHelpers.dp(a, 16), UiHelpers.dp(a, 18))
        header.elevation = UiHelpers.dp(a, 4).toFloat()

        val titleTv = TextView(a)
        titleTv.text = title
        titleTv.textSize = 18f
        titleTv.setTypeface(null, Typeface.BOLD)
        titleTv.setTextColor(Color.WHITE)
        titleTv.setShadowLayer(4f, 0f, 2f, Color.parseColor("#40000000"))
        val titleParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        titleTv.layoutParams = titleParams
        header.addView(titleTv)

        val closeWrap = LinearLayout(a)
        closeWrap.gravity = Gravity.CENTER
        val closeBg = GradientDrawable()
        closeBg.shape = GradientDrawable.OVAL
        closeBg.setColor(Color.parseColor("#80FFFFFF"))
        closeBg.setStroke(UiHelpers.dp(a, 1), Color.parseColor("#FFFFFF"))
        closeWrap.background = closeBg
        closeWrap.elevation = UiHelpers.dp(a, 4).toFloat()
        val closeParams = LinearLayout.LayoutParams(UiHelpers.dp(a, 30), UiHelpers.dp(a, 30))
        closeWrap.layoutParams = closeParams
        val closeBtn = TextView(a)
        closeBtn.text = "✕"
        closeBtn.textSize = 14f
        closeBtn.setTypeface(null, Typeface.BOLD)
        closeBtn.setTextColor(Color.WHITE)
        closeBtn.gravity = Gravity.CENTER
        closeWrap.addView(closeBtn)
        closeWrap.setOnClickListener { dialog.dismiss() }
        header.addView(closeWrap)

        outer.addView(header)

        val line = TextView(a)
        line.setBackgroundColor(Color.parseColor(Theme.line(a)))
        val lineParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, UiHelpers.dp(a, 1))
        line.layoutParams = lineParams
        outer.addView(line)

        val content = LinearLayout(a)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 16), UiHelpers.dp(a, 20), UiHelpers.dp(a, 30))
        outer.addView(content)

        build(content, dialog)

        dialog.setContentView(outer)
        val w = dialog.window
        if (w != null) {
            w.setLayout(
                (a.resources.displayMetrics.widthPixels * 0.90).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            w.setGravity(Gravity.CENTER)
            w.setDimAmount(0.55f)
            val wBg = GradientDrawable()
            wBg.shape = GradientDrawable.RECTANGLE
            wBg.cornerRadius = UiHelpers.dp(a, 24).toFloat()
            wBg.setColor(Color.TRANSPARENT)
            w.setBackgroundDrawable(wBg)
        }
        dialog.show()
    }
}