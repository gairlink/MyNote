package ge.kakha.mynote

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

object CreatePicker {

    private fun gradientBtn(a: MainActivity): GradientDrawable {
        val d = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#7A6FF0"), Color.parseColor("#B9AFFF")))
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = UiHelpers.dp(a, 16).toFloat()
        return d
    }

    fun show(a: MainActivity) {
        UiDialog.show(a, Lang.str(a, "whatType")) { box, dialog ->
            val row = LinearLayout(a)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER
            row.setPadding(0, UiHelpers.dp(a, 8), 0, 0)

            val textBtn = TextView(a)
            textBtn.text = Lang.str(a, "textNote")
            textBtn.textSize = 16f
            textBtn.setTypeface(null, Typeface.BOLD)
            textBtn.gravity = Gravity.CENTER
            textBtn.setTextColor(Color.WHITE)
            textBtn.background = gradientBtn(a)
            textBtn.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 14), UiHelpers.dp(a, 20), UiHelpers.dp(a, 14))
            val textParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textParams.rightMargin = UiHelpers.dp(a, 8)
            textBtn.layoutParams = textParams
            textBtn.setOnClickListener {
                dialog.dismiss()
                a.showEditor(null, "text")
            }
            row.addView(textBtn)

            val listBtn = TextView(a)
            listBtn.text = Lang.str(a, "checkNote")
            listBtn.textSize = 16f
            listBtn.setTypeface(null, Typeface.BOLD)
            listBtn.gravity = Gravity.CENTER
            listBtn.setTextColor(Color.WHITE)
            listBtn.background = gradientBtn(a)
            listBtn.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 14), UiHelpers.dp(a, 20), UiHelpers.dp(a, 14))
            val listParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            listParams.leftMargin = UiHelpers.dp(a, 8)
            listBtn.layoutParams = listParams
            listBtn.setOnClickListener {
                dialog.dismiss()
                a.showEditor(null, "checklist")
            }
            row.addView(listBtn)

            box.addView(row)
        }
    }
}