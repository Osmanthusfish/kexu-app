package com.schedule.app

import com.schedule.app.data.*
import com.schedule.app.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.json.JSONObject
import java.io.File
import java.time.*

class WidgetExperienceTest {
    @get:Rule val temp=TemporaryFolder()
    private fun course(id:String,day:Int,start:Int,end:Int)=Course(id,id,"","教师","A101",day,start,end,"1-16")
    private val monday=LocalDate.parse("2026-11-09")
    private fun data(courses:List<Course>)=BackupCodec.emptyData().copy(semesters=listOf(Semester("s","测试",monday.toString(),courses)))
    @Test fun weeklyWidgetContainsAllSevenDaysAndEveryOccurrence() {
        val d=data((1..7).map{course("c$it",it,it,it+1)})
        val plan=WidgetSchedule.week(d,d.semesters.single(),monday.plusDays(3))
        assertEquals(monday,plan.monday);assertEquals(1,plan.week)
        assertEquals((1..7).toList(),plan.days);assertEquals(7,plan.occurrences.size);assertEquals(8,plan.periods)
    }
    @Test fun nextWeekUsesActualParityAndMovesRatherThanRepeatingThisWeek() {
        val c=course("c",1,1,2).copy(weeks="1-16单")
        val d=data(listOf(c));assertEquals(1,WidgetSchedule.week(d,d.semesters.single(),monday).occurrences.size)
        assertTrue(WidgetSchedule.week(d,d.semesters.single(),monday.plusWeeks(1)).occurrences.isEmpty())
        val moved=OccurrenceRules.put(d,CourseChange("change","s","c",monday.toString(),monday.plusWeeks(1).toString(),"move",3,5,"B201"))
        assertEquals("B201",WidgetSchedule.week(moved,moved.semesters.single(),monday.plusWeeks(1)).occurrences.single().course.room)
    }
    @Test fun hiddenWeekendPreferenceCannotHideARealWeekendClass() {
        val d=data(listOf(course("sat",6,1,2))).copy(visual=VisualPreferences(widgetWeekends=false))
        assertEquals((1..6).toList(),WidgetSchedule.week(d,d.semesters.single(),monday).days)
    }
    @Test fun overlapGroupsKeepEachDurationAndReuseColumnsOnlyOutsideOverlap() {
        val groups=WidgetSchedule.groups(listOf(course("long",1,1,5),course("short",1,1,2),course("later",1,3,4),course("separate",1,8,9)),12)
        assertEquals(2,groups.size);assertEquals(1,groups[0].start);assertEquals(5,groups[0].end)
        val slots=groups[0].slots.associateBy{it.course.id}
        assertEquals(2,slots["short"]!!.course.endPeriod);assertEquals(5,slots["long"]!!.course.endPeriod)
        assertNotEquals(slots["long"]!!.column,slots["short"]!!.column)
        assertEquals(slots["short"]!!.column,slots["later"]!!.column)
    }
    @Test fun sameBaseMakeupAndRegularClassBothRemainInWeek() {
        val d=data(listOf(course("c",1,1,2)))
        val withExtra=OccurrenceRules.put(d,CourseChange("extra","s","c",monday.toString(),monday.toString(),"extra",5,6,"B201"))
        val all=WidgetSchedule.week(withExtra,withExtra.semesters.single(),monday).occurrences
        assertEquals(2,all.size);assertEquals(2,all.map{it.key}.distinct().size)
    }
    @Test fun todayWidgetStaysOnTodayWhenAllClassesAreOver() {
        val d=data(listOf(course("today",1,1,2),course("tomorrow",2,1,2)))
        val rows=WidgetSchedule.today(d,d.semesters.single(),monday.atTime(23,0).atZone(ZoneId.of("Asia/Shanghai")))
        assertEquals(listOf("today"),rows.map{it.base.id})
    }
    @Test fun shrinkingAndGrowingWidgetDoesNotLoseLastPageCourses() {
        assertEquals(3,WidgetSchedule.pageCount(7,3));assertEquals(2,WidgetSchedule.boundedPage(99,7,3))
        assertEquals(0,WidgetSchedule.boundedPage(2,0,3));assertEquals(1,WidgetSchedule.pageCount(0,0))
        assertTrue(WidgetSchedule.dailyCapacity(300f,1f,false)>WidgetSchedule.dailyCapacity(130f,1f,false))
    }
    @Test fun appearanceRoundTripsAndV143StillReadsWithDefaults() {
        val d=data(emptyList()).copy(visual=VisualPreferences(2,1.2f,.7f,false,false,true,false,0))
        val text=BackupCodec.encode(BackupPayload(d,null,null));assertEquals(d,BackupCodec.decode(text).data)
        val old=JSONObject(text).apply{put("schemaVersion",2);remove("visual")}
        assertEquals(VisualPreferences(),BackupCodec.decode(old.toString()).data.visual)
    }
    @Test fun invalidAppearanceIsRejectedBeforeStorage() {
        val d=data(emptyList())
        assertThrows(IllegalArgumentException::class.java){BackupCodec.validate(d.copy(visual=VisualPreferences(widgetStyle=9)))}
        assertThrows(IllegalArgumentException::class.java){BackupCodec.validate(d.copy(visual=VisualPreferences(widgetFontScale=Float.NaN)))}
        val old=JSONObject(BackupCodec.encode(BackupPayload(d,null,null))).apply{put("visual","wrong")}
        assertThrows(IllegalArgumentException::class.java){BackupCodec.decode(old.toString())}
    }
    @Test fun newUpgradeSnapshotKeepsTheExactV143StateAndTheOlderSnapshot() {
        val old=JSONObject(BackupCodec.encode(BackupPayload(data(emptyList()),null,null))).apply{put("schemaVersion",2);remove("visual")}.toString(2)
        File(temp.root,"data.json").writeText(old)
        val older=File(temp.root,"recovery/before-v143-upgrade.json").apply{parentFile.mkdirs();writeText("earlier snapshot kept")}
        val store=Store(temp.root);store.save(store.load().copy(visual=VisualPreferences(motion=0)))
        store.save(store.load().copy(theme=2))
        assertEquals(old,store.exportUpgradeSnapshot());assertEquals("earlier snapshot kept",older.readText())
    }
    @Test fun midnightEndingCourseRemainsCurrentAndRefreshesAtStart() {
        val d=data(listOf(course("late",1,1,1))).let { it.copy(semesters=listOf(it.semesters.single().copy(periodStarts=listOf("23:45")))) }
        val sem=d.semesters.single();val zone=ZoneId.of("Asia/Shanghai")
        val before=monday.atTime(23,44).atZone(zone)
        assertEquals(monday.atTime(23,45).atZone(zone).toInstant().toEpochMilli(),WidgetRules.nextRefresh(d,sem,before))
        val current=OccurrenceRules.upcoming(d,sem,monday.atTime(23,50).atZone(zone),1).single()
        assertEquals(monday.plusDays(1).atTime(0,30).atZone(zone),OccurrenceRules.endsAt(current,sem.periodStarts,zone))
        assertEquals(current,OccurrenceRules.upcoming(d,sem,monday.plusDays(1).atTime(0,10).atZone(zone),1).single())
        assertTrue(OccurrenceRules.upcoming(d,sem,monday.plusDays(1).atTime(0,30).atZone(zone),1).single().date.isAfter(monday))
    }

}
