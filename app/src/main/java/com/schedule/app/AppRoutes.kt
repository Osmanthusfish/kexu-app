package com.schedule.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Explicit identities: extras alone cannot distinguish PendingIntents. */
object AppRoutes {
    /** Collections need fill-in extras, but the destination stays fixed to this Activity. */
    fun collectionTemplate(context: Context, widgetId: Int): PendingIntent = PendingIntent.getActivity(context, 0,
        Intent(context, MainActivity::class.java).setData(Uri.parse("scheduleapp://collection-open/$widgetId"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or (if (android.os.Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0))

    fun pending(context: Context, entry: String, semester: String = "", course: String = "", date: String = "", todo: String = "", occurrence: String = ""): PendingIntent {
        val uri = Uri.Builder().scheme("scheduleapp").authority("open").appendPath(entry)
            .appendQueryParameter("semester", semester).appendQueryParameter("course", course)
            .appendQueryParameter("date", date).appendQueryParameter("todo", todo).appendQueryParameter("occurrence", occurrence).build()
        val intent = Intent(context, MainActivity::class.java).setData(uri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("open", entry).putExtra("semester", semester).putExtra("course", course).putExtra("date", date).putExtra("todo", todo).putExtra("occurrence", occurrence)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
