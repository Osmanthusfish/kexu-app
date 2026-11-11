package com.schedule.app

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.text.*
import android.text.style.*
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.schedule.app.data.*
import com.schedule.app.model.*
import com.schedule.app.ui.*
import java.time.ZonedDateTime
import kotlin.math.ceil

internal data class WidgetContent(val rows: List<RemoteViews>, val keys: List<String>, val layout: WidgetContentLayout,
    val fontSp: Float, val semester: Semester?, val week: WidgetWeek?, val occurrences: List<CourseOccurrence>)
internal data class WidgetPreviewContent(val remote: RemoteViews, val rows: List<RemoteViews>)

/** Full-day/full-week collections. No course or period window is ever dropped for a size limit. */
internal object FullWidgetRenderer {
    private val dayViews = intArrayOf(R.id.weekDay1,R.id.weekDay2,R.id.weekDay3,R.id.weekDay4,R.id.weekDay5,R.id.weekDay6,R.id.weekDay7)
    private val labelViews = intArrayOf(R.id.weekLabel1,R.id.weekLabel2,R.id.weekLabel3,R.id.weekLabel4,R.id.weekLabel5,R.id.weekLabel6,R.id.weekLabel7)
    private fun route(o: CourseOccurrence, sem: Semester) = Intent().putExtra("open","course").putExtra("semester",sem.id)
        .putExtra("course",o.base.id).putExtra("date",o.date.toString()).putExtra("occurrence",o.key)

