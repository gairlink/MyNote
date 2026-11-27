package ge.kakha.mynote


import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.widget.ImageView
import android.graphics.Typeface
import android.view.Gravity
import android.widget.TextView
import android.widget.LinearLayout
import kotlin.math.cos
import kotlin.math.sin

/**
 * ყველა იკონი ერთ ფაილში — ნახატი, ერთი სტილით:
 * ლავანდისფერი ხაზი (#7A6FF0), თეთრი ფონი.
 * ემოჯების (🌿 ⭐ 🔒 🗑️ ...) მაგივრად — ისინი ტექსტია და ყველგან სხვადასხვანაირად ჩანს.
 * უკვე არსებული ნახატი პიქტოგრამებიც აქაა: back (უკან ისარი) და restore (დაბრუნება).
 *
 * გამოყენება: Icons.back(a) { a.handleBack() }  ან  Icons.star(a)  (დაჭერის გარეშე)
 */
object Icons {

    private const val LAV = "#7A6FF0"
    private const val BLUE = "#2196F3"
    private const val GOLD = "#FFC107"

    private fun paint(stroke: Float, color: String = LAV): Paint {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.strokeWidth = stroke
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.color = Color.parseColor(color)
        return p
    }

    private fun fillPaint(color: String): Paint {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.color = Color.parseColor(color)
        return p
    }

    private fun dotPaint(color: String = LAV): Paint {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.color = Color.parseColor(color)
        return p
    }

    private fun canvas(): Pair<Bitmap, Canvas> {
        val bmp = Bitmap.createBitmap(168, 168, Bitmap.Config.ARGB_8888)
        return bmp to Canvas(bmp)
    }

    private fun finish(a: MainActivity, bmp: Bitmap, sizeDp: Int = 42, onClick: (() -> Unit)?): ImageView {
        val iv = ImageView(a)
        iv.setImageBitmap(bmp)
        iv.layoutParams = LinearLayout.LayoutParams(UiHelpers.dp(a, sizeDp), UiHelpers.dp(a, sizeDp))
        if (onClick != null) iv.setOnClickListener { onClick() }
        return iv
    }

    private fun circleBg(c: Canvas, border: Boolean = true) {
        c.drawCircle(84f, 84f, 78f, fillPaint("#FFFFFF"))
        if (border) c.drawCircle(84f, 84f, 78f, paint(7f))
    }

    // ---------- უკვე არსებული ნახატი პიქტოგრამები ----------

    /** უკან ისარი — ვარიანტი 3: თეთრი წრე, ლავანდისფერი ჩარჩო, ლავანდისფერი ისარი */
    fun back(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        circleBg(c)
        val p = paint(13f)
        c.drawLine(108f, 52f, 66f, 84f, p)
        c.drawLine(66f, 84f, 108f, 116f, p)
        return finish(a, bmp, 42, onClick)
    }

    /** დაბრუნება / აღდგენა ↩ — მრუდე ისარი უკან */
    fun restore(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        circleBg(c)
        val p = paint(12f)
        c.drawArc(RectF(46f, 46f, 122f, 122f), 300f, 240f, false, p)
        c.drawLine(46f, 84f, 63f, 69f, p)
        c.drawLine(46f, 84f, 63f, 99f, p)
        return finish(a, bmp, 38, onClick)
    }

    // ---------- ნავიგაცია ----------

    fun home(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        c.drawLine(42f, 84f, 84f, 42f, p)
        c.drawLine(84f, 42f, 126f, 84f, p)
        c.drawLine(54f, 80f, 54f, 126f, p)
        c.drawLine(54f, 126f, 114f, 126f, p)
        c.drawLine(114f, 126f, 114f, 80f, p)
        return finish(a, bmp, 42, onClick)
    }

    fun calendar(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        c.drawRoundRect(RectF(38f, 46f, 130f, 130f), 12f, 12f, p)
        c.drawLine(38f, 74f, 130f, 74f, p)
        c.drawLine(60f, 32f, 60f, 54f, p)
        c.drawLine(108f, 32f, 108f, 54f, p)
        val d = dotPaint()
        c.drawCircle(68f, 98f, 7f, d)
        c.drawCircle(100f, 98f, 7f, d)
        return finish(a, bmp, 42, onClick)
    }

