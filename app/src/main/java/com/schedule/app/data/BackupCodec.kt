package com.schedule.app.data

import com.schedule.app.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

data class BackupPayload(val data: AppData, val wallpaper: ByteArray?, val widgetWallpaper: ByteArray?)

/** Strict, side-effect-free codec. Existing v1.x field names and defaults stay compatible. */
object BackupCodec {
    const val FORMAT = "scheduleapp-backup"
    const val MAX_IMAGE_BYTES = 12 * 1024 * 1024
    fun emptyData() = AppData(emptyList(), emptyList(), emptyList(), emptyList(), null, 1f, 0f, 0f, 0, 0)

    fun decode(text: String): BackupPayload {
        require(text.length <= AtomicSnapshotFile.MAX_BYTES) { "备份文件过大" }
        val o = JSONObject(text)
        require(o.optJSONArray("semesters") != null) { "这不是课表备份：缺少学期列表" }
        require(!o.has("format") || string(o, "format") == FORMAT) { "备份类型不匹配" }
        require(integer(o, "schemaVersion", 1) in 1..4) { "该备份来自更新版本，请先升级应用" }
        require(!o.has("visual") || o.optJSONObject("visual") != null) { "外观设置无效" }
        val visual = o.optJSONObject("visual") ?: JSONObject()
        val wallpaper = image(o, "wallpaperData")
        val widgetWallpaper = image(o, "widgetWallpaperData")
        val data = AppData(
            semesters = objects(o, "semesters") { s -> Semester(
                string(s, "id"), string(s, "title"), string(s, "start", "2026-09-07"),
                objects(s, "courses") { c -> Course(string(c, "id"), string(c, "name"), string(c, "code"),
                    string(c, "teacher"), string(c, "room"), integer(c, "weekday"), integer(c, "startPeriod"),
                    integer(c, "endPeriod"), string(c, "weeks")) },
                boolean(s, "showSat", true), boolean(s, "showSun", true), integer(s, "perDay", -1), strings(s, "periodStarts")
            ) },
            reminders = objects(o, "reminders") { r -> Reminder(string(r, "id"), string(r, "label"), integer(r, "weekday"),
                integer(r, "hour"), integer(r, "minute"), integer(r, "lead"), string(r, "note"), boolean(r, "enabled"), integer(r, "linked").takeIf { it > 0 },
                string(r, "linkedSemesterId").ifEmpty { null }, string(r, "linkedCourseId").ifEmpty { null }) },
            countdowns = objects(o, "countdowns") { c -> Countdown(string(c, "id"), string(c, "title"), string(c, "date")) },
            todos = objects(o, "todos") { t -> Todo(string(t, "id"), string(t, "title"), string(t, "start").ifEmpty { null }, string(t, "due").ifEmpty { null }, boolean(t, "done"),
                string(t, "linkedSemesterId").ifEmpty { null }, string(t, "linkedCourseId").ifEmpty { null }, string(t, "dueTime").ifEmpty { null },
                integer(t, "remindMinutesBefore", -1).also { require(it >= -1) { "待办提醒无效" } }.takeIf { it >= 0 }) },
            bgImageUri = if (wallpaper != null) Store.WALLPAPER_MARKER else string(o, "bg").ifEmpty { null },
            bgScale = number(o, "bgScale", 1f), bgOffX = number(o, "bgOffX", 0f), bgOffY = number(o, "bgOffY", 0f),
            theme = integer(o, "theme"), pattern = integer(o, "pattern"), darkMode = integer(o, "darkMode"),
            customAccent = integer(o, "customAccent", -1), periodsPerDay = integer(o, "periodsPerDay", 12), periodStarts = strings(o, "periodStarts"),
            widgetBgImageUri = if (widgetWallpaper != null) Store.WIDGET_WALLPAPER_MARKER else string(o, "widgetBg").ifEmpty { null },
            widgetBgScale = number(o, "widgetBgScale", 1f), widgetBgOffX = number(o, "widgetBgOffX", 0f), widgetBgOffY = number(o, "widgetBgOffY", 0f),
            fontScale = number(o, "fontScale", 1f), homeSemesterId = string(o, "homeSemesterId").ifEmpty { null }, blockOpacity = number(o, "blockOpacity", 1f),
            courseChanges = objects(o, "courseChanges") { c -> CourseChange(string(c, "id"), string(c, "semesterId"), string(c, "courseId"),
                string(c, "originalDate"), string(c, "date"), string(c, "mode"), integer(c, "startPeriod"), integer(c, "endPeriod"), string(c, "room")) },
            visual = VisualPreferences(integer(visual, "widgetStyle", 0), number(visual, "widgetFontScale", 1f), number(visual, "widgetOpacity", .96f),
                boolean(visual, "widgetShowTasks", true), boolean(visual, "widgetShowTeacher", true), boolean(visual, "widgetShowRoom", true),
                boolean(visual, "widgetWeekends", true), integer(visual, "motion", 2))
        )
        validate(data)
        return BackupPayload(data, wallpaper, widgetWallpaper)
    }

