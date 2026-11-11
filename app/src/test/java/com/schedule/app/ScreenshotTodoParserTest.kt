package com.schedule.app

import com.schedule.app.data.ScreenshotTodoParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScreenshotTodoParserTest {
    private val today = LocalDate.of(2026, 11, 11)

    @Test fun fullDateAndTimeAreConfident() {
        val draft = ScreenshotTodoParser.parse("截止：2026年11月20日下午3点交实验报告", today)
        assertEquals("实验报告", draft.title)
        assertEquals(LocalDate.of(2026, 11, 20), draft.dueDate)
        assertEquals("15:00", draft.dueTime)
        assertFalse(draft.needsDateConfirmation)
    }

    @Test fun relativeDateIsResolvedAgainstToday() {
        val draft = ScreenshotTodoParser.parse("明天 23:59 前提交作业", today)
        assertEquals(LocalDate.of(2026, 11, 12), draft.dueDate)
        assertEquals("23:59", draft.dueTime)
        assertTrue(draft.needsDateConfirmation)
    }

    @Test fun monthDayWithoutYearNeedsConfirmation() {
        val draft = ScreenshotTodoParser.parse("11月20日交报告", today)
        assertEquals(LocalDate.of(2026, 11, 20), draft.dueDate)
        assertTrue(draft.needsDateConfirmation)
    }

    @Test fun weekdayNeedsConfirmation() {
        val draft = ScreenshotTodoParser.parse("周五交实验", today)
        assertEquals(LocalDate.of(2026, 11, 13), draft.dueDate)
        assertTrue(draft.needsDateConfirmation)
    }

    @Test fun nextWeekdayUsesNextCalendarWeek() {
        val draft = ScreenshotTodoParser.parse("下周五交实验", today)
        assertEquals(LocalDate.of(2026, 11, 20), draft.dueDate)
        assertTrue(draft.needsDateConfirmation)
    }

    @Test fun bareClockTimeIsNotGuessed() {
        val draft = ScreenshotTodoParser.parse("明天3点交报告", today)
        assertEquals(null, draft.dueTime)
        assertEquals("3点", draft.ambiguousTimeText)
    }

    @Test fun missingDateNeverInventsOne() {
        val draft = ScreenshotTodoParser.parse("完成课程作业", today)
        assertEquals(null, draft.dueDate)
        assertTrue(draft.needsDateConfirmation)
    }

    @Test fun conflictingDatesRequireConfirmation() {
        val draft = ScreenshotTodoParser.parse("11月20日开题，11月22日提交", today)
        assertTrue(draft.needsDateConfirmation)
    }
}
