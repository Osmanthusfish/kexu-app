package com.schedule.app

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModelProvider
import com.schedule.app.data.*
import com.schedule.app.model.*
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import com.schedule.app.ui.ScreenshotTodoReviewDialog
import com.schedule.app.ui.ScreenshotTodoAiSession

/** Device regression runner using Android's existing framework, with no additional test dependencies.
 * Refuses to touch the regular app. It preserves a pre-test snapshot inside the isolated app.
 */
class RegressionInstrumentation : Instrumentation() {
    private val latest = AtomicReference<MainActivity>()
    private val passed = mutableListOf<String>()
    private val screenshotFolder = "qa-screenshots-${System.currentTimeMillis()}"
    private var dailyChecks = false
    private var skyOnly = false
    private var ocrOnly = false
    private var reviewOnly = false
    private var aiReviewOnly = false
    private var rotationOnly = false
    private var ocrRotationOnly = false
    private var aiRotationOnly = false
    private var skipWidgetMatrix = false
    private var skipWidgetContent = false
    private var pickerOnly = false
    private var originalBackup: String? = null
    override fun onCreate(arguments: Bundle?) { dailyChecks = arguments?.getString("daily") == "true"; skyOnly = arguments?.getString("sky_only") == "true"; ocrOnly = arguments?.getString("ocr_only") == "true"; reviewOnly = arguments?.getString("review_only") == "true"; aiReviewOnly = arguments?.getString("ai_review_only") == "true"; rotationOnly = arguments?.getString("rotation_only") == "true"; ocrRotationOnly = arguments?.getString("ocr_rotation_only") == "true"; aiRotationOnly = arguments?.getString("ai_rotation_only") == "true"; skipWidgetMatrix = arguments?.getString("skip_widget_matrix") == "true"; skipWidgetContent = arguments?.getString("skip_widget_content") == "true"; pickerOnly = arguments?.getString("picker_only") == "true"; super.onCreate(arguments); start() }
    override fun onStart() {
        try {
            check(targetContext.packageName == "com.schedule.app.review") { "Only the isolated review package may be tested" }
            sendStatus(0, Bundle().apply { putString("stream", "Screenshots: files/$screenshotFolder\n") })
            val application = targetContext.applicationContext as Application
            application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) { if (activity is MainActivity) latest.set(activity) }
                override fun onActivityCreated(activity: Activity, state: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            })
            startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            await { latest.get() != null && !model().isLoading }
            if (pickerOnly) {
                val store = Store(targetContext)
                val before = store.load()
                val original = store.dump(before)
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "kexu-synthetic-" + java.util.UUID.randomUUID() + ".png")
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KexuSyntheticTests")
                }
                val resolver = targetContext.contentResolver
                val uri = checkNotNull(resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
                var wroteTodo = false
                try {
                    context.assets.open("screenshot-todo-sample.png").use { input -> checkNotNull(resolver.openOutputStream(uri)).use(input::copyTo) }
                    main { model().go(Screen.Todos) }
                    await { containsText(uiAutomation.rootInActiveWindow, "截图转待办") }
                    check(clickLabel(uiAutomation.rootInActiveWindow, "截图转待办"))
                    await { containsText(uiAutomation.rootInActiveWindow, "截图识别与隐私") }
                    check(clickLabel(uiAutomation.rootInActiveWindow, "选择截图"))
                    await { containsText(uiAutomation.rootInActiveWindow, "Photos") }
                    await { clickFirstPhoto(uiAutomation.rootInActiveWindow) }
                    await { model().screenshotTodoDraft != null && containsText(uiAutomation.rootInActiveWindow, "检查识别结果") }
                    check(model().screenshotTodoDraft!!.recognizedText.contains("实验报告")) { "Unexpected picker image" }
                    check(setFirstEditableText(uiAutomation.rootInActiveWindow, "合成截图测试事项"))
                    check(clickLabel(uiAutomation.rootInActiveWindow, "确认 2026-11-20 是截止日期"))
                    repeat(5) { if (!containsText(uiAutomation.rootInActiveWindow, "确认 15:00 是截止时间")) scrollForward(uiAutomation.rootInActiveWindow) }
                    check(clickLabel(uiAutomation.rootInActiveWindow, "确认 15:00 是截止时间"))
                    await { isEnabled(uiAutomation.rootInActiveWindow, "确认添加") }
                    wroteTodo = true
                    check(clickLabel(uiAutomation.rootInActiveWindow, "确认添加"))
                    saved()
                    check(store.load().todos.any { it.title == "合成截图测试事项" && it.dueDate == "2026-11-20" && it.dueTime == "15:00" })
                    pass("image selection result flows through OCR, review and existing task storage")
                } finally {
                    resolver.delete(uri, null, null)
                    main { model().screenshotTodoDraft = null; if (wroteTodo) model().importJson(original) }
                    if (wroteTodo) {
                        saved()
                        check(store.load() == before) { "Test app data was not restored" }
                    }
                }
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "\nPASS system picker end-to-end\n") })
                return
            }
            if (aiRotationOnly) {
                val started = java.util.concurrent.CountDownLatch(1)
                val release = java.util.concurrent.CountDownLatch(1)
                val calls = java.util.concurrent.atomic.AtomicInteger()
                var session: ScreenshotTodoAiSession? = null
                try {
                    val draft = ScreenshotTodoParser.parse("2026年11月20日 15:00 提交示例材料")
                    main { model().go(Screen.Todos); model().screenshotTodoDraft = draft }
                    await { containsText(uiAutomation.rootInActiveWindow, "检查识别结果") }
                    main {
                        session = ViewModelProvider(latest.get())[ScreenshotTodoAiSession::class.java]
                        session!!.start(draft, { "synthetic" }) { _, _, source ->
                            calls.incrementAndGet(); started.countDown()
                            check(release.await(15, java.util.concurrent.TimeUnit.SECONDS))
                            source.copy(title = "旋转后整理完成")
                        }
                    }
                    check(started.await(5, java.util.concurrent.TimeUnit.SECONDS))
                    val oldActivity = latest.get()
                    main { latest.get().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                    await { latest.get() !== oldActivity && latest.get().resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }
                    check(session!!.working)
                    release.countDown()
                    await { model().screenshotTodoDraft?.title == "旋转后整理完成" }
                    check(calls.get() == 1)
                    await { containsText(uiAutomation.rootInActiveWindow, "旋转后整理完成") }
                    pass("single AI request survives rotation and returns to the draft")
                    val canceledStarted = java.util.concurrent.CountDownLatch(1)
                    val canceledRelease = java.util.concurrent.CountDownLatch(1)
                    val canceledFinished = java.util.concurrent.CountDownLatch(1)
                    main {
                        session!!.start(draft, { "synthetic" }) { _, _, source ->
                            canceledStarted.countDown()
                            canceledRelease.await(5, java.util.concurrent.TimeUnit.SECONDS)
                            canceledFinished.countDown()
                            source.copy(title = "已取消的结果")
                        }
                    }
                    check(canceledStarted.await(5, java.util.concurrent.TimeUnit.SECONDS))
                    main { session!!.cancel() }
                    canceledRelease.countDown()
                    check(canceledFinished.await(5, java.util.concurrent.TimeUnit.SECONDS))
                    Thread.sleep(300)
                    check(session!!.result == null && !session!!.working && model().screenshotTodoDraft?.title == "旋转后整理完成")
                    pass("cancelled AI response cannot replace the current draft")
                } finally {
                    release.countDown()
                    main { session?.cancel(); model().screenshotTodoDraft = null; latest.get().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
                }
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "\nPASS AI rotation\n") })
                return
            }
            if (ocrRotationOnly) {
                val image = File(targetContext.cacheDir, "ocr-rotation-synthetic.png")
                context.assets.open("screenshot-todo-sample.png").use { input -> image.outputStream().use(input::copyTo) }
                try {
                    main {
                        model().go(Screen.Todos)
                        model().screenshotTodoDraft = null
                        model().recognizeScreenshotTodo(android.net.Uri.fromFile(image))
                        check(model().readingScreenshot)
                        latest.get().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                    await { model().screenshotTodoDraft != null && !model().readingScreenshot }
                    await { latest.get().resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }
                    check(model().screenshotTodoDraft!!.recognizedText.contains("实验报告"))
                    await { containsText(uiAutomation.rootInActiveWindow, "检查识别结果") }
                    pass("OCR result survives an orientation change while recognition is running")
                } finally {
                    main { model().screenshotTodoDraft = null; latest.get().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
                    image.delete()
                }
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "\nPASS OCR rotation\n") })
                return
            }
            if (rotationOnly) {
                try {
                    main { model().go(Screen.Todos); model().screenshotTodoDraft = ScreenshotTodoParser.parse("2026年11月20日 15:00 提交示例材料") }
                    await { containsText(uiAutomation.rootInActiveWindow, "检查识别结果") }
                    check(setFirstEditableText(uiAutomation.rootInActiveWindow, "已编辑的示例事项"))
                    await { containsText(uiAutomation.rootInActiveWindow, "已编辑的示例事项") }
                    main { latest.get().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                    await { latest.get().resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE && containsText(uiAutomation.rootInActiveWindow, "已编辑的示例事项") }
                    check(model().screenshotTodoDraft != null)
                    pass("rotation retains the screenshot draft and edited title")
                } finally {
                    main { model().screenshotTodoDraft = null; latest.get().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
                }
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "\nPASS screenshot draft rotation\n") })
                return
            }
            if (aiReviewOnly) {
                val keyStore = TodoAiKeyStore(targetContext)
                check(!keyStore.configured()) { "Refusing to overwrite a configured AI key" }
                try {
                    keyStore.save("synthetic-test-" + java.util.UUID.randomUUID())
                    val draft = ScreenshotTodoParser.parse("2026年11月20日 15:00 提交示例材料")
                    val savedTodo = AtomicReference<Todo?>()
                    val sentText = AtomicReference<String?>()
                    val resultDraft = AtomicReference<ScreenshotTodoDraft?>()
                    main { latest.get().setContent { MaterialTheme { ScreenshotTodoReviewDialog(draft, {}, { resultDraft.set(it) }, organize = { text, _, original -> sentText.set(text); original.copy(title = "整理后的示例") }, onSave = { savedTodo.set(it) }) } } }
                    await { containsText(uiAutomation.rootInActiveWindow, "检查识别结果") }
                    repeat(8) { if (!containsText(uiAutomation.rootInActiveWindow, "AI 整理文字")) scrollForward(uiAutomation.rootInActiveWindow) }
                    check(clickLabel(uiAutomation.rootInActiveWindow, "AI 整理文字")) { "AI button not accessible" }
                    await { containsText(uiAutomation.rootInActiveWindow, "发送识别文字给 DeepSeek？") }
                    check(containsText(uiAutomation.rootInActiveWindow, draft.recognizedText))
                    check(containsText(uiAutomation.rootInActiveWindow, "图片不会发送"))
                    check(clickLabel(uiAutomation.rootInActiveWindow, "取消"))
                    await { !containsText(uiAutomation.rootInActiveWindow, "发送识别文字给 DeepSeek？") }
                    check(savedTodo.get() == null && sentText.get() == null)
                    check(containsText(uiAutomation.rootInActiveWindow, "检查识别结果"))
                    pass("AI consent shows text and cancel returns to the unsaved draft")
                    check(clickLabel(uiAutomation.rootInActiveWindow, "AI 整理文字"))
                    await { containsText(uiAutomation.rootInActiveWindow, "发送识别文字给 DeepSeek？") }
                    check(clickLabel(uiAutomation.rootInActiveWindow, "同意并发送文字"))
                    await { resultDraft.get() != null }
                    check(sentText.get() == draft.recognizedText && resultDraft.get()?.title == "整理后的示例" && savedTodo.get() == null)
                    pass("AI starts only after consent and returns an unsaved draft")
                    main { latest.get().setContent { MaterialTheme { ScreenshotTodoReviewDialog(draft, {}, {}, organize = { _, _, _ -> throw IllegalStateException("synthetic failure") }, onSave = { savedTodo.set(it) }) } } }
                    await { containsText(uiAutomation.rootInActiveWindow, "检查识别结果") }
                    repeat(8) { if (!containsText(uiAutomation.rootInActiveWindow, "AI 整理文字")) scrollForward(uiAutomation.rootInActiveWindow) }
                    check(clickLabel(uiAutomation.rootInActiveWindow, "AI 整理文字"))
                    await { containsText(uiAutomation.rootInActiveWindow, "发送识别文字给 DeepSeek？") }
                    check(clickLabel(uiAutomation.rootInActiveWindow, "同意并发送文字"))
                    await {
                        val root = uiAutomation.rootInActiveWindow
                        if (containsText(root, "原草稿仍在")) true else { scrollForward(root); false }
                    }
                    check(savedTodo.get() == null)
                    pass("AI failure leaves the draft unsaved")
                } finally { keyStore.clear() }
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "\nPASS AI consent cancellation\n") })
                return
            }
            if (reviewOnly) {
                val savedTodo = AtomicReference<Todo?>()
                val draft = ScreenshotTodoParser.parse("2026年11月20日 15:00 提交示例材料")
                main { latest.get().setContent { MaterialTheme { ScreenshotTodoReviewDialog(draft, {}, {}, onSave = { savedTodo.set(it) }) } } }
                await { containsText(uiAutomation.rootInActiveWindow, "确认添加") }
                check(!isEnabled(uiAutomation.rootInActiveWindow, "确认添加")) { "Draft saved without confirmation" }
                check(clickLabel(uiAutomation.rootInActiveWindow, "确认 2026-11-20 是截止日期"))
                waitForIdleSync()
                check(!isEnabled(uiAutomation.rootInActiveWindow, "确认添加")) { "Draft saved without confirming its time" }
                repeat(4) { if (!containsText(uiAutomation.rootInActiveWindow, "确认 15:00 是截止时间")) scrollForward(uiAutomation.rootInActiveWindow) }
                check(clickLabel(uiAutomation.rootInActiveWindow, "确认 15:00 是截止时间"))
                await { isEnabled(uiAutomation.rootInActiveWindow, "确认添加") }
                check(clickLabel(uiAutomation.rootInActiveWindow, "确认添加"))
                await { savedTodo.get() != null }
                check(savedTodo.get()?.dueDate == "2026-11-20" && savedTodo.get()?.dueTime == "15:00")
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "\nPASS review requires date and time confirmation\n") })
                return
            }
            val ocrImage = File(targetContext.cacheDir, "ocr-synthetic-input.png")
            context.assets.open("screenshot-todo-sample.png").use { input -> ocrImage.outputStream().use(input::copyTo) }
            val ocrText = ScreenshotTodoOcr.recognize(targetContext, android.net.Uri.fromFile(ocrImage))
            check(ocrText.contains("实验报告") && ocrText.contains("2026年11月20日") && ocrText.contains("15:00")) {
                "Offline Chinese OCR missed the synthetic deadline screenshot"
            }
            ocrImage.delete()
            pass("bundled Chinese OCR reads the synthetic task and deadline screenshot locally")
            if (ocrOnly) {
                fun recognizeFixture(asset: String): String {
                    val image = File(targetContext.cacheDir, "ocr-fixture-synthetic.png")
                    return try {
                        context.assets.open(asset).use { input -> image.outputStream().use(input::copyTo) }
                        ScreenshotTodoOcr.recognize(targetContext, android.net.Uri.fromFile(image))
                    } finally { image.delete() }
                }
                val chatText = recognizeFixture("screenshot-todo-chat-synthetic.png")
                val chatDraft = ScreenshotTodoParser.parse(chatText)
                check(chatText.contains("09:30") && chatText.contains("15:00") && chatDraft.title.contains("问卷")) { "Synthetic chat was not recognized as expected" }
                check(chatDraft.dueTime == null && chatDraft.ambiguousTimeText != null) { "Chat timestamp was guessed as a deadline" }
                pass("chat screenshot does not mistake its message timestamp for the deadline")
                val ambiguousText = recognizeFixture("screenshot-todo-ambiguous-synthetic.png")
                val ambiguousDraft = ScreenshotTodoParser.parse(ambiguousText)
                check(ambiguousText.contains("明天") && ambiguousText.contains("3点")) { "Ambiguous synthetic screenshot was not recognized" }
                check(ambiguousDraft.dueTime == null && ambiguousDraft.ambiguousTimeText != null && ambiguousDraft.needsDateConfirmation)
                pass("ambiguous screenshot time is left for user confirmation")
                val keyStore = TodoAiKeyStore(targetContext)
                check(!keyStore.configured()) { "Refusing to overwrite a configured AI key" }
                val sampleKey = "synthetic-test-" + java.util.UUID.randomUUID()
                try {
                    keyStore.save(sampleKey)
                    check(keyStore.load() == sampleKey)
                    val bytes = File(targetContext.noBackupFilesDir, "screenshot_todo_ai_key").readBytes()
                    check(!bytes.toString(Charsets.ISO_8859_1).contains(sampleKey))
                    keyStore.clear()
                    check(!keyStore.configured())
                    pass("AI key is encrypted and can be removed")
                } finally { keyStore.clear() }
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "\\nPASS local Chinese OCR only\\n") })
                return
            }
            val store = Store(targetContext)
            val before = store.load()
            originalBackup = store.dump(before)
            File(targetContext.filesDir, "qa-before-${System.currentTimeMillis()}.json").writeText(store.dump(before))
            if (skyOnly) {
                runHomeCourseChecks()
                main { model().importJson(originalBackup!!) }; saved()
                check(model().data == before && store.load() == before)
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "\nPASS sky interaction checks; original data restored\n") })
                return
            }
            val fixture = context.assets.open("regression-backup.json").bufferedReader().use { it.readText() }
            main { model().importJson(fixture) }
            saved()
            check(model().semesters.size == 2 && model().semesters.sumOf { it.courses.size } == 4)
            check(store.load() == model().data)
            pass("backup import persists all fixture data")

            val stable = model().data
            main { model().importJson("{broken") }
            saved()
            check(model().data == stable && store.load() == stable)
            main { model().dismissError() }
            pass("malformed import leaves current data intact")

            main {
                model().upsertTodo(Todo("queue-a", "Queued A", null, null, false))
                model().upsertTodo(Todo("queue-b", "Queued B", null, null, false))
                model().setTheme(2)
            }
            saved()
            check(store.load().todos.map { it.id }.containsAll(listOf("queue-a", "queue-b")))
            check(model().theme == 2)
            pass("rapid independent edits all persist")
            main {
                model().updateVisual { it.copy(widgetStyle = 1) }
                model().updateVisual { it.copy(widgetFontScale = 1.2f) }
                model().updateVisual { it.copy(widgetShowTeacher = false, motion = 1) }
            }
            saved()
            check(store.load().visual.let { it.widgetStyle == 1 && it.widgetFontScale == 1.2f && !it.widgetShowTeacher && it.motion == 1 })
            pass("rapid widget customizations compose without overwriting each other")
            main { model().setTodoDone("queue-a", true); model().setTodoDone("queue-a", false) }
            saved()
            check(!store.load().todos.first { it.id == "queue-a" }.done)
            pass("task completion and undo persist through the write queue")
            if (!skipWidgetMatrix) {
            main {
                val parent = android.appwidget.AppWidgetHostView(targetContext)
                for (style in 0..6) for (font in listOf(.8f,1f,1.3f)) for (weekly in listOf(false, true)) for (empty in listOf(false,true)) for (size in (if(weekly) listOf(android.util.SizeF(332f,145f),android.util.SizeF(600f,120f),android.util.SizeF(250f,303f),android.util.SizeF(332f,303f), android.util.SizeF(332f,618f)) else listOf(android.util.SizeF(180f,100f),android.util.SizeF(332f,100f),android.util.SizeF(180f,145f),android.util.SizeF(250f,303f),android.util.SizeF(332f,145f), android.util.SizeF(332f,303f)))) {
                    val day = java.time.LocalDate.now()
                    val data = model().data.copy(visual = model().data.visual.copy(widgetStyle = style,widgetFontScale=font,widgetShowTeacher=true,widgetShowRoom=true), semesters = model().semesters.map { sem ->
                        sem.copy(startDate = Wk.mondayOf(day).toString(),courses = if(empty)emptyList() else sem.courses.map { it.copy(weekday = day.dayOfWeek.value,name=if(it.id=="math")"示例课程戊戊戊戊戊实验（课程设计）" else it.name) })
                    })
                    val remote = WidgetRenderer.page(targetContext,data,-1,weekly,size,WidgetBrowseState(Wk.mondayOf(java.time.LocalDate.now()),0))
                    val v = remote.apply(targetContext,parent)
                    val density = targetContext.resources.displayMetrics.density
                    v.measure(android.view.View.MeasureSpec.makeMeasureSpec(((size.width-24)*density).toInt(),android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(((size.height-24)*density).toInt(),android.view.View.MeasureSpec.EXACTLY))
                    v.layout(0,0,v.measuredWidth,v.measuredHeight)
                    check(v.measuredHeight > 0 && v.measuredWidth > 0)
                    fun verifyText(view: android.view.View) {
                        if(view is android.widget.TextView && view.id in setOf(R.id.courseCell,R.id.todayText,R.id.todayTime,R.id.glanceText) && view.text.length>3) {
                            check(view.layout.height<=view.height-view.compoundPaddingTop-view.compoundPaddingBottom) { "Widget text clips at $size style=$style font=$font weekly=$weekly: ${view.text}, ${view.layout.height} > ${view.height-view.compoundPaddingTop-view.compoundPaddingBottom}" }
                        }
                        if(view is android.view.ViewGroup)for(i in 0 until view.childCount)verifyText(view.getChildAt(i))
                    }
                    verifyText(v)
                    fun rect(id:Int):android.graphics.Rect {
                        val child=v.findViewById<android.view.View>(id)
                        val r=android.graphics.Rect();child.getDrawingRect(r);(v as android.view.ViewGroup).offsetDescendantRectToMyCoords(child,r);return r
                    }
                    val header=rect(R.id.widgetHeader);val content=rect(R.id.widgetBody)
                    check(header.bottom+7*density<=content.top) { "Header overlaps content at $size" }
                    if(weekly && v.findViewById<android.view.View>(R.id.widgetDatesContainer)?.visibility==android.view.View.VISIBLE) {
                        val dates=rect(R.id.widgetDates)
                        check(dates.bottom+7*density<=content.top) { "Date pill touches courses at $size" }
                        for(label in listOf(R.id.weekLabel1,R.id.weekLabel2,R.id.weekLabel3,R.id.weekLabel4,R.id.weekLabel5,R.id.weekLabel6,R.id.weekLabel7)) {
                            if(v.findViewById<android.view.View>(label).visibility==android.view.View.VISIBLE)check(dates.contains(rect(label))) { "Date pill clipped by parent baseline alignment at $size" }
                        }
                    }
                    if(v.findViewById<android.view.View>(R.id.widgetActions)?.visibility==android.view.View.VISIBLE)
                        check(content.bottom<=rect(R.id.widgetActions).top) { "Courses overlap footer at $size" }
                    val controls=listOf(R.id.tvHeader,R.id.widgetSettings,R.id.widgetPrevious,R.id.widgetNext,R.id.widgetThisWeek).mapNotNull { v.findViewById<android.view.View>(it) }.filter { it.visibility==android.view.View.VISIBLE }
                    for(a in controls)for(b in controls)if(a!==b)check(!android.graphics.Rect.intersects(rect(a.id),rect(b.id))) { "Header controls overlap at $size" }
                }
            }
            pass("native widget layouts keep full text in occupied and empty states across seven materials and narrow/short/tall sizes; header, dates and footer never overlap courses")
            } else sendStatus(0, Bundle().apply { putString("stream", "SKIP widget layout matrix (known baseline failure)\n") })
            if (!skipWidgetContent) { WidgetContentChecks.run(targetContext,::main,File(targetContext.filesDir,screenshotFolder)) { message ->
                sendStatus(0,Bundle().apply { putString("stream",message+"\n") })
            }
            pass("full collections retain every course, completed classes, unequal overlaps and long metadata at readable sizes; collection endpoints stay protected")
            } else sendStatus(0, Bundle().apply { putString("stream", "SKIP widget content checks (known baseline failure)\n") })

            main {
                model().upsertReminder(Reminder("link-a", "Test A", 1, 8, 0, 10, "", false, 1, "qa-autumn", "math"))
                model().upsertReminder(Reminder("link-b", "Test B", 1, 9, 0, 10, "", false, 1, "qa-spring", "other"))
                model().setSemesterPeriodConfig("qa-autumn", model().semesters.first().periodStarts.toMutableList().also { it[0] = "08:30" }, 12)
            }
            saved()
            check(model().reminders.first { it.id == "link-a" }.minute == 30)
            check(model().reminders.first { it.id == "link-b" }.hour == 9)
            pass("semester changes update only their linked reminders")

            main { model().restoreLastImport() }
            saved()
            check(model().data == before)
            pass("undo import restores pre-test data")
            main { model().importJson(fixture) }
            saved()

            main { model().openRoute("new-todo", "qa-autumn", "") }
            await { containsText(uiAutomation.rootInActiveWindow,"关联课程（可选）") }
            check(!containsText(uiAutomation.rootInActiveWindow,"原课程已删除"))
            check(clickLabel(uiAutomation.rootInActiveWindow,"取消"))
            pass("widget quick-add opens an unlinked task without a false deleted-course warning")

            // Render all existing primary pages with real Android resources.
            for (screen in Screen.entries.filter { it != Screen.Import }) {
                main { model().go(screen, "qa-autumn") }
                waitForIdleSync()
                val title = when (screen) {
                    Screen.Home -> "我的课表"
                    Screen.Schedule -> "测试秋季"
                    Screen.Reminders -> "课程闹钟"
                    Screen.Theme -> "主题换肤"
                    Screen.Countdown -> "倒计时"
                    Screen.Backup -> "数据备份"
                    Screen.Todos -> "待办"
                    Screen.Widgets -> "小组件工作室"
                    else -> "导入课表"
                }
                sendStatus(0, Bundle().apply { putString("stream", "Checking page: ${screen.name}\n") })
                await { containsText(uiAutomation.rootInActiveWindow, title) }
                Thread.sleep(1200)
                capture("screen-${screen.name}")
                if (screen == Screen.Theme) {
                    check(containsText(uiAutomation.rootInActiveWindow, "内置主题")) { "Theme selection should be visible on entry" }
                    val oldTheme = model().theme
                    await { clickLabel(uiAutomation.rootInActiveWindow, "湖蓝") }
                    await { model().theme == 1 }
                    saved()
                    main { model().setTheme(oldTheme) }
                    saved()
                    check(store.load().theme == oldTheme)
                    val oldMode = model().darkMode
                    await {
                        val root = uiAutomation.rootInActiveWindow
                        if (clickLabel(root, "深色")) true else { swipeUpSmall(); Thread.sleep(400); false }
                    }
                    await { model().darkMode == 2 }
                    saved(); capture("theme-dark")
                    main { model().setDarkMode(oldMode) }
                    saved()
                    pass("theme selection remains visible and persists after the layout update")
                }
            }
            pass("all primary pages render on device")
            await { nativePreviewHas(R.id.todayText) }
            check(!containsText(uiAutomation.rootInActiveWindow, "预览暂时不可用"))
            check(clickLabel(uiAutomation.rootInActiveWindow, "整周课表"))
            await { nativePreviewHas(R.id.widgetDates) }
            check(!containsText(uiAutomation.rootInActiveWindow, "预览暂时不可用"))
            for (label in listOf("紧凑", "展开", "标准")) check(clickLabel(uiAutomation.rootInActiveWindow, label))
            await { nativePreviewHas(R.id.widgetDates) }
            main { model().go(Screen.Todos) }
            await { containsText(uiAutomation.rootInActiveWindow, "待办") }
            main { model().home(); model().go(Screen.Widgets) }
            await { containsText(uiAutomation.rootInActiveWindow, "小组件工作室") && nativePreviewHas(R.id.todayText) }
            PerformanceChecks.run(targetContext) { message -> sendStatus(0, Bundle().apply { putString("stream", message + "\n") }) }
            pass("asynchronous widget preview renders and cached previews invalidate for wallpaper, date and size")
            await { containsText(uiAutomation.rootInActiveWindow, "尺寸仅供预览") }
            capture("widget-size-explanation")
            repeat(8) {
                check(!containsText(uiAutomation.rootInActiveWindow, "小组件按压反馈"))
                if (scrollForward(uiAutomation.rootInActiveWindow)) Thread.sleep(250)
            }
            check(!containsText(uiAutomation.rootInActiveWindow, "小组件按压反馈"))
            pass("widget size preview explains launcher sizing and obsolete press settings are absent")

            main { model().go(Screen.Schedule, "qa-autumn") }
            await { containsText(uiAutomation.rootInActiveWindow, "下一周") }
            Thread.sleep(1200)
            val nextWeek = Wk.currentWeek(Wk.parseISO("2026-11-09")) + 1
            sendStatus(0, Bundle().apply { putString("stream", "Expect next week=$nextWeek at ${Wk.today()}\n") })
            check(clickLabel(uiAutomation.rootInActiveWindow, "下一周")) { "Next-week button is not clickable" }
            await { containsText(uiAutomation.rootInActiveWindow, "第${nextWeek}周") }
            val old = latest.get()
            main { old.recreate() }
            await { latest.get() !== old && !model().isLoading }
            await { containsText(uiAutomation.rootInActiveWindow, "第${nextWeek}周") }
            Thread.sleep(1200)
            check(model().screen == Screen.Schedule && model().selectedSemesterId == "qa-autumn")
            check(model().data == store.load())
            capture("recreated-schedule")
            pass("activity recreation retains the displayed week, navigation and committed data")

            main { latest.get().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            await { latest.get().resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }
            await { containsText(uiAutomation.rootInActiveWindow, "第${nextWeek}周") }
            waitForIdleSync(); Thread.sleep(1200); capture("landscape-schedule")
            main { latest.get().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
            await { latest.get().resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT }
            await { containsText(uiAutomation.rootInActiveWindow, "第${nextWeek}周") }
            waitForIdleSync(); Thread.sleep(1200); capture("returned-portrait-schedule")
            pass("landscape and portrait round trip retains the displayed week")
            if (dailyChecks) { runHomeCourseChecks(); runWidgetLauncherChecks(); runDailyChecks() }
            main { model().importJson(originalBackup!!) }
            saved()
            check(model().data == before && store.load() == before)
            pass("device checks restore the exact original app data")
            main { model().home() }
            finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "\nPASS ${passed.size} device checks\n" + passed.joinToString("\n")) })
        } catch (t: Throwable) {
            if (targetContext.packageName == "com.schedule.app.review") runCatching { capture("failure") }
            originalBackup?.let { backup -> runCatching { main { model().importJson(backup) }; saved() } }
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("stream", "\nFAILED after ${passed.size} checks: ${t.stackTraceToString()}") })
        }
    }
    private fun runHomeCourseChecks() {
        val data = WidgetContentChecks.fixture(true)
        val sem = data.semesters.single()
        val focus = checkNotNull(OccurrenceRules.upcoming(data, sem, java.time.ZonedDateTime.now(), 1).firstOrNull())
        main { model().importJson(BackupCodec.encode(BackupPayload(data, null, null))) }; saved()
        main { model().home() }
        await { containsText(uiAutomation.rootInActiveWindow, focus.course.name) }
        capture("home-courses")
        check(clickLabel(uiAutomation.rootInActiveWindow, focus.course.name)) { "Home course card could not be opened" }
        await { containsText(uiAutomation.rootInActiveWindow, "课程详情") }
        check(model().screen == Screen.Home) { "Home course opened the full timetable first" }
        check(containsText(uiAutomation.rootInActiveWindow, focus.course.room))
        capture("home-course-detail")
        check(clickLabel(uiAutomation.rootInActiveWindow, "编辑常规课程")) { "Animated detail edit action unavailable" }
        await { containsText(uiAutomation.rootInActiveWindow, "编辑课程") }
        check(clickLabel(uiAutomation.rootInActiveWindow, "取消"))
        await { !containsText(uiAutomation.rootInActiveWindow, "编辑课程") }
        await { clickLabel(uiAutomation.rootInActiveWindow, focus.course.name) }
        await { containsText(uiAutomation.rootInActiveWindow, "下滑关闭课程详情") }
        Thread.sleep(500)
        check(swipeDownLabel(uiAutomation.rootInActiveWindow, "下滑关闭课程详情"))
        await { !containsText(uiAutomation.rootInActiveWindow, "课程详情") }
        await { clickLabel(uiAutomation.rootInActiveWindow, focus.course.name) }
        await { containsText(uiAutomation.rootInActiveWindow, "课程详情") }
        Thread.sleep(500)
        sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        await { !containsText(uiAutomation.rootInActiveWindow, "课程详情") }
        check(model().screen == Screen.Home)
        val longClass = sem.courses.first { it.id == "long" }
        await { clickLabel(uiAutomation.rootInActiveWindow, longClass.name) }
        await { containsText(uiAutomation.rootInActiveWindow, "课程详情") && containsText(uiAutomation.rootInActiveWindow, longClass.room) }
        capture("home-long-course-detail")
        await { clickLabel(uiAutomation.rootInActiveWindow, "关闭") }
        await { !containsText(uiAutomation.rootInActiveWindow, "课程详情") }
        await { clickLabel(uiAutomation.rootInActiveWindow, focus.course.name) }
        await { containsText(uiAutomation.rootInActiveWindow, "课程详情") }
        sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        await { !containsText(uiAutomation.rootInActiveWindow, "课程详情") }
        check(model().screen == Screen.Home)
        await { clickLabel(uiAutomation.rootInActiveWindow, focus.course.name) }
        await { containsText(uiAutomation.rootInActiveWindow, "课程详情") }
        val activityBeforeRecreate = latest.get()
        main { activityBeforeRecreate.recreate() }
        await { latest.get() !== activityBeforeRecreate && !model().isLoading }
        await { containsText(uiAutomation.rootInActiveWindow, "课程详情") }
        check(model().screen == Screen.Home)
        await { containsText(uiAutomation.rootInActiveWindow, focus.course.room) }
        await { clickLabel(uiAutomation.rootInActiveWindow, "编辑常规课程") }
        await { containsText(uiAutomation.rootInActiveWindow, "编辑课程") }
        check(clickLabel(uiAutomation.rootInActiveWindow, "取消"))
        await { !containsText(uiAutomation.rootInActiveWindow, "编辑课程") }
        check(model().screen == Screen.Home)
        await { clickLabel(uiAutomation.rootInActiveWindow, focus.course.name) }
        await { containsText(uiAutomation.rootInActiveWindow, "课程详情") }
        await { clickLabel(uiAutomation.rootInActiveWindow, "关闭") }
        await { !containsText(uiAutomation.rootInActiveWindow, "课程详情") }
        check(model().screen == Screen.Home)
        pass("home course opens, edits and closes its detail without navigating away")
    }
    private fun runWidgetLauncherChecks() {
        val data=WidgetContentChecks.fixture(true);val sem=data.semesters.single()
        main { model().importJson(BackupCodec.encode(BackupPayload(data,null,null))) };saved()
        CourseWidgetProvider.updateAll(targetContext)
        fun launcher() {
            uiAutomation.injectInputEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_HOME),true)
            uiAutomation.injectInputEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_HOME),true)
            await { uiAutomation.rootInActiveWindow?.packageName?.toString()=="com.android.launcher3" }
        }
        launcher()
        await { containsText(uiAutomation.rootInActiveWindow,"示例课程戊戊戊戊戊实验") }
        capture("launcher-full-week-and-today")
        val o=OccurrenceRules.onDate(data,sem,java.time.LocalDate.now()).first { it.base.id=="overlap" }
        val from=PeriodTimes.start(data.periodStarts,o.course.startPeriod)
        val until=PeriodTimes.start(data.periodStarts,o.course.endPeriod).plusMinutes(45)
        check(clickLabel(uiAutomation.rootInActiveWindow,"${o.course.name}，$from 至 $until，${o.course.room}，${o.course.teacher}")) { "Daily collection item was not clickable" }
        await { containsText(uiAutomation.rootInActiveWindow,"调整这一次 / 补课") && containsText(uiAutomation.rootInActiveWindow,"第1-2节") }
        check(containsText(uiAutomation.rootInActiveWindow,"B302"));capture("daily-collection-correct-course")
        await { clickLabel(uiAutomation.rootInActiveWindow,"关闭") };launcher()
        val long=sem.courses.first { it.id=="long" }
        val weeklyDescription="${com.schedule.app.ui.weekdayFull(long.weekday)} ${long.name}，${com.schedule.app.ui.periodLabel(long.startPeriod,long.endPeriod)}，${long.room}，${long.teacher}"
        check(clickLabel(uiAutomation.rootInActiveWindow,weeklyDescription)) { "Weekly collection course was not clickable" }
        await { containsText(uiAutomation.rootInActiveWindow,"第1-4节") && containsText(uiAutomation.rootInActiveWindow,"调整这一次 / 补课") }
        check(containsText(uiAutomation.rootInActiveWindow,long.room));capture("weekly-collection-correct-long-course")
        await { clickLabel(uiAutomation.rootInActiveWindow,"关闭") };launcher()
        check(clickLabel(uiAutomation.rootInActiveWindow,"组件下一周"))
        await { containsText(uiAutomation.rootInActiveWindow,"第2周") }
        check(clickLabel(uiAutomation.rootInActiveWindow,"组件回本周"))
        await { containsText(uiAutomation.rootInActiveWindow,"第1周") }
        val manager=android.appwidget.AppWidgetManager.getInstance(targetContext)
        for(weekly in listOf(false,true)) {
            val type=if(weekly)AgendaWidgetProvider::class.java else CourseWidgetProvider::class.java
            val id=manager.getAppWidgetIds(android.content.ComponentName(targetContext,type)).first()
            val factory=WidgetCollectionService.Factory(targetContext,WidgetCollectionService.intent(targetContext,id,weekly,android.util.SizeF(332f,460f)))
            factory.onCreate();factory.onDataSetChanged()
            check(factory.count==if(weekly)1 else 4);check(factory.getViewAt(0)!=null)
            factory.onDestroy();check(factory.count==0)
        }
        val invalid=WidgetCollectionService.Factory(targetContext,WidgetCollectionService.intent(targetContext,-123,false,android.util.SizeF(332f,303f)))
        invalid.onDataSetChanged();check(invalid.count==0)
        pass("launcher collection clicks open the correct short and long overlapping courses; week navigation and protected legacy factories work")
        AppRoutes.pending(targetContext,"home").send()
        await { uiAutomation.rootInActiveWindow?.packageName?.toString()==targetContext.packageName && containsText(uiAutomation.rootInActiveWindow,"我的课表") }
    }
    private fun runDailyChecks() {
        val today = java.time.LocalDate.now()
        val monday = Wk.mondayOf(today)
        val c = Course("daily-long", "示例课程戊戊戊戊戊实验", "", "王老师", "示例实验楼-XX3115", today.dayOfWeek.value, 1, 4, "1-16")
        val short = Course("daily-short", "电路分析", "", "李老师", "B302", today.dayOfWeek.value, 1, 2, "1-16")
        val sem = Semester("daily", "日常流程测试", monday.toString(), listOf(c, short))
        val initial = BackupCodec.emptyData().copy(semesters = listOf(sem), homeSemesterId = sem.id)
        main { model().importJson(BackupCodec.encode(BackupPayload(initial, null, null))) }
        saved()
        main { model().batchReminders(sem.id, setOf(c.id, short.id), 10, false) }
        saved()
        val first = model().reminders
        main { model().upsertReminder(first.first().copy(leadMinutes = 25, enabled = false)); model().batchReminders(sem.id, setOf(c.id, short.id), 5, false) }
        saved()
        check(model().reminders.size == 2 && model().reminders.first().leadMinutes == 25 && !model().reminders.first().enabled)
        pass("batch reminders preserve individual exceptions without duplicates")

        val change = CourseChange("daily-change", sem.id, c.id, today.toString(), today.plusDays(1).toString(), "move", 3, 5, "QA-9001")
        main { model().upsertCourseChange(change) }; saved()
        check(OccurrenceRules.onDate(model().data, sem, today).none { it.base.id == c.id })
        check(OccurrenceRules.onDate(model().data, sem, today.plusDays(1)).single().course.room == "QA-9001")
        main { model().openRoute("course", sem.id, c.id, change.date, occurrence = "change:${change.id}") }
        await { containsText(uiAutomation.rootInActiveWindow, "第3-5节") && containsText(uiAutomation.rootInActiveWindow, "调整这一次 / 补课") }
        check(containsText(uiAutomation.rootInActiveWindow, "QA-9001"))
        capture("moved-course-detail")
        check(clickLabel(uiAutomation.rootInActiveWindow, "调整这一次 / 补课"))
        await { containsText(uiAutomation.rootInActiveWindow, "保存本次调整") }
        val activity = latest.get()
        main { activity.recreate() }
        await { latest.get() !== activity && !model().isLoading }
        await { containsText(uiAutomation.rootInActiveWindow, "保存本次调整") }
        check(clickLabel(uiAutomation.rootInActiveWindow, "取消"))
        check(model().data.courseChanges.single() == change)
        pass("rescheduled date opens the correct details and editing survives recreation")
        main { model().deleteCourseChange(change.id) }; saved()
        check(OccurrenceRules.onDate(model().data, sem, today).size == 2)
        main { model().openRoute("day", sem.id, date = today.toString()) }
        await { containsText(uiAutomation.rootInActiveWindow, "与 1 门课程时间重叠") }
        capture("overlap-day-agenda")
        check(clickLabel(uiAutomation.rootInActiveWindow, "关闭"))
        pass("restoring one exception restores the original overlap and daily view")

        val manager = targetContext.getSystemService(android.appwidget.AppWidgetManager::class.java)
        listOf(CourseWidgetProvider::class.java, AgendaWidgetProvider::class.java).forEach { type ->
            manager.getAppWidgetIds(android.content.ComponentName(targetContext, type)).forEach { id ->
                sendStatus(0, Bundle().apply { putString("stream", "Widget $id ${type.simpleName}: ${manager.getAppWidgetOptions(id)}\n") })
            }
        }
        check(targetContext.getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms()) { "Grant exact alarms to the isolated emulator app before the timed check" }
        val fire = java.time.ZonedDateTime.now().plusSeconds(35).withSecond(0).withNano(0).plusMinutes(1)
        val timedCourse = c.copy(name = "组件准点刷新测试", weekday = fire.dayOfWeek.value, startPeriod = 1, endPeriod = 1)
        val timedSem = sem.copy(startDate = Wk.mondayOf(fire.toLocalDate()).toString(), courses = listOf(timedCourse), periodStarts = listOf(fire.toLocalTime().toString()))
        val task = Todo("daily-task", "作业通知实测", null, fire.toLocalDate().toString(), false, sem.id, c.id, fire.toLocalTime().toString(), 0)
        val timed = initial.copy(semesters = listOf(timedSem), todos = listOf(task))
        main { model().importJson(BackupCodec.encode(BackupPayload(timed, null, null))) }; saved()
        CourseWidgetProvider.updateAll(targetContext)
        uiAutomation.injectInputEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_HOME), true)
        uiAutomation.injectInputEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_HOME), true)
        await { uiAutomation.rootInActiveWindow?.packageName?.toString() == "com.android.launcher3" && containsText(uiAutomation.rootInActiveWindow, "组件准点刷新测试") }
        check(!containsText(uiAutomation.rootInActiveWindow, "正在上课"))
        sendStatus(0, Bundle().apply { putString("stream", "Waiting for actual notification and widget boundary at $fire\n") })
        capture("widget-before-class-start")
        val until = fire.toInstant().toEpochMilli() + 15000
        val notifications = targetContext.getSystemService(android.app.NotificationManager::class.java)
        while (System.currentTimeMillis() < until && (notifications.activeNotifications.none { it.tag == "todo:${task.id}" } || !containsText(uiAutomation.rootInActiveWindow, "正在上课"))) {
            Thread.sleep(1000)
        }
        check(notifications.activeNotifications.any { it.tag == "todo:${task.id}" }) { "Deadline notification did not arrive" }
        check(containsText(uiAutomation.rootInActiveWindow, "正在上课")) { "Widget did not refresh at class start" }
        capture("widget-after-class-start")
        pass("actual deadline notification and widget class boundary refresh arrive")
        notifications.activeNotifications.first { it.tag == "todo:${task.id}" }.notification.contentIntent.send()
        await { containsText(uiAutomation.rootInActiveWindow, "编辑作业 / 待办") }
        check(containsText(uiAutomation.rootInActiveWindow, task.title))
        capture("notification-opens-linked-task")
        check(clickLabel(uiAutomation.rootInActiveWindow, "取消"))
        main { model().upsertTodo(task.copy(done = true)) }; saved()
        check(notifications.activeNotifications.none { it.tag == "todo:${task.id}" })
        check(TodoRules.nextMillis(model().todos.single()) == -1L)
        pass("notification opens the correct task and completion removes its alert")
    }
    private fun nativePreviewHas(id: Int): Boolean {
        var found = false
        main {
            // AppWidgetHostView is an ID namespace boundary; explicitly descend into its content.
            fun has(view: android.view.View): Boolean = view.id == id ||
                (view is android.view.ViewGroup && (0 until view.childCount).any { has(view.getChildAt(it)) })
            found = has(latest.get().window.decorView)
        }
        return found
    }
    private fun model(): AppViewModel {
        var result: AppViewModel? = null
        main { result = ViewModelProvider(latest.get())[AppViewModel::class.java] }
        return result!!
    }
    private fun main(block: () -> Unit) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) block() else {
            val error = AtomicReference<Throwable?>()
            runOnMainSync { try { block() } catch (t: Throwable) { error.set(t) } }
            error.get()?.let { throw it }
        }
    }
    private fun await(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 20000
        while (!condition()) { check(System.currentTimeMillis() < deadline) { "Device check timed out" }; Thread.sleep(50) }
    }
    private fun saved() { await { !model().isSaving }; waitForIdleSync() }
    private fun containsText(node: android.view.accessibility.AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        node.refresh()
        if (node.text?.toString()?.contains(text) == true || node.contentDescription?.toString()?.contains(text) == true) return true
        return (0 until node.childCount).any { containsText(node.getChild(it), text) }
    }
    private fun isEnabled(node: android.view.accessibility.AccessibilityNodeInfo?, label: String): Boolean {
        if (node == null) return false
        if (node.text?.toString() == label) {
            var ancestor = node
            while (ancestor != null) {
                if (!ancestor.isEnabled) return false
                if (ancestor.isClickable) return true
                ancestor = ancestor.parent
            }
            return false
        }
        return (0 until node.childCount).any { isEnabled(node.getChild(it), label) }
    }
    private fun setFirstEditableText(node: android.view.accessibility.AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        if (node.isEditable) {
            val args = Bundle().apply { putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
            return node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        return (0 until node.childCount).any { setFirstEditableText(node.getChild(it), text) }
    }
    private fun clickFirstPhoto(node: android.view.accessibility.AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.contentDescription?.toString()?.startsWith("Photo taken on ") == true && node.isClickable) {
            return node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
        }
        return (0 until node.childCount).any { clickFirstPhoto(node.getChild(it)) }
    }
    private fun clickLabel(node: android.view.accessibility.AccessibilityNodeInfo?, label: String): Boolean {
        if (node == null) return false
        if (node.contentDescription?.toString() == label || node.text?.toString() == label) {
            var clickable = node
            while (clickable != null) {
                if (clickable.isClickable) return clickable.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                clickable = clickable.parent
            }
        }
        return (0 until node.childCount).any { clickLabel(node.getChild(it), label) }
    }
    private fun scrollForward(node: android.view.accessibility.AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isScrollable && node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return true
        return (0 until node.childCount).any { scrollForward(node.getChild(it)) }
    }
    private fun swipeUpSmall() {
        val metrics = targetContext.resources.displayMetrics
        val x = metrics.widthPixels * .5f
        val startY = metrics.heightPixels * .72f
        val distance = metrics.heightPixels * .22f
        val start = android.os.SystemClock.uptimeMillis()
        fun event(action: Int, y: Float) {
            val e = android.view.MotionEvent.obtain(start, android.os.SystemClock.uptimeMillis(), action, x, y, 0)
            e.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
            uiAutomation.injectInputEvent(e, true); e.recycle()
        }
        event(android.view.MotionEvent.ACTION_DOWN, startY)
        repeat(8) { i -> Thread.sleep(20); event(android.view.MotionEvent.ACTION_MOVE, startY - distance * (i + 1) / 8) }
        event(android.view.MotionEvent.ACTION_UP, startY - distance)
    }
    private fun swipeDownLabel(node: android.view.accessibility.AccessibilityNodeInfo?, label: String): Boolean {
        if (node == null) return false
        if (node.contentDescription?.toString() == label) {
            val bounds = android.graphics.Rect(); node.getBoundsInScreen(bounds)
            val x = bounds.exactCenterX(); val y = bounds.exactCenterY()
            val start = android.os.SystemClock.uptimeMillis()
            fun event(action: Int, at: Long, dy: Float) {
                val e = android.view.MotionEvent.obtain(start, at, action, x, y + dy, 0)
                e.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
                uiAutomation.injectInputEvent(e, true); e.recycle()
            }
            event(android.view.MotionEvent.ACTION_DOWN, start, 0f)
            val distance = 100f * targetContext.resources.displayMetrics.density
            repeat(8) { i -> Thread.sleep(20); event(android.view.MotionEvent.ACTION_MOVE, android.os.SystemClock.uptimeMillis(), distance * (i + 1) / 8) }
            event(android.view.MotionEvent.ACTION_UP, android.os.SystemClock.uptimeMillis(), distance)
            return true
        }
        return (0 until node.childCount).any { swipeDownLabel(node.getChild(it), label) }
    }
    private fun pass(message: String) { passed.add(message); sendStatus(0, Bundle().apply { putString("stream", "PASS $message\n") }) }
    private fun capture(name: String) {
        waitForIdleSync()
        Thread.sleep(500) // Accessibility can settle before the launcher finishes drawing its frame.
        val screenshot = checkNotNull(uiAutomation.takeScreenshot()) { "Screenshot unavailable" }
        val folder = File(targetContext.filesDir, screenshotFolder).apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
    }
}
