package com.schedule.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

private val palette = listOf(
    Color(0xFFE8F0FF), Color(0xFFEAF7EE), Color(0xFFFFF2E3),
    Color(0xFFF3E8FF), Color(0xFFFFE9EC), Color(0xFFE7F7F9),
    Color(0xFFFFF8DB), Color(0xFFEFEAF7), Color(0xFFFFEFE9)
)
private val accent = listOf(
    Color(0xFF3A67D6), Color(0xFF3C8C5A), Color(0xFFC67B2E),
    Color(0xFF8A52C2), Color(0xFFC4556E), Color(0xFF2E8A8F),
    Color(0xFF9A8A2F), Color(0xFF4A6CF7), Color(0xFF6A53A8)
)

fun courseColor(name: String): Color = palette[(name.hashCode() and 0x7fffffff) % palette.size]
fun courseAccent(name: String): Color = accent[(name.hashCode() and 0x7fffffff) % accent.size]

private val cn = listOf("一", "二", "三", "四", "五", "六", "日")
private val wf = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

fun weekdayShort(w: Int): String = if (w in 1..7) "周${cn[w - 1]}" else ""
fun weekdayChar(w: Int): String = if (w in 1..7) cn[w - 1] else ""
fun weekdayFull(w: Int): String = if (w in 1..7) wf[w - 1] else ""

/** 课程周数描述里最大的周数（空 = 每周都上，按 60 算）。 */
fun courseMaxWeek(weeks: String): Int =
    if (weeks.isBlank()) 60 else Regex("\\d+").findAll(weeks).maxOfOrNull { it.value.toInt() } ?: 60

fun periodLabel(s: Int, e: Int): String {
    if (s <= 0) return ""
    return if (e > s) "第${s}-${e}节" else "第${s}节"
}

fun monthLabel(month: Int): String = "${month}月"

/** 一节课的默认时长（分钟），用于估算"进行中/已结束"。 */
const val PERIOD_MINUTES = 45L

/** 节次 → 默认开始时间。与课程提醒用的参考时间一致。 */
fun defaultPeriodStart(p: Int): java.time.LocalTime = com.schedule.app.data.PeriodTimes.default(p)

fun periodStartFor(config: List<String>, p: Int): java.time.LocalTime = com.schedule.app.data.PeriodTimes.start(config, p)

/** HH:mm 格式化。 */
fun fmtTime(t: java.time.LocalTime): String = "%02d:%02d".format(t.hour, t.minute)

// ===========================================================================
// 课程块文字自适应（自绘分词换行）：编号/数字串绝不拆行，显示与计算完全一致
// ===========================================================================

/**
 * 分词断行：中文/全角逐字可断开；连续字母/数字/连字符（如 -1430、05）视为"不可断整体"，
 * 绝不从数字串中间劈开。charPx = 1em 渲染像素（fs × fontScale × densityPx）；
 * 单个 token 超宽（罕见）才拆成单字符。ultra=true 时全部字符按 1.0em 算（保底：
 * 任何字形的 advance 都在 1em 内，这样算出的行实测一定放得下）。
 * 返回的行串接起来 === 原文本，保证一个字符不丢。
 */
