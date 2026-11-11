package com.schedule.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.schedule.app.data.ReminderScheduler
import com.schedule.app.data.Store

/** 开机后重新调度所有课程提醒（没有它，重启一次闹钟就全丢了）。 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_DATE_CHANGED, android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) return
        val pending = goAsync()
        Thread {
            try {
                ReminderReceiver.ensureChannel(context)
                val data = Store(context).load()
                data.reminders.forEach { ReminderScheduler.schedule(context, it, data) }
                data.todos.forEach { TodoReminderReceiver.schedule(context, it) }
                CourseWidgetProvider.updateAll(context)
            } catch (e: Exception) { android.util.Log.w("ScheduleApp", "Refresh after system event failed", e) }
            finally { pending.finish() }
        }.start()
    }
}
