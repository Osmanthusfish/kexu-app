package com.schedule.app

import android.app.*
import android.content.*
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.schedule.app.data.*
import com.schedule.app.model.Todo

/** Homework uses a normal notification, independent of the existing full-screen course alarm. */
class TodoReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val id = intent.getStringExtra("id") ?: return
        val pending = goAsync()
        Thread {
            try {
                val todo = Store(context).load().todos.find { it.id == id && !it.done && it.remindMinutesBefore != null } ?: return@Thread
                val deadline = TodoRules.deadline(todo) ?: return@Thread
                val fire = deadline.minusMinutes(todo.remindMinutesBefore!!.toLong()).toInstant().toEpochMilli()
                if (fire > System.currentTimeMillis() + 1000) { schedule(context, todo); return@Thread }
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(NotificationChannel(CHANNEL, "作业与待办提醒", NotificationManager.IMPORTANCE_DEFAULT))
                val notification = NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(todo.title)
                    .setContentText("截止 ${todo.dueDate} ${todo.dueTime ?: "23:59"}")
                    .setContentIntent(AppRoutes.pending(context, "todo", todo = id)).setAutoCancel(true).build()
                try { manager.notify("todo:$id", 0, notification) } catch (_: SecurityException) { }
            } finally { pending.finish() }
        }.start()
    }
    companion object {
        private const val ACTION = "com.schedule.app.TODO_REMINDER"
        private const val CHANNEL = "homework_reminders"
        private fun operation(context: Context, id: String): PendingIntent = PendingIntent.getBroadcast(context, 0,
            Intent(context, TodoReminderReceiver::class.java).setAction(ACTION).setData(Uri.Builder().scheme("scheduleapp").authority("todo-alarm").appendPath(id).build()).putExtra("id", id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun cancel(context: Context, id: String) {
            context.getSystemService(AlarmManager::class.java).cancel(operation(context, id))
            context.getSystemService(NotificationManager::class.java).cancel("todo:$id", 0)
        }
        fun schedule(context: Context, todo: Todo) {
            val time = TodoRules.nextMillis(todo)
            cancel(context, todo.id)
            if (time < 0) return
            val manager = context.getSystemService(AlarmManager::class.java)
            try { manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, operation(context, todo.id)) }
            catch (_: SecurityException) { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, operation(context, todo.id)) }
        }
    }
}
