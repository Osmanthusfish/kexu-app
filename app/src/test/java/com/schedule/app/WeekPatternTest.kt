package com.schedule.app

import com.schedule.app.data.Wk
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Executors

class WeekPatternTest {
    // Independent copy of the released rule, guarding imported/legacy string semantics.
    private fun previous(weeks: String, w: Int): Boolean {
        if (weeks.isBlank()) return true
        if (w < 1) return false
        if (weeks.contains("单") && w % 2 == 0) return false
        if (weeks.contains("双") && w % 2 != 0) return false
        if (!weeks.any { it.isDigit() }) return weeks.contains("单") || weeks.contains("双")
        return weeks.replace('，', ',').replace('～', '~').split(",").any { piece ->
            val m = Regex("(\\d+)(?:[-~](\\d+))?").find(piece.trim()) ?: return@any false
            val a = m.groupValues[1].toIntOrNull() ?: return@any false
            val b = if (m.groupValues[2].isEmpty()) a else m.groupValues[2].toIntOrNull() ?: return@any false
            w in a..b
        }
    }
    private val patterns = listOf("", " ", "单", "双", "单双", "每周", "未知", "1-16", "1-16单", "2~18双", "1-4,6-9", "1，3，5～7", "第1-3周，5周", "16-1", "0-3", "519-520", "3,3,5-8", "999999999999-10", "2-999999999999", "１-３", "-1", "1 – 8")
    @Test fun optimizedRulesMatchReleasedBehaviorAcrossBoundaryWeeksAndFormats() {
        for (text in patterns) for (week in -2..522) assertEquals("$text @ $week", previous(text, week), Wk.weeksContains(text, week))
    }
    @Test fun batchSearchIsIdenticalAndEmptyRangesStayEmpty() {
        for (text in patterns) {
            assertEquals((-2..522).filter { previous(text, it) }, Wk.weeksIn(text, -2..522))
            assertTrue(Wk.weeksIn(text, 521..520).isEmpty())
        }
    }
    @Test fun evictionAndConcurrentUiAlarmWidgetReadsKeepTheirOwnRules() {
        val pool = Executors.newFixedThreadPool(4)
        try {
            val jobs = (1..200).map { n -> pool.submit<Boolean> {
                val text = "$n-${n + 2}单"
                Wk.weeksIn(text, 1..520) == (1..520).filter { it % 2 == 1 && it in n..n + 2 }
            } }
            jobs.forEach { assertTrue(it.get()) }
            for (text in patterns) assertEquals((1..520).filter { previous(text, it) }, Wk.weeksIn(text, 1..520))
        } finally { pool.shutdownNow() }
    }
}
