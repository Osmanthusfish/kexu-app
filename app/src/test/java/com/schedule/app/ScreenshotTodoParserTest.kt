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
        assertEquals("交实验报告", draft.title)
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
        assertTrue(draft.multipleDateMentions)
    }

    @Test fun mixedRelativeAndFullDatesAreExplicitlyMarked() {
        val draft = ScreenshotTodoParser.parse("今天发布通知，2026年11月20日前提交", today)
        assertTrue(draft.multipleDateMentions)
        assertTrue(draft.needsDateConfirmation)
    }

    @Test fun fullSlashDateIsNotCountedTwice() {
        val draft = ScreenshotTodoParser.parse("2026/11/20 提交报告", today)
        assertFalse(draft.multipleDateMentions)
        assertEquals(LocalDate.of(2026, 11, 20), draft.dueDate)
    }

    @Test fun multipleTimesAreNotGuessed() {
        val draft = ScreenshotTodoParser.parse("消息 09:30\n请在 11月20日 15:00 前提交", today)
        assertEquals(null, draft.dueTime)
        assertEquals("识别到多个时间", draft.ambiguousTimeText)
    }

    @Test fun multipleClockSuggestionsRemainUnselected() {
        val draft = ScreenshotTodoParser.parse("聊天 09:31\n时间：11月18日 下午17: 20\n地点：示例地点", today)
        assertEquals(null, draft.dueTime)
        assertEquals(listOf("09:31", "17:20"), draft.timeCandidates)
        assertEquals("识别到多个时间", draft.ambiguousTimeText)
    }

    @Test fun rangeSuggestionsDoNotGuessADeadline() {
        val draft = ScreenshotTodoParser.parse("11月18日中午 12:50-13:30 提交", today)
        assertEquals(null, draft.dueTime)
        assertEquals(listOf("12:50", "13:30"), draft.timeCandidates)
    }

    @Test fun ambiguousNoonAndMidnightAreNotGuessed() {
        listOf("中午1点", "晚上12点").forEach { expression ->
            val draft = ScreenshotTodoParser.parse("明天${expression}交报告", today)
            assertEquals(null, draft.dueTime)
            assertTrue(draft.ambiguousTimeText != null)
        }
    }

    @Test fun clockWithPeriodUsesThePeriod() {
        assertEquals("15:00", ScreenshotTodoParser.parse("明天下午 3:00提交作业", today).dueTime)
        assertEquals("00:30", ScreenshotTodoParser.parse("明天凌晨12:30提交作业", today).dueTime)
        val invalid = ScreenshotTodoParser.parse("明天下午23:00提交作业", today)
        assertEquals(null, invalid.dueTime)
        assertTrue(invalid.ambiguousTimeText != null)
    }

    @Test fun eventHeadingAndSpacedClockRemainUseful() {
        val draft = ScreenshotTodoParser.parse("【学期学习交流大会】\n时间：11月18日 下午17: 20\n请提前到场签到", today)
        assertTrue(draft.title.contains("交流大会"))
        assertEquals("17:20", draft.dueTime)
        assertTrue(draft.needsDateConfirmation)
    }

    @Test fun differentDeliveryFormatsAreMarkedForReview() {
        val draft = ScreenshotTodoParser.parse("提交研究作业\n周一交打印稿，电子稿另行发送", today)
        assertTrue(draft.title.contains("提交"))
        assertTrue(draft.multipleDeliveryFormats)
    }

    @Test fun chineseTimePreservesSingleDigitMinutesAndRejectsInvalidMinutes() {
        assertEquals("15:05", ScreenshotTodoParser.parse("明天下午3点5分提交作业", today).dueTime)
        val invalid = ScreenshotTodoParser.parse("明天下午3点99分提交作业", today)
        assertEquals(null, invalid.dueTime)
        assertTrue(invalid.ambiguousTimeText != null)
    }

    @Test fun malformedOrApproximateTimeRequiresReviewInsteadOfBeingDroppedOrRounded() {
        listOf("25:00", "15:99", "15:3", "下午3点一刻", "下午3点半左右", "15:00左右").forEach { expression ->
            val draft = ScreenshotTodoParser.parse("明天${expression}提交作业", today)
            assertEquals(expression, null, draft.dueTime)
            assertTrue(expression, draft.ambiguousTimeText != null)
        }
        val mixed = ScreenshotTodoParser.parse("09:30发布通知，明天25:00提交作业", today)
        assertEquals(null, mixed.dueTime)
        assertEquals("识别到多个时间", mixed.ambiguousTimeText)
    }

    @Test fun numberedActionsProduceSeparateLocalDrafts() {
        val text = "事项清单\n1、2026年11月20日15:00提交报告\n2、2026年11月22日18:30填写问卷"
        val draft = ScreenshotTodoParser.parse(text, today)
        assertEquals("提交报告", draft.title)
        assertEquals(LocalDate.of(2026, 11, 20), draft.dueDate)
        assertEquals("15:00", draft.dueTime)
        assertEquals(1, draft.additionalItems.size)
        assertEquals("填写问卷", draft.additionalItems[0].title)
        assertEquals(LocalDate.of(2026, 11, 22), draft.additionalItems[0].dueDate)
        assertEquals("18:30", draft.additionalItems[0].dueTime)
        assertEquals(text, draft.recognizedText)
    }

    @Test fun longLocalListWarnsInsteadOfQuietlyLosingItems() {
        val text = (1..16).joinToString("\n") { "$it、提交第${it}项材料" }
        val draft = ScreenshotTodoParser.parse(text, today)
        assertEquals(14, draft.additionalItems.size)
        assertTrue(draft.mayHaveMoreItems)
    }

    @Test fun chatSenderAndTimestampDoNotBecomeTheTaskWhenThereIsAnActionLine() {
        val draft = ScreenshotTodoParser.parse("示例同学 09:30\n请于11月20日15:00前提交调查问卷", today)
        assertTrue(draft.title.contains("提交调查问卷"))
        assertEquals(null, draft.dueTime)
        assertTrue(draft.ambiguousTimeText != null)
    }
}