    fun menu(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(11f)
        c.drawLine(44f, 58f, 124f, 58f, p)
        c.drawLine(44f, 84f, 124f, 84f, p)
        c.drawLine(44f, 110f, 124f, 110f, p)
        return finish(a, bmp, 42, onClick)
    }

    /** მცურავი + ღილაკი — ლურჯი წრე, თეთრი პლუსი */
    fun plus(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        c.drawCircle(84f, 84f, 78f, fillPaint(BLUE))
        val p = paint(13f, "#FFFFFF")
        c.drawLine(84f, 52f, 84f, 116f, p)
        c.drawLine(52f, 84f, 116f, 84f, p)
        return finish(a, bmp, 64, onClick)
    }

    // ---------- მენიუს პუნქტები ----------

    fun trash(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        c.drawLine(52f, 46f, 116f, 46f, p)
        c.drawLine(66f, 46f, 66f, 34f, p)
        c.drawLine(102f, 46f, 102f, 34f, p)
        c.drawLine(66f, 34f, 102f, 34f, p)
        c.drawLine(60f, 46f, 66f, 128f, p)
        c.drawLine(108f, 46f, 102f, 128f, p)
        c.drawLine(66f, 128f, 102f, 128f, p)
        c.drawLine(76f, 68f, 76f, 108f, p)
        c.drawLine(92f, 68f, 92f, 108f, p)
        return finish(a, bmp, 42, onClick)
    }

    fun archive(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        c.drawRoundRect(RectF(42f, 34f, 126f, 60f), 6f, 6f, p)
        c.drawRoundRect(RectF(36f, 60f, 132f, 134f), 8f, 8f, p)
        c.drawLine(66f, 84f, 84f, 102f, p)
        c.drawLine(84f, 102f, 102f, 84f, p)
        return finish(a, bmp, 42, onClick)
    }

    fun settings(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        c.drawCircle(84f, 84f, 24f, p)
        val cx = 84f
        val cy = 84f
        for (i in 0 until 8) {
            val ang = i * Math.PI / 4
            val x1 = (cx + 38 * cos(ang)).toFloat()
            val y1 = (cy + 38 * sin(ang)).toFloat()
            val x2 = (cx + 56 * cos(ang)).toFloat()
            val y2 = (cy + 56 * sin(ang)).toFloat()
            c.drawLine(x1, y1, x2, y2, p)
        }
        return finish(a, bmp, 42, onClick)
    }

    fun feedback(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        c.drawRoundRect(RectF(36f, 36f, 132f, 106f), 14f, 14f, p)
        val tail = Path()
        tail.moveTo(58f, 106f)
        tail.lineTo(58f, 130f)
        tail.lineTo(82f, 106f)
        tail.close()
        c.drawPath(tail, p)
        c.drawLine(58f, 62f, 110f, 62f, p)
        c.drawLine(58f, 84f, 92f, 84f, p)
        return finish(a, bmp, 42, onClick)
    }

    fun widget(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        c.drawRoundRect(RectF(36f, 36f, 90f, 90f), 10f, 10f, p)
        c.drawRoundRect(RectF(102f, 36f, 132f, 66f), 8f, 8f, fillPaint(LAV))
        c.drawRoundRect(RectF(36f, 102f, 66f, 132f), 8f, 8f, fillPaint(LAV))
        c.drawRoundRect(RectF(78f, 102f, 132f, 132f), 8f, 8f, p)
        return finish(a, bmp, 42, onClick)
    }

    fun exit(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        c.drawLine(54f, 42f, 54f, 126f, p)
        c.drawLine(54f, 84f, 110f, 84f, p)
        c.drawLine(92f, 62f, 112f, 84f, p)
        c.drawLine(112f, 84f, 92f, 106f, p)
        return finish(a, bmp, 42, onClick)
    }

    // ---------- ნოუტის ბარათი და მენიუ ----------

