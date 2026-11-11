package com.schedule.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.*
import android.net.Uri
import com.schedule.app.data.Wk
import java.time.LocalDate

data class WidgetBrowseState(val monday: LocalDate, val page: Int, val periodPage: Int = 0)

object WidgetNavigation {
    private fun prefs(context: Context) = context.getSharedPreferences("widget-browsing",Context.MODE_PRIVATE)
    fun read(context: Context,id: Int,today: LocalDate = LocalDate.now()): WidgetBrowseState {
        val p=prefs(context)
        val anchor=if(p.getBoolean("follow-$id",true)) Wk.mondayOf(today)
            else runCatching { LocalDate.parse(p.getString("week-$id",null)) }.getOrNull() ?: Wk.mondayOf(today)
        val validPage=if(p.getString("day-$id",null)==today.toString()) p.getInt("page-$id",0) else 0
        return WidgetBrowseState(anchor,validPage,if(anchor.toString()==p.getString("week-$id",null)) p.getInt("period-$id",0) else 0)
    }
    fun move(context: Context,id: Int,action: String, targetPage: Int? = null): WidgetBrowseState {
        val old=read(context,id); val today=LocalDate.now()
        val moved=when(action) {
            "previous-week" -> old.copy(monday=old.monday.minusWeeks(1).coerceAtLeast(Wk.mondayOf(today).minusWeeks(52)))
            "next-week" -> old.copy(monday=old.monday.plusWeeks(1).coerceAtMost(Wk.mondayOf(today).plusWeeks(104)))
            "this-week" -> old.copy(monday=Wk.mondayOf(today))
            "previous-page" -> old.copy(page=(targetPage ?: (old.page-1)).coerceIn(0,10000))
            "next-page" -> old.copy(page=(targetPage ?: (old.page+1)).coerceIn(0,10000))
            "period-page" -> old.copy(periodPage=(targetPage ?: 0).coerceIn(0,15))
            else -> old
        }
        val next=if(action.endsWith("week")) moved.copy(periodPage=0) else moved
        prefs(context).edit().putString("week-$id",next.monday.toString()).putBoolean("follow-$id",next.monday==Wk.mondayOf(today))
            .putString("day-$id",today.toString()).putInt("page-$id",next.page).putInt("period-$id",next.periodPage).apply()
        return old
    }
    fun forget(context: Context,ids: IntArray) { val e=prefs(context).edit(); ids.forEach { e.remove("week-$it").remove("follow-$it").remove("day-$it").remove("page-$it").remove("period-$it") };e.apply() }
    fun pending(context: Context,id: Int,command: String, targetPage: Int = -1): PendingIntent = PendingIntent.getBroadcast(context,0,
        Intent(context,WidgetNavigationReceiver::class.java).setAction(ACTION)
            .setData(Uri.parse("scheduleapp://widget/$id/$command/$targetPage")).putExtra("widgetId",id).putExtra("command",command).putExtra("targetPage",targetPage),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    const val ACTION="com.schedule.app.WIDGET_NAVIGATION"
}

class WidgetNavigationReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        if(intent.action!=WidgetNavigation.ACTION) return
        val id=intent.getIntExtra("widgetId",AppWidgetManager.INVALID_APPWIDGET_ID)
        val info=AppWidgetManager.getInstance(context).getAppWidgetInfo(id) ?: return
        if(info.provider.packageName!=context.packageName || info.provider.className !in setOf(CourseWidgetProvider::class.java.name,AgendaWidgetProvider::class.java.name)) return
        val command=intent.getStringExtra("command") ?: return
        if(command !in setOf("previous-week","next-week","this-week","previous-page","next-page","period-page")) return
        val pending=goAsync()
        Thread { try { synchronized(WidgetNavigation) {
            val before=WidgetNavigation.move(context,id,command,intent.getIntExtra("targetPage",-1).takeIf{it>=0})
            CourseWidgetProvider.updateAll(context,id,before)
        } } finally { pending.finish() } }.start()
    }
}