fun wrapByTokens(text: String, charPx: Float, availPx: Float, ultra: Boolean = false): List<String> {
    if (text.isBlank()) return emptyList()
    if (availPx <= 0f) return listOf(text)

    fun wide(code: Int): Boolean = code in 0x1100..0x11FF || code in 0x2E80..0xA4CF || code in 0xAC00..0xD7A3 ||
        code in 0xF900..0xFAFF || code in 0xFE30..0xFE4F || code in 0xFF00..0xFFEF ||
        code in 0x3000..0x303F || code in 0x20000..0x2A6DF

    // 分词：CJK 单字一个 token；连续非 CJK（含空格/字母/数字/连字符）一个 token
    val toks = ArrayList<Pair<String, Float>>()
    var i = 0
    val n = text.length
    while (i < n) {
        val code = text[i].code
        if (wide(code)) {
            toks.add(text[i].toString() to charPx)
            i++
        } else {
            val st = i
            while (i < n && !wide(text[i].code)) i++
            val s = text.substring(st, i)
            toks.add(s to s.length * charPx * (if (ultra) 1f else 0.62f))
        }
    }

    val out = ArrayList<String>()
    val line = StringBuilder()
    var lineW = 0f
    for ((s, w) in toks) {
        if (w > availPx) {
            // 单个 token 超宽：逐字符硬拆（极罕见）
            if (line.isNotEmpty()) { out.add(line.toString()); line.setLength(0); lineW = 0f }
            val sb = StringBuilder()
            var ww = 0f
            for (ch in s) {
                val cw = charPx * (if (ultra) 1f else 0.62f)
                if (ww + cw > availPx && ww > 0f) { out.add(sb.toString()); sb.setLength(0); ww = 0f }
                sb.append(ch); ww += cw
            }
            out.add(sb.toString())
            continue
        }
        if (lineW + w > availPx && line.isNotEmpty()) { out.add(line.toString()); line.setLength(0); lineW = 0f }
        line.append(s); lineW += w
    }
    if (line.isNotEmpty()) out.add(line.toString())
    return out
}

/** 课程块自适应结果：字号(sp) + 各自已断好的行（渲染逐行画，行数即真实显示行数）。 */
data class CourseFit(
    val fs: Float,
    val showTeacher: Boolean,
    val nameLines: List<String>,
    val roomLines: List<String>,
    val teacherLines: List<String>,
    val measuredHeightPx: Float = 0f
)

fun courseNameFontSize(fs: Float): Float = minOf(fs + minOf(1.5f, fs * 0.25f), 15f)

/**
 * 计算课程块内「课程名(略大)+教室+教师」的最大字号与断行结果。
 * 行由 wrapByTokens 自绘（数字串不拆、串接=原文），总高 = 行数×行高，**无需依赖 Composer 换行**；
 * 字号由纯数学模型二分求出（零布局），随后用 measureWidth 逐行实测校验宽度：
 * 有行超宽（模型低估了字宽）→ 按 1.0em 保底重排；校验不过继续降 0.5 档（最多几轮）。
 * 三项完整保留；名称按最终标题字号换行，真实 Android 行高参与最终验收。
 */
