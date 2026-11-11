package com.schedule.app

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.schedule.app.data.ReminderScheduler

/**
 * 响铃前台服务：仅用于"保活进程"让悬浮窗持久显示。
 * 声音统一由响铃通知负责（系统级播放、单一音源），本服务不再播放音频；
 * 通知按钮「停止」「稍后提醒」走广播，不依赖本服务。
 */
class AlarmRingService : Service() {

    companion object {
        const val ACTION_RING = "com.schedule.app.RING"
        private const val AUTO_STOP_MS = 2 * 60 * 1000L
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ReminderReceiver.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra("id")
        if (id != null) {
            startRing(id, intent.getStringExtra("label") ?: "课程提醒", intent.getStringExtra("note") ?: "")
        }
        return START_NOT_STICKY
    }

    private fun startRing(id: String, label: String, note: String) {
        // 悬浮窗只做弹窗 UI（不放音，避免和通知铃声双音源）
        RingOverlay.show(this, id, label, note, withSound = false)
        startForeground(id.hashCode(), buildNotification(id, label, note))
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ cleanupAndStop() }, AUTO_STOP_MS)
    }

    private fun buildNotification(id: String, label: String, note: String): Notification {
        val text = buildString {
            append(label)
            if (note.isNotBlank()) append("\n备注：$note")
        }
        val contentPi = PendingIntent.getActivity(
            this, id.hashCode() + 4,
            Intent(this, AlarmRingActivity::class.java)
                .putExtra("id", id).putExtra("label", label).putExtra("note", note),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getBroadcast(
            this, id.hashCode() + 7,
            Intent(this, ReminderReceiver::class.java)
                .setAction(ReminderReceiver.ACTION_STOP_RING).putExtra("id", id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePi = PendingIntent.getBroadcast(
            this, id.hashCode() + 8,
            Intent(this, ReminderReceiver::class.java)
                .setAction(ReminderReceiver.ACTION_SNOOZE_RING).putExtra("id", id).putExtra("label", label),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, ReminderReceiver.ALARM_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("课程提醒")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentPi)
            .setFullScreenIntent(contentPi, true)
            .setAutoCancel(true)
            .addAction(0, "停止", stopPi)
            .addAction(0, "稍后提醒（10 分钟）", snoozePi)
            .setOngoing(true)
            .build()
    }

    private fun cleanupAndStop() {
        handler.removeCallbacksAndMessages(null)
        RingOverlay.dismiss(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
