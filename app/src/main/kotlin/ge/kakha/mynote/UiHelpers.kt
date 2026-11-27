package ge.kakha.mynote

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

object UiHelpers {

    fun dp(a: MainActivity, v: Int): Int {
        return (v * a.resources.displayMetrics.density).toInt()
    }

    fun clean(s: String): String {
        return s.replace("|", " ").replace("~", " ").replace("\n", " ")
    }

    // 【entity-Samsung¦canonical_name=Samsung】-ის ქვედა სისტემური ღილაკების სიმაღლე — რომ კონტენტი მათ ქვეშ არ წავიდეს
    fun navBarHeight(a: MainActivity): Int {
        val id = a.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id > 0) a.resources.getDimensionPixelSize(id) else 0
    }

    // თხელი გამყოფი ხაზი
    fun lineView(a: MainActivity): View {
        val v = TextView(a)
        v.setBackgroundColor(Color.parseColor(Theme.line(a)))
        val p = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(a, 1)
        )
        v.layoutParams = p
        return v
    }

    // მომრგვალებული ფონი — ViewPage იყენებს: rounded(a.LAV, 24, a)
    fun rounded(color: String, radius: Int, a: MainActivity): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = dp(a, radius).toFloat()
        d.setColor(Color.parseColor(color))
        return d
    }

    fun titleBar(a: MainActivity, title: String, onBack: () -> Unit): LinearLayout {
        val bar = LinearLayout(a)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setBackgroundColor(Color.parseColor(Theme.bg(a)))
        bar.setPadding(dp(a, 16), dp(a, 16), dp(a, 16), dp(a, 16))
        val back = TextView(a)
        back.text = "←"
        back.textSize = 22f
        back.setTextColor(Color.parseColor(Theme.title(a)))
        back.setPadding(0, 0, dp(a, 16), 0)
        back.setOnClickListener { onBack() }
        bar.addView(back)
        val head = TextView(a)
        head.text = title
        head.textSize = 24f
        head.setTypeface(null, Typeface.BOLD)
        head.setTextColor(Color.parseColor(Theme.title(a)))
        bar.addView(head)
        return bar
    }

    fun dialogTitleBar(a: MainActivity, title: String, onClose: () -> Unit): LinearLayout {
        val bar = LinearLayout(a)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(a, 4), dp(a, 4), dp(a, 4), dp(a, 12))
        val head = TextView(a)
        head.text = title
        head.textSize = 20f
        head.setTypeface(null, Typeface.BOLD)
        head.setTextColor(Color.parseColor(Theme.title(a)))
        val headParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        head.layoutParams = headParams
        bar.addView(head)
        val close = TextView(a)
        close.text = "✕"
        close.textSize = 18f
        close.setTextColor(Color.parseColor(Theme.sub(a)))
        close.setPadding(dp(a, 12), dp(a, 4), dp(a, 4), dp(a, 4))
        close.setOnClickListener { onClose() }
        bar.addView(close)
        return bar
    }
}