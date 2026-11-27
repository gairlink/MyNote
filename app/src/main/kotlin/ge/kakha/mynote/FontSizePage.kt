package ge.kakha.mynote

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

object FontSizePage {

    // 4 ზომა — ესაა ძირითადი (ტექსტის) ზომა
    // სათაურები ამიდან ითვლება პროპორციულად
    private val SIZES = intArrayOf(14, 16, 18, 20)
    // NAMES აღარ არის ჩაწერილი — იკითხება Lang-იდან

    fun show(a: MainActivity) {
        val outer = LinearLayout(a)
        outer.orientation = LinearLayout.VERTICAL
        outer.setBackgroundColor(Color.parseColor(a.LAV_BG))
        outer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        // ზედა ზოლი — უკან + სათაური
        val topBar = LinearLayout(a)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(UiHelpers.dp(a, 12), UiHelpers.dp(a, 14), UiHelpers.dp(a, 12), UiHelpers.dp(a, 14))

        val btnBack = Icons.backButton(a) { a.showSettings() }
        topBar.addView(btnBack)

        val topTitle = TextView(a)
        topTitle.text = Lang.str(a, "fontTitle")
        topTitle.textSize = 19f
        topTitle.setTypeface(null, Typeface.BOLD)
        topTitle.setTextColor(Color.parseColor("#3D3566"))
        topBar.addView(topTitle)
        outer.addView(topBar)

        // თეთრი ფურცელი — წინასწარი ნახვა
        val scroll = ScrollView(a)
        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
        scroll.layoutParams = scrollParams

        val paper = LinearLayout(a)
        paper.orientation = LinearLayout.VERTICAL
        paper.setBackgroundColor(Color.WHITE)
        paper.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 20), UiHelpers.dp(a, 20), UiHelpers.dp(a, 20))

        val previewH1 = TextView(a)
        previewH1.text = Lang.str(a, "previewTitle1")
        previewH1.setTypeface(null, Typeface.BOLD)
        previewH1.setTextColor(Color.parseColor("#2B2447"))
        paper.addView(previewH1)

        val space1 = TextView(a)
        space1.text = ""
        space1.textSize = 8f
        paper.addView(space1)

        val previewH2 = TextView(a)
        previewH2.text = Lang.str(a, "previewTitle2")
        previewH2.setTypeface(null, Typeface.BOLD)
        previewH2.setTextColor(Color.parseColor("#2B2447"))
        paper.addView(previewH2)

        val space2 = TextView(a)
        space2.text = ""
        space2.textSize = 8f
        paper.addView(space2)

        val previewBody = TextView(a)
        previewBody.text = Lang.str(a, "previewBody")
        previewBody.setTextColor(Color.parseColor("#444444"))
        paper.addView(previewBody)

        scroll.addView(paper)
        outer.addView(scroll)

        // არჩეული ზომის სახელი — სლაიდის ზემოთ
        val lblName = TextView(a)
        lblName.gravity = Gravity.CENTER
        lblName.textSize = 15f
        lblName.setTypeface(null, Typeface.BOLD)
        lblName.setTextColor(Color.parseColor("#3D3566"))
        lblName.setPadding(0, UiHelpers.dp(a, 12), 0, UiHelpers.dp(a, 4))
        outer.addView(lblName)

        // ქვედა ზოლი — სლაიდი, როგორც სურათზე
        val bottomBar = LinearLayout(a)
        bottomBar.orientation = LinearLayout.HORIZONTAL
        bottomBar.gravity = Gravity.CENTER_VERTICAL
        bottomBar.setBackgroundColor(Color.parseColor(a.LAV_LIGHT))
        bottomBar.setPadding(UiHelpers.dp(a, 20), UiHelpers.dp(a, 16), UiHelpers.dp(a, 20), UiHelpers.dp(a, 20))

        val smallA = TextView(a)
        smallA.text = "A"
        smallA.textSize = 16f
        smallA.setTypeface(null, Typeface.BOLD)
        smallA.setTextColor(Color.parseColor("#3D3566"))
        bottomBar.addView(smallA)

        val seek = SeekBar(a)
        val seekParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        seekParams.leftMargin = UiHelpers.dp(a, 12)
        seekParams.rightMargin = UiHelpers.dp(a, 12)
        seek.layoutParams = seekParams
        seek.max = 3
        bottomBar.addView(seek)

        val bigA = TextView(a)
        bigA.text = "A"
        bigA.textSize = 28f
        bigA.setTypeface(null, Typeface.BOLD)
        bigA.setTextColor(Color.parseColor("#3D3566"))
        bottomBar.addView(bigA)

        outer.addView(bottomBar)

        // ამჟამინდელი ზომა -> სლაიდის პოზიცია
        var startIndex = 1
        for (i in SIZES.indices) {
            if (SIZES[i] == a.viewFontSize) {
                startIndex = i
            }
        }

        fun applySize(index: Int) {
            val base = SIZES[index].toFloat()
            // პროპორციული ზრდა ძირითადი ზომიდან:
            // ტექსტი = ძირითადი
            // სათაური 2 = ძირითადი x 1.3
            // სათაური 1 = ძირითადი x 1.6
            previewBody.textSize = base
            previewH2.textSize = base * 1.3f
            previewH1.textSize = base * 1.6f
            lblName.text = Lang.str(a, arrayOf("fontSmall","fontMedium","fontLarge","fontXLarge")[index])
            a.viewFontSize = SIZES[index]
            a.saveFontSize()
        }

        seek.progress = startIndex
        applySize(startIndex)

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                applySize(progress)
            }

            override fun onStartTrackingTouch(s: SeekBar?) {
            }

            override fun onStopTrackingTouch(s: SeekBar?) {
            }
        })

        a.setContentView(outer)
        a.currentScreen = "fontsize"
    }
}