package com.schedule.app

import androidx.compose.ui.text.font.FontWeight
import com.schedule.app.data.Parser
import com.schedule.app.data.Wk
import com.schedule.app.model.Course
import com.schedule.app.model.Semester
import com.schedule.app.model.Todo
import com.schedule.app.ui.defaultPeriodStart
import com.schedule.app.ui.pickCurrentSemester
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** 核心逻辑回归测试：解析器 / 周次 / 当前学期选择。 */
class LogicTest {

    private fun sem(id: String, start: LocalDate, weeks: String = "1-16", endPeriod: Int = 12) =
        Semester(
            id, "学期$id", start.toString(), listOf(
                Course(id + "-c", "测试课", "", "老师", "A101", 1, 1, endPeriod, weeks)
            )
        )

    // ---------- 上课时间解析 ----------

    @Test
    fun parseTime_listFormat() {
        val t = Parser.parseTime("1-4周星期一10-12节")
        assertEquals(1, t.weekday)
        assertEquals(10, t.startPeriod)
        assertEquals(12, t.endPeriod)
        assertEquals("1-4", t.weeks)
    }

    @Test
    fun parseTime_weeksOnly() {
        val t = Parser.parseTime("17-18周")
        assertEquals(0, t.weekday)
        assertEquals(0, t.startPeriod)
        assertEquals("17-18", t.weeks)
    }

    @Test
    fun parseTime_oddWeeks() {
        val t = Parser.parseTime("1-16周(单)星期三1-2节")
        assertEquals(3, t.weekday)
        assertEquals(1, t.startPeriod)
        assertEquals("1-16单", t.weeks)
        assertTrue(Wk.weeksContains(t.weeks, 3))
        assertFalse(Wk.weeksContains(t.weeks, 4))
    }

    @Test
    fun parseTime_multiPeriodRanges() {
        val t = Parser.parseTime("1-16周星期五10-12节,13-14节")
        assertEquals(5, t.weekday)
        assertEquals(10, t.startPeriod)
        assertEquals(14, t.endPeriod)
        assertEquals("1-16", t.weeks)
    }

    @Test
    fun parseTime_noWeeks() {
        val t = Parser.parseTime("星期日8-9节")
        assertEquals(7, t.weekday)
        assertEquals("", t.weeks)
        assertTrue(Wk.weeksContains(t.weeks, 10))
    }

    @Test
    fun parseTime_clockTimes() {
        // AI 漏换算钟点时 App 自己按默认节次表换算：8:30-10:10 → 1-2节
        val t = Parser.parseTime("3-17周(单)星期一8:30-10:10")
        assertEquals(1, t.weekday)
        assertEquals(1, t.startPeriod)
        assertEquals(2, t.endPeriod)
        assertEquals("3-17单", t.weeks)
    }

    @Test
    fun parseTime_clockTimesEvening() {
        // 19:30-21:10 → 9-11节（19:00 第9节起，21:00 前最后一节为第11节）
        val t = Parser.parseTime("5-8,11-15周星期一19:30-21:10")
        assertEquals(1, t.weekday)
        assertEquals(9, t.startPeriod)
        assertEquals(11, t.endPeriod)
        assertEquals("5-8,11-15", t.weeks)
    }

    // ---------- 周次匹配 ----------

    @Test
    fun weeksContains_ranges() {
        assertTrue(Wk.weeksContains("1-4,6-9", 6))
        assertFalse(Wk.weeksContains("1-4,6-9", 5))
        assertTrue(Wk.weeksContains("1-16双", 8))
        assertFalse(Wk.weeksContains("1-16双", 7))
        assertTrue(Wk.weeksContains("", 15))
    }

    // ---------- 周一对齐 ----------

    @Test
    fun mondayOf_alignsToMonday() {
        val t = LocalDate.parse("2026-09-01") // 星期二
        assertEquals(2, Wk.dayOfWeek(t))
        assertEquals("2026-08-31", Wk.mondayOf(t).toString())
        assertEquals("2026-09-07", Wk.mondayOf(LocalDate.parse("2026-09-07")).toString())
    }

    // ---------- 节次时间 ----------

    @Test
    fun periodStartTime_table() {
        assertEquals(8, defaultPeriodStart(1).hour)
        assertEquals(0, defaultPeriodStart(1).minute)
        assertEquals(14, defaultPeriodStart(5).hour)
        assertEquals(19, defaultPeriodStart(9).hour)
    }

    // ---------- 当前学期选择 ----------

    @Test
    fun pickCurrent_activePrefersNewest() {
        val today = LocalDate.now()
        val old = sem("old", today.minusDays(28))   // 第5周，仍在范围
        val new = sem("new", today.minusDays(7))    // 第2周，仍在范围
        assertEquals("new", pickCurrentSemester(listOf(old, new), today)!!.id)
    }

    @Test
    fun pickCurrent_vacationPicksUpcoming() {
        val today = LocalDate.now()
        val past = sem("past", today.minusDays(200))
        val future = sem("future", today.plusDays(10))
        assertEquals("future", pickCurrentSemester(listOf(past, future), today)!!.id)
    }