fun fitCourseFontMeasured(
    wPx: Float, hPx: Float,
    name: String, room: String, teacher: String,
    maxFs: Float = 13.5f, minFs: Float = 5.5f,
    densityPx: Float = 1f, fontScale: Float = 1f,
    measureWidth: (text: String, fsSp: Float, weight: FontWeight) -> Float,
    measureHeight: ((text: String, fsSp: Float, weight: FontWeight) -> Float)? = null
): CourseFit {
    val lhPx = 1.26f * fontScale * densityPx
    // Tall overlap cards should read in short horizontal phrases, rather than a giant one-character column.
    val nameWidthLimit = wPx / (2f * densityPx * fontScale)
    val widthCeiling = if (nameWidthLimit <= 7.5f) nameWidthLimit / 1.25f else nameWidthLimit - 1.5f
    val ceiling = if (name.length >= 4) minOf(maxFs, widthCeiling.coerceAtLeast(1f)) else maxFs

    fun em(f: Float) = f * fontScale * densityPx

    fun wrapped(fs: Float, s: String, ultra: Boolean = false) = wrapByTokens(s, em(fs), wPx, ultra)

    fun nameFsOf(fs: Float) = courseNameFontSize(fs)

    fun modelHeight(fs: Float, nl: List<String>, rl: List<String>, tl: List<String>) =
        nl.size * ceil(nameFsOf(fs) * lhPx) + (rl.size + tl.size) * ceil(fs * lhPx)

    fun balanced(lines: List<String>): List<String> {
        if (lines.size < 2) return lines
        val result = lines.toMutableList()
        val longest = lines.maxOf { it.length }
        // Widow balancing can expose an opening bracket at the end of the previous line.
        // A second pass repairs that without changing the text or increasing the line count.
        repeat(2) {
            for (i in 0 until result.lastIndex) {
                if (result[i].length > 1 && result[i].last() in "（([【" && result[i + 1].length < longest) {
                    result[i + 1] = result[i].takeLast(1) + result[i + 1]
                    result[i] = result[i].dropLast(1)
                }
            }
            for (i in 1..result.lastIndex) {
                val previous = result[i - 1]
                if (result[i].length == 1 && previous.length >= 3 && previous.last().isLetterOrDigit()) {
                    result[i] = previous.takeLast(1) + result[i]
                    result[i - 1] = previous.dropLast(1)
                }
            }
        }
        return result
    }

    /** 构建 fs 档的行并实测校验：超宽则 ultra 重排；总高超限返回 null。 */
    fun build(fs: Float, teacherText: String): CourseFit? {
        var n = balanced(wrapped(nameFsOf(fs), name))
        var r = balanced(wrapped(fs, room))
        var t = balanced(wrapped(fs, teacherText))
        fun wideNow(): Boolean {
            val checks = listOf(
                Triple(n, nameFsOf(fs), FontWeight.SemiBold),
                Triple(r, fs, FontWeight.Medium),
                Triple(t, fs, FontWeight.Normal)
            )
            return checks.any { (lines, f, w) -> lines.any { measureWidth(it, f, w) > wPx } }
        }
        if (wideNow()) {
            n = balanced(wrapped(nameFsOf(fs), name, ultra = true))
            r = balanced(wrapped(fs, room, ultra = true))
            t = balanced(wrapped(fs, teacherText, ultra = true))
            if (wideNow()) return null
        }
        val measured = if (measureHeight == null) modelHeight(fs, n, r, t) else {
            fun height(lines: List<String>, font: Float, weight: FontWeight) =
                if (lines.isEmpty()) 0f else measureHeight.invoke(lines.joinToString("\n"), font, weight)
            height(n, nameFsOf(fs), FontWeight.SemiBold) + height(r, fs, FontWeight.Medium) + height(t, fs, FontWeight.Normal)
        }
        if (measured > hPx) return null
        return CourseFit(fs, teacherText.isNotBlank(), n, r, t, measured)
    }

    /** 模型二分给出候选；按相同绘制样式实测到满足宽高为止。 */
    fun searchAndBuild(teacherText: String, floor: Float): CourseFit? {
        fun okModel(fs: Float) =
            modelHeight(fs, wrapped(nameFsOf(fs), name), wrapped(fs, room), wrapped(fs, teacherText)) <= hPx
        val cand: Float? = when {
            okModel(ceiling) -> ceiling
            !okModel(floor) -> null
            else -> {
                var lo = 1; var hi = ((ceiling - floor) / 0.5f).toInt()
                while (lo < hi) {
                    val mid = (lo + hi) / 2
                    if (okModel(ceiling - mid * 0.5f)) hi = mid else lo = mid + 1
                }
                ceiling - lo * 0.5f
            }
        }
        var fs = cand ?: return null
        while (fs >= floor - 0.01f) {
            build(fs, teacherText)?.let { return it }
            fs -= 0.5f
        }
        return null
    }

    searchAndBuild(teacher, minFs)?.let { return it }
    // Narrow overlap blocks still retain the teacher; no information is silently discarded.
    searchAndBuild(teacher, 1f)?.let { return it }
    var tiny = 0.75f
    repeat(8) {
        build(tiny, teacher)?.let { return it }
        tiny *= 0.5f
    }
    return CourseFit(tiny, teacher.isNotBlank(), listOf(name), listOf(room).filter { it.isNotBlank() }, listOf(teacher).filter { it.isNotBlank() })
}

