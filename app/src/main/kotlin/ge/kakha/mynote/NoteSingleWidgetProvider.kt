package ge.kakha.mynote

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class NoteSingleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateOne(context, appWidgetManager, id)
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, NoteSingleWidgetProvider::class.java)
            )
            for (id in ids) {
                updateOne(context, manager, id)
            }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_single)
            val notes = WidgetNotes.load(context)
            if (notes.isNotEmpty()) {
                val n = notes[0]
                views.setTextViewText(R.id.widgetSingleTitle, n.title)
                val body = if (n.noteType == "checklist") {
                    n.checklist.joinToString("\n") { (if (it.done) "✓ " else "○ ") + it.text }
                } else {
                    n.text
                }
                views.setTextViewText(R.id.widgetSingleText, body)
            } else {
                views.setTextViewText(R.id.widgetSingleTitle, "MyNote")
                views.setTextViewText(R.id.widgetSingleText, "")
            }
            val intent = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetSingleRoot, pending)
            manager.updateAppWidget(widgetId, views)
        }
    }
}