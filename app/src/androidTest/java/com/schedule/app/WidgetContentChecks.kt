package com.schedule.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.SizeF
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.schedule.app.data.*
import com.schedule.app.model.*
import java.io.File
import java.time.LocalDate
import java.time.ZonedDateTime

/** Synthetic schedules only, measured with Android's actual TextView and collection adapter. */
internal object WidgetContentChecks {
    fun fixture(longNames: Boolean = false): AppData {
        val today=LocalDate.now();val monday=Wk.mondayOf(today)
        val names=listOf("高等数学","大学英语","电路分析","体育")
        val courses=(1..5).flatMap { day ->
            (0 until (if(day==today.dayOfWeek.value)4 else 3)).map { n ->
                Course("full-$day-$n",names[(day+n)%names.size],"",listOf("张老师","李老师","王老师")[n%3],
                    listOf("A201","B302","C105")[n%3],day,n*2+1,n*2+2,"1-16")
            }
        }.toMutableList()
        if(today.dayOfWeek.value>5)repeat(4) { n -> courses+=Course("today-$n",names[n],"","张老师","A201",today.dayOfWeek.value,n*2+1,n*2+2,"1-16") }
        if(longNames) {
            val day=today.dayOfWeek.value
            courses.removeAll { it.weekday==day }
            courses+=Course("long","示例课程戊戊戊戊戊实验（课程设计）","","王老师","示例实验楼-XX3115",day,1,4,"1-16")
            courses+=Course("overlap","电路分析","","李老师","B302",day,1,2,"1-16")
            courses+=Course("afternoon","大学英语","","张老师","A201",day,5,6,"1-16")
            courses+=Course("evening","工程实践","","陈老师","实验楼C205",day,9,10,"1-16")
        }
        val sem=Semester("full-widget","整周课程示例",monday.toString(),courses)
        return BackupCodec.emptyData().copy(semesters=listOf(sem),homeSemesterId=sem.id,
            visual=VisualPreferences(widgetWeekends=false),todos=listOf(Todo("sample-task","提交实验报告",null,null,false)))
    }
    fun run(context: Context, main: (() -> Unit) -> Unit, folder: File, report: (String) -> Unit) {
        val density=context.resources.displayMetrics.density
        val state=WidgetBrowseState(Wk.mondayOf(LocalDate.now()),999,15) // Old saved pages must not hide content.
        val now=ZonedDateTime.now().withHour(23).withMinute(59)
        val normal=fixture();val long=fixture(true)
        main {
            for (weekly in listOf(false, true)) {
                val rendered = (0..2).map { level ->
                    val data = long.copy(visual = long.visual.copy(motion = level))
                    val content = FullWidgetRenderer.content(context, data, weekly, SizeF(332f, 410f), state, now)
                    val root = content.rows.first().apply(context, FrameLayout(context))
                    val target = root.findViewById<View>(if (weekly) R.id.courseCell else R.id.todayRow)
                    check(target != null && target.background is android.graphics.drawable.RippleDrawable) {
                        "Widget press feedback not applied to ${if (weekly) "weekly" else "daily"} course at level $level"
                    }
                    val drawable = target.background.mutate()
                    drawable.setBounds(0, 0, 100, 80)
                    Bitmap.createBitmap(100, 80, Bitmap.Config.ARGB_8888).also { drawable.draw(Canvas(it)) }
                }
                check(rendered[0].sameAs(rendered[1]) && rendered[0].sameAs(rendered[2])) { "Press setting changed widget appearance at rest" }
                rendered.forEach { it.recycle() }
            }
        }
        report("Legacy widget motion values all use gentle native backgrounds on both widgets")
        val dense=normal.copy(semesters=normal.semesters.map { s -> s.copy(courses=(0 until 12).map { n ->
            s.courses.first().copy(id="dense-$n",name="长名称课程 $n 的完整信息",weekday=now.dayOfWeek.value,startPeriod=n+1,endPeriod=n+1)
        }) })
        val cases=listOf(Triple("week-normal",normal,true) to SizeF(332f,460f),
            Triple("today-normal",normal,false) to SizeF(332f,303f),
            Triple("week-long-overlap",long,true) to SizeF(332f,618f),
            Triple("today-long-overlap",long,false) to SizeF(332f,410f),
            Triple("week-dense",dense,true) to SizeF(250f,303f),
            Triple("week-expanded-dense",dense,true) to SizeF(332f,618f),
            Triple("today-dense",dense,false) to SizeF(180f,145f))
        folder.mkdirs()
        for((case,size) in cases) {
            val (name,data,weekly)=case
            val c=FullWidgetRenderer.content(context,data,weekly,size,state,now)
            val expected=if(weekly)OccurrenceRules.inWeek(data,data.semesters.single(),1) else OccurrenceRules.onDate(data,data.semesters.single(),now.toLocalDate())
            check(c.keys.toSet()==expected.map { it.key }.toSet() && c.keys.size==expected.size) { "$name lost courses" }
            check(c.fontSp>=(if(weekly)9f else 11.5f)*data.visual.widgetFontScale)
            if(name.endsWith("normal"))check(!c.layout.scrolls) { "$name should fit: ${c.layout}" }
            if(name.endsWith("dense"))check(c.layout.scrolls)
            main {
                val parent=FrameLayout(context);val actualNames=mutableListOf<String>();var measuredHeight=0
                fun verify(v: View) {
                    if(v is TextView && v.text.isNotEmpty() && v.id in setOf(R.id.courseCell,R.id.todayText,R.id.todayTime)) {
                        check(v.layout.height<=v.height-v.compoundPaddingTop-v.compoundPaddingBottom) { "$name clips ${v.text}" }
                        if(v.id!=R.id.todayTime && v.text.toString().contains('\n'))actualNames+=v.text.toString().substringBefore('\n')
                    }
                    if(v is ViewGroup)for(i in 0 until v.childCount)verify(v.getChildAt(i))
                }
                for(row in c.rows) {
                    val v=row.apply(context,parent)
                    v.measure(View.MeasureSpec.makeMeasureSpec(((size.width-24)*density).toInt(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
                    v.layout(0,0,v.measuredWidth,v.measuredHeight);verify(v);measuredHeight+=v.measuredHeight
                }
                check(actualNames.sorted()==expected.map { it.course.name }.sorted()) { "$name missing native text" }
                if(!c.layout.scrolls)check(measuredHeight+(c.rows.size-1)*4*density<=c.layout.bodyHeight*density+2*c.rows.size) { "$name actual content exceeds viewport" }
                val rv=CourseWidgetProvider.preview(context,data,weekly,size).remote
                val v=rv.apply(context,android.appwidget.AppWidgetHostView(context))
                v.measure(View.MeasureSpec.makeMeasureSpec((size.width*density).toInt(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec((size.height*density).toInt(),View.MeasureSpec.EXACTLY))
                v.layout(0,0,v.measuredWidth,v.measuredHeight)
                if(weekly) {
                    check(v.findViewById<View>(R.id.widgetActions).visibility==View.VISIBLE) { "$name hides weekly actions" }
                    for(id in listOf(R.id.widgetOpen,R.id.widgetAddTask,R.id.widgetAlarms))
                        check(v.findViewById<View>(id).visibility==View.VISIBLE) { "$name hides a weekly action" }
                }
                val bitmap=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(bitmap))
                File(folder,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
                if(name.endsWith("dense")) {
                    val list=v.findViewById<android.widget.ListView>(R.id.widgetList)
                    check(list.canScrollVertically(1)) { "$name cannot reach remaining courses" }
                    list.scrollListBy((500*density).toInt())
                    check(list.firstVisiblePosition>0 || list.getChildAt(0).top<0) { "$name did not actually scroll" }
                    check(list.canScrollVertically(-1))
                }
            }
            report("$name: ${c.keys.size} complete courses, ${c.fontSp}sp, scroll=${c.layout.scrolls}")
        }
        val sem=long.semesters.single();val longClass=sem.courses.first { it.id=="long" };val short=sem.courses.first { it.id=="overlap" }
        check(longClass.startPeriod==short.startPeriod && longClass.endPeriod>short.endPeriod)
        val info=context.packageManager.getServiceInfo(android.content.ComponentName(context,WidgetCollectionService::class.java),0)
        check(!info.exported && info.permission=="android.permission.BIND_REMOTEVIEWS")
        check(!AppRoutes.collectionTemplate(context,-123).isImmutable)
        check(AppRoutes.pending(context,"home").isImmutable)
    }
}