    @Test
    fun pickCurrent_allEndedPicksLatest() {
        val today = LocalDate.now()
        val a = sem("a", today.minusDays(400))
        val b = sem("b", today.minusDays(300))
        assertEquals("b", pickCurrentSemester(listOf(a, b), today)!!.id)
    }

    @Test
    fun pickCurrent_emptyReturnsNull() {
        assertEquals(null, pickCurrentSemester(emptyList(), LocalDate.now()))
    }

    // ---------- 分词断行 + 课程块字号自适应（编号不拆、一字不丢、测量次数有限） ----------

    /** 与实现同口径的假宽度测量（CJK=1.0em、其它=0.62em × fs × scale）：正常场景验证必然通过。 */
    private fun fakeWidthConsistent(scale: Float): (String, Float, FontWeight) -> Float {
        return { text, fs, _ ->
            var w = 0f
            for (ch in text) {
                val c = ch.code
                val wide = c in 0x1100..0x11FF || c in 0x2E80..0xA4CF || c in 0xAC00..0xD7A3 ||
                    c in 0xF900..0xFAFF || c in 0xFE30..0xFE4F || c in 0xFF00..0xFFEF ||
                    c in 0x3000..0x303F || c in 0x20000..0x2A6DF
                w += (if (wide) 1f else 0.62f) * fs * scale
            }
            w
        }
    }

    /** 假宽度：所有字符按 1.0em（比模型宽，模拟"模型低估字宽"→ 触发保底重排路径）。 */
    private fun fakeWidthAllEm(scale: Float): (String, Float, FontWeight) -> Float =
        { text, fs, _ -> text.length * fs * scale }

    @Test
    fun wrapByTokens_numberNeverSplit() {
        // 窄宽：-1430 必须整串保留在行内，不得拆成 "-143" + "0"
        val lines = com.schedule.app.ui.wrapByTokens("示例楼四栋-1430", 10f, 33f)
        assertEquals("示例楼四栋-1430", lines.joinToString(""))
        assertTrue(lines.last().contains("1430"))
        assertTrue(lines.any { it.startsWith("-") })
    }

    @Test
    fun wrapByTokens_wideFitsOneLine() {
        assertEquals(listOf("示例楼四栋-1430"), com.schedule.app.ui.wrapByTokens("示例楼四栋-1430", 10f, 200f))
        assertEquals(listOf("示例课程己"), com.schedule.app.ui.wrapByTokens("示例课程己", 10f, 50f))
    }

    @Test
    fun wrapByTokens_joinedEqualsOriginal() {
        for (s in listOf("示例课程丙丙丙丙丙", "示例课程乙乙乙乙乙乙", "示例课程甲甲甲甲甲甲甲", "A102", "示例楼四栋-14301")) {
            val lines = com.schedule.app.ui.wrapByTokens(s, 9f, 31f)
            assertTrue(lines.isNotEmpty())
            assertEquals(s, lines.joinToString(""))
        }
    }

    private fun assertComplete(
        r: com.schedule.app.ui.CourseFit, name: String, room: String, teacher: String,
        wPx: Float, mw: (String, Float, FontWeight) -> Float
    ) {
        assertEquals(name, r.nameLines.joinToString(""))
        assertEquals(room, r.roomLines.joinToString(""))
        if (r.showTeacher) assertEquals(teacher, r.teacherLines.joinToString(""))
        else assertEquals("", r.teacherLines.joinToString(""))
        // 每行的（假）宽度都必须 ≤ 格宽，否则渲染会横向截字
        val nameFs = com.schedule.app.ui.courseNameFontSize(r.fs)
        for (l in r.nameLines) assertTrue(mw(l, nameFs, FontWeight.SemiBold) <= wPx)
        for (l in r.roomLines) assertTrue(mw(l, r.fs, FontWeight.Medium) <= wPx)
        for (l in r.teacherLines) assertTrue(mw(l, r.fs, FontWeight.Normal) <= wPx)
    }

    @Test
    fun fitFontMeasured_normalScaleKeepsAllThree() {
        val name = "高等数学A"; val room = "示例楼四栋-14402"; val teacher = "甲老师"
        val r = com.schedule.app.ui.fitCourseFontMeasured(
            300f, 500f, name, room, teacher, measureWidth = fakeWidthConsistent(1f)
        )
        assertTrue(r.showTeacher)
        assertComplete(r, name, room, teacher, 300f, fakeWidthConsistent(1f))
    }

    @Test
    fun fitFontMeasured_largeFontShrinksAndStillFits() {
        val name = "高等数学A"; val room = "示例楼四栋-14402"; val teacher = "甲老师"
        val r1 = com.schedule.app.ui.fitCourseFontMeasured(110f, 160f, name, room, teacher, measureWidth = fakeWidthConsistent(1f))
        val r2 = com.schedule.app.ui.fitCourseFontMeasured(
            110f, 160f, name, room, teacher, fontScale = 2f, measureWidth = fakeWidthConsistent(2f)
        )
        assertTrue(r2.fs < r1.fs) // 字体放大 → 字号必须缩小，且内容仍完整
        assertComplete(r2, name, room, if (r2.showTeacher) teacher else "", 110f, fakeWidthConsistent(2f))
    }