    fun search(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        c.drawCircle(74f, 74f, 36f, p)
        c.drawLine(100f, 100f, 128f, 128f, p)
        return finish(a, bmp, 42, onClick)
    }

    fun star(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        c.drawPath(starPath(84f, 84f, 62f, 26f), paint(9f))
        return finish(a, bmp, 42, onClick)
    }

    fun starFilled(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        c.drawPath(starPath(84f, 84f, 62f, 26f), fillPaint(GOLD))
        return finish(a, bmp, 42, onClick)
    }

    private fun starPath(cx: Float, cy: Float, outer: Float, inner: Float): Path {
        val path = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val ang = -Math.PI / 2 + i * Math.PI / 5
            val x = (cx + r * cos(ang)).toFloat()
            val y = (cy + r * sin(ang)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    fun lock(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        c.drawRoundRect(RectF(46f, 74f, 122f, 134f), 12f, 12f, p)
        c.drawArc(RectF(58f, 36f, 110f, 88f), 180f, 180f, false, p)
        c.drawCircle(84f, 102f, 7f, dotPaint())
        return finish(a, bmp, 42, onClick)
    }

    fun reminder(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        c.drawCircle(84f, 90f, 44f, p)
        c.drawLine(84f, 68f, 84f, 92f, p)
        c.drawLine(84f, 92f, 102f, 104f, p)
        c.drawLine(52f, 42f, 40f, 30f, p)
        c.drawLine(116f, 42f, 128f, 30f, p)
        return finish(a, bmp, 42, onClick)
    }

    fun exportPdf(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        val doc = Path()
        doc.moveTo(46f, 34f)
        doc.lineTo(96f, 34f)
        doc.lineTo(122f, 60f)
        doc.lineTo(122f, 134f)
        doc.lineTo(46f, 134f)
        doc.close()
        c.drawPath(doc, p)
        c.drawLine(96f, 34f, 96f, 60f, p)
        c.drawLine(96f, 60f, 122f, 60f, p)
        c.drawLine(62f, 88f, 106f, 88f, p)
        c.drawLine(62f, 108f, 106f, 108f, p)
        return finish(a, bmp, 42, onClick)
    }

    fun info(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        c.drawCircle(84f, 84f, 58f, p)
        c.drawLine(84f, 62f, 84f, 72f, p)
        c.drawLine(84f, 88f, 84f, 112f, p)
        return finish(a, bmp, 42, onClick)
    }

    fun edit(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        c.drawLine(42f, 126f, 50f, 92f, p)
        c.drawLine(50f, 92f, 104f, 38f, p)
        c.drawLine(104f, 38f, 126f, 60f, p)
        c.drawLine(126f, 60f, 72f, 114f, p)
        c.drawLine(72f, 114f, 42f, 126f, p)
        c.drawLine(96f, 46f, 118f, 68f, p)
        return finish(a, bmp, 42, onClick)
    }

    fun copy(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        c.drawRoundRect(RectF(54f, 54f, 126f, 126f), 10f, 10f, p)
        c.drawLine(42f, 114f, 42f, 50f, p)
        c.drawLine(42f, 50f, 50f, 42f, p)
        c.drawLine(50f, 42f, 110f, 42f, p)
        return finish(a, bmp, 42, onClick)
    }

    fun dots(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val d = dotPaint()
        c.drawCircle(42f, 84f, 11f, d)
        c.drawCircle(84f, 84f, 11f, d)
        c.drawCircle(126f, 84f, 11f, d)
        return finish(a, bmp, 42, onClick)
    }

    // ---------- შექმნა / ტიპი / პარამეტრები ----------

    fun textNote(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        c.drawLine(50f, 46f, 118f, 46f, p)
        c.drawLine(50f, 70f, 118f, 70f, p)
        c.drawLine(50f, 94f, 106f, 94f, p)
        c.drawLine(50f, 118f, 92f, 118f, p)
        return finish(a, bmp, 42, onClick)
    }

    fun checklist(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        c.drawRoundRect(RectF(38f, 38f, 66f, 66f), 8f, 8f, p)
        c.drawLine(46f, 52f, 50f, 56f, p)
        c.drawLine(50f, 56f, 59f, 45f, p)
        c.drawLine(84f, 44f, 130f, 44f, p)
        c.drawLine(84f, 68f, 130f, 68f, p)
        c.drawLine(84f, 92f, 130f, 92f, p)
        c.drawLine(84f, 116f, 130f, 116f, p)
        return finish(a, bmp, 42, onClick)
    }

    fun categories(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        for (y in listOf(56f, 84f, 112f)) {
            c.drawLine(38f, y, 66f, y, p)
            c.drawLine(84f, y, 130f, y, p)
        }
        return finish(a, bmp, 42, onClick)
    }

    fun font(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        c.drawLine(58f, 126f, 84f, 42f, p)
        c.drawLine(84f, 42f, 110f, 126f, p)
        c.drawLine(70f, 100f, 98f, 100f, p)
        return finish(a, bmp, 42, onClick)
    }

    fun tag(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        val path = Path()
        path.moveTo(42f, 42f)
        path.lineTo(96f, 42f)
        path.lineTo(126f, 72f)
        path.lineTo(96f, 126f)
        path.lineTo(42f, 126f)
        path.close()
        c.drawPath(path, p)
        c.drawCircle(62f, 84f, 7f, dotPaint())
        return finish(a, bmp, 42, onClick)
    }

    fun leaf(a: MainActivity, onClick: (() -> Unit)? = null): ImageView {
        val (bmp, c) = canvas()
        val p = paint(10f)
        val leafPath = Path()
        leafPath.moveTo(42f, 126f)
        leafPath.cubicTo(42f, 70f, 70f, 42f, 126f, 42f)
        leafPath.cubicTo(126f, 98f, 98f, 126f, 42f, 126f)
        leafPath.close()
        c.drawPath(leafPath, p)
        c.drawLine(42f, 126f, 112f, 56f, p)
        return finish(a, bmp, 42, onClick)
    }

    /**
     * უკან დაბრუნების ღილაკი — ზუსტად ისე, როგორც BackArrow.view:
     * თეთრი წრე, ლავანდისფერი ჩარჩო, შიგნით სქელი კუთხე-ისარი (ვარიანტი 3).
     * გამოყენება: Icons.backButton(a) { a.handleBack() }
     */
    fun backButton(a: MainActivity, onClick: () -> Unit): ImageView {
        val size = 168
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        fill.style = Paint.Style.FILL
        fill.color = Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6f, fill)
        val border = Paint(Paint.ANTI_ALIAS_FLAG)
        border.style = Paint.Style.STROKE
        border.strokeWidth = 7f
        border.color = Color.parseColor(LAV)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6f, border)

        val arrow = Paint(Paint.ANTI_ALIAS_FLAG)
        arrow.style = Paint.Style.STROKE
        arrow.strokeWidth = 13f
        arrow.strokeCap = Paint.Cap.ROUND
        arrow.strokeJoin = Paint.Join.ROUND
        arrow.color = Color.parseColor(LAV)
        canvas.drawLine(108f, 52f, 66f, 84f, arrow)
        canvas.drawLine(66f, 84f, 108f, 116f, arrow)

        val iv = ImageView(a)
        iv.setImageBitmap(bmp)
        iv.layoutParams = LinearLayout.LayoutParams(UiHelpers.dp(a, 42), UiHelpers.dp(a, 42))
        iv.elevation = UiHelpers.dp(a, 1).toFloat()
        iv.setOnClickListener { onClick() }
        return iv
    }

