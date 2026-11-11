package com.schedule.app

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.schedule.app.data.*
import com.schedule.app.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.asAndroidBitmap

enum class Screen { Home, Schedule, Import, Reminders, Theme, Countdown, Backup, Todos, Widgets }

class AppViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    private val store = Store(app)
    var data by mutableStateOf(BackupCodec.emptyData())
        private set
    private val commits = CommitQueue(data)
    var isLoading by mutableStateOf(true)
        private set
    private var pendingWrites by mutableStateOf(0)
    val isSaving: Boolean get() = pendingWrites > 0
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var canRestoreImport by mutableStateOf(false)
        private set
    var canExportUpgradeSnapshot by mutableStateOf(false)
        private set
    var wallpaperRevision by mutableStateOf(0L)
        private set
    val importDraft = ImportDraft()
    var pendingBackupImport by mutableStateOf<Pair<String, AppData>?>(null)
    // The full OCR text stays in memory and is not persisted in app backups or saved state.
    var screenshotTodoDraft by mutableStateOf<ScreenshotTodoDraft?>(null)
    var screenshotTodoError by mutableStateOf<String?>(null)
        private set
    var readingScreenshot by mutableStateOf(false)
        private set

    fun dismissScreenshotTodoError() { screenshotTodoError = null }
    fun recognizeScreenshotTodo(uri: android.net.Uri) {
        if (readingScreenshot) return
        viewModelScope.launch {
            readingScreenshot = true
            screenshotTodoError = null
            try {
                val text = withContext(Dispatchers.IO) { ScreenshotTodoOcr.recognize(getApplication(), uri) }
                if (text.isBlank()) screenshotTodoError = "没有识别到文字，请试试更清晰的截图。"
                else screenshotTodoDraft = ScreenshotTodoParser.parse(text)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                screenshotTodoError = "图片识别失败，请换一张清晰的截图再试。"
            } finally { readingScreenshot = false }
        }
    }

    private var screenState by mutableStateOf(runCatching { Screen.valueOf(saved.get<String>("screen") ?: "Home") }.getOrDefault(Screen.Home))
    var screen: Screen
        get() = screenState
        set(value) { screenState = value; saved["screen"] = value.name }
    private var semesterState by mutableStateOf(saved.get<String>("semester"))
    var selectedSemesterId: String?
        get() = semesterState
        set(value) { semesterState = value; saved["semester"] = value }
    private var weekState by mutableStateOf(saved.get<Int>("week") ?: 1)
    var selectedWeek: Int
        get() = weekState
        set(value) { weekState = value; saved["week"] = value }
    private var pendingWidgetEntry: String? = null
    var pendingRoute by mutableStateOf(saved.get<ArrayList<String>>("route"))
        private set
    fun clearRoute() { pendingRoute = null; saved["route"] = null }
    fun openRoute(entry: String, semesterId: String? = null, courseId: String? = null, date: String? = null, todoId: String? = null, occurrence: String? = null) {
        if (entry !in setOf("course", "day", "week", "widgets", "new-reminder", "new-todo", "todo", "home", "schedule", "reminders", "todos", "countdown")) return
        pendingRoute = arrayListOf(entry, semesterId.orEmpty(), courseId.orEmpty(), date.orEmpty(), todoId.orEmpty(), occurrence.orEmpty())
        saved["route"] = pendingRoute
        if (!isLoading) dispatchRoute()
    }
    private fun dispatchRoute() {
        val route = pendingRoute ?: return
        val sem = semesters.find { it.id == route[1] }
        when (route[0]) {
            "day", "week" -> if (sem != null && Wk.parseDateOrNull(route[3]) != null) go(Screen.Schedule, sem.id) else { clearRoute(); home() }
            "widgets" -> { clearRoute(); go(Screen.Widgets) }
            "course" -> if (sem?.courses?.any { it.id == route[2] } == true && Wk.parseDateOrNull(route[3]) != null) go(Screen.Schedule, sem.id) else { clearRoute(); home() }
            "new-reminder" -> go(Screen.Reminders, sem?.id)
            "new-todo", "todo" -> go(Screen.Todos, sem?.id)
            else -> { val entry = route[0]; clearRoute(); if (entry == "home") home() else openFromWidget(entry) }
        }
    }
    private val stack = mutableStateListOf<Screen>().apply {
        addAll(saved.get<ArrayList<String>>("navigation").orEmpty().mapNotNull { runCatching { Screen.valueOf(it) }.getOrNull() })
    }
    private fun saveStack() { saved["navigation"] = ArrayList(stack.map { it.name }) }
    val canBack: Boolean get() = stack.isNotEmpty()
    fun go(s: Screen, semesterId: String? = null) {
        if (semesterId != null) selectedSemesterId = semesterId
        if (screen != s) stack.add(screen)
        screen = s; saveStack()
    }
    fun back() { screen = if (stack.isNotEmpty()) stack.removeLast() else Screen.Home; saveStack() }
    fun home() { stack.clear(); screen = Screen.Home; saveStack() }
    fun dismissError() { errorMessage = null }

    init {
        viewModelScope.launch {
            try {
                commits.initialize(
                    load = { withContext(Dispatchers.IO) { store.load() } },
                    ready = { next ->
                        data = next
                        errorMessage = store.loadWarning
                        finishLoading()
                    },
                    maintain = { next ->
                        refresh(BackupCodec.emptyData(), next)
                        canRestoreImport = withContext(Dispatchers.IO) { store.hasImportSnapshot() }
                        canExportUpgradeSnapshot = withContext(Dispatchers.IO) { store.hasUpgradeSnapshot() }
                    }
                )
            } catch (e: Exception) { if (e is CancellationException) throw e; errorMessage = "读取失败，原文件已保留：${e.message}" }
            finally { finishLoading() }
        }
    }

    private fun finishLoading() {
        if (!isLoading) return
        isLoading = false
        pendingWidgetEntry?.let { pendingWidgetEntry = null; openFromWidget(it) }
        dispatchRoute()
    }
    internal val widgetPreviews = com.schedule.app.ui.WidgetPreviewCache()

    val semesters get() = data.semesters
    val reminders get() = data.reminders
    val countdowns get() = data.countdowns
    val todos get() = data.todos
    val selectedSemester get() = semesters.firstOrNull { it.id == selectedSemesterId }
    val bgImageUri get() = data.bgImageUri
    val bgScale get() = data.bgScale
    val bgOffX get() = data.bgOffX
    val bgOffY get() = data.bgOffY
    val widgetBgImageUri get() = data.widgetBgImageUri
    val widgetBgScale get() = data.widgetBgScale
    val widgetBgOffX get() = data.widgetBgOffX
    val widgetBgOffY get() = data.widgetBgOffY
    val theme get() = data.theme
    val pattern get() = data.pattern
    val darkMode get() = data.darkMode
    val customAccent get() = data.customAccent
    val fontScale get() = data.fontScale
    val homeSemesterId get() = data.homeSemesterId
    val blockOpacity get() = data.blockOpacity

    var wallpaperEditUri by mutableStateOf<String?>(null)
        private set
    var widgetWallpaperEditUri by mutableStateOf<String?>(null)
        private set
    fun openWallpaperEdit(uri: String) { wallpaperEditUri = uri }
    fun closeWallpaperEdit() { wallpaperEditUri = null }
    fun openWidgetWallpaperEdit(uri: String) { widgetWallpaperEditUri = uri }
    fun closeWidgetWallpaperEdit() { widgetWallpaperEditUri = null }

    fun openFromWidget(entry: String) {
        if (isLoading) { pendingWidgetEntry = entry; return }
        when (entry) {
            "schedule" -> go(Screen.Schedule, com.schedule.app.ui.pickDisplaySemester(semesters, homeSemesterId, Wk.today())?.id)
            "reminders" -> go(Screen.Reminders)
            "todos" -> go(Screen.Todos)
            "countdown" -> go(Screen.Countdown)
        }
    }
    private fun <T> List<T>.upsert(value: T, id: (T) -> String): List<T> =
        if (any { id(it) == id(value) }) map { if (id(it) == id(value)) value else it } else this + value

    private fun mutate(edit: (AppData) -> AppData) = transact { old ->
        ReminderRules.synchronize(edit(old)).also { store.save(it) }
    }
    fun upsertSemester(s: Semester) = mutate { it.copy(semesters = it.semesters.upsert(s) { v -> v.id }) }
    fun deleteSemester(id: String) = mutate { it.copy(semesters = it.semesters.filterNot { s -> s.id == id }, homeSemesterId = it.homeSemesterId.takeUnless { h -> h == id }) }
    fun upsertReminder(r: Reminder) = mutate { it.copy(reminders = it.reminders.upsert(r) { v -> v.id }) }
    fun deleteReminder(id: String) = mutate { it.copy(reminders = it.reminders.filterNot { r -> r.id == id }) }
    fun upsertCountdown(c: Countdown) = mutate { it.copy(countdowns = it.countdowns.upsert(c) { v -> v.id }) }
    fun deleteCountdown(id: String) = mutate { it.copy(countdowns = it.countdowns.filterNot { c -> c.id == id }) }
    fun upsertTodo(t: Todo) = mutate { it.copy(todos = it.todos.upsert(t) { v -> v.id }) }
    fun deleteTodo(id: String) = mutate { it.copy(todos = it.todos.filterNot { t -> t.id == id }) }
    fun upsertCourseChange(change: CourseChange) = mutate { OccurrenceRules.put(it, change) }
    fun deleteCourseChange(id: String) = mutate { it.copy(courseChanges = it.courseChanges.filterNot { c -> c.id == id }) }
    fun batchReminders(semId: String, ids: Set<String>, lead: Int, replace: Boolean) = mutate { BatchReminderRules.apply(it, semId, ids, lead, replace) }
    fun setTheme(t: Int) = mutate { it.copy(theme = t) }
    fun setPattern(p: Int) = mutate { it.copy(pattern = p) }
    fun setDarkMode(i: Int) = mutate { it.copy(darkMode = i) }
    fun setCustomAccent(argb: Int) = mutate { it.copy(customAccent = argb, theme = 6) }
    fun setFontScale(f: Float) = mutate { it.copy(fontScale = f.coerceIn(0.6f, 2f)) }
    fun setBlockOpacity(f: Float) = mutate { it.copy(blockOpacity = f.coerceIn(0.25f, 1f)) }
    fun updateVisual(edit: (VisualPreferences) -> VisualPreferences) = mutate { it.copy(visual = edit(it.visual)) }
    fun setTodoDone(id: String, done: Boolean) = mutate { old -> old.copy(todos = old.todos.map { if (it.id == id) it.copy(done = done) else it }) }
    fun setVisual(value: VisualPreferences) = mutate { it.copy(visual = value) }
    fun setHomeSemester(id: String?) = mutate { it.copy(homeSemesterId = id) }
    fun setBg(uri: String?) = mutate { it.copy(bgImageUri = uri, bgScale = 1f, bgOffX = 0f, bgOffY = 0f) }
    fun setWidgetBg(uri: String?) = mutate { it.copy(widgetBgImageUri = uri, widgetBgScale = 1f, widgetBgOffX = 0f, widgetBgOffY = 0f) }
    fun setSemesterPeriodConfig(semId: String, starts: List<String>, perDay: Int) = mutate { d ->
        require(PeriodTimes.validate(starts.take(perDay)) == null) { "节次时间无效" }
        d.copy(semesters = d.semesters.map { if (it.id == semId) it.copy(periodStarts = starts, periodsPerDay = perDay.coerceIn(4, 16)) else it })
    }

    fun setBgFull(uri: String?, scale: Float, offX: Float, offY: Float) = saveWallpaper(uri, scale, offX, offY, false)
    fun setWidgetBgFull(uri: String?, scale: Float, offX: Float, offY: Float) = saveWallpaper(uri, scale, offX, offY, true)
    private fun saveWallpaper(uri: String?, scale: Float, offX: Float, offY: Float, widget: Boolean) = transact(imagesChanged = true) { old ->
        val marker = if (widget) Store.WIDGET_WALLPAPER_MARKER else Store.WALLPAPER_MARKER
        val next = if (widget) old.copy(widgetBgImageUri = if (uri == null) null else marker, widgetBgScale = scale, widgetBgOffX = offX, widgetBgOffY = offY)
            else old.copy(bgImageUri = if (uri == null) null else marker, bgScale = scale, bgOffX = offX, bgOffY = offY)
        if (uri == null || uri == marker) store.save(next) else {
            val bmp = com.schedule.app.ui.loadBitmapSampled(getApplication(), uri, 1440)?.asAndroidBitmap() ?: error("无法读取这张图片")
            val bytes = java.io.ByteArrayOutputStream().use { output ->
                check(bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, output)) { "图片编码失败" }
                output.toByteArray()
            }
            store.saveWallpaper(next, bytes, widget)
        }
        next
    }

    suspend fun exportJson(): String = commits.read { withContext(Dispatchers.IO) { store.dump(it) } }
    suspend fun exportUpgradeSnapshot(): String = withContext(Dispatchers.IO) { store.exportUpgradeSnapshot() }
    fun previewImport(text: String): AppData = store.preview(text)
    fun importJson(text: String, onComplete: (Boolean) -> Unit = {}) = transact(onComplete, imagesChanged = true) { old -> store.importBackup(text, old) }
    fun restoreLastImport(onComplete: (Boolean) -> Unit = {}) = transact(onComplete, imagesChanged = true) { store.restoreBeforeImport() }

    /** Keep the whole commit and publication ordered; only successful writes become visible. */
    private fun transact(onComplete: (Boolean) -> Unit = {}, imagesChanged: Boolean = false, write: (AppData) -> AppData) {
        pendingWrites++
        viewModelScope.launch {
            try {
                commits.commit({ old -> withContext(Dispatchers.IO) { write(old) } }) { old, next ->
                    data = next
                    if (selectedSemesterId != null && next.semesters.none { it.id == selectedSemesterId }) selectedSemesterId = null
                    if (imagesChanged || old.bgImageUri != next.bgImageUri || old.widgetBgImageUri != next.widgetBgImageUri) wallpaperRevision++
                    // Refresh is inside the same serial transaction so an older alarm cannot replace a newer one.
                    refresh(old, next)
                }
                canRestoreImport = withContext(Dispatchers.IO) { store.hasImportSnapshot() }
                canExportUpgradeSnapshot = withContext(Dispatchers.IO) { store.hasUpgradeSnapshot() }
                onComplete(true)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                errorMessage = "修改未保存，已保留原数据：${e.message ?: "请检查存储空间"}"
                onComplete(false)
            } finally { pendingWrites-- }
        }
    }

    private suspend fun refresh(previous: AppData, next: AppData) {
        val issue = withContext(Dispatchers.IO) {
            runCatching {
                if (previous.reminders != next.reminders || previous.semesters != next.semesters || previous.courseChanges != next.courseChanges) {
                    ReminderReceiver.ensureChannel(getApplication())
                    previous.reminders.filter { old -> next.reminders.none { it.id == old.id && it.enabled } }
                        .forEach { ReminderScheduler.cancel(getApplication(), it.id) }
                    next.reminders.forEach { ReminderScheduler.schedule(getApplication(), it, next) }
                }
                if (previous.todos != next.todos) {
                    previous.todos.filter { old -> next.todos.none { it.id == old.id } }.forEach { TodoReminderReceiver.cancel(getApplication(), it.id) }
                    next.todos.forEach { TodoReminderReceiver.schedule(getApplication(), it) }
                }
                CourseWidgetProvider.updateAll(getApplication())
            }.exceptionOrNull()
        }
        if (issue != null) errorMessage = "数据已保存，提醒或小组件刷新失败，请重新打开应用：${issue.message}"
    }
}

/** Large import tables stay in the ViewModel across rotation, outside Android's saved-state Bundle. */
class ImportDraft {
    var step by mutableStateOf(0)
    var grid by mutableStateOf<List<List<String>>>(emptyList())
    var headerRow by mutableStateOf(0)
    var mapping by mutableStateOf(ManualMap())
    var parsed by mutableStateOf<List<Course>>(emptyList())
    var paste by mutableStateOf("")
}

data class ManualMap(val name: Int = -1, val code: Int = -1, val time: Int = -1, val room: Int = -1, val teacher: Int = -1)
