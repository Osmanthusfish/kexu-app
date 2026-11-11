package com.schedule.app.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.schedule.app.ReminderReceiver
import com.schedule.app.model.Reminder
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** 用 AlarmManager 为课程提醒调度下一次通知。 */
object ReminderScheduler {

    private fun alarm(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** 下一次触发时间（含提前 lead 分钟）。找不到返回 -1。 */
    fun nextMillis(r: Reminder): Long = ReminderRules.nextMillis(r, BackupCodec.emptyData())

    fun schedule(context: Context, r: Reminder, data: com.schedule.app.model.AppData = Store(context).load()) {
        if (!r.enabled) { cancel(context, r.id); return }
        val ms = ReminderRules.nextMillis(r, data)
        if (ms < 0) { cancel(context, r.id); return }
        val am = alarm(context)
        val op = pending(context, r.id, ms)
        // 用系统闹钟级调度（同系统自带闹钟）：精确、勿扰/省电模式下也按时响、可在状态栏显示。
        // 若设备未授权「精确闹钟」会抛 SecurityException —— 必须捕获并降级为普通允许空闲闹钟，
        // 否则启动时重排一次就闪退。
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                val show = launch?.let {
                    PendingIntent.getActivity(
                        context, r.id.hashCode() + 1, it,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
                am.setAlarmClock(AlarmManager.AlarmClockInfo(ms, show), op)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, op)
            }
        } catch (t: Throwable) {
            // 降级：普通允许空闲响铃的闹钟（可能被系统稍微延迟，但绝不闪退）
            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, op)
            } catch (t2: Throwable) { /* 尽力而为 */ }
        }
    }

    fun cancel(context: Context, id: String) {
        alarm(context).cancel(pending(context, id))
        alarm(context).cancel(snoozePending(context, id))
    }

    /** 「稍后提醒」：10 分钟后响一次（独立一次性闹钟，不影响原周闹钟）。 */
    fun snooze(context: Context, id: String, label: String) {
        val am = alarm(context)
        val ms = System.currentTimeMillis() + SNOOZE_MINUTES * 60 * 1000L
        val pi = snoozePending(context, id, label)
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                val show = launch?.let {
                    PendingIntent.getActivity(
                        context, id.hashCode() + 3, it,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
                am.setAlarmClock(AlarmManager.AlarmClockInfo(ms, show), pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
            }
        } catch (t: Throwable) {
            try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi) } catch (t2: Throwable) { }
        }
    }

    /** 正常闹钟的 PendingIntent。 */
    private fun pending(context: Context, id: String, fireMillis: Long = -1): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REMINDER)
            .putExtra("id", id)
        if (fireMillis >= 0) intent.putExtra("fireMillis", fireMillis)
        return PendingIntent.getBroadcast(
            context, id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 「稍后提醒」的一次性 PendingIntent（与正常闹钟用不同 requestCode，互不覆盖）。 */
    private fun snoozePending(context: Context, id: String, label: String? = null): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_SNOOZE)
            .putExtra("id", id)
        if (label != null) intent.putExtra("label", label)
        return PendingIntent.getBroadcast(
            context, id.hashCode() + 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    const val ACTION_REMINDER = "com.schedule.app.REMINDER"
    const val ACTION_SNOOZE = "com.schedule.app.SNOOZE"
    const val SNOOZE_MINUTES = 10
}
