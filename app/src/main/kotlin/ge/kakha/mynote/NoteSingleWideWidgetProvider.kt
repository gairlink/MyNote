package ge.kakha.mynote

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews

// ვიჯეტი: ერთი ნოუტი 3×3 — კვადრატი — ჩარჩოს გარეშე, ვიჯეტი = თვითონ ნოუტი.
// ზედა ზოლი (MyNote / +), გრადიენტი, თეთრი გარე ფონი — აღარ არის.
// რჩება მხოლოდ ნოუტის ბარათი: ფერი + ზოლი ზემოთ, სათაური, თარიღი, ტექსტი.
// დაჭერა მთელ ბარათზე ნოუტს ხსნის. ნოუტი არ არის არჩეული — ბარათში ცარიელი ტექსტია.
class NoteSingleWideWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateOne(context, appWidgetManager, id)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        updateOne(context, appWidgetManager, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) WidgetNotes.clearWidget(context, id)
    }

    companion object {
        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, NoteSingleWideWidgetProvider::class.java))
            for (id in ids) updateOne(context, mgr, id)
        }

        private fun updateOne(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val views = try {
                buildViews(context, mgr, widgetId)
            } catch (e: Exception) {
                RemoteViews(context.packageName, R.layout.widget_single_wide).apply {
                    setOnClickPendingIntent(R.id.widget_root, WidgetIntents.openApp(context))
                }
            }
            try { mgr.updateAppWidget(widgetId, views) } catch (e: Exception) { }
        }

        private fun buildViews(context: Context, mgr: AppWidgetManager, widgetId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_single_wide)
            val opts = try { mgr.getAppWidgetOptions(widgetId) } catch (e: Exception) { null }
            val cardW = opts?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
            val cardH = opts?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
            val n = WidgetNotes.notesForWidget(context, widgetId, "wide").firstOrNull()
            if (n == null) {
                // ნოუტი არ არის არჩეული — ბარათში ცარიელი ტექსტი, დაჭერა აპს ხსნის
                setCardBg(views, R.id.widget_card1, Color.parseColor("#EFEAFF"), Color.parseColor("#4A3FA0"), cardW, cardH)
                views.setTextViewText(R.id.widget_title1, "")
                views.setTextViewText(R.id.widget_star1, "")
                views.setTextViewText(R.id.widget_meta1, "")
                views.setTextViewText(R.id.widget_body1, Lang.strCtx(context, "widgetEmpty"))
                views.setOnClickPendingIntent(R.id.widget_root, WidgetIntents.openApp(context))
            } else {
                val locked = WidgetNotes.lockedIds(context).contains(n.id.toString())
                val cat = WidgetNotes.catColor(context, n.folder)
                setCardBg(views, R.id.widget_card1, WidgetNotes.paleColor(cat), cat, cardW, cardH)
                val rawTitle = n.title
                views.setTextViewText(R.id.widget_title1, if (locked) (if (rawTitle.isNotBlank()) rawTitle + "  🔒" else "🔒") else rawTitle)
                views.setTextViewTextSize(R.id.widget_title1, TypedValue.COMPLEX_UNIT_SP, 24f) // პირველის შრიფტი
                views.setTextViewText(R.id.widget_star1, "★")
                views.setInt(R.id.widget_star1, "setTextColor", if (n.isFav) Color.BLACK else Color.parseColor("#AAAAAA"))
                views.setTextViewText(R.id.widget_meta1, WidgetNotes.openedMeta(context, n))
                views.setTextViewText(R.id.widget_body1, WidgetNotes.openedText(context, n))
                views.setTextViewTextSize(R.id.widget_body1, TypedValue.COMPLEX_UNIT_SP, 17f) // პირველის შრიფტი
                views.setInt(R.id.widget_body1, "setMaxLines", 1000) // ხაზები აღარ არის ფიქსირებული — ტექსტი ბარათში დარჩენილ მთელ ადგილს ივსებს
                views.setOnClickPendingIntent(R.id.widget_root, WidgetIntents.openNote(context, n.id))
            }
            return views
        }

        // ---- ფონის ხატვა კოდში, ზუსტად HomePage-ის მიხედვით ----
        // ნოუტის ბარათი გახსნილის დიზაინით:
        // გარე კუთხეები 22dp, ზოლი ზემოთ (ჰორიზონტალური) 14dp — მარცხნივ კი არა,
        // ზოლს მხოლოდ ზედა კუთხეები აქვს მომრგვალებული, როგორც გახსნილ ნოუტში.
        // ARGB_8888 აუცილებელია — RGB_565-ს ალფა არ აქვს და მომრგვალებული კუთხეები შავად ჩანდა.
        // ბიტმაპი ბარათის რეალური პროპორციით იხატება — ზოლი ყოველთვის 14dp რჩება,
        // რადგან სიმაღლეც (cardH) გადაეცემა და არა მარტო სიგანე.
        private fun cardBitmap(pale: Int, strong: Int, cardW: Int, cardH: Int): Bitmap {
            val w = 320
            val scale = w.toFloat() / (if (cardW > 0) cardW else 340)
            val h = if (cardH > 0) (cardH * scale).toInt().coerceIn(96, 1200) else 160
            val radius = 22f * scale
            val stripeH = 14f * scale
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.TRANSPARENT)
            val c = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = pale
            c.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, paint)
            paint.color = strong
            val stripe = Path()
            stripe.addRoundRect(
                RectF(0f, 0f, w.toFloat(), stripeH),
                floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f),
                Path.Direction.CW
            )
            c.drawPath(stripe, paint)
            return bmp
        }

        private fun setCardBg(views: RemoteViews, cardId: Int, pale: Int, strong: Int, cardW: Int, cardH: Int) {
            try { views.setImageViewBitmap(cardId, cardBitmap(pale, strong, cardW, cardH)) } catch (e: Exception) { }
        }
    }
}