    fun encode(payload: BackupPayload): String {
        val d = payload.data
        validate(d)
        return JSONObject().apply {
            put("format", FORMAT); put("schemaVersion", 4)
            put("visual", JSONObject().apply {
                put("widgetStyle", d.visual.widgetStyle); put("widgetFontScale", d.visual.widgetFontScale); put("widgetOpacity", d.visual.widgetOpacity)
                put("widgetShowTasks", d.visual.widgetShowTasks); put("widgetShowTeacher", d.visual.widgetShowTeacher); put("widgetShowRoom", d.visual.widgetShowRoom)
                put("widgetWeekends", d.visual.widgetWeekends); put("motion", d.visual.motion)
            })
            put("bg", d.bgImageUri ?: ""); put("bgScale", d.bgScale); put("bgOffX", d.bgOffX); put("bgOffY", d.bgOffY)
            put("theme", d.theme); put("pattern", d.pattern); put("darkMode", d.darkMode); put("customAccent", d.customAccent)
            put("periodsPerDay", d.periodsPerDay); put("periodStarts", array(d.periodStarts) { it })
            put("widgetBg", d.widgetBgImageUri ?: ""); put("widgetBgScale", d.widgetBgScale); put("widgetBgOffX", d.widgetBgOffX); put("widgetBgOffY", d.widgetBgOffY)
            put("fontScale", d.fontScale); put("homeSemesterId", d.homeSemesterId ?: ""); put("blockOpacity", d.blockOpacity)
            if (d.bgImageUri == Store.WALLPAPER_MARKER) payload.wallpaper?.let { put("wallpaperData", encodeImage(it)) }
            if (d.widgetBgImageUri == Store.WIDGET_WALLPAPER_MARKER) payload.widgetWallpaper?.let { put("widgetWallpaperData", encodeImage(it)) }
            put("semesters", array(d.semesters) { s -> JSONObject().apply {
                put("id", s.id); put("title", s.title); put("start", s.startDate); put("showSat", s.showSaturday); put("showSun", s.showSunday)
                put("perDay", s.periodsPerDay); put("periodStarts", array(s.periodStarts) { it })
                put("courses", array(s.courses) { c -> JSONObject().apply {
                    put("id", c.id); put("name", c.name); put("code", c.code); put("teacher", c.teacher); put("room", c.room)
                    put("weekday", c.weekday); put("startPeriod", c.startPeriod); put("endPeriod", c.endPeriod); put("weeks", c.weeks)
                } })
            } })
            put("reminders", array(d.reminders) { r -> JSONObject().apply {
                put("id", r.id); put("label", r.label); put("weekday", r.weekday); put("hour", r.hour); put("minute", r.minute)
                put("lead", r.leadMinutes); put("note", r.note); put("enabled", r.enabled); put("linked", r.linkedPeriod ?: 0)
                put("linkedSemesterId", r.linkedSemesterId ?: ""); put("linkedCourseId", r.linkedCourseId ?: "")
            } })
            put("countdowns", array(d.countdowns) { c -> JSONObject().apply { put("id", c.id); put("title", c.title); put("date", c.date) } })
            put("todos", array(d.todos) { t -> JSONObject().apply {
                put("id", t.id); put("title", t.title); put("start", t.startDate ?: ""); put("due", t.dueDate ?: ""); put("done", t.done)
                put("linkedSemesterId", t.linkedSemesterId ?: ""); put("linkedCourseId", t.linkedCourseId ?: "")
                put("dueTime", t.dueTime ?: ""); put("remindMinutesBefore", t.remindMinutesBefore ?: -1)
            } })
            put("courseChanges", array(d.courseChanges) { c -> JSONObject().apply {
                put("id", c.id); put("semesterId", c.semesterId); put("courseId", c.courseId); put("originalDate", c.originalDate)
                put("date", c.date); put("mode", c.mode); put("startPeriod", c.startPeriod); put("endPeriod", c.endPeriod); put("room", c.room)
            } })
        }.toString().also { require(it.toByteArray(Charsets.UTF_8).size <= AtomicSnapshotFile.MAX_BYTES) { "备份文件超过 32 MB" } }
    }

