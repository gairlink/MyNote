package ge.kakha.mynote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "MyNote"
        val body = intent.getStringExtra("body") ?: ""
        val channelId = "mynote_reminder"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "Reminders", NotificationManager.IMPORTANCE_HIGH)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(ch)
        }
        val openIntent = Intent(context, MainActivity::class.java)
        openIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pi = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        val prefs = context.getSharedPreferences("mynote", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notif", true)) return
        val nid = intent.getIntExtra("noteId", 0)
        NotificationManagerCompat.from(context).notify(if (nid != 0) nid else 1, notif)
        // ყოველთვიური / ყოველწლიური: თავიდან დაჯავშნა, რადგან setRepeating 30/365 დღე არაზუსტია
        val repeat = intent.getStringExtra("repeat") ?: "repeatNone"
        if (repeat == "repeatMonthly" || repeat == "repeatYearly") {
            try {
                val cal = java.util.Calendar.getInstance(); if (repeat == "repeatMonthly") cal.add(java.util.Calendar.MONTH, 1) else cal.add(java.util.Calendar.YEAR, 1)
                val ni = Intent(context, ReminderReceiver::class.java); ni.putExtras(intent); ni.putExtra("noteId", nid)
                val pi = PendingIntent.getBroadcast(context, nid, ni, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                try { am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi) } catch (e: Exception) { am.set(android.app.AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi) }
            } catch (e: Exception) {}
        }
    }
}