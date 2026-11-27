package ge.kakha.mynote

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

// ვიჯეტის დაჭერები: აპის გახსნა, ახალი ნოუტი, კონკრეტული ნოუტი
object WidgetIntents {
    fun openApp(context: Context): PendingIntent {
        val i = Intent(context, MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        return PendingIntent.getActivity(context, 9001, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    fun createNote(context: Context): PendingIntent {
        val i = Intent(context, MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        i.putExtra("widgetAction", "create")
        return PendingIntent.getActivity(context, 9002, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    fun openNote(context: Context, noteId: Int): PendingIntent {
        val i = Intent(context, MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        i.putExtra("widgetAction", "open")
        i.putExtra("noteId", noteId)
        return PendingIntent.getActivity(context, 10000 + noteId, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
