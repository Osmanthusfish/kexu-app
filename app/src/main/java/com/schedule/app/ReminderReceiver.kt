package com.schedule.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.schedule.app.data.Store
import com.schedule.app.data.ReminderScheduler
import com.schedule.app.model.Reminder

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 通知按钮的动作：独立于提醒存在与否，直接关停 / 稍后提醒
        when (intent.action) {
            ACTION_STOP_RING -> {
                val rid = intent.getStringExtra("id") ?: return
                stopAll(context, rid)
                return
            }
            ACTION_SNOOZE_RING -> {
                val rid = intent.getStringExtra("id") ?: return
                val label = intent.getStringExtra("label") ?: "课程提醒"
                ReminderScheduler.snooze(context, rid, label)
                stopAll(context, rid)
                return
            }
        }

        val snooze = intent.action == ReminderScheduler.ACTION_SNOOZE
        val id = intent.getStringExtra("id") ?: return
        val data = Store(context).load()
        var rem = data.reminders.firstOrNull { it.id == id && it.enabled }
        // 稍后提醒时提醒可能已被删/停用：用 extra 里的 label 再响最后一次
        if (rem == null && snooze) {
            val label = intent.getStringExtra("label") ?: return
            rem = Reminder(id, label, 1, 0, 0, 0, "", true)
        }
        rem ?: return
        val scheduled = intent.getLongExtra("fireMillis", -1)
        if (!snooze && scheduled >= 0) {
            val justBefore = java.time.Instant.ofEpochMilli(scheduled - 1).atZone(java.time.ZoneId.systemDefault())
            if (com.schedule.app.data.ReminderRules.nextMillis(rem, data, justBefore) != scheduled) {
                ReminderScheduler.schedule(context, rem, data)
                return
            }
            rem = rem.copy(note = com.schedule.app.data.ReminderRules.noteAtTrigger(rem, data, scheduled))
        }
        ensureChannel(context)

        // 第一层（声音兜底，任何品牌都可靠）：系统级响铃通知。
        // 通知带「停止」「稍后提醒」按钮，走广播，不依赖任何服务是否活着。
        if (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(
                id.hashCode(), loudNotification(context, id, rem.label, rem.note)
            )
        }

        // 第二层（弹窗）：悬浮窗 + 前台服务保活进程，让弹窗持久显示。
        // 前台服务被系统拦截/失败也不影响上面的响铃通知。
        if (android.provider.Settings.canDrawOverlays(context)) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AlarmRingService::class.java)
                        .setAction(AlarmRingService.ACTION_RING)
                        .putExtra("id", id)
                        .putExtra("label", rem.label)
                        .putExtra("note", rem.note)
                )
            } catch (e: Exception) { }
        }

        if (!snooze) ReminderScheduler.schedule(context, rem, data)
    }

    /** 关停所有响铃：通知 + 悬浮窗 + 前台服务。 */
    private fun stopAll(context: Context, id: String) {
        try { NotificationManagerCompat.from(context).cancel(id.hashCode()) } catch (e: Exception) { }
        RingOverlay.dismiss(context)
        try {
            context.stopService(Intent(context, AlarmRingService::class.java))
        } catch (e: Exception) { }
    }

    private fun loudNotification(context: Context, id: String, label: String, note: String): Notification {
        val text = buildString {
            append(label)
            if (note.isNotBlank()) append("\n备注：$note")
        }
        // 点通知/锁屏全屏 → 响铃控制页
        val ringIntent = Intent(context, AlarmRingActivity::class.java)
            .putExtra("id", id)
            .putExtra("label", label)
            .putExtra("note", note)
        val fullScreen = PendingIntent.getActivity(
            context, id.hashCode() + 4, ringIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getBroadcast(
            context, id.hashCode() + 7,
            Intent(context, ReminderReceiver::class.java)
                .setAction(ACTION_STOP_RING).putExtra("id", id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePi = PendingIntent.getBroadcast(
            context, id.hashCode() + 8,
            Intent(context, ReminderReceiver::class.java)
                .setAction(ACTION_SNOOZE_RING).putExtra("id", id).putExtra("label", label),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, ALARM_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("课程提醒")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .setAutoCancel(true)
            .addAction(0, "停止", stopPi)
            .addAction(0, "稍后提醒（10 分钟）", snoozePi)
            .build()
    }

    companion object {
        /** 普通提醒频道（旧数据/兼容）。 */
        const val CHANNEL = "course_reminder"

        /** 响铃频道：高音 + 震动 + 系统闹钟铃声。 */
        const val ALARM_CHANNEL = "course_alarm"

        /** 通知上的「停止」/「稍后提醒」按钮（广播直达，不依赖服务）。 */
        const val ACTION_STOP_RING = "com.schedule.app.RING_STOP"
        const val ACTION_SNOOZE_RING = "com.schedule.app.RING_SNOOZE"

        fun ensureChannel(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "课程提醒", NotificationManager.IMPORTANCE_HIGH)
            )
            val alarm = NotificationChannel(ALARM_CHANNEL, "课程响铃", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "课程到点响铃，弹全屏提醒"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 400, 800)
                // 用系统闹钟铃声 + 闹钟音频属性，保证在静音/勿扰下也按闹钟音量响
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                uri?.let {
                    setSound(
                        it,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
            }
            nm.createNotificationChannel(alarm)
        }
    }
}