    fun validate(d: AppData) {
        require(d.visual.widgetStyle in 0..6 && d.visual.motion in 0..2 && d.visual.widgetFontScale in .8f..1.3f && d.visual.widgetOpacity in .5f..1f) { "外观设置范围无效" }
        uniqueIds(d.semesters.map { it.id }, "学期"); uniqueIds(d.reminders.map { it.id }, "提醒")
        uniqueIds(d.countdowns.map { it.id }, "倒计时"); uniqueIds(d.todos.map { it.id }, "待办")
        require(d.semesters.size <= 100 && d.semesters.sumOf { it.courses.size } <= 10000) { "课程数据量过大" }
        require(d.periodsPerDay in 4..16) { "每天节数应为 4～16" }
        d.semesters.forEach { s ->
            date(s.startDate, "开学日期"); uniqueIds(s.courses.map { it.id }, "课程")
            require(s.periodsPerDay == -1 || s.periodsPerDay in 4..16) { "学期节数无效" }; times(s.periodStarts)
            s.courses.forEach { c ->
                require(c.name.isNotBlank()) { "课程名称不能为空" }
                // Legacy unassigned/out-of-grid courses remain in the list; never silently discard them.
                require(c.weekday in 0..7 && c.startPeriod in 0..100 && c.endPeriod in 0..100) { "课程星期或节次无效" }
                require(c.weeks.length <= 1000) { "课程周次描述过长" }
                Regex("\\d+").findAll(c.weeks).forEach { require((it.value.toIntOrNull() ?: Int.MAX_VALUE) in 0..520) { "课程周次应在 520 周以内" } }
            }
        }
        d.reminders.forEach { r -> require(r.weekday in 1..7 && r.hour in 0..23 && r.minute in 0..59 && r.leadMinutes in 0..10080) { "提醒日期或时间无效" } }
        d.countdowns.forEach { date(it.date, "倒计时日期") }
        d.todos.forEach { t ->
            t.startDate?.let { date(it, "待办开始日期") }; t.dueDate?.let { date(it, "待办截止日期") }
            t.dueTime?.let { require(Regex("\\d{2}:\\d{2}").matches(it) && runCatching { java.time.LocalTime.parse(it) }.isSuccess) { "截止时间无效" } }
            require(t.remindMinutesBefore == null || (t.remindMinutesBefore in 0..10080 && t.dueDate != null)) { "待办提醒无效" }
        }
        uniqueIds(d.courseChanges.map { it.id }, "单次调课")
        val replacements = d.courseChanges.filter { it.mode != "extra" }.map { Triple(it.semesterId, it.courseId, it.originalDate) }
        require(replacements.distinct().size == replacements.size) { "同一次课程有重复调整" }
        d.courseChanges.forEach { c ->
            require(c.semesterId.isNotBlank() && c.courseId.isNotBlank() && c.mode in setOf("move", "cancel", "extra")) { "单次调课类型无效" }
            date(c.originalDate, "原上课日期"); date(c.date, "调整日期")
            require(c.startPeriod in 1..16 && c.endPeriod in c.startPeriod..16) { "调课节次无效" }
        }
        times(d.periodStarts)
        require(d.theme in 0..6 && d.darkMode in 0..2 && d.pattern in 0..20) { "主题设置无效" }
        require(d.fontScale.isFinite() && d.fontScale in 0.6f..2f && d.blockOpacity.isFinite() && d.blockOpacity in 0.25f..1f) { "字体或不透明度无效" }
        listOf(d.bgScale, d.widgetBgScale).forEach { require(it.isFinite() && it > 0f && it <= 20f) { "壁纸缩放无效" } }
        listOf(d.bgOffX, d.bgOffY, d.widgetBgOffX, d.widgetBgOffY).forEach { require(it.isFinite() && kotlin.math.abs(it) <= 100f) { "壁纸位置无效" } }
    }