    /**
     * აღდგენის ღილაკი — ზუსტად ისე, როგორც BackArrow.restoreView:
     * თეთრი წრე, ლავანდისფერი ჩარჩო, შიგნით მრუდე ისარი უკან.
     * გამოყენება: Icons.restoreButton(a) { restoreNote() }
     */
    fun restoreButton(a: MainActivity, onClick: () -> Unit): TextView {
        // ტექსტური პიქტოგრამა, როგორც პროექტის დანარჩენი პიქტოგრამები — ნახატის გარეშე
        val tv = TextView(a)
        tv.text = Pictograms.restore
        tv.textSize = 22f
        tv.setTypeface(null, Typeface.BOLD)
        tv.setTextColor(Color.parseColor(LAV))
        tv.gravity = Gravity.CENTER
        tv.layoutParams = LinearLayout.LayoutParams(UiHelpers.dp(a, 32), UiHelpers.dp(a, 32))
        tv.setOnClickListener { onClick() }
        return tv
    }
}

object Pictograms {

    // ---------- მთავარი / ნავიგაცია ----------
    const val home = "🏠"          // მთავარი — HomePage
    const val menu = "☰"           // მენიუ — HomePage
    const val chevronLeft = "‹"    // წინა — CalendarPage, ViewPage
    const val chevronRight = "›"   // შემდეგი — CalendarPage, ViewPage
    const val chevronDown = "▼"    // ჩამოსაშლელი — SettingsPage, EditorPage, ViewPage
    const val arrowUp = "↑"        // ზემოთ — EditorPage
    const val arrowDown = "↓"      // ქვემოთ — EditorPage
    const val close = "✕"          // დახურვა — EditorPage, PinGate
    const val dots = "⋯"           // მეტი — ViewPage
    const val dot = "●"            // წერტილი (დღეს ნოუტია) — CalendarPage, HomePage

