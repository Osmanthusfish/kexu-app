package com.schedule.app

import android.app.*
import android.content.*
import com.schedule.app.data.*
import com.schedule.app.ui.pickDisplaySemester

class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val pending = goAsync()
        Thread { try { CourseWidgetProvider.updateAll(context) } finally { pending.finish() } }.start()
    }
    companion object {
        private const val ACTION = "com.schedule.app.WIDGET_REFRESH"
        fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(context, 0,
            Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun cancel(context: Context) = context.getSystemService(AlarmManager::class.java).cancel(pending(context))
        fun schedule(context: Context, data: com.schedule.app.model.AppData) {
            val now = java.time.ZonedDateTime.now()
            val sem = pickDisplaySemester(data.semesters, data.homeSemesterId, now.toLocalDate())
            val time = WidgetRules.nextRefresh(data, sem, now)
            val manager = context.getSystemService(AlarmManager::class.java)
            try { manager.setExactAndAllowWhileIdle(AlarmManager.RTC, time, pending(context)) }
            catch (_: SecurityException) { manager.setAndAllowWhileIdle(AlarmManager.RTC, time, pending(context)) }
        }
    }
}