    private fun courseText(o: CourseOccurrence, p: VisualPreferences, weekly: Boolean): CharSequence {
        val detail = listOfNotNull(o.course.room.ifBlank { "地点未填" }.takeIf { p.widgetShowRoom },
            o.course.teacher.ifBlank { "教师未填" }.takeIf { p.widgetShowTeacher })
        val meta = detail.joinToString(if(weekly) "\n" else " · ")
        val period = if(o.course.startPeriod==o.course.endPeriod) "${o.course.startPeriod}节" else "${o.course.startPeriod}–${o.course.endPeriod}节"
        return SpannableString(o.course.name + (if(meta.isEmpty()) "" else "\n$meta") + if(weekly) "\n$period" else "").apply {
            setSpan(StyleSpan(Typeface.BOLD),0,o.course.name.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(RelativeSizeSpan(1.08f),0,o.course.name.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if(length>o.course.name.length) setSpan(RelativeSizeSpan(if(weekly).91f else .85f),o.course.name.length,length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
    private fun textHeight(context: Context,text: CharSequence,widthDp: Float,font: Float): Float {
        val metrics=context.resources.displayMetrics
        val paint=TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,font,metrics);typeface=Typeface.create("sans-serif",Typeface.NORMAL) }
        val builder=StaticLayout.Builder.obtain(text,0,text.length,paint,(widthDp*metrics.density).toInt().coerceAtLeast(1))
            .setIncludePad(false).setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
        if(Build.VERSION.SDK_INT>=28)builder.setUseLineSpacingFromFallbacks(true)
        return ceil(builder.build().height/metrics.density)+2f
    }
    fun content(context: Context,data: AppData,weekly: Boolean,size: SizeF,state: WidgetBrowseState,
                now: ZonedDateTime=ZonedDateTime.now()): WidgetContent {
        val sem=pickDisplaySemester(data.semesters,data.homeSemesterId,now.toLocalDate())
        val week=if(weekly && sem!=null)WidgetSchedule.week(data,sem,state.monday) else null
        val all=if(weekly)week?.occurrences.orEmpty() else sem?.let { OccurrenceRules.onDate(data,it,now.toLocalDate()) }.orEmpty()
        val p=data.visual;val colors=WidgetRenderer.colors(context,data)
        val starts=sem?.periodStarts?.ifEmpty { data.periodStarts } ?: data.periodStarts
        val days=week?.days ?: (1..7).toList()
        val width=if(weekly)(size.width-24)/days.size-8 else size.width-24-44-16
        val texts=all.associate { it.key to courseText(it,p,weekly) }
        val times=all.associate { o ->
            val active=!now.isBefore(OccurrenceRules.startsAt(o,starts,now.zone)) && now.isBefore(OccurrenceRules.endsAt(o,starts,now.zone))
            val over=!OccurrenceRules.endsAt(o,starts,now.zone).isAfter(now)
            o.key to "${PeriodTimes.start(starts,o.course.startPeriod)}\n${PeriodTimes.start(starts,o.course.endPeriod).plusMinutes(45)}"+
                (if(active)"\n正在上课" else if(over)"\n已结束" else "")
        }
        val inlineDays=weekly && size.height<180f
        val dayLabelHeight=if(inlineDays)24f*p.widgetFontScale else 0f
        fun heights(font: Float) = all.associate { o -> o.key to maxOf(if(weekly)30f else 32f,
            textHeight(context,texts.getValue(o.key),width,font)+(if(weekly)10f else 12f),
            if(weekly)0f else textHeight(context,times.getValue(o.key),44f,9f*p.widgetFontScale)+12f) }
        fun needed(h: Map<String,Float>): Float = if(weekly) days.maxOfOrNull { day ->
            dayLabelHeight+WidgetContentLayout.needed(all.filter { it.course.weekday==day }.map { h.getValue(it.key) }) } ?: 0f
            else WidgetContentLayout.needed(all.map { h.getValue(it.key) })
        val minFont=(if(weekly)9f else 11.5f)*p.widgetFontScale
        var font=(if(weekly)10.5f else 13.5f)*p.widgetFontScale
        val available=WidgetContentLayout(size.height,weekly,0f,false).available
        var measured=heights(font)
        while(needed(measured)>available && font>minFont) { font=maxOf(minFont,font-.25f);measured=heights(font) }
        val spec=WidgetContentLayout(size.height,weekly,needed(measured),p.widgetShowTasks && data.todos.any { !it.done })
        val density=context.resources.displayMetrics.density
        if(weekly) {
            val body=RemoteViews(context.packageName,R.layout.widget_week_overview)
            body.setInt(R.id.weekGrid,"setMinimumHeight",(spec.bodyHeight*density).toInt())
            for(day in 1..7) {
                val column=dayViews[day-1]
                body.setViewVisibility(column,if(day in days)View.VISIBLE else View.GONE)
                body.removeAllViews(column)
                if(inlineDays)body.addView(column,RemoteViews(context.packageName,R.layout.widget_week_cell).apply {
                    setTextViewText(R.id.courseCell,weekdayFull(day));setTextColor(R.id.courseCell,colors.muted)
                    setTextViewTextSize(R.id.courseCell,TypedValue.COMPLEX_UNIT_SP,9f*p.widgetFontScale)
                    setInt(R.id.courseCell,"setHeight",ceil(dayLabelHeight*density).toInt());setInt(R.id.courseCell,"setBackgroundResource",0)
                })
                val entries=all.filter { it.course.weekday==day }
                if(entries.isEmpty() && day in days) {
                    body.addView(column,RemoteViews(context.packageName,R.layout.widget_week_cell).apply {
                        setTextViewText(R.id.courseCell,if(sem==null)"—" else "无课");setTextColor(R.id.courseCell,colors.muted)
                        setTextViewTextSize(R.id.courseCell,TypedValue.COMPLEX_UNIT_SP,9f*p.widgetFontScale)
                        setInt(R.id.courseCell,"setHeight",(32*density).toInt());setInt(R.id.courseCell,"setBackgroundResource",0)
                    })
                }
                val heights=WidgetContentLayout.distribute(entries.map { measured.getValue(it.key) },spec.bodyHeight-dayLabelHeight)
                entries.forEachIndexed { index,o ->
                    if(index>0)body.addView(column,RemoteViews(context.packageName,R.layout.widget_week_space).apply {
                        setInt(R.id.courseCell,"setHeight",(4*density).toInt());setTextViewText(R.id.courseCell,"")
                    })
                    // A nested RemoteViews root only receives a collection-level tag. Keep the
                    // clickable TextView below a wrapper so Android installs its own item listener.
                    val cell=RemoteViews(context.packageName,R.layout.widget_week_course)
                    cell.setInt(R.id.courseCell,"setHeight",ceil(heights[index]*density).toInt())
                    cell.setInt(R.id.courseCell,"setBackgroundResource",WidgetPressFeedback.course((o.base.name.hashCode() and 0x7fffffff)%6))
                    cell.setTextViewText(R.id.courseCell,texts.getValue(o.key));cell.setTextViewTextSize(R.id.courseCell,TypedValue.COMPLEX_UNIT_SP,font)
                    cell.setContentDescription(R.id.courseCell,"${weekdayFull(day)} ${o.course.name}，${periodLabel(o.course.startPeriod,o.course.endPeriod)}，${o.course.room}，${o.course.teacher}")
                    cell.setOnClickFillInIntent(R.id.courseCell,route(o,sem!!))
                    body.addView(column,cell)
                }
            }
            return WidgetContent(listOf(body),all.map { it.key },spec,font,sem,week,all)
        }
        val heights=WidgetContentLayout.distribute(all.map { measured.getValue(it.key) },spec.bodyHeight)
        val rows=all.mapIndexed { index,o ->
            val active=!now.isBefore(OccurrenceRules.startsAt(o,starts,now.zone)) && now.isBefore(OccurrenceRules.endsAt(o,starts,now.zone))
            RemoteViews(context.packageName,R.layout.widget_day_item).apply {
                val from=PeriodTimes.start(starts,o.course.startPeriod);val until=PeriodTimes.start(starts,o.course.endPeriod).plusMinutes(45)
                setTextViewText(R.id.todayTime,times.getValue(o.key))
                setTextColor(R.id.todayTime,if(active)colors.accent else colors.muted)
                setTextViewTextSize(R.id.todayTime,TypedValue.COMPLEX_UNIT_SP,9f*p.widgetFontScale)
                setTextViewText(R.id.todayText,texts.getValue(o.key));setTextColor(R.id.todayText,colors.text)
                setTextViewTextSize(R.id.todayText,TypedValue.COMPLEX_UNIT_SP,font)
                setInt(R.id.todayText,"setHeight",ceil((heights[index]-12)*density).toInt())
                setInt(R.id.todayRow,"setBackgroundResource",if(p.widgetStyle==0) WidgetPressFeedback.sky(active)
                    else if(active) WidgetPressFeedback.day() else WidgetPressFeedback.chip())
                setContentDescription(R.id.todayRow,"${o.course.name}，$from 至 $until，${o.course.room}，${o.course.teacher}")
                setOnClickFillInIntent(R.id.todayRow,route(o,sem!!))
            }
        }.ifEmpty { listOf(RemoteViews(context.packageName,R.layout.widget_empty_item).apply {
            val message=if(sem==null)"导入课表\n开始安排你的学期" else "今天没有课\n把时间留给自己"
            setTextViewText(R.id.todayText,message)
            setTextColor(R.id.todayText,colors.text)
            setInt(R.id.todayText,"setHeight",ceil(maxOf(spec.bodyHeight,textHeight(context,message,size.width-40f,13f)+16f)*density).toInt())
            setOnClickFillInIntent(R.id.todayText,Intent().putExtra("open","home"))
        }) }
        return WidgetContent(rows,all.map { it.key },spec,font,sem,null,all)
    }

    fun page(context: Context,data: AppData,id: Int,weekly: Boolean,size: SizeF,state: WidgetBrowseState,
             preview: Boolean=false): WidgetPreviewContent {
        val now=ZonedDateTime.now();val c=content(context,data,weekly,size,state,now);val sem=c.semester;val colors=WidgetRenderer.colors(context,data)
        val views=RemoteViews(context.packageName,if(weekly)R.layout.widget_week_page else R.layout.widget_today_page)
        views.removeAllViews(R.id.widgetBody)
        views.addView(R.id.widgetBody,RemoteViews(context.packageName,R.layout.widget_collection_body))
        views.setViewVisibility(R.id.widgetActions,if(c.layout.footer)View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.widgetCaption,if(c.layout.scrolls) "${c.occurrences.size} 次课 · 上下滑动看全" else "${c.occurrences.size} 次课 · ${if(weekly)"整周一览" else "全天安排"}")
        views.setTextColor(R.id.widgetCaption,colors.muted)
        views.setViewVisibility(R.id.widgetCaption,if(c.layout.captionHeight>0)View.VISIBLE else View.GONE)
        if(weekly)views.setViewVisibility(R.id.widgetDatesContainer,if(c.layout.dateHeight>0)View.VISIBLE else View.GONE)
        val date=if(weekly)state.monday else now.toLocalDate()
        val open=if(sem==null)AppRoutes.pending(context,"home") else AppRoutes.pending(context,if(weekly)"week" else "day",sem.id,date=date.toString())
        for (view in listOf(R.id.tvHeader,R.id.widgetCaption)) views.setInt(view,"setBackgroundResource",WidgetPressFeedback.transparent())
        views.setOnClickPendingIntent(R.id.tvHeader,open);views.setOnClickPendingIntent(R.id.widgetCaption,open)
        views.setTextColor(R.id.tvHeader,colors.text)
        views.setTextViewText(R.id.tvHeader,if(weekly) "第${c.week?.week ?: 1}周" else "今天 · ${date.monthValue}/${date.dayOfMonth}")
        for((view,entry) in mapOf(R.id.widgetOpen to "schedule",R.id.widgetAddTask to "new-todo",R.id.widgetAlarms to "reminders",R.id.widgetSettings to "widgets")) {
            views.setInt(view,"setBackgroundResource",WidgetPressFeedback.chip())
            views.setTextColor(view,colors.text);views.setOnClickPendingIntent(view,AppRoutes.pending(context,entry,sem?.id.orEmpty()))
        }
        if(weekly) {
            views.setViewVisibility(R.id.widgetThisWeek,if(size.width>=280f)View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widgetPrevious,if(size.width>=200f)View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widgetNext,if(size.width>=200f)View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.widgetThisWeek,if(state.monday==Wk.mondayOf(now.toLocalDate()))"本周" else "回本周")
            for((view,entry) in mapOf(R.id.widgetPrevious to "previous-week",R.id.widgetNext to "next-week",R.id.widgetThisWeek to "this-week")) {
                views.setInt(view,"setBackgroundResource",if(view==R.id.widgetThisWeek) WidgetPressFeedback.transparent() else WidgetPressFeedback.chip())
                views.setTextColor(view,colors.accent);views.setOnClickPendingIntent(view,WidgetNavigation.pending(context,id,entry))
            }
            for(day in 1..7) {
                val label=labelViews[day-1];val d=state.monday.plusDays((day-1).toLong());val selected=d==now.toLocalDate()
                views.setViewVisibility(label,if(day in (c.week?.days ?: (1..7).toList()))View.VISIBLE else View.GONE)
                views.setTextViewText(label,"${weekdayChar(day)}\n${d.dayOfMonth}");views.setTextColor(label,if(selected)colors.accent else colors.muted)
                views.setInt(label,"setBackgroundResource",if(selected)WidgetPressFeedback.day() else WidgetPressFeedback.transparent())
                views.setOnClickPendingIntent(label,if(sem==null)open else AppRoutes.pending(context,"day",sem.id,date=d.toString()))
            }
        } else {
            views.setTextViewText(R.id.tvInfo,"${c.occurrences.size}课");views.setTextColor(R.id.tvInfo,colors.muted)
            views.setViewVisibility(R.id.tvTodo,if(c.layout.taskHeight>0)View.VISIBLE else View.GONE)
            views.setInt(R.id.tvTodo,"setBackgroundResource",WidgetPressFeedback.transparent())
            data.todos.filter { !it.done }.minByOrNull { it.dueDate ?: "9999" }?.let { task ->
                views.setTextViewText(R.id.tvTodo,"待办 · ${task.title}");views.setTextColor(R.id.tvTodo,colors.muted)
                views.setOnClickPendingIntent(R.id.tvTodo,AppRoutes.pending(context,"todo",todo=task.id))
            }
        }
        return WidgetPreviewContent(views,c.rows)
    }

    /** Adapter actions must live on the outermost RemoteViews, whose parent is AppWidgetHostView. */
    fun bindCollection(context: Context,views: RemoteViews,rows: List<RemoteViews>,id: Int,weekly: Boolean,size: SizeF,preview: Boolean=false) {
        if(!preview)views.setPendingIntentTemplate(R.id.widgetList,AppRoutes.collectionTemplate(context,id))
        if(Build.VERSION.SDK_INT>=31) {
            val items=RemoteViews.RemoteCollectionItems.Builder().setViewTypeCount(3).setHasStableIds(false)
            rows.forEachIndexed { i,row -> items.addItem(i.toLong(),row) }
            views.setRemoteAdapter(R.id.widgetList,items.build())
        } else if(!preview) views.setRemoteAdapter(R.id.widgetList,WidgetCollectionService.intent(context,id,weekly,size))
    }
}