/** 逐行渲染的单行文字：永远整行显示，不受换行器干扰。 */
@Composable
private fun FittedLineText(s: String, fs: Float, weight: FontWeight, color: Color) {
    Text(
        s,
        style = courseTextStyle(fs, weight),
        color = color,
        maxLines = s.count { it == '\n' } + 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun courseTextStyle(fs: Float, weight: FontWeight) = androidx.compose.ui.text.TextStyle(
    fontSize = fs.sp, lineHeight = (fs * 1.26f).sp, fontWeight = weight,
    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
)

/** 自动挑选"当前学期"：优先选今天正处在教学周范围内的最新学期；假期里选最近将来的；都结束了选最新的。 */
fun pickCurrentSemester(semesters: List<com.schedule.app.model.Semester>, today: java.time.LocalDate): com.schedule.app.model.Semester? {
    if (semesters.isEmpty()) return null
    data class Info(val sem: com.schedule.app.model.Semester, val start: java.time.LocalDate, val cw: Int, val lastWeek: Int)
    val infos = semesters.map { s ->
        val st = com.schedule.app.data.Wk.parseISO(s.startDate)
        val maxWeekInStrings = s.courses.maxOfOrNull { c ->
            Regex("\\d+").findAll(c.weeks).maxOfOrNull { it.value.toInt() } ?: 0
        } ?: 0
        val maxEnd = s.courses.maxOfOrNull { it.endPeriod } ?: 0
        Info(s, st, com.schedule.app.data.Wk.currentWeek(st), maxOf(maxWeekInStrings, maxEnd, 1))
    }
    val active = infos.filter { it.cw in 1..it.lastWeek }
    if (active.isNotEmpty()) return active.maxByOrNull { it.start }!!.sem
    infos.filter { it.cw < 1 }.minByOrNull { it.start }?.let { return it.sem }
    return infos.maxByOrNull { it.start }!!.sem
}

/** 主页/小组件显示的学期：用户指定的优先，未指定时自动挑选。 */
fun pickDisplaySemester(
    semesters: List<com.schedule.app.model.Semester>,
    homeSemesterId: String?,
    today: java.time.LocalDate
): com.schedule.app.model.Semester? =
    semesters.firstOrNull { it.id == homeSemesterId } ?: pickCurrentSemester(semesters, today)

/** 学期生效的节次时间表：本学期未单独设置时跟随全局。 */
fun effectivePeriodStarts(sem: com.schedule.app.model.Semester, global: List<String>): List<String> =
    sem.periodStarts.ifEmpty { global }

/** 学期生效的每天节数：本学期未单独设置时跟随全局。 */
fun effectivePeriodsPerDay(sem: com.schedule.app.model.Semester, global: Int): Int =
    if (sem.periodsPerDay in 4..16) sem.periodsPerDay else global.coerceIn(4, 16)

/** 课表网格显示的星期列（按学期的周六/周日开关过滤，其余日不显示、不留空白）。 */
fun visibleDays(sem: com.schedule.app.model.Semester): List<Int> =
    (1..7).filter { d -> when (d) { 6 -> sem.showSaturday; 7 -> sem.showSunday; else -> true } }

/**
 * 桌面小组件的待办行（最多 maxRows 条）：未完成且有截止日的待办，按截止日升序。
 * 返回 (文本, ARGB 颜色)：逾期/今天=红 0xFFFF7A7A；1~7 天=白；一周以后=绿 0xFF9BE89B。
 */
fun widgetTodoRows(
    todos: List<com.schedule.app.model.Todo>,
    today: java.time.LocalDate,
    maxRows: Int = 3
): List<Pair<String, Int>> {
    val pend: List<Pair<com.schedule.app.model.Todo, Int>> = todos
        .filter { !it.done && !it.dueDate.isNullOrBlank() }
        .mapNotNull { t ->
            val d: Int? = try {
                com.schedule.app.data.Wk.daysBetween(today, java.time.LocalDate.parse(t.dueDate!!)).toInt()
            } catch (e: Exception) { null }
            d?.let { Pair(t, it) }
        }
        .sortedBy { it.second }
        .take(maxRows)
    val out = ArrayList<Pair<String, Int>>(pend.size)
    for ((t, d) in pend) {
        val text: String = when {
            d < 0 -> "已逾期${-d}天 · ${t.title}"
            d == 0 -> "今天截止 · ${t.title}"
            d <= 7 -> (if (d == 1) "明天截止" else "${d}天后截止") + " · ${t.title}"
            else -> "${d}天后截止 · ${t.title}"
        }
        val color: Int = when {
            d < 0 -> 0xFFFF7A7A.toInt()
            d == 0 -> 0xFFFF7A7A.toInt()
            d <= 7 -> 0xFFFFFFFF.toInt()
            else -> 0xFF9BE89B.toInt()
        }
        out.add(text to color)
    }
    return out
}

/**
 * 课程块内容：课程名在上（略大、加粗），教室、教师依次在下。
 * 行由 wrapByTokens 自绘：数字串不拆、一个字符不丢；字号按 模型二分 + 逐行实测校验 求得，
 * 课程名、教室和教师共同参与实际文字测量，缩小字号时仍保留全部字段。
 */
@Composable
fun FittedCourseText(
    name: String, room: String, teacher: String,
    modifier: Modifier = Modifier,
    maxFs: Float = 13.5f, minFs: Float = 5.5f,
    showRoom: Boolean = true, showTeacher: Boolean = true
) {
    BoxWithConstraints(modifier) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val measurer = androidx.compose.ui.text.rememberTextMeasurer()
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        if (wPx <= 0f || hPx <= 0f) return@BoxWithConstraints
        val fit = remember(name, room, teacher, wPx, hPx, density.fontScale, maxFs, minFs, showRoom, showTeacher) {
            // 实测每行宽度（与渲染同样式单行；不放大行数），仅用于"行超宽"校验，偶发才触发保底重排
            fun mw(text: String, fs: Float, weight: FontWeight): Float {
                val l = measurer.measure(
                    text = text,
                    style = courseTextStyle(fs, weight),
                    softWrap = false,
                    maxLines = 1,
                    constraints = androidx.compose.ui.unit.Constraints(),
                    density = density
                )
                return l.size.width.toFloat()
            }
            fun mh(text: String, fs: Float, weight: FontWeight): Float = measurer.measure(
                text = text, style = courseTextStyle(fs, weight), softWrap = false,
                constraints = androidx.compose.ui.unit.Constraints(), density = density
            ).size.height.toFloat()
            fitCourseFontMeasured(
                wPx, hPx, name, if (showRoom) room else "", if (showTeacher) teacher else "", maxFs, minFs,
                densityPx = density.density, fontScale = density.fontScale, measureWidth = ::mw, measureHeight = ::mh
            )
        }
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FittedLineText(fit.nameLines.joinToString("\n"), courseNameFontSize(fit.fs), FontWeight.SemiBold, Color(0xFF212836))
            if (fit.roomLines.isNotEmpty()) FittedLineText(fit.roomLines.joinToString("\n"), fit.fs, FontWeight.Medium, Color(0xFF3D4557))
            if (fit.teacherLines.isNotEmpty()) FittedLineText(fit.teacherLines.joinToString("\n"), fit.fs, FontWeight.Normal, Color(0xFF555D6F))
        }
    }
}

// ===========================================================================
// 删除确认弹窗
// ===========================================================================

@Composable
fun ConfirmDelete(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text, color = Color(0xFF6A7180), fontSize = 14.sp) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("删除", color = Color(0xFFD25A5A)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
