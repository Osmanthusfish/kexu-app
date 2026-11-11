package com.schedule.app.model

/** 一门课的某一次上课安排（一个时间段）。 */
data class Course(
    val id: String,
    val name: String,        // 课程名称
    val code: String,        // 教学班号 / 课程代码
    val teacher: String,
    val room: String,
    val weekday: Int,        // 1=周一 ... 7=周日, 0=未知(未排)
    val startPeriod: Int,    // 1-based 起始节, 0=未知
    val endPeriod: Int,
    val weeks: String        // 周数描述，如 "1-4,6-9"
)

/** 一个学期（+ 第一周周一日期 + 本学期独立的显示/节次设置）。 */
data class Semester(
    val id: String,
    val title: String,       // 如 "2024-2025 下学期"
    val startDate: String,   // 第一周周一，ISO yyyy-MM-dd，如 "2026-09-07"
    val courses: List<Course>,
    val showSaturday: Boolean = true,   // 本学期是否显示周六列
    val showSunday: Boolean = true,     // 本学期是否显示周日列
    val periodsPerDay: Int = -1,        // 每天节数（4..16）；-1 = 跟随全局
    val periodStarts: List<String> = emptyList()  // 本学期每节课开始时间（"HH:mm"）；空 = 跟随全局
)

/** 一门课的提醒。时间为具体钟点（HH:mm），duration 提前分钟数。 */
data class Reminder(
    val id: String,
    val label: String,       // 提示语，如 "准备上：高等数学"
    val weekday: Int,        // 1..7
    val hour: Int,
    val minute: Int,
    val leadMinutes: Int,    // 提前多少分钟提醒
    val note: String,
    val enabled: Boolean,
    val linkedPeriod: Int? = null,
    val linkedSemesterId: String? = null,
    val linkedCourseId: String? = null
)

/** 倒计时事件。date 为 ISO yyyy-MM-dd（目标日 00:00 起算）。 */
data class Countdown(
    val id: String,
    val title: String,
    val date: String
)

/** 待办/备忘。startDate/dueDate 可选（ISO yyyy-MM-dd）。 */
data class Todo(
    val id: String,
    val title: String,
    val startDate: String?,   // 可空
    val dueDate: String?,     // 可空
    val done: Boolean,
    val linkedSemesterId: String? = null,
    val linkedCourseId: String? = null,
    val dueTime: String? = null,
    val remindMinutesBefore: Int? = null
)

/** An exception applies to one original date only; deleting it restores the recurring course. */
data class CourseChange(
    val id: String,
    val semesterId: String,
    val courseId: String,
    val originalDate: String,
    val date: String,
    val mode: String, // move, cancel, extra
    val startPeriod: Int,
    val endPeriod: Int,
    val room: String
)

/** 全局应用数据。 */
data class AppData(
    val semesters: List<Semester>,
    val reminders: List<Reminder>,
    val countdowns: List<Countdown>,
    val todos: List<Todo>,
    val bgImageUri: String?,  // 壁纸图片 URI，null = 默认
    val bgScale: Float,        // 壁纸缩放（1..4）
    val bgOffX: Float,         // 壁纸水平偏移（相对屏宽比例）
    val bgOffY: Float,         // 壁纸垂直偏移
    val theme: Int,            // 0=默认, 1..5 内置主题, 6=自定义
    val pattern: Int,          // 0=素(无图案), 1..3 图案
    val darkMode: Int = 0,     // 0=跟随系统, 1=浅色, 2=深色
    val customAccent: Int = -1, // theme==6 时的自定义主题色（ARGB），-1=未设置
    val periodsPerDay: Int = 12,  // 每天显示的节次总数（可按学校调整）
    val periodStarts: List<String> = emptyList(), // 每节课开始时间（"HH:mm"），空 = 用默认表
    val widgetBgImageUri: String? = null,  // 桌面小组件壁纸（null = 用主题渐变）
    val widgetBgScale: Float = 1f,         // 小组件壁纸缩放（1..4）
    val widgetBgOffX: Float = 0f,          // 小组件壁纸水平偏移
    val widgetBgOffY: Float = 0f,          // 小组件壁纸垂直偏移
    val fontScale: Float = 1f,             // 应用内字体缩放（1.0=跟随系统，可调 0.85~1.3）
    val homeSemesterId: String? = null,    // 主页/小组件显示的学期；null = 自动挑选
    val blockOpacity: Float = 1f,          // 课程块不透明度（0.25~1.0）
    val courseChanges: List<CourseChange> = emptyList(),
    val visual: VisualPreferences = VisualPreferences()
)

/** Appearance is backed up with the timetable; widget browsing stays local to each widget. */
data class VisualPreferences(
    val widgetStyle: Int = 0, // 0 aurora, 1 midnight, 2 theme, 3 sunset, 4 mint, 5 lilac, 6 paper
    val widgetFontScale: Float = 1f,
    val widgetOpacity: Float = .96f,
    val widgetShowTasks: Boolean = true,
    val widgetShowTeacher: Boolean = true,
    val widgetShowRoom: Boolean = true,
    val widgetWeekends: Boolean = true,
    val motion: Int = 2 // Legacy backup field; widget feedback is always gentle.
)
