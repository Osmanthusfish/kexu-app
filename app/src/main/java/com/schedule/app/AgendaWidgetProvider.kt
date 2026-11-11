package com.schedule.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

class AgendaWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refresh(context)
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = refresh(context)
    override fun onDisabled(context: Context) = refresh(context)
    override fun onDeleted(context: Context, ids: IntArray) = WidgetNavigation.forget(context,ids)
    private fun refresh(context: Context) {
        val pending=goAsync()
        Thread { try { CourseWidgetProvider.updateAll(context) } finally { pending.finish() } }.start()
    }
}
