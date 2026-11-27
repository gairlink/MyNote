package ge.kakha.mynote

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("mynote", Context.MODE_PRIVATE)
        for ((k, v) in prefs.all) {
            if (!k.startsWith("reminder_") || k.endsWith("_repeat") || k.endsWith("_title") || k.endsWith("_body")) continue
            val time = (v as? Long) ?: continue
            if (time <= System.currentTimeMillis()) continue
            val id = k.removePrefix("reminder_").toIntOrNull() ?: continue
            val repeat = prefs.getString(k + "_repeat", "repeatNone") ?: "repeatNone"
            val ni = Intent(context, ReminderReceiver::class.java)
            ni.putExtra("title", prefs.getString(k + "_title", "MyNote"))
            ni.putExtra("body", prefs.getString(k + "_body", ""))
            ni.putExtra("noteId", id); ni.putExtra("repeat", repeat)
            val pi = PendingIntent.getBroadcast(context, id, ni, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try {
                if (repeat == "repeatDaily") am.setRepeating(AlarmManager.RTC_WAKEUP, time, AlarmManager.INTERVAL_DAY, pi)
                else if (repeat == "repeatWeekly") am.setRepeating(AlarmManager.RTC_WAKEUP, time, AlarmManager.INTERVAL_DAY * 7, pi)
                else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
            } catch (e: Exception) { try { am.set(AlarmManager.RTC_WAKEUP, time, pi) } catch (e2: Exception) {} }
        }
    }
}