    // ---------- ნოუტი ----------
    const val lock = "🔒"          // დაბლოკილი ნოუტი — ყველგან
    const val star = "⭐"          // რჩეული — HomePage, ViewPage
    const val tag = "🏷️"          // კატეგორია — HomePage, ViewPage, TrashPage, ArchivePage
    const val search = "🔍"        // ძიება — HomePage, ViewPage
    const val edit = "✏️"          // რედაქტირება — ViewPage
    const val reminder = "⏰"      // შეხსენება — ViewPage
    const val repeat = "🔁"        // გამეორება — ViewPage
    const val calendar = "📅"      // კალენდარი — HomePage, ViewPage, SettingsPage
    const val calendarSpiral = "🗓️" // კალენდარი (ზამბარით) — ViewPage
    const val page = "📄"          // ფურცელი / ნოუტი — ViewPage
    const val clipboard = "📋"     // ბუფერი / ინფო — ViewPage
    const val tap = "👆"           // დაჭერა (მინიშნება) — ViewPage
    const val heart = "💜"         // გული — EditorPage
    const val leaf = "🌿"          // MyNote ლოგო — MainActivity, HomePage, EditorPage, MenuPage

    // ---------- ჩამონათვალი ----------
    const val checkbox = "☐"       // ცარიელი ველი — ViewPage
    const val checkboxChecked = "☑️" // მონიშნული ველი — ViewPage
    const val checkmark = "✓"      // მონიშნული — SettingsPage

    // ---------- მენიუ / სანაგვე / არქივი ----------
    const val trash = "🗑️"         // სანაგვე — MenuPage, TrashPage, CategoriesPage, ViewPage
    const val archive = "🗄️"       // არქივი — MenuPage, ViewPage
    const val settings = "⚙️"      // პარამეტრები — MenuPage, HomePage
    const val feedback = "💬"      // გამოხმაურება — MenuPage
    const val widget = "🧩"       // ვიჯეტი — MenuPage, ViewPage
    const val exit = "🚪"          // გასვლა — MenuPage
    const val restore = "🔁"        // აღდგენა — TrashPage, ArchivePage (ემოჯი-პიქტოგრამა, როგორც 🔒 ⭐ 🗑️)

    // ---------- სეთინგები ----------
    const val globe = "🌐"         // ენა — SettingsPage
    const val moon = "🌙"          // მუქი რეჟიმი — SettingsPage
    const val eye = "👁️"          // ნახვა — SettingsPage
    const val person = "👤"        // პროფილი — SettingsPage
    const val folder = "📁"        // საქაღალდე — SettingsPage
    const val refresh = "🔄"       // განახლება — SettingsPage
    const val bell = "🔔"          // შეტყობინება — SettingsPage
    const val link = "🔗"          // ბმული — SettingsPage
    const val inputAbc = "🔤"      // შრიფტი / ენა — SettingsPage
}