    private fun uniqueIds(ids: List<String>, label: String) { require(ids.all { it.isNotBlank() } && ids.toSet().size == ids.size) { "${label}标识缺失或重复" } }
    private fun date(s: String, label: String) { require(Wk.parseDateOrNull(s) != null) { "${label}不是有效日期" } }
    private fun times(values: List<String>) {
        require(values.size <= 16) { "节次时间过多" }
        values.forEach { require(Regex("\\d{1,2}:\\d{2}").matches(it) && runCatching { java.time.LocalTime.parse(it.padStart(5, '0')) }.isSuccess) { "节次时间无效：$it" } }
    }
    private fun string(o: JSONObject, key: String, default: String = ""): String {
        if (!o.has(key) || o.isNull(key)) return default
        val value = o.get(key); require(value is String) { "$key 应为文本" }; return value
    }
    private fun integer(o: JSONObject, key: String, default: Int = 0): Int {
        if (!o.has(key) || o.isNull(key)) return default
        val value = o.get(key); require(value is Number && value.toDouble() == value.toInt().toDouble()) { "$key 应为整数" }; return value.toInt()
    }
    private fun number(o: JSONObject, key: String, default: Float): Float {
        if (!o.has(key) || o.isNull(key)) return default
        val value = o.get(key); require(value is Number && value.toDouble().isFinite()) { "$key 应为有效数值" }; return value.toFloat()
    }
    private fun boolean(o: JSONObject, key: String, default: Boolean = false): Boolean {
        if (!o.has(key) || o.isNull(key)) return default
        val value = o.get(key); require(value is Boolean) { "$key 应为布尔值" }; return value
    }
    private fun image(o: JSONObject, key: String): ByteArray? {
        val encoded = string(o, key).filterNot { it.isWhitespace() }
        if (encoded.isEmpty()) return null
        require(encoded.length <= MAX_IMAGE_BYTES * 4 / 3 + 4) { "壁纸数据过大" }
        return Base64.getDecoder().decode(encoded).also { require(it.isNotEmpty() && it.size <= MAX_IMAGE_BYTES) { "壁纸数据无效" } }
    }
    private fun encodeImage(bytes: ByteArray): String { require(bytes.size <= MAX_IMAGE_BYTES) { "壁纸数据过大" }; return Base64.getEncoder().encodeToString(bytes) }
    private fun strings(o: JSONObject, key: String): List<String> {
        if (!o.has(key)) return emptyList()
        val a = o.getJSONArray(key)
        return (0 until a.length()).map { val v = a.get(it); require(v is String) { "$key 包含非文本项" }; v }
    }
    private fun <T> objects(o: JSONObject, key: String, decode: (JSONObject) -> T): List<T> {
        if (!o.has(key)) return emptyList()
        val a = o.getJSONArray(key); require(a.length() <= 10000) { "$key 数据量过大" }
        return (0 until a.length()).map { decode(a.getJSONObject(it)) }
    }
    private fun <T> array(values: List<T>, encode: (T) -> Any): JSONArray = JSONArray().apply { values.forEach { put(encode(it)) } }
}