    @Test
    fun fitFontMeasured_narrowBlockStillKeepsTeacher() {
        // User now requires all three fields even in a narrow block; do not silently discard the teacher.
        val name = "示例课程己"; val room = "示例楼四栋-143"; val teacher = "乙老师"
        val r = com.schedule.app.ui.fitCourseFontMeasured(
            40f, 60f, name, room, teacher, fontScale = 2f, measureWidth = fakeWidthConsistent(2f)
        )
        assertTrue(r.showTeacher)
        assertComplete(r, name, room, teacher, 40f, fakeWidthConsistent(2f))
        assertTrue(r.measuredHeightPx <= 60f)
    }

    @Test
    fun fitFontMeasured_measureWidthCallsStaysSmall() {
        // Retaining all three fields still needs a bounded amount of actual text measurement.
        var calls = 0
        val mw = { t: String, fs: Float, w: FontWeight ->
            calls++
            fakeWidthConsistent(2f)(t, fs, w)
        }
        val r = com.schedule.app.ui.fitCourseFontMeasured(
            40f, 60f, "示例课程己", "示例楼四栋-143", "乙老师", 13.5f, 5.5f, fontScale = 2f, measureWidth = mw
        )
        assertTrue(r.showTeacher)
        assertTrue("Measured $calls lines", calls <= 80)
    }

    @Test
    fun fitFontMeasured_fallbackRewrapWhenModelUnderestimates() {
        // 模型低估字宽（假测量按 1.0em）→ 触发 1.0em 保底重排：内容必须完整、每行都不超宽
        val name = "示例课程丁丁丁丁丁"; val room = "示例楼四栋-1430"; val teacher = "丙老师"
        val r = com.schedule.app.ui.fitCourseFontMeasured(
            70f, 130f, name, room, teacher, fontScale = 1.5f, measureWidth = fakeWidthAllEm(1.5f)
        )
        assertComplete(r, name, room, if (r.showTeacher) teacher else "", 70f, fakeWidthAllEm(1.5f))
    }

    // ---------- 学期选择 / 可见日 / 小组件待办行 ----------

    @Test
    fun pickDisplaySemester_prefersUserChoice() {
        val a = sem("a", LocalDate.now().minusDays(400))   // 已结束
        val b = sem("b", LocalDate.now().plusDays(30))     // 未开始（假期里自动挑最近将来的）
        assertEquals("a", com.schedule.app.ui.pickDisplaySemester(listOf(a, b), "a", LocalDate.now())!!.id)
        // 未指定时回退自动挑选 → b
        assertEquals("b", com.schedule.app.ui.pickDisplaySemester(listOf(a, b), null, LocalDate.now())!!.id)
        // 指定的学期已被删除 → 回退自动
        assertEquals("b", com.schedule.app.ui.pickDisplaySemester(listOf(b), "gone", LocalDate.now())!!.id)
        assertEquals(null, com.schedule.app.ui.pickDisplaySemester(emptyList(), null, LocalDate.now()))
    }

    @Test
    fun visibleDays_followsSemesterToggles() {
        val all = com.schedule.app.ui.visibleDays(sem("s", LocalDate.now()))
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), all)
        val noWeekend = com.schedule.app.ui.visibleDays(
            sem("s", LocalDate.now()).copy(showSaturday = false, showSunday = false)
        )
        assertEquals(listOf(1, 2, 3, 4, 5), noWeekend)
        val satOnly = com.schedule.app.ui.visibleDays(
            sem("s", LocalDate.now()).copy(showSaturday = true, showSunday = false)
        )
        assertEquals(listOf(1, 2, 3, 4, 5, 6), satOnly)
    }

    @Test
    fun widgetTodoRows_sortedCappedAndColored() {
        val today = LocalDate.of(2026, 9, 6)
        fun t(id: String, title: String, due: String?) = Todo(id, title, null, due, done = false)
        val todos = listOf(
            t("1", "一周后", today.plusDays(10).toString()),
            t("2", "今天", today.toString()),
            t("3", "三天后", today.plusDays(3).toString()),
            t("4", "已完成", today.plusDays(1).toString()).copy(done = true),
            t("5", "无日期", null),
            t("6", "逾期", today.minusDays(2).toString())
        )
        val rows = com.schedule.app.ui.widgetTodoRows(todos, today, 3)
        assertEquals(3, rows.size)
        assertEquals("已逾期2天 · 逾期", rows[0].first)
        assertEquals(0xFFFF7A7A.toInt(), rows[0].second)
        assertEquals("今天截止 · 今天", rows[1].first)
        assertEquals(0xFFFF7A7A.toInt(), rows[1].second)
        assertEquals("3天后截止 · 三天后", rows[2].first)
        assertEquals(0xFFFFFFFF.toInt(), rows[2].second)
        assertEquals(0, com.schedule.app.ui.widgetTodoRows(listOf(t("x", "无日期", null)), today, 3).size)
    }
}
