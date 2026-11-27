package ge.kakha.mynote

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class NoteListWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateOne(context, appWidgetManager, id)
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, NoteListWidgetProvider::class.java)
            )
            for (id in ids) {
                updateOne(context, manager, id)
            }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_list)
            val notes = WidgetNotes.load(context)
            val titles = listOf(R.id.widgetItem1, R.id.widgetItem2, R.id.widgetItem3, R.id.widgetItem4)
            for (i in titles.indices) {
                if (i < notes.size) {
                    views.setTextViewText(titles[i], "•  " + notes[i].title)
                } else {
                    views.setTextViewText(titles[i], "")
                }
            }
            val intent = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetListRoot, pending)
            manager.updateAppWidget(widgetId, views)
        }
    }
}