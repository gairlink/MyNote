package ge.kakha.mynote

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews

// ვიჯეტი: ერთი — ბოლო ნოუტი, ზუსტად HomePage-ის ნოუტის სტილში.
// ნოუტი ფანჯრის შუაშია დასმული — ზემოთაც და ქვემოთაც თანაბარი დაშორებით.
// ტექსტის ხაზები ფანჯრის მიხედვით ითვლება (მაღალ ფანჯარაში 8 ხაზამდე).
class NoteSingleWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateOne(context, appWidgetManager, id)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        updateOne(context, appWidgetManager, appWidgetId)
    }

    companion object {
        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, NoteSingleWidgetProvider::class.java))
            for (id in ids) updateOne(context, mgr, id)
        }

        private fun updateOne(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val views = try {
                buildViews(context, mgr, widgetId)
            } catch (e: Exception) {
                RemoteViews(context.packageName, R.layout.widget_single).apply {
                    setOnClickPendingIntent(R.id.widget_header, WidgetIntents.openApp(context))
                }
            }
            try { mgr.updateAppWidget(widgetId, views) } catch (e: Exception) { }
        }

        private fun buildViews(context: Context, mgr: AppWidgetManager, widgetId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_single)
            views.setOnClickPendingIntent(R.id.widget_header, WidgetIntents.openApp(context))
            views.setOnClickPendingIntent(R.id.widget_add, WidgetIntents.createNote(context))
            setHeaderBg(views)
            val n = WidgetNotes.load(context).firstOrNull()
            if (n == null) {
                views.setViewVisibility(R.id.widget_slot1, View.GONE)
                views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
                views.setTextViewText(R.id.widget_empty, Lang.strCtx(context, "widgetEmpty"))
            } else {
                val locked = WidgetNotes.lockedIds(context).contains(n.id.toString())
                val cat = WidgetNotes.catColor(context, n.folder)
                val fit = fitSlots(mgr, widgetId, 1, 8)
                val slotH = fit.first
                val bodyLines = fit.second
                views.setViewVisibility(R.id.widget_slot1, View.VISIBLE)
                views.setViewVisibility(R.id.widget_empty, View.GONE)
                if (slotH > 0) {
                    try { views.setViewLayoutHeight(R.id.widget_slot1, slotH.toFloat(), TypedValue.COMPLEX_UNIT_DIP) } catch (e: Exception) { }
                }
                setCardBg(views, R.id.widget_card1, WidgetNotes.paleColor(cat), cat)
                val rawTitle = n.title
                views.setTextViewText(R.id.widget_title1, if (locked) (if (rawTitle.isNotBlank()) rawTitle + "  🔒" else "🔒") else rawTitle)
                views.setTextViewText(R.id.widget_star1, "★")
                views.setInt(R.id.widget_star1, "setTextColor", if (n.isFav) Color.BLACK else Color.parseColor("#AAAAAA"))
                views.setTextViewText(R.id.widget_meta1, "🏷️  " + WidgetNotes.folderName(context, n.folder) + "  •  " + n.date)
                views.setTextViewText(R.id.widget_body1, WidgetNotes.preview(context, n))
                views.setInt(R.id.widget_body1, "setMaxLines", bodyLines)
                views.setOnClickPendingIntent(R.id.widget_slot1, WidgetIntents.openNote(context, n.id))
            }
            return views
        }

        // ---- ფონების ხატვა კოდში, ზუსტად HomePage-ის მიხედვით ----
        // სათაურის გრადიენტი: #B9AFFF (მარცხნივ) -> #4A3FA0 (მარჯვნივ), როგორც აპის დიალოგში
        private fun headerBitmap(): Bitmap {
            val w = 240
            val bmp = Bitmap.createBitmap(w, 1, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val paint = Paint()
            paint.shader = LinearGradient(
                0f, 0f, w.toFloat(), 0f,
                Color.parseColor("#B9AFFF"), Color.parseColor("#4A3FA0"),
                Shader.TileMode.CLAMP
            )
            c.drawRect(0f, 0f, w.toFloat(), 1f, paint)
            return bmp
        }

        // ნოუტის ბარათი, HomePage-ის მიხედვით:
        // გარე კუთხეები 22dp, მარცხნივ ზოლი 14dp, ზოლს მხოლოდ მარცხენა კუთხეები აქვს მომრგვალებული.
        // ARGB_8888 აუცილებელია — RGB_565-ს ალფა არ აქვს და მომრგვალებული კუთხეები შავად ჩანდა.
        private fun cardBitmap(pale: Int, strong: Int): Bitmap {
            val w = 320
            val h = 96
            val radius = 22f * w / 340f
            val stripeW = 14f * w / 340f
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.TRANSPARENT)
            val c = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = pale
            c.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, paint)
            paint.color = strong
            val stripe = Path()
            stripe.addRoundRect(
                RectF(0f, 0f, stripeW, h.toFloat()),
                floatArrayOf(radius, radius, 0f, 0f, 0f, 0f, radius, radius),
                Path.Direction.CW
            )
            c.drawPath(stripe, paint)
            return bmp
        }

        private fun setCardBg(views: RemoteViews, cardId: Int, pale: Int, strong: Int) {
            try { views.setImageViewBitmap(cardId, cardBitmap(pale, strong)) } catch (e: Exception) { }
        }

        private fun setHeaderBg(views: RemoteViews) {
            try { views.setImageViewBitmap(R.id.widget_header_bg, headerBitmap()) } catch (e: Exception) { }
        }

        // ფანჯრის სიმაღლის მიხედვით ნოუტების თანაბარი განაწილება:
        // სათაური 60dp + ზემოთ 12dp + ქვემოთ 12dp + სლოტებს შორის 10dp თითო —
        // დარჩენილი სიმაღლე იყოფა ნოუტების რაოდენობაზე, ყველას ერთნაირი სიმაღლე აქვს.
        // ტექსტის ხაზებიც სლოტის სიმაღლის მიხედვით ითვლება, რომ არაფერი ჩამოიჭრას.
        // აბრუნებს: Pair(სლოტის სიმაღლე dp-ში, 0 = არ ვიცით; ტექსტის მაქსიმუმ ხაზები)
        private fun fitSlots(mgr: AppWidgetManager, widgetId: Int, count: Int, maxLines: Int): Pair<Int, Int> {
            if (count <= 0) return Pair(0, maxLines)
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return Pair(0, maxLines)
            val totalH = try {
                mgr.getAppWidgetOptions(widgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            } catch (e: Exception) { 0 }
            if (totalH <= 0) return Pair(0, maxLines)
            val avail = totalH - 60 - 24 - 10 * count
            val per = (avail / count).coerceAtLeast(94)
            val lines = ((per - 74) / 20).coerceIn(1, maxLines)
            return Pair(per, lines)
        }
    }
}
