package ge.kakha.mynote

import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.CalendarContract
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object ViewPage {

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

    private fun dateText(a: MainActivity, date: String): String {
        val todayIso = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        return if (date == "today" || date == "Today" || date == "Oggi" || date == "Сегодня" || date == "დღეს" || date == todayIso) Lang.str(a, "today") else date
    }

    private fun grayFilter(): ColorMatrixColorFilter {
        val m = ColorMatrix()
        m.setSaturation(0f)
        return ColorMatrixColorFilter(m)
    }

    // ---------- შეხსენება — კალენდარი და ციფერბლატიანი საათი ცალკე UI ფანჯრებში ----------
    private fun showReminder(a: MainActivity, note: Note) {
        val cal = Calendar.getInstance()
        var selYear = cal.get(Calendar.YEAR)
        var selMonth = cal.get(Calendar.MONTH)
        var selDay = cal.get(Calendar.DAY_OF_MONTH)
        var selHour = -1
        var selMinute = 0
        var repeatKey = "repeatNone"
        var addToCal = true

        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        fun pillBg(selected: Boolean): GradientDrawable {
            val d = GradientDrawable()
            d.shape = GradientDrawable.RECTANGLE
            d.cornerRadius = UiHelpers.dp(a, 12).toFloat()
            if (selected) d.setColor(Color.parseColor("#7A6FF0")) else d.setColor(Color.parseColor("#F3F0FF"))
            return d
        }

        // --- ცალკე ფანჯარა: კალენდარი ---
        fun showDateDialog(onPicked: () -> Unit) {
            var viewYear = selYear
            var viewMonth = selMonth
            UiDialog.show(a, Lang.str(a, "reminderDate")) { box2, dialog2 ->
                fun buildCalendar() {
                    box2.removeAllViews()
                    val head = LinearLayout(a)
                    head.orientation = LinearLayout.HORIZONTAL
                    head.gravity = Gravity.CENTER_VERTICAL
                    val prev = TextView(a); prev.text = "  ‹  "; prev.textSize = 26f; prev.setTypeface(null, Typeface.BOLD); prev.setTextColor(Color.parseColor("#7A6FF0"))
                    prev.setOnClickListener {
                        viewMonth -= 1
                        if (viewMonth < 0) { viewMonth = 11; viewYear -= 1 }
                        buildCalendar()
                    }
                    val title = TextView(a)
                    val vc = Calendar.getInstance(); vc.set(viewYear, viewMonth, 1)
                    title.text = monthFmt.format(vc.time)
                    title.textSize = 17f; title.setTypeface(null, Typeface.BOLD); title.setTextColor(Color.BLACK)
                    title.gravity = Gravity.CENTER
                    title.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    val next = TextView(a); next.text = "  ›  "; next.textSize = 26f; next.setTypeface(null, Typeface.BOLD); next.setTextColor(Color.parseColor("#7A6FF0"))
                    next.setOnClickListener {
                        viewMonth += 1
                        if (viewMonth > 11) { viewMonth = 0; viewYear += 1 }
                        buildCalendar()
                    }
                    head.addView(prev); head.addView(title); head.addView(next)
                    box2.addView(head)

                    val weekNames = when (Lang.code(a)) {
                        "en" -> listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                        "it" -> listOf("Lu", "Ma", "Me", "Gi", "Ve", "Sa", "Do")
                        "ru" -> listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                        else -> listOf("ორშ", "სამ", "ოთხ", "ხუთ", "პარ", "შაბ", "კვი")
                    }
                    val weekRow = LinearLayout(a)
                    weekRow.orientation = LinearLayout.HORIZONTAL
                    for (w in weekNames) {
                        val tv = TextView(a); tv.text = w; tv.textSize = 12f; tv.setTypeface(null, Typeface.BOLD)
                        tv.setTextColor(Color.parseColor("#888888")); tv.gravity = Gravity.CENTER
                        tv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        weekRow.addView(tv)
                    }
                    box2.addView(weekRow)

                    val first = Calendar.getInstance(); first.set(viewYear, viewMonth, 1)
                    val weekStart = a.getSharedPreferences("mynote", 0).getString("weekStart", "monday") ?: "monday"
                    var offset = if (weekStart == "sunday") first.get(Calendar.DAY_OF_WEEK) - 1 else first.get(Calendar.DAY_OF_WEEK) - 2
                    if (offset < 0) offset += 7
                    val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
                    var day = 1 - offset
                    for (r in 0 until 6) {
                        val row = LinearLayout(a)
                        row.orientation = LinearLayout.HORIZONTAL
                        for (c in 0 until 7) {
                            val cell = TextView(a)
                            cell.gravity = Gravity.CENTER
                            cell.layoutParams = LinearLayout.LayoutParams(0, UiHelpers.dp(a, 40), 1f)
                            if (day in 1..daysInMonth) {
                                cell.text = day.toString()
                                cell.textSize = 15f
                                val isSel = (day == selDay && viewMonth == selMonth && viewYear == selYear)
                                cell.background = pillBg(isSel)
                                cell.setTextColor(if (isSel) Color.WHITE else Color.BLACK)
                                if (isSel) cell.setTypeface(null, Typeface.BOLD)
                                val d = day
                                cell.setOnClickListener {
                                    selYear = viewYear; selMonth = viewMonth; selDay = d
                                    onPicked()
                                    dialog2.dismiss()
                                }
                            }
                            row.addView(cell)
                            day++
                        }
                        box2.addView(row)
                        if (day > daysInMonth) break
                    }
                }
                buildCalendar()
            }
        }

        // --- ცალკე ფანჯარა: ციფერბლატიანი საათი ---
        fun showTimeDialog(onPicked: () -> Unit) {
            // 12-საათიანი ციფერბლატი + AM/PM
            var tmpHour12 = if (selHour < 0) 9 else {
                val h = selHour % 12
                if (h == 0) 12 else h
            }
            var tmpMinute = if (selHour < 0) 0 else selMinute
            var isPM = selHour >= 12
            var modeHour = true

            UiDialog.show(a, Lang.str(a, "reminderTime")) { box2, dialog2 ->

                // clockView თავიდანვეა გამოცხადებული — ტაბები მასზე მიუთითებენ, შეცდომა აღარ იქნება
                var clockView: View? = null

                val big = TextView(a)
                big.textSize = 30f; big.setTypeface(null, Typeface.BOLD)
                big.setTextColor(Color.parseColor("#7A6FF0")); big.gravity = Gravity.CENTER
                big.setPadding(0, 0, 0, UiHelpers.dp(a, 8))
                fun refreshBig() {
                    val h24 = when {
                        tmpHour12 == 12 && !isPM -> 0
                        tmpHour12 == 12 && isPM -> 12
                        isPM -> tmpHour12 + 12
                        else -> tmpHour12
                    }
                    big.text = String.format(Locale.getDefault(), "%02d : %02d", h24, tmpMinute) + if (isPM) "  PM" else "  AM"
                }
                refreshBig()
                box2.addView(big)

                // საათი / წუთი ტაბები
                val tabsRow = LinearLayout(a)
                tabsRow.orientation = LinearLayout.HORIZONTAL
                tabsRow.gravity = Gravity.CENTER
                tabsRow.setPadding(0, 0, 0, UiHelpers.dp(a, 8))
                val hourTab = TextView(a); hourTab.text = "  " + "HH" + "  "
                hourTab.textSize = 15f; hourTab.gravity = Gravity.CENTER
                hourTab.setPadding(UiHelpers.dp(a, 18), UiHelpers.dp(a, 8), UiHelpers.dp(a, 18), UiHelpers.dp(a, 8))
                val minTab = TextView(a); minTab.text = "  " + "MM" + "  "
                minTab.textSize = 15f; minTab.gravity = Gravity.CENTER
                minTab.setPadding(UiHelpers.dp(a, 18), UiHelpers.dp(a, 8), UiHelpers.dp(a, 18), UiHelpers.dp(a, 8))
                fun refreshTabs() {
                    hourTab.background = pillBg(modeHour)
                    hourTab.setTextColor(if (modeHour) Color.WHITE else Color.BLACK)
                    hourTab.setTypeface(null, if (modeHour) Typeface.BOLD else Typeface.NORMAL)
                    minTab.background = pillBg(!modeHour)
                    minTab.setTextColor(if (!modeHour) Color.WHITE else Color.BLACK)
                    minTab.setTypeface(null, if (!modeHour) Typeface.BOLD else Typeface.NORMAL)
                }
                refreshTabs()
                hourTab.setOnClickListener { modeHour = true; refreshTabs(); clockView?.invalidate() }
                minTab.setOnClickListener { modeHour = false; refreshTabs(); clockView?.invalidate() }
                tabsRow.addView(hourTab); tabsRow.addView(minTab)
                box2.addView(tabsRow)

                // AM / PM
                val ampmRow = LinearLayout(a)
                ampmRow.orientation = LinearLayout.HORIZONTAL
                ampmRow.gravity = Gravity.CENTER
                ampmRow.setPadding(0, 0, 0, UiHelpers.dp(a, 8))
                val amBtn = TextView(a); amBtn.text = "  AM  "; amBtn.textSize = 15f; amBtn.gravity = Gravity.CENTER
                amBtn.setPadding(UiHelpers.dp(a, 18), UiHelpers.dp(a, 8), UiHelpers.dp(a, 18), UiHelpers.dp(a, 8))
                val pmBtn = TextView(a); pmBtn.text = "  PM  "; pmBtn.textSize = 15f; pmBtn.gravity = Gravity.CENTER
                pmBtn.setPadding(UiHelpers.dp(a, 18), UiHelpers.dp(a, 8), UiHelpers.dp(a, 18), UiHelpers.dp(a, 8))
                fun refreshAmpm() {
                    amBtn.background = pillBg(!isPM)
                    amBtn.setTextColor(if (!isPM) Color.WHITE else Color.BLACK)
                    pmBtn.background = pillBg(isPM)
                    pmBtn.setTextColor(if (isPM) Color.WHITE else Color.BLACK)
                }
                refreshAmpm()
                amBtn.setOnClickListener { isPM = false; refreshAmpm(); refreshBig(); clockView?.invalidate() }
                pmBtn.setOnClickListener { isPM = true; refreshAmpm(); refreshBig(); clockView?.invalidate() }
                ampmRow.addView(amBtn); ampmRow.addView(pmBtn)
                box2.addView(ampmRow)

                // ციფერბლატი
                clockView = object : View(a) {
                    val facePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                    val numPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                    val handPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                    init {
                        facePaint.color = Color.parseColor("#F3F0FF")
                        facePaint.style = Paint.Style.FILL
                        numPaint.color = Color.BLACK
                        numPaint.textSize = UiHelpers.dp(a, 16).toFloat()
                        numPaint.textAlign = Paint.Align.CENTER
                        numPaint.typeface = Typeface.DEFAULT_BOLD
                        handPaint.color = Color.parseColor("#7A6FF0")
                        handPaint.strokeWidth = UiHelpers.dp(a, 4).toFloat()
                        handPaint.strokeCap = Paint.Cap.ROUND
                        dotPaint.color = Color.parseColor("#7A6FF0")
                        dotPaint.style = Paint.Style.FILL
                    }
                    override fun onMeasure(wms: Int, hms: Int) {
                        val s = UiHelpers.dp(a, 280)
                        setMeasuredDimension(s, s)
                    }
                    override fun onDraw(canvas: Canvas) {
                        super.onDraw(canvas)
                        val cx = width / 2f
                        val cy = height / 2f
                        val radius = (width / 2f) - UiHelpers.dp(a, 8)
                        canvas.drawCircle(cx, cy, radius, facePaint)
                        // რიცხვები
                        for (i in 0 until 12) {
                            val angle = Math.toRadians((i * 30).toDouble())
                            val x = cx + (sin(angle) * (radius - UiHelpers.dp(a, 28))).toFloat()
                            val y = cy - (cos(angle) * (radius - UiHelpers.dp(a, 28))).toFloat() + UiHelpers.dp(a, 6)
                            val label = if (modeHour) {
                                if (i == 0) "12" else i.toString()
                            } else {
                                if (i == 0) "00" else String.format(Locale.getDefault(), "%02d", i * 5)
                            }
                            val isSel = if (modeHour) {
                                (if (i == 0) 12 else i) == tmpHour12
                            } else {
                                (i * 5) == tmpMinute
                            }
                            if (isSel) {
                                canvas.drawCircle(x, y - UiHelpers.dp(a, 6), UiHelpers.dp(a, 18).toFloat(), dotPaint)
                                numPaint.color = Color.WHITE
                            } else {
                                numPaint.color = Color.BLACK
                            }
                            canvas.drawText(label, x, y, numPaint)
                        }
                        // ისარი
                        val selAngleDeg = if (modeHour) {
                            (tmpHour12 % 12) * 30.0
                        } else {
                            tmpMinute * 6.0
                        }
                        val rad = Math.toRadians(selAngleDeg)
                        val hx = cx + (sin(rad) * (radius - UiHelpers.dp(a, 52))).toFloat()
                        val hy = cy - (cos(rad) * (radius - UiHelpers.dp(a, 52))).toFloat()
                        canvas.drawLine(cx, cy, hx, hy, handPaint)
                        canvas.drawCircle(hx, hy, UiHelpers.dp(a, 10).toFloat(), dotPaint)
                        canvas.drawCircle(cx, cy, UiHelpers.dp(a, 8).toFloat(), dotPaint)
                    }
                    override fun onTouchEvent(e: MotionEvent): Boolean {
                        if (e.action == MotionEvent.ACTION_DOWN || e.action == MotionEvent.ACTION_MOVE) {
                            val cx = width / 2f
                            val cy = height / 2f
                            val dx = e.x - cx
                            val dy = e.y - cy
                            var deg = Math.toDegrees(atan2(dx.toDouble(), (-dy).toDouble()))
                            if (deg < 0) deg += 360.0
                            if (modeHour) {
                                var h = ((deg + 15.0) / 30.0).toInt() % 12
                                if (h == 0) h = 12
                                tmpHour12 = h
                            } else {
                                tmpMinute = ((deg / 6.0).toInt()) % 60
                            }
                            refreshBig()
                            invalidate()
                            return true
                        }
                        if (e.action == MotionEvent.ACTION_UP) {
                            if (modeHour) {
                                modeHour = false
                                refreshTabs()
                                invalidate()
                            }
                            return true
                        }
                        return super.onTouchEvent(e)
                    }
                }
                box2.addView(clockView)

                val hint = TextView(a)
                hint.text = "👆 " + Lang.str(a, if (modeHour) "pickHour" else "pickMinute")
                hint.textSize = 13f; hint.setTextColor(Color.parseColor("#888888")); hint.gravity = Gravity.CENTER
                hint.setPadding(0, UiHelpers.dp(a, 8), 0, 0)
                box2.addView(hint)

                box2.addView(UiDialog.actionButton(a, Lang.str(a, "save")) {
                    val h24 = when {
                        tmpHour12 == 12 && !isPM -> 0
                        tmpHour12 == 12 && isPM -> 12
                        isPM -> tmpHour12 + 12
                        else -> tmpHour12
                    }
                    selHour = h24; selMinute = tmpMinute
                    onPicked()
                    dialog2.dismiss()
                })
            }
        }

        // --- მთავარი შეხსენების ფანჯარა ---
        UiDialog.show(a, Lang.str(a, "reminder")) { box, dialog ->

            fun divider() {
                val v = View(a)
                v.setBackgroundColor(Color.parseColor("#EDE9FF"))
                box.addView(v, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, UiHelpers.dp(a, 1)).apply {
                    setMargins(0, UiHelpers.dp(a, 6), 0, UiHelpers.dp(a, 6))
                })
            }

            val dateRow = LinearLayout(a)
            dateRow.orientation = LinearLayout.HORIZONTAL
            dateRow.gravity = Gravity.CENTER_VERTICAL
            dateRow.setPadding(UiHelpers.dp(a, 4), UiHelpers.dp(a, 14), UiHelpers.dp(a, 4), UiHelpers.dp(a, 14))
            val dateIcon = TextView(a); dateIcon.text = "📅  "; dateIcon.textSize = 20f
            val dateLabel = TextView(a); dateLabel.text = Lang.str(a, "reminderDate"); dateLabel.textSize = 17f; dateLabel.setTextColor(Color.BLACK)
            dateLabel.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val dateValue = TextView(a)
            dateValue.textSize = 16f; dateValue.setTypeface(null, Typeface.BOLD); dateValue.setTextColor(Color.parseColor("#7A6FF0"))
            fun refreshDate() {
                val c = Calendar.getInstance(); c.set(selYear, selMonth, selDay)
                dateValue.text = dateFmt.format(c.time) + "  ▼"
            }
            refreshDate()
            dateRow.addView(dateIcon); dateRow.addView(dateLabel); dateRow.addView(dateValue)
            dateRow.setOnClickListener { showDateDialog { refreshDate() } }
            box.addView(dateRow)
            divider()

            val timeRow = LinearLayout(a)
            timeRow.orientation = LinearLayout.HORIZONTAL
            timeRow.gravity = Gravity.CENTER_VERTICAL
            timeRow.setPadding(UiHelpers.dp(a, 4), UiHelpers.dp(a, 14), UiHelpers.dp(a, 4), UiHelpers.dp(a, 14))
            val timeIcon = TextView(a); timeIcon.text = "⏰  "; timeIcon.textSize = 20f
            val timeLabel = TextView(a); timeLabel.text = Lang.str(a, "reminderTime"); timeLabel.textSize = 17f; timeLabel.setTextColor(Color.BLACK)
            timeLabel.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val timeValue = TextView(a)
            timeValue.textSize = 16f; timeValue.setTypeface(null, Typeface.BOLD); timeValue.setTextColor(Color.parseColor("#7A6FF0"))
            fun refreshTime() {
                timeValue.text = if (selHour < 0) Lang.str(a, "repeatNone") + "  ▼" else String.format(Locale.getDefault(), "%02d:%02d  ▼", selHour, selMinute)
            }
            refreshTime()
            timeRow.addView(timeIcon); timeRow.addView(timeLabel); timeRow.addView(timeValue)
            timeRow.setOnClickListener { showTimeDialog { refreshTime() } }
            box.addView(timeRow)
            divider()

            val repRow = LinearLayout(a)
            repRow.orientation = LinearLayout.HORIZONTAL
            repRow.gravity = Gravity.CENTER_VERTICAL
            repRow.setPadding(UiHelpers.dp(a, 4), UiHelpers.dp(a, 14), UiHelpers.dp(a, 4), UiHelpers.dp(a, 14))
            val repIcon = TextView(a); repIcon.text = "🔁  "; repIcon.textSize = 20f
            val repLabel = TextView(a); repLabel.text = Lang.str(a, "repeat"); repLabel.textSize = 17f; repLabel.setTextColor(Color.BLACK)
            repLabel.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val repValue = TextView(a)
            repValue.textSize = 16f; repValue.setTypeface(null, Typeface.BOLD); repValue.setTextColor(Color.parseColor("#7A6FF0"))
            fun refreshRep() { repValue.text = Lang.str(a, repeatKey) + "  ▼" }
            refreshRep()
            repRow.addView(repIcon); repRow.addView(repLabel); repRow.addView(repValue)
            repRow.setOnClickListener {
                val keys = listOf("repeatNone", "repeatDaily", "repeatWeekly", "repeatMonthly", "repeatYearly")
                UiDialog.show(a, Lang.str(a, "repeat")) { box2, dialog2 ->
                    for (k in keys) {
                        val row = TextView(a)
                        row.text = Lang.str(a, k)
                        row.textSize = 17f
                        row.setTextColor(if (k == repeatKey) Color.parseColor("#7A6FF0") else Color.BLACK)
                        if (k == repeatKey) row.setTypeface(null, Typeface.BOLD)
                        row.setPadding(UiHelpers.dp(a, 8), UiHelpers.dp(a, 14), UiHelpers.dp(a, 8), UiHelpers.dp(a, 14))
                        row.setOnClickListener {
                            repeatKey = k; refreshRep(); dialog2.dismiss()
                        }
                        box2.addView(row)
                    }
                }
            }
            box.addView(repRow)
            divider()

            val calRow = LinearLayout(a)
            calRow.orientation = LinearLayout.HORIZONTAL
            calRow.gravity = Gravity.CENTER_VERTICAL
            calRow.setPadding(UiHelpers.dp(a, 4), UiHelpers.dp(a, 14), UiHelpers.dp(a, 4), UiHelpers.dp(a, 14))
            val calIcon = TextView(a); calIcon.text = "🗓️  "; calIcon.textSize = 20f
            val calLabel = TextView(a); calLabel.text = Lang.str(a, "addToCalendar"); calLabel.textSize = 17f; calLabel.setTextColor(Color.BLACK)
            calLabel.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val calSwitch = Switch(a)
            calSwitch.isChecked = true
            calSwitch.setOnCheckedChangeListener { _, c -> addToCal = c }
            calRow.addView(calIcon); calRow.addView(calLabel); calRow.addView(calSwitch)
            calRow.setOnClickListener { calSwitch.isChecked = !calSwitch.isChecked }
            box.addView(calRow)

            box.addView(UiDialog.actionButton(a, Lang.str(a, "save")) {
                if (selHour < 0) {
                    Toast.makeText(a, Lang.str(a, "pickTimeFirst"), Toast.LENGTH_SHORT).show()
                    return@actionButton
                }
                val target = Calendar.getInstance()
                target.set(selYear, selMonth, selDay, selHour, selMinute, 0)
                target.set(Calendar.MILLISECOND, 0)
                if (target.timeInMillis <= System.currentTimeMillis() && repeatKey == "repeatNone") {
                    target.add(Calendar.DAY_OF_MONTH, 1)
                }
                if (!a.getSharedPreferences("mynote", 0).getBoolean("notif", true)) { Toast.makeText(a, Lang.str(a, "notifOffMsg"), Toast.LENGTH_SHORT).show() }
                else a.askNotifPermission(force = true) // შეხსენების დაყენებისას სისტემურ ნებართვას ვითხოვთ, თორემ შეტყობინება ჩუმად დაიკარგება
                val prefs = a.getSharedPreferences("mynote", 0)
                val key = "reminder_" + note.id
                val bodyText = if (note.noteType == "checklist") note.checklist.joinToString(", ") { it.text } else note.text
                prefs.edit().putLong(key, target.timeInMillis).putString(key + "_repeat", repeatKey).putString(key + "_title", note.title).putString(key + "_body", bodyText).apply()
                val alarmIntent = Intent(a, ReminderReceiver::class.java)
                alarmIntent.putExtra("title", note.title); alarmIntent.putExtra("body", bodyText); alarmIntent.putExtra("noteId", note.id); alarmIntent.putExtra("repeat", repeatKey)
                val oldPi = PendingIntent.getBroadcast(a, note.id, Intent(a, ReminderReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE); if (oldPi != null) { try { (a.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(oldPi) } catch (e: Exception) {} }
                val pi = PendingIntent.getBroadcast(a, note.id, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val am = a.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val interval = when (repeatKey) {
                    "repeatDaily" -> AlarmManager.INTERVAL_DAY
                    "repeatWeekly" -> AlarmManager.INTERVAL_DAY * 7
                    else -> 0L // ყოველთვიური/ყოველწლიური: ზუსტად იჯავშნება და Receiver-ი თავიდან ჯავშნის
                }
                try {
                    if (interval > 0) am.setRepeating(AlarmManager.RTC_WAKEUP, target.timeInMillis, interval, pi)
                    else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pi)
                } catch (e: Exception) {
                    am.set(AlarmManager.RTC_WAKEUP, target.timeInMillis, pi)
                }

                if (addToCal) {
                    try {
                        val calIntent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                        calIntent.putExtra(CalendarContract.Events.TITLE, note.title)
                        calIntent.putExtra(CalendarContract.Events.DESCRIPTION, if (note.noteType == "checklist") note.checklist.joinToString("\n") { it.text } else note.text)
                        calIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, target.timeInMillis)
                        calIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, target.timeInMillis + 60 * 60 * 1000)
                        a.startActivity(calIntent)
                    } catch (e: Exception) { }
                }
                dialog.dismiss()
                Toast.makeText(a, Lang.str(a, "reminderSaved"), Toast.LENGTH_SHORT).show()
            })
        }
    }

    private fun exportPdf(a: MainActivity, note: Note) {
        try {
            val body = if (note.noteType == "checklist") {
                note.checklist.joinToString("\n") { (if (it.done) "☑  " else "☐  ") + it.text }
            } else {
                note.text
            }
            val doc = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 48
            val contentWidth = pageWidth - margin * 2

            val titlePaint = TextPaint()
            titlePaint.color = Color.BLACK
            titlePaint.textSize = 26f
            titlePaint.typeface = Typeface.DEFAULT_BOLD
            val metaPaint = TextPaint()
            metaPaint.color = Color.parseColor("#666666")
            metaPaint.textSize = 14f
            val bodyPaint = TextPaint()
            bodyPaint.color = Color.BLACK
            bodyPaint.textSize = 16f

            var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            var canvas: Canvas = page.canvas
            var y = margin.toFloat()

            fun drawText(paint: TextPaint, text: String) {
                if (text.isBlank()) return
                val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.25f)
                    .setIncludePad(false)
                    .build()
                if (y + layout.height > pageHeight - margin) {
                    doc.finishPage(page)
                    page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
                    canvas = page.canvas
                    y = margin.toFloat()
                }
                canvas.save()
                canvas.translate(margin.toFloat(), y)
                layout.draw(canvas)
                canvas.restore()
                y += layout.height + 12f
            }

            drawText(titlePaint, note.title)
            drawText(metaPaint, note.folder + "  •  " + dateText(a, note.date))
            val linePaint = Paint()
            linePaint.color = Color.parseColor("#DDDDDD")
            linePaint.strokeWidth = 2f
            canvas.drawLine(margin.toFloat(), y, (pageWidth - margin).toFloat(), y, linePaint)
            y += 20f
            drawText(bodyPaint, body.ifBlank { Lang.str(a, "emptyNote") })

            doc.finishPage(page)

            val safeName = note.title.ifBlank { "note" }
                .replace("[^a-zA-Z0-9ა-ჰ\\s_-]".toRegex(), "")
                .trim().ifBlank { "note" } + ".pdf"
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MyNote")
            val uri = a.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri == null) {
                doc.close()
                Toast.makeText(a, Lang.str(a, "pdfError"), Toast.LENGTH_SHORT).show()
                return
            }
            val out = a.contentResolver.openOutputStream(uri)
            if (out == null) {
                doc.close()
                Toast.makeText(a, Lang.str(a, "pdfError"), Toast.LENGTH_SHORT).show()
                return
            }
            out.use { doc.writeTo(it) }
            doc.close()
            Toast.makeText(a, Lang.str(a, "pdfSaved"), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(a, Lang.str(a, "pdfError"), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirm(a: MainActivity, note: Note) {
        UiDialog.show(a, Lang.str(a, "delete")) { box, dialog ->
            val msg = TextView(a)
            msg.text = Lang.str(a, "deleteConfirm")
            msg.textSize = 16f
            msg.setTextColor(Color.BLACK)
            box.addView(msg)
            box.addView(UiDialog.actionButton(a, Lang.str(a, "delete")) {
                // წაშლა = სანაგვეში გადატანა, სამუდამოდ წაშლა სანაგვიდან ხდება
                note.isDeleted = true
                note.isArchived = false
                a.saveNotes()
                dialog.dismiss()
                Toast.makeText(a, Lang.str(a, "movedToTrash"), Toast.LENGTH_SHORT).show()
                a.showHomePage()
            })
        }
    }

    private fun showNoteInfo(a: MainActivity, note: Note) {
        val fullText = if (note.noteType == "checklist") {
            (listOf(note.title) + note.checklist.map { it.text }).joinToString("\n")
        } else {
            note.title + "\n" + note.text
        }
        val words = fullText.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        val chars = fullText.length

        UiDialog.show(a, Lang.str(a, "noteInfo")) { box, _ ->
            fun infoRow(labelKey: String, value: String) {
                val label = TextView(a)
                label.text = Lang.str(a, labelKey) + ":"
                label.textSize = 14f
                label.setTextColor(Color.parseColor("#888888"))
                box.addView(label)
                val v = TextView(a)
                v.text = value
                v.textSize = 18f
                v.setTextColor(Color.BLACK)
                v.setPadding(0, 0, 0, UiHelpers.dp(a, 18))
                box.addView(v)
            }
            infoRow("folder", note.folder)
                    val fmt = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
        infoRow("createdTime", if (note.createdAt > 0L) fmt.format(java.util.Date(note.createdAt)) else dateText(a, note.date))
        infoRow("lastModified", if (note.updatedAt > 0L) fmt.format(java.util.Date(note.updatedAt)) else dateText(a, note.date))
            infoRow("words", words.toString())
            infoRow("characters", chars.toString())
        }
    }

    private fun showMoreMenu(a: MainActivity, note: Note) {
        val dialog = Dialog(a)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        val card = LinearLayout(a)
        card.orientation = LinearLayout.VERTICAL
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.RECTANGLE
        bg.cornerRadius = UiHelpers.dp(a, 16).toFloat()
        bg.setColor(Color.WHITE)
        bg.setStroke(UiHelpers.dp(a, 1), Color.parseColor("#EDE9FF"))
        card.background = bg
        card.elevation = UiHelpers.dp(a, 12).toFloat()
        card.setPadding(UiHelpers.dp(a, 8), UiHelpers.dp(a, 8), UiHelpers.dp(a, 8), UiHelpers.dp(a, 8))

        fun menuRow(icon: String, title: String, isFavRow: Boolean = false, isFav: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
            val row = LinearLayout(a)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(UiHelpers.dp(a, 12), UiHelpers.dp(a, 12), UiHelpers.dp(a, 12), UiHelpers.dp(a, 12))
            val iconTv = TextView(a)
            iconTv.text = icon
            iconTv.textSize = 20f
            if (!enabled || (isFavRow && !isFav)) {
                iconTv.paint.colorFilter = grayFilter()
            }
            iconTv.setPadding(0, 0, UiHelpers.dp(a, 12), 0)
            row.addView(iconTv)
            val titleTv = TextView(a)
            titleTv.text = title
            titleTv.textSize = 17f
            titleTv.setTextColor(if (enabled) Color.BLACK else Color.parseColor("#AAAAAA"))
            row.addView(titleTv)
            row.setOnClickListener {
                if (!enabled) return@setOnClickListener
                dialog.dismiss()
                onClick()
            }
            card.addView(row)
        }
        fun soon() {
            Toast.makeText(a, Lang.str(a, "comingSoon"), Toast.LENGTH_SHORT).show()
        }
        menuRow(Pictograms.star, Lang.str(a, "check"), isFavRow = true, isFav = note.isFav) {
            note.isFav = !note.isFav
            a.saveNotes()
            show(a, note)
        }
        // არქივის ღილაკი ნოუტის მდგომარეობის მიხედვით:
        // წაშლილი -> გათიშულია | არქივშია -> არქივიდან აღდგენა | ჩვეულებრივი -> არქივში გადატანა
        if (note.isDeleted) {
            menuRow(Pictograms.archive, Lang.str(a, "archive"), enabled = false) {}
        } else if (note.isArchived) {
            menuRow(Pictograms.restore, Lang.str(a, "restoreFromArchive")) {
                note.isArchived = false
                a.saveNotes()
                Toast.makeText(a, Lang.str(a, "restoredMsg"), Toast.LENGTH_SHORT).show()
                a.showHomePage()
            }
        } else {
            menuRow(Pictograms.archive, Lang.str(a, "archive")) {
                note.isArchived = true
                a.saveNotes()
                Toast.makeText(a, Lang.str(a, "archivedMsg"), Toast.LENGTH_SHORT).show()
                a.showHomePage()
            }
        }
        menuRow(Pictograms.page, Lang.str(a, "exportPdf"), enabled = !note.isDeleted) { exportPdf(a, note) }
        menuRow(Pictograms.widget, Lang.str(a, "addWidget"), enabled = !note.isDeleted) { soon() }
        menuRow(Pictograms.reminder, Lang.str(a, "reminder"), enabled = !note.isDeleted) { showReminder(a, note) }
        // წაშლის ღილაკი: წაშლილ ნოუტზე -> წაშლილიდან აღდგენა, სხვა შემთხვევაში -> წაშლა
        if (note.isDeleted) {
            menuRow(Pictograms.restore, Lang.str(a, "restoreFromTrash")) {
                note.isDeleted = false
                a.saveNotes()
                Toast.makeText(a, Lang.str(a, "restoredMsg"), Toast.LENGTH_SHORT).show()
                a.showHomePage()
            }
        } else {
            menuRow(Pictograms.trash, Lang.str(a, "delete")) { showDeleteConfirm(a, note) }
        }
        menuRow(Pictograms.lock, Lang.str(a, if (a.isNoteLocked(note.id)) "unlockNote" else "lock"), enabled = !note.isDeleted) {
            if (a.isNoteLocked(note.id)) {
                PinGate.promptForNote(a, onUnlock = {
                    a.setNoteLocked(note.id, false)
                    Toast.makeText(a, Lang.str(a, "noteUnlocked"), Toast.LENGTH_SHORT).show()
                    show(a, note)
                }, onCancel = {})
            } else {
                PinGate.ensurePin(a) {
                    a.setNoteLocked(note.id, true)
                    Toast.makeText(a, Lang.str(a, "noteLocked"), Toast.LENGTH_SHORT).show()
                    show(a, note)
                }
            }
        }
        menuRow(Pictograms.search, Lang.str(a, "search")) { a.showHomePage() }
        menuRow(Pictograms.clipboard, Lang.str(a, "noteInfo")) { showNoteInfo(a, note) }

        dialog.setContentView(card)
        val w = dialog.window
        if (w != null) {
            w.setLayout(
                (a.resources.displayMetrics.widthPixels * 0.72).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            w.setGravity(Gravity.TOP or Gravity.END)
            val params = w.attributes
            params.y = UiHelpers.dp(a, 70)
            params.x = UiHelpers.dp(a, 12)
            w.attributes = params
            w.setDimAmount(0.25f)
            w.setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog.show()
    }

    fun show(a: MainActivity, note: Note) {
        a.currentScreen = "view"
        val catColor = getCatColor(a, note.folder)
        val outer = LinearLayout(a)
        outer.orientation = LinearLayout.VERTICAL
        outer.setBackgroundColor(Color.parseColor(a.LAV_BG))
        outer.setPadding(
            UiHelpers.dp(a, 10),
            UiHelpers.dp(a, 10),
            UiHelpers.dp(a, 10),
            UiHelpers.dp(a, 10) + UiHelpers.navBarHeight(a)
        )

        val topBar = LinearLayout(a)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(UiHelpers.dp(a, 6), UiHelpers.dp(a, 4), UiHelpers.dp(a, 6), UiHelpers.dp(a, 10))

        val btnBackTop = Icons.backButton(a) { a.handleBack() }
        topBar.addView(btnBackTop)

        val spacer = TextView(a)
        spacer.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        topBar.addView(spacer)

        val moreWrap = LinearLayout(a)
        moreWrap.gravity = Gravity.CENTER
        val moreBg = GradientDrawable()
        moreBg.shape = GradientDrawable.OVAL
        moreBg.setColor(Color.WHITE)
        moreBg.setStroke(UiHelpers.dp(a, 1), Color.parseColor("#EDE9FF"))
        moreWrap.background = moreBg
        moreWrap.elevation = UiHelpers.dp(a, 4).toFloat()
        moreWrap.layoutParams = LinearLayout.LayoutParams(UiHelpers.dp(a, 40), UiHelpers.dp(a, 40))
        val moreTv = TextView(a)
        moreTv.text = "⋯"
        moreTv.textSize = 22f
        moreTv.setTypeface(null, Typeface.BOLD)
        moreTv.setTextColor(Color.parseColor("#5A4DB8"))
        moreTv.gravity = Gravity.CENTER
        moreWrap.addView(moreTv)
        moreWrap.setOnClickListener { showMoreMenu(a, note) }
        topBar.addView(moreWrap)

        outer.addView(topBar)

        val scroll = ScrollView(a)
        val paper = LinearLayout(a)
        paper.orientation = LinearLayout.VERTICAL
        val paperBg = GradientDrawable()
        paperBg.shape = GradientDrawable.RECTANGLE
        paperBg.cornerRadius = UiHelpers.dp(a, 22).toFloat()
        paperBg.setColor(paleColor(catColor))
        paper.background = paperBg
        paper.clipToOutline = true

        val bar = View(a)
        val barBg = GradientDrawable()
        barBg.shape = GradientDrawable.RECTANGLE
        val r = UiHelpers.dp(a, 22).toFloat()
        barBg.cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        barBg.setColor(Color.parseColor(catColor))
        bar.background = barBg
        bar.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            UiHelpers.dp(a, 14)
        )
        paper.addView(bar)

        val content = LinearLayout(a)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(
            UiHelpers.dp(a, 18),
            UiHelpers.dp(a, 16),
            UiHelpers.dp(a, 18),
            UiHelpers.dp(a, 18)
        )
        paper.addView(content)

        val titleRow = LinearLayout(a)
        titleRow.orientation = LinearLayout.HORIZONTAL
        titleRow.gravity = Gravity.CENTER_VERTICAL
        titleRow.setPadding(0, UiHelpers.dp(a, 16), 0, UiHelpers.dp(a, 6))

        val starTv = TextView(a)
        starTv.text = "⭐  "
        starTv.textSize = 22f
        starTv.typeface = Typeface.DEFAULT_BOLD
        starTv.paint.colorFilter = if (note.isFav) null else grayFilter()
        titleRow.addView(starTv)

        val tvTitle = TextView(a)
        tvTitle.text = note.title
                tvTitle.setTextIsSelectable(true)
        tvTitle.textSize = 22f
        tvTitle.typeface = Typeface.DEFAULT_BOLD
        tvTitle.setTextColor(Color.BLACK)
        titleRow.addView(tvTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val tvMeta = TextView(a)
        val fmtMeta = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
        tvMeta.text = "🏷️  " + note.folder + "   •   " + dateText(a, note.date) + if (note.updatedAt > 0L) "   •   " + Lang.str(a, "lastModified") + ": " + fmtMeta.format(java.util.Date(note.updatedAt)) else ""
        tvMeta.textSize = 12f
        tvMeta.setTextColor(Color.parseColor("#444444"))
        tvMeta.setPadding(0, 0, 0, UiHelpers.dp(a, 8))

        content.addView(titleRow)
        content.addView(tvMeta)
        content.addView(
            UiHelpers.lineView(a),
            LinearLayout.LayoutParams(-1, UiHelpers.dp(a, 1)).apply {
                setMargins(0, UiHelpers.dp(a, 4), 0, UiHelpers.dp(a, 12))
            }
        )

        if (note.noteType == "checklist") {
            val done = note.checklist.count { it.done }
            val tvProg = TextView(a)
            tvProg.text = "☑️  " + done.toString() + " / " + note.checklist.size.toString()
            tvProg.textSize = 13f
            tvProg.typeface = Typeface.DEFAULT_BOLD
            tvProg.setTextColor(Color.parseColor("#7A6FF0"))
            tvProg.setPadding(0, 0, 0, UiHelpers.dp(a, 10))
            content.addView(tvProg)
            for (ci in note.checklist) {
                val rRow = LinearLayout(a)
                rRow.orientation = LinearLayout.HORIZONTAL
                rRow.gravity = Gravity.CENTER_VERTICAL
                rRow.setPadding(0, UiHelpers.dp(a, 6), 0, UiHelpers.dp(a, 6))
                val cb = CheckBox(a)
                cb.isChecked = ci.done
                cb.setOnCheckedChangeListener { _, checked ->
                    ci.done = checked
                    a.saveNotes()
                    show(a, note)
                }
                val tv = TextView(a)
                tv.text = ci.text
                tv.setTextIsSelectable(true)
                tv.textSize = a.viewFontSize.toFloat()
                if (ci.done) {
                    tv.setTextColor(Color.parseColor("#888888"))
                    tv.paintFlags = tv.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    tv.setTextColor(Color.BLACK)
                }
                rRow.addView(cb)
                rRow.addView(tv, LinearLayout.LayoutParams(0, -2, 1f))
                content.addView(rRow)
                content.addView(
                    UiHelpers.lineView(a),
                    LinearLayout.LayoutParams(-1, UiHelpers.dp(a, 1))
                )
            }
        } else {
            val tvText = TextView(a)
            tvText.text = note.text.ifBlank { Lang.str(a, "emptyNote") }
            tvText.textSize = a.viewFontSize.toFloat()
            tvText.setTextColor(Color.BLACK)
            tvText.setPadding(0, 0, 0, UiHelpers.dp(a, 12))
            // ნახვის რეჟიმში ტექსტის კოპირება: მონიშვნა + Copy, ფორმატი ისე რჩება როგორც დაწერილია
            tvText.setTextIsSelectable(true)
            tvText.setSingleLine(false)
            tvText.setHorizontallyScrolling(false)
            content.addView(tvText)
        }

        scroll.addView(paper)

        val bottomBar = LinearLayout(a)
        bottomBar.orientation = LinearLayout.HORIZONTAL
        bottomBar.setBackgroundColor(Color.parseColor(a.LAV_BG))
        bottomBar.setPadding(UiHelpers.dp(a, 4), UiHelpers.dp(a, 10), UiHelpers.dp(a, 4), UiHelpers.dp(a, 4))
        bottomBar.gravity = Gravity.CENTER

        val btnEdit = TextView(a)
        btnEdit.text = "✏️  " + Lang.str(a, "edit")
        btnEdit.textSize = 15f
        btnEdit.typeface = Typeface.DEFAULT_BOLD
        btnEdit.setTextColor(Color.WHITE)
        btnEdit.gravity = Gravity.CENTER
        btnEdit.background = UiHelpers.rounded(a.LAV, 24, a)
        btnEdit.setPadding(
            UiHelpers.dp(a, 20),
            UiHelpers.dp(a, 16),
            UiHelpers.dp(a, 20),
            UiHelpers.dp(a, 16)
        )
        btnEdit.setOnClickListener { a.showEditor(note, note.noteType) }
        bottomBar.addView(btnEdit, LinearLayout.LayoutParams(-1, -2))

        outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        outer.addView(bottomBar)
        a.setContentView(outer)
    }
}