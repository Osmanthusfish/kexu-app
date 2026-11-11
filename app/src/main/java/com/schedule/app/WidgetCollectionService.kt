package com.schedule.app

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.SizeF
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.schedule.app.data.Store

/** Android 8–11 collection transport; protected by BIND_REMOTEVIEWS and never exported. */
class WidgetCollectionService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext,intent)
    internal class Factory(private val context: Context,intent: Intent) : RemoteViewsFactory {
        private val id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID)
        private val weekly=intent.getBooleanExtra("weekly",false)
        private val size=SizeF(intent.getFloatExtra("width",332f).takeIf { it.isFinite() }?.coerceIn(160f,1200f) ?: 332f,
            intent.getFloatExtra("height",303f).takeIf { it.isFinite() }?.coerceIn(80f,1600f) ?: 303f)
        private var rows=emptyList<RemoteViews>()
        override fun onCreate() { }
        override fun onDataSetChanged() {
            val provider=AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider
            val expected=if(weekly)AgendaWidgetProvider::class.java.name else CourseWidgetProvider::class.java.name
            rows=if(provider?.packageName==context.packageName && provider.className==expected)
                FullWidgetRenderer.content(context,Store(context).load(),weekly,size,WidgetNavigation.read(context,id)).rows else emptyList()
        }
        override fun onDestroy() { rows=emptyList() }
        override fun getCount()=rows.size
        override fun getViewAt(position: Int): RemoteViews?=rows.getOrNull(position)
        override fun getLoadingView()=RemoteViews(context.packageName,R.layout.widget_empty_item).apply { setTextViewText(R.id.todayText,"正在读取课程") }
        override fun getViewTypeCount()=3
        override fun getItemId(position: Int)=position.toLong()
        override fun hasStableIds()=false
    }
    companion object {
        fun intent(context: Context,id: Int,weekly: Boolean,size: SizeF)=Intent(context,WidgetCollectionService::class.java)
            .setData(Uri.parse("scheduleapp://collection/$id/$weekly/${size.width}/${size.height}"))
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra("weekly",weekly)
            .putExtra("width",size.width).putExtra("height",size.height)
    }
}
