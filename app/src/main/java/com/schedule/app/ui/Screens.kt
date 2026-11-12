package com.schedule.app.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.Screen
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope
import com.schedule.app.ManualMap
import androidx.compose.runtime.saveable.rememberSaveable
import com.schedule.app.data.CsvReader
import com.schedule.app.data.Parser
import com.schedule.app.data.Store
import com.schedule.app.data.Wk
import com.schedule.app.data.OccurrenceRules
import com.schedule.app.data.XlsxReader
import com.schedule.app.model.Course
import com.schedule.app.model.Semester
import java.util.UUID

/** 统一的卡片圆角。 */
private val CardShape = RoundedCornerShape(18.dp)

// ===========================================================================
// 首页
// ===========================================================================

@Composable
fun HomeScreen(vm: com.schedule.app.AppViewModel) = HomeExperience(vm)

@Composable
internal fun SectionTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SemesterCard(vm: com.schedule.app.AppViewModel, sem: Semester) {
    val today = remember { Wk.today() }
    val isHome = vm.homeSemesterId == sem.id
    val cw = Wk.currentWeek(Wk.parseISO(sem.startDate))
    val maxWeek = maxOf(
        sem.courses.maxOfOrNull { c -> Regex("\\d+").findAll(c.weeks).maxOfOrNull { it.value.toInt() } ?: 0 } ?: 0,
        sem.courses.maxOfOrNull { it.endPeriod } ?: 0,
        1
    )
    val (stateText, stateColor) = when {
        cw in 1..maxWeek -> "进行中" to MaterialTheme.colorScheme.primary
        cw < 1 -> "未开始" to Color(0xFF8A90A0)
        else -> "已结束" to Color(0xFF9AA0AE)
    }
    Card(onClick = { vm.go(Screen.Schedule, sem.id) }, modifier = Modifier.fillMaxWidth().pressMotion(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ViewWeek, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text(sem.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(22.dp))
            }
            Text("${sem.courses.size} 门课 · 开学 ${sem.startDate} · $stateText", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            TextButton(onClick = { vm.setHomeSemester(if (isHome) null else sem.id) }) { Text(if (isHome) "主页课表 ✓" else "设为主页", fontSize = 12.sp) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodoCard(vm: com.schedule.app.AppViewModel) {
    val pending = vm.todos.count { !it.done }
    val overdue = vm.todos.count { t ->
        !t.done && t.dueDate?.let { d ->
            try { java.time.LocalDate.parse(d).isBefore(Wk.today()) } catch (e: Exception) { false }
        } == true
    }
    Card(onClick = { vm.go(Screen.Todos) }, modifier = Modifier.fillMaxWidth().pressMotion(), shape = CardShape) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("待办 / 备忘", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    when {
                        pending == 0 -> "没有待办事项，点这里新建"
                        overdue > 0 -> "还有 $pending 项待完成，$overdue 项已逾期"
                        else -> "还有 $pending 项待完成"
                    },
                    color = if (overdue > 0) Color(0xFFD25A5A) else Color(0xFF8A90A0), fontSize = 13.sp
                )
            }
            if (pending > 0) {
                Box(
                    Modifier.clip(RoundedCornerShape(999.dp)).background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 9.dp, vertical = 2.dp)
                ) { Text("$pending", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(10.dp))
            }
            Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFFB8BECC), modifier = Modifier.size(22.dp))
        }
    }
}

/** 最近的倒计时（首页卡片，无倒计时时不出现在布局里）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NextCountdownCard(vm: com.schedule.app.AppViewModel) {
    val nearest = vm.countdowns.mapNotNull { c ->
        val days = try { Wk.daysBetween(Wk.today(), java.time.LocalDate.parse(c.date)) } catch (e: Exception) { null }
        if (days != null && days >= 0) c to days else null
    }.minByOrNull { it.second } ?: return
    Spacer(Modifier.height(14.dp))
    Card(onClick = { vm.go(Screen.Countdown) }, modifier = Modifier.fillMaxWidth().pressMotion(), shape = CardShape) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("倒计时 · ${nearest.first.title}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(nearest.first.date, color = Color(0xFF8A90A0), fontSize = 13.sp)
            }
            Text(
                if (nearest.second == 0L) "今天" else "${nearest.second}天",
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 17.sp
            )
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFFB8BECC), modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
internal fun QuickActions(vm: com.schedule.app.AppViewModel) {
    val tiles = listOf(
        Triple("课程闹钟", Icons.Filled.Alarm, Screen.Reminders),
        Triple("倒计时", Icons.Filled.Schedule, Screen.Countdown),
        Triple("数据备份", Icons.Filled.Save, Screen.Backup),
        Triple("导入课表", Icons.Filled.Add, Screen.Import),
    )
    tiles.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { (label, icon, screen) ->
                QuickTile(label, icon, Modifier.weight(1f)) { vm.go(screen) }
            }
            repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun QuickTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && LocalMotionLevel.current > 0) 0.96f else 1f, spatialSpring(), label = "press")
    val haptic = LocalHapticFeedback.current
    Card(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        interactionSource = src,
        shape = CardShape,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** 横向长条入口（如「导入课表」）：交互风格与快捷块一致，只是形状为通栏。 */
@Composable
private fun WideTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && LocalMotionLevel.current > 0) 0.97f else 1f, spatialSpring(), label = "pressWide")
    val haptic = LocalHapticFeedback.current
    Card(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        interactionSource = src,
        shape = CardShape,
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(10.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ===========================================================================
// 学期 / 周块状课表
// ===========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterScreen(vm: com.schedule.app.AppViewModel) {
    val sem = vm.selectedSemester
    if (sem == null) {
        LaunchedEffect(Unit) { if (vm.screen == Screen.Schedule && vm.selectedSemester == null) vm.home() }
        Box(Modifier.fillMaxSize())
        return
    }
    // 默认落在真实"本周"：开学前/结束后允许 0 或负数周（与小组件"第0周"、回本周一致）
    var week by rememberSaveable(sem.id) { mutableIntStateOf(Wk.currentWeek(Wk.parseISO(sem.startDate))) }
    var showList by rememberSaveable { mutableStateOf(false) }
    var toolsMenu by remember { mutableStateOf(false) }
    val weekMotion = motionDuration(260)
    var dialogCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    val dialogCourse = sem.courses.firstOrNull { it.id == dialogCourseId }
    var occurrenceKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showChanges by rememberSaveable { mutableStateOf(false) }
    var dayToShow by rememberSaveable { mutableStateOf<Int?>(null) }
    val occurrences = remember(vm.data, sem, week) { OccurrenceRules.inWeek(vm.data, sem, week) }
    val selectedOccurrence = occurrences.find { it.key == occurrenceKey }
    var dialogEdit by rememberSaveable { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var editMode by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showPeriodSettings by rememberSaveable { mutableStateOf(false) }
    var confirmDeleteSem by rememberSaveable { mutableStateOf(false) }
    // 本学期生效的节次设置（学期未单独设置时跟随全局）——课表/弹窗/提醒预览统一用它
    val effStarts = effectivePeriodStarts(sem, vm.data.periodStarts)
    val effPerDay = effectivePeriodsPerDay(sem, vm.data.periodsPerDay)
    LaunchedEffect(vm.pendingRoute) {
        vm.pendingRoute?.takeIf { it[0] in setOf("course", "day", "week") && it[1] == sem.id }?.let { route ->
            val date = Wk.parseDateOrNull(route[3]) ?: return@let
            week = OccurrenceRules.week(sem, date)
            val occurrence = OccurrenceRules.onDate(vm.data, sem, date).find { it.base.id == route[2] && (route.getOrNull(5).isNullOrEmpty() || it.key == route[5]) }
            dialogCourseId = occurrence?.base?.id; dialogEdit = false; showList = false
            occurrenceKey = occurrence?.key
            if (route[0] == "day" || (route[0] == "course" && occurrence == null)) dayToShow = date.dayOfWeek.value
            vm.clearRoute()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = topBarTransparent(),
                title = {
                    var titleMenu by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(
                                enabled = vm.semesters.size > 1,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { titleMenu = true }
                        ) {
                            Text(sem.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (vm.semesters.size > 1) {
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Filled.ArrowDropDown, "切换学期", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        DropdownMenu(expanded = titleMenu, onDismissRequest = { titleMenu = false }) {
                            vm.semesters.sortedByDescending { it.startDate }.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text((if (s.id == sem.id) "● " else "○ ") + s.title) },
                                    onClick = { vm.selectedSemesterId = s.id; titleMenu = false }
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showList = !showList }, modifier = Modifier.pressMotion()) { Icon(Icons.Filled.ViewWeek, "列表") }
                    IconButton(onClick = { toolsMenu = true }) { Icon(Icons.Filled.MoreVert, "课表选项") }
                    DropdownMenu(expanded = toolsMenu, onDismissRequest = { toolsMenu = false }) {
                        DropdownMenuItem(text = { Text("单次调整") }, onClick = { toolsMenu = false; showChanges = true })
                        DropdownMenuItem(text = { Text(if (editMode) "退出修改" else "修改课程") }, onClick = { toolsMenu = false; editMode = !editMode })
                        DropdownMenuItem(text = { Text("设置开学日期") }, onClick = { toolsMenu = false; showSettings = true })
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (editMode) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(motionDuration(220))) + expandVertically(tween(motionDuration(280))),
                    exit = fadeOut(tween(motionDuration(140))) + shrinkVertically(tween(motionDuration(220)))
                ) {
                    Box(
                        Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "修改模式已开启：点击课程即可编辑或删除",
                            color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            if (showList) {
                CourseList(sem.courses, Modifier.weight(1f), onOpen = { dialogCourseId = it.id; occurrenceKey = occurrences.find { o -> o.base.id == it.id }?.key; dialogEdit = editMode })
            } else {
                AnimatedContent(
                    targetState = week,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(tween(weekMotion)) { dir * it / 4 } + fadeIn(tween(weekMotion)) + scaleIn(tween(weekMotion),initialScale=.975f)) togetherWith
                            (slideOutHorizontally(tween(weekMotion)) { -dir * it / 10 } + fadeOut(tween(weekMotion/2)) + scaleOut(tween(weekMotion),targetScale=.985f))
                    },
                    label = "week"
                ) { w ->
                    val actual = remember(vm.data, sem, w) { OccurrenceRules.inWeek(vm.data, sem, w) }
                    WeekGrid(
                        sem = sem.copy(showSaturday = sem.showSaturday || actual.any { it.changeId != null && it.course.weekday == 6 },
                            showSunday = sem.showSunday || actual.any { it.changeId != null && it.course.weekday == 7 }), week = w,
                        displayCourses = actual.map { it.course.copy(id = it.key) },
                        modifier = Modifier.fillMaxSize(),
                        periodStarts = effStarts,
                        periodsPerDay = maxOf(effPerDay, actual.filter { it.changeId != null }.maxOfOrNull { it.course.endPeriod } ?: 0),
                        blockOpacity = vm.blockOpacity,
                        onTap = { shown -> actual.find { it.key == shown.id }?.let { dialogCourseId = it.base.id; occurrenceKey = it.key; dialogEdit = editMode } },
                        onDay = { dayToShow = it },
                        onWeek = { week = it }
                    )
                }
            }
            WeekNavBar(week, start = Wk.parseISO(sem.startDate), onWeek = { week = it })
            if (editMode) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { adding = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("添加课程")
                    }
                    OutlinedButton(onClick = { confirmDeleteSem = true }, modifier = Modifier.weight(1f)) { Text("删除学期", color = Color(0xFFD25A5A)) }
                }
            }
        }
    }

    if (confirmDeleteSem) {
        ConfirmDelete(
            title = "删除学期",
            text = "「${sem.title}」及其全部 ${sem.courses.size} 条课程将被删除，无法恢复。",
            onDismiss = { confirmDeleteSem = false },
            onConfirm = {
                confirmDeleteSem = false
                vm.deleteSemester(sem.id)
            }
        )
    }

    if (showSettings) {
        SemesterSettingsDialog(
            sem = sem,
            periodStarts = effStarts,
            periodsPerDay = effPerDay,
            onDismiss = { showSettings = false },
            onSave = { d, sat, sun ->
                vm.upsertSemester(sem.copy(startDate = d, showSaturday = sat, showSunday = sun))
                showSettings = false
            },
            onOpenPeriodSettings = {
                showSettings = false
                showPeriodSettings = true
            }
        )
    }

    if (showPeriodSettings) {
        PeriodSettingsDialog(
            currentStarts = effStarts,
            perDay = effPerDay,
            maxEndPeriod = sem.courses.maxOfOrNull { it.endPeriod } ?: 0,
            onDismiss = { showPeriodSettings = false },
            onApply = { starts, perDay ->
                // 每学期独立的节次时间表：写回本学期，并同步关联提醒 + 重排闹钟
                vm.setSemesterPeriodConfig(sem.id, starts, perDay)
            }
        )
    }

    if (showChanges) CourseChangesDialog(vm, sem) { showChanges = false }
    dayToShow?.let { day ->
        val date = Wk.weekDate(Wk.parseISO(sem.startDate), week, day)
        DayAgendaDialog(vm, sem, date, onDismiss = { dayToShow = null }, onOpen = {
            dayToShow = null; dialogCourseId = it.base.id; occurrenceKey = it.key; dialogEdit = false
        })
    }
    if (dialogCourse != null && !dialogEdit && !adding) {
        CourseDetails(vm, sem, dialogCourse, selectedOccurrence,
            onDismiss = { dialogCourseId = null; occurrenceKey = null }, onEdit = { dialogEdit = true })
    }
    if (adding || (dialogCourse != null && dialogEdit)) {
        val isEdit = adding || dialogEdit
        CourseDialog(
            course = if (adding) null else dialogCourse,
            editable = isEdit,
            periodStarts = effStarts,
            onDismiss = { dialogCourseId = null; adding = false },
            onDelete = if (adding || !isEdit) null else { { id ->
                vm.upsertSemester(sem.copy(courses = sem.courses.filterNot { it.id == id }))
                dialogCourseId = null
            } },
            onConfirm = { c ->
                if (adding) vm.upsertSemester(sem.copy(courses = sem.courses + c))
                else vm.upsertSemester(sem.copy(courses = sem.courses.map { if (it.id == c.id) c else it }))
                dialogCourseId = null; adding = false
            }
        )
    }
}

@Composable
private fun WeekNavBar(week: Int, start: java.time.LocalDate, onWeek: (Int) -> Unit) {
    val firstDay = try { Wk.weekDate(start, week, 1) } catch (e: Exception) { start }
    val cur = Wk.currentWeek(start)
    val haptic = LocalHapticFeedback.current
    var showJump by remember { mutableStateOf(false) }
    if (showJump) {
        var input by remember { mutableStateOf(week.toString()) }
        AlertDialog(
            onDismissRequest = { showJump = false },
            title = { Text("跳转到指定周") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("输入周数后跳转。0 或负数表示开学之前的周。", color = Color(0xFF8A90A0), fontSize = 12.sp)
                    OutlinedTextField(
                        input, { input = it.filter { c -> c.isDigit() || c == '-' } },
                        label = { Text("周数") }, singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = input.toIntOrNull()
                    if (v != null) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onWeek(v); showJump = false }
                }) { Text("跳转") }
            },
            dismissButton = { TextButton(onClick = { showJump = false }) { Text("取消") } }
        )
    }
    Surface(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .95f), shadowElevation = 3.dp) {
        Row(Modifier.height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onWeek(week - 1) }, modifier = Modifier.pressMotion()) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上一周") }
            Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { showJump = true }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("第${week}周 ⌄", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("${firstDay.monthValue}/${firstDay.dayOfMonth} — ${firstDay.plusDays(6).monthValue}/${firstDay.plusDays(6).dayOfMonth}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { onWeek(cur) }, enabled = week != cur) { Text(if (week == cur) "本周" else "回本周", fontSize = 12.sp) }
            IconButton(onClick = { onWeek(week + 1) }, modifier = Modifier.pressMotion()) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下一周") }
        }
    }
}

@Composable
private fun SemesterSettingsDialog(
    sem: Semester,
    periodStarts: List<String>,
    periodsPerDay: Int,
    onDismiss: () -> Unit,
    onSave: (String, Boolean, Boolean) -> Unit,
    onOpenPeriodSettings: () -> Unit
) {
    var date by rememberSaveable { mutableStateOf(sem.startDate) }
    var err by remember { mutableStateOf(false) }
    var showSat by rememberSaveable { mutableStateOf(sem.showSaturday) }
    var showSun by rememberSaveable { mutableStateOf(sem.showSunday) }
    val parsed = remember(date) { Wk.parseDateOrNull(date) }
    val monday = parsed?.let { Wk.mondayOf(it) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("学期设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("第一周周一的日期，用于计算每周与今日所在周。", color = Color(0xFF8A90A0), fontSize = 12.sp)
                OutlinedTextField(
                    date, { date = it; err = false },
                    label = { Text("第一周周一  yyyy-MM-dd") }, singleLine = true,
                    isError = err,
                    supportingText = { if (err) Text("格式应为 yyyy-MM-dd，例如 2026-09-07", color = Color(0xFFD25A5A), fontSize = 12.sp) }
                )
                if (parsed != null && monday != null && monday != parsed) {
                    Text(
                        "${parsed} 是${weekdayFull(Wk.dayOfWeek(parsed))}，将自动按所在周的周一 $monday 计算，星期与日期一一对应。",
                        color = MaterialTheme.colorScheme.primary, fontSize = 12.sp
                    )
                }
                // 周六/周日显示开关（每学期独立）：关掉的列不显示，其余列均分宽度
                Text("显示星期（本学期独立）", color = Color(0xFF8A90A0), fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("显示周六", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = showSat, onCheckedChange = { showSat = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("显示周日", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = showSun, onCheckedChange = { showSun = it })
                }
                OutlinedButton(onClick = onOpenPeriodSettings, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Schedule, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("节次时间与每天节数（当前 ${periodsPerDay} 节）")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (monday != null) onSave(monday.toString(), showSat, showSun)
                else err = true
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/** 节次时间与每天节数设置：草稿确认后一次性保存，取消不修改数据。 */
@Composable
private fun PeriodSettingsDialog(
    currentStarts: List<String>,
    perDay: Int,
    maxEndPeriod: Int,
    onDismiss: () -> Unit,
    onApply: (List<String>, Int) -> Unit
) {
    var count by rememberSaveable { mutableStateOf(perDay.coerceIn(4, 16)) }
    var times by rememberSaveable {
        mutableStateOf<List<String>>(List(16) { i -> currentStarts.getOrNull(i) ?: fmtTime(defaultPeriodStart(i + 1)) })
    }
    fun currentConfig(): List<String> = times.take(count).map { it.trim().padStart(5, '0') }
    val validation = com.schedule.app.data.PeriodTimes.validate(times.take(count))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("节次时间设置") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("每天节数", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { if (count > 4) { count-- } },
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) { Text("−") }
                    Text(
                        "$count", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    OutlinedButton(
                        onClick = { if (count < 16) { count++ } },
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) { Text("+") }
                }
                Text(
                    "保存后生效：课表只显示前 $count 节，第 $count 节之后不出现在课表里（列表里仍可查看）。每节填开始时间（如 08:00），下课后按「开始 + 45 分钟」估算。",
                    color = Color(0xFF8A90A0), fontSize = 12.sp
                )
                if (validation != null) Text(validation, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                if (maxEndPeriod > count) {
                    Text(
                        "有课程排在第 $count 节之后（最多到第 $maxEndPeriod 节），将不出现在课表里。可调大节数，或点开该课程修改节次。",
                        color = Color(0xFFD25A5A), fontSize = 12.sp
                    )
                }
                for (i in 0 until count) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("第${i + 1}节", fontSize = 13.sp, modifier = Modifier.width(46.dp))
                        OutlinedTextField(
                            times[i],
                            { v ->
                                times = times.toMutableList().also { it[i] = v }
                            },
                            singleLine = true,
                            placeholder = { Text(fmtTime(defaultPeriodStart(i + 1)), fontSize = 13.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                TextButton(onClick = {
                    times = List(16) { i -> fmtTime(defaultPeriodStart(i + 1)) }
                    count = 12
                }) { Text("恢复默认") }
            }
        },
        confirmButton = { TextButton(enabled = validation == null, onClick = { onApply(currentConfig(), count); onDismiss() }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun WeekGrid(
    sem: Semester, week: Int, modifier: Modifier,
    periodStarts: List<String>, periodsPerDay: Int,
    blockOpacity: Float = 1f,
    displayCourses: List<Course>? = null,
    onDay: (Int) -> Unit = {},
    onTap: (Course) -> Unit, onWeek: (Int) -> Unit
) {
    val start = Wk.parseISO(sem.startDate)
    val courses = displayCourses ?: sem.courses
    // 每天节数严格跟随用户设置：第 N 节之后不出现在课表里
    val periodCount = periodsPerDay.coerceIn(4, 16)
    // 周六/周日可按学期开关；关闭的列不显示，其余列均分宽度（不留空白）
    val days = visibleDays(sem)
    val weeksCourses = courses.filter { it.weekday in 1..7 && it.weekday in days && (displayCourses != null || Wk.weeksContains(it.weeks, week)) }
    val today = LocalAppTime.current.toLocalDate()
    val isCurrent = Wk.isWeekOf(start, week)
    val primary = MaterialTheme.colorScheme.primary
    val nowT = LocalAppTime.current.toLocalTime()

    BoxWithConstraints(modifier) {
        val headerH = 48.dp
        val periodW = 46.dp
        val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        // Portrait retains the original geometry; only landscape gets a scrollable minimum row height.
        val bodyH = if (landscape) maxOf(maxHeight - headerH, 28.dp * periodCount) else maxHeight - headerH
        val dayW = (maxWidth - periodW) / days.size.coerceAtLeast(1)
        val rowH = bodyH / periodCount.toFloat()
        val rowHpx = rowH.value
        val gridH = bodyH
        val gridW = periodW + dayW * days.size

        // 左右滑动翻周（轻扫超过阈值即翻页，不影响点课程块）
        val curWeek by rememberUpdatedState(week)
        val curOnWeek by rememberUpdatedState(onWeek)
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState(), enabled = landscape)) {
        Box(
            Modifier.width(gridW).height(bodyH + headerH).pointerInput(Unit) {
                var acc = 0f
                detectHorizontalDragGestures(
                    onDragStart = { acc = 0f },
                    onHorizontalDrag = { change, amount -> change.consume(); acc += amount },
                    onDragEnd = {
                        if (acc < -100f) curOnWeek(curWeek + 1)
                        else if (acc > 100f) curOnWeek(curWeek - 1)
                    }
                )
            }
        ) {
            // ===== 课表主体（先画，垫在表头下面）=====
            Row(Modifier.align(Alignment.BottomStart).height(gridH).width(gridW)) {
                // 最左列：节序 + 每节课开始/结束时间
                Box(Modifier.width(periodW).height(gridH)) {
                    for (p in 1..periodCount) {
                        val st = periodStartFor(periodStarts, p)
                        Column(
                            Modifier.offset(y = ((p - 1) * rowHpx).dp).height(rowH).width(periodW),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "$p", fontSize = 10.sp, lineHeight = 11.sp, maxLines = 1,
                                fontWeight = FontWeight.SemiBold, color = Color(0xFF79808F)
                            )
                            Text(
                                fmtTime(st), fontSize = 7.sp, lineHeight = 8.sp, maxLines = 1,
                                color = Color(0xFF9AA0AE)
                            )
                            Text(
                                fmtTime(st.plusMinutes(PERIOD_MINUTES)), fontSize = 6.5.sp, lineHeight = 7.5.sp,
                                maxLines = 1, color = Color(0xFFB4BAC6)
                            )
                        }
                    }
                }
                for (d in days) {
                    val isTodayCol = isCurrent && Wk.dayOfWeek(today) == d
                    Box(Modifier.width(dayW).height(gridH).clipToBounds()) {
                        Box(
                            Modifier.fillMaxSize().padding(horizontal = 1.dp).background(
                                if (isTodayCol) primary.copy(alpha = 0.07f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                        )
                        com.schedule.app.data.layoutCourses(weeksCourses.filter { it.weekday == d }, periodCount).forEach { slot ->
                            val c = slot.course
                            val sp = c.startPeriod
                            val slotW = dayW / slot.columns
                            val cls = periodStartFor(periodStarts, sp)
                            val cle = periodStartFor(periodStarts, c.endPeriod).plusMinutes(PERIOD_MINUTES)
                            CourseBlock(
                                c = c,
                                modifier = Modifier.offset(x = slotW * slot.column, y = ((sp - 1) * rowHpx).dp)
                                    .width(slotW).height(((c.endPeriod - sp + 1) * rowHpx).dp),
                                isNow = isTodayCol && !nowT.isBefore(cls) && nowT.isBefore(cle),
                                isPast = isTodayCol && !nowT.isBefore(cle), opacity = blockOpacity, compact = slot.columns > 1
                            ) { onTap(c) }
                        }
                    }
                }
            }

            // ===== 表头（后画，永远在最上层），日期数字加大并留足行高，完整凸出来 =====
            Row(Modifier.align(Alignment.TopStart).height(headerH).fillMaxWidth()) {
                Box(Modifier.width(periodW).height(headerH), contentAlignment = Alignment.Center) {
                    val m = try { Wk.weekDate(start, week, 1).monthValue } catch (e: Exception) { start.monthValue }
                    Text(monthLabel(m), fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                }
                for (d in days) {
                    val date = try { Wk.weekDate(start, week, d) } catch (e: Exception) { start }
                    val isToday = isCurrent && Wk.dayOfWeek(today) == d
                    Box(Modifier.width(dayW).height(headerH).clickable(onClickLabel = "查看${weekdayFull(d)}安排") { onDay(d) }, contentAlignment = Alignment.Center) {
                        Column(
                            Modifier.clip(RoundedCornerShape(12.dp))
                                .background(if (isToday) primary.copy(alpha = 0.15f) else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                weekdayShort(d), fontSize = 10.5.sp, lineHeight = 12.sp, maxLines = 1,
                                color = if (isToday) primary else Color(0xFF8A90A0),
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                "${date.dayOfMonth}", fontSize = 14.sp, lineHeight = 16.sp, maxLines = 1,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isToday) primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

/** 单个课程块：渐变底色 + 按压缩放 + 三条信息自动缩字号完整显示。（课表与选课弹窗共用） */
@Composable
fun CourseBlock(c: Course, modifier: Modifier, isNow: Boolean = false, isPast: Boolean = false, opacity: Float = 1f, compact: Boolean = false, onTap: () -> Unit) {
    val base = courseColor(c.name)
    val backgroundAlpha = (opacity.coerceIn(0.25f, 1f) * if (isPast) 0.55f else 1f)
    val grad = remember(c.name, backgroundAlpha) { Brush.verticalGradient(listOf(lighten(base, 0.25f).copy(alpha = backgroundAlpha), base.copy(alpha = backgroundAlpha))) }
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && LocalMotionLevel.current > 0) 0.96f else 1f, spatialSpring(), label = "blockPress")
    val haptic = LocalHapticFeedback.current
    Box(modifier.padding(horizontal = if (compact) 1.dp else 2.dp, vertical = 1.5.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale; scaleY = scale
                // Transparency affects the background only, keeping course text legible.
            }
            .background(grad, RoundedCornerShape(9.dp))
            .border(
                if (isNow) 2.dp else .6.dp,
                Color.White.copy(alpha = if(isNow).95f else .55f), RoundedCornerShape(9.dp)
            )
            .clipToBounds()
            .clickable(interactionSource = src, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onTap()
            }
            .padding(horizontal = if (compact) 1.dp else 3.dp, vertical = 2.dp)
        ) {
            FittedCourseText(c.name, c.room.ifBlank { "地点未填" }, c.teacher.ifBlank { "教师未填" }, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun CourseList(courses: List<Course>, modifier: Modifier, onOpen: (Course) -> Unit) {
    val useful = courses.filter { it.weekday in 1..7 }
        .sortedWith(compareBy({ it.weekday }, { it.startPeriod }))
    val unknown = courses.filter { it.weekday !in 1..7 }
    LazyColumn(modifier, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(useful, key = { it.id }) { c -> CourseRow(c) { onOpen(c) } }
        if (unknown.isNotEmpty()) {
            item { Spacer(Modifier.height(8.dp)); Text("未排周次的课", color = Color.Gray, fontSize = 13.sp) }
            items(unknown, key = { it.id }) { c -> CourseRow(c) { onOpen(c) } }
        }
    }
}

@Composable
private fun CourseRow(c: Course, badge: String? = null, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().pressMotion(), shape = CardShape) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(6.dp).height(44.dp).background(courseAccent(c.name), RoundedCornerShape(3.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    buildString {
                        if (c.weekday in 1..7) append(weekdayShort(c.weekday) + " ")
                        append(periodLabel(c.startPeriod, c.endPeriod))
                        if (c.weeks.isNotEmpty()) append(" · ${c.weeks}周")
                        if (c.room.isNotEmpty()) append(" · ${c.room}")
                        if (c.teacher.isNotEmpty()) append(" · ${c.teacher}")
                        if (c.code.isNotEmpty()) append(" · ${c.code}")
                    }.trim(),
                    color = Color(0xFF8A90A0), fontSize = 13.sp
                )
            }
            if (badge != null) {
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFD25A5A).copy(alpha = 0.14f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) { Text(badge, color = Color(0xFFD25A5A), fontSize = 10.sp, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

// ===========================================================================
// 导入向导
// ===========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(vm: com.schedule.app.AppViewModel) {
    var step by vm.importDraft::step
    var grid by vm.importDraft::grid
    var error by remember { mutableStateOf<String?>(null) }
    var headerRow by vm.importDraft::headerRow
    var manual by vm.importDraft::mapping
    var parsed by vm.importDraft::parsed
    var showPaste by rememberSaveable { mutableStateOf(false) }
    var showGuide by rememberSaveable { mutableStateOf(false) }
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val importScope = rememberCoroutineScope()
    var reading by remember { mutableStateOf(false) }
    var sheetChoices by remember { mutableStateOf<List<XlsxReader.Sheet>>(emptyList()) }
    fun useGrid(g: List<List<String>>) {
        require(g.isNotEmpty()) { "工作表没有可读取的数据" }
        grid = g
        headerRow = Parser.findHeaderRow(g).coerceAtLeast(0)
        val auto = Parser.autoMap(g[headerRow])
        manual = ManualMap(auto?.name ?: -1, auto?.code ?: -1, auto?.time ?: -1, auto?.room ?: -1, auto?.teacher ?: -1)
        error = null
        step = 1
    }

    // 引导页打开时，系统返回键先关引导而不是退出导入
    BackHandler(enabled = showGuide) { showGuide = false }
    // 系统返回键在导入向导内逐级后退，而不是直接退出到主页
    BackHandler(enabled = step > 0) { if (step == 2) step = 1 else step = 0 }

    if (showGuide) {
        ImportGuideScreen(
            onBack = { showGuide = false },
            onGotoPaste = { showGuide = false; showPaste = true }
        )
        return
    }

    /** 把（AI 整理好的）粘贴文本按 CSV 解析：命中标准表头直接进预览，否则进手动映射。 */
    fun usePastedText(raw: String) {
        val noFences = raw.lines().filterNot { it.trim().startsWith("```") }.joinToString("\n").trim()
        if (noFences.isEmpty()) { error = "粘贴的内容是空的"; return }
        val g = try { CsvReader.readText(noFences) } catch (e: Exception) { error = e.message; return }
        if (g.isEmpty()) { error = "粘贴的内容没有解析出表格"; return }
        grid = g
        val hr = Parser.findHeaderRow(g)
        headerRow = if (hr >= 0) hr else 0
        val auto = if (hr >= 0) Parser.autoMap(g[hr]) else null
        error = null
        if (auto != null) {
            parsed = try { Parser.parse(g, headerRow, Parser.ColMap(auto.name, auto.code, auto.time, auto.room, auto.teacher), vm.data.periodStarts) } catch (e: Exception) { error = e.message; return }
            step = 2
        } else {
            manual = ManualMap(
                name = auto?.name ?: -1, code = auto?.code ?: -1, time = auto?.time ?: -1,
                room = auto?.room ?: -1, teacher = auto?.teacher ?: -1
            )
            step = 1
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importScope.launch {
            reading = true
            try {
                val sheets = withContext(Dispatchers.IO) { readSheets(ctx, uri) }
                require(sheets.isNotEmpty()) { "文件没有可读取的工作表" }
                if (sheets.size == 1) useGrid(sheets.single().rows) else sheetChoices = sheets
            } catch (e: Exception) { error = "无法导入：${e.message ?: "请检查文件"}" }
            finally { reading = false }
        }
    }
    if (sheetChoices.isNotEmpty()) AlertDialog(
        onDismissRequest = { sheetChoices = emptyList() }, title = { Text("选择工作表") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            sheetChoices.forEach { sheet -> TextButton(onClick = {
                try { useGrid(sheet.rows); sheetChoices = emptyList() } catch (e: Exception) { error = e.message; sheetChoices = emptyList() }
            }) { Text("${sheet.name}（${sheet.rows.size} 行）") } }
        } }, confirmButton = {}, dismissButton = { TextButton(onClick = { sheetChoices = emptyList() }) { Text("取消") } }
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = topBarTransparent(),
                title = { Text("导入课表") },
                navigationIcon = {
                    IconButton(onClick = { when (step) { 2 -> step = 1; 1 -> step = 0; else -> vm.back() } }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = { IconButton(onClick = { vm.home() }) { Icon(Icons.Filled.Refresh, "取消") } }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            when (step) {
                0 -> {
                    Text("导入你的课表：选 Excel / CSV 文件，或让 AI 整理成标准格式后粘贴", fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(enabled = !reading, onClick = { launcher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text(if (reading) "正在读取…" else "选择文件（Excel / CSV）") }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { showPaste = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.ContentPaste, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("粘贴文本导入")
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { showGuide = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Info, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("引导：你的学校课表怎么导入？")
                    }
                    error?.let { Spacer(Modifier.height(16.dp)); Text(it, color = Color.Red) }
                }
                1 -> {
                    val hdr = grid.getOrNull(headerRow) ?: emptyList()
                    Text("确认各列对应（第 ${headerRow + 1} 行为表头）", fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(hdr.joinToString("　"), color = Color.Gray, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    FieldPicker("课程名称 *", hdr, manual.name) { manual = manual.copy(name = it) }
                    FieldPicker("上课时间 *", hdr, manual.time) { manual = manual.copy(time = it) }
                    FieldPicker("教学班号", hdr, manual.code) { manual = manual.copy(code = it) }
                    FieldPicker("上课地点", hdr, manual.room) { manual = manual.copy(room = it) }
                    FieldPicker("上课教师", hdr, manual.teacher) { manual = manual.copy(teacher = it) }
                    error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Color.Red) }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (manual.name < 0 || manual.time < 0) error = "请至少指定 课程名称 和 上课时间"
                            else {
                                try {
                                    parsed = Parser.parse(grid, headerRow, Parser.ColMap(manual.name, manual.code, manual.time, manual.room, manual.teacher), vm.data.periodStarts)
                                    error = null
                                    step = 2
                                } catch (e: Exception) { error = e.message }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("解析预览（${grid.size - headerRow - 1} 行）") }
                }
                2 -> PreviewAndSave(parsed, vm)
            }
        }
    }

    if (showPaste) {
        PasteImportDialog(
            initial = clipboard.getText()?.toString().orEmpty(), draft = vm.importDraft,
            onDismiss = { showPaste = false },
            onConfirm = { text -> showPaste = false; usePastedText(text) }
        )
    }
}

@Composable
private fun PasteImportDialog(initial: String, draft: com.schedule.app.ImportDraft, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by draft::paste
    LaunchedEffect(Unit) { if (text.isEmpty()) text = initial }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("粘贴课表文本") },
        text = {
            Column {
                Text(
                    "把 AI 整理好的标准表格（或任意 CSV 课表）粘贴到这里，表头需含「课程名称 / 上课时间」等列",
                    color = Color(0xFF8A90A0), fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    text, { text = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                    placeholder = { Text("课程名称,教学班号,上课时间,上课地点,上课教师\n…") }
                )
            }
        },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) { Text("解析") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewAndSave(parsed: List<Course>, vm: com.schedule.app.AppViewModel) {
    var year by rememberSaveable { mutableStateOf("") }
    var term by rememberSaveable { mutableStateOf(0) }
    var startDate by rememberSaveable { mutableStateOf("2026-09-07") }
    var dateErr by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = vm.importDraft.parsed.firstOrNull { it.id == editingId }
    var list by vm.importDraft::parsed
    // (标题 to 已存在学期id)：非空时弹"替换 / 另建"确认
    var replaceInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
    var mondaySave by rememberSaveable { mutableStateOf("") }

    fun doSave(title: String, monday: String, replaceId: String?) {
        val existing = vm.semesters.firstOrNull { it.id == replaceId }
        vm.upsertSemester(existing?.copy(title = title, startDate = monday, courses = list)
            ?: Semester(UUID.randomUUID().toString(), title, monday, list))
        vm.home()
    }

    // 时间冲突检测：同一天 + 同一节次 + 周数有交集的多条课程才算真冲突
    // （同一门课在不同周段/不同节次上课是正常排课，之前只看星期和节次会误报）
    val wTop = list.maxOfOrNull { c -> courseMaxWeek(c.weeks) } ?: 12
    val slotIds = mutableMapOf<String, MutableSet<String>>()
    for (c in list) {
        if (c.weekday !in 1..7 || c.startPeriod !in 1..16 || c.endPeriod < c.startPeriod) continue
        for (w in 1..wTop) {
            if (!Wk.weeksContains(c.weeks, w)) continue
            for (p in c.startPeriod..c.endPeriod) {
                slotIds.getOrPut("$w/${c.weekday}/$p") { mutableSetOf() }.add(c.id)
            }
        }
    }
    val conflictIds = slotIds.filterValues { it.size > 1 }.values.flatten().toSet()

    Column(Modifier.fillMaxSize()) {
        Text("解析出 ${list.size} 条课程记录，点击可修改", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        if (list.isEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("没有解析到课程：请返回上一步检查列映射，或重新选文件 / 粘贴文本", color = Color(0xFFD25A5A), fontSize = 12.sp)
        }
        if (conflictIds.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "检测到 ${conflictIds.size} 条课程时间冲突（同一天同一节次同一周有多门课，行尾标红），建议点开核对",
                color = Color(0xFFD25A5A), fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)) {
            items(list, key = { it.id }) { c ->
                CourseRow(c, badge = if (c.id in conflictIds) "时间重叠" else null) { editingId = c.id }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("学年，如 2024-2025") },
                singleLine = true, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            DropdownField("学期", listOf("上学期", "下学期"), term, Modifier.weight(1f)) { term = it }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = startDate, onValueChange = { startDate = it; dateErr = false },
            label = { Text("第一周周一日期  yyyy-MM-dd") }, singleLine = true,
            isError = dateErr,
            supportingText = { if (dateErr) Text("格式应为 yyyy-MM-dd，例如 2026-09-07", color = Color(0xFFD25A5A), fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth())
        val parsedStart = Wk.parseDateOrNull(startDate)
        val startMonday = parsedStart?.let { Wk.mondayOf(it) }
        if (parsedStart != null && startMonday != null && startMonday != parsedStart) {
            Text(
                "${parsedStart} 是${weekdayFull(Wk.dayOfWeek(parsedStart))}，将自动按所在周的周一 $startMonday 计算，星期与日期一一对应。",
                color = MaterialTheme.colorScheme.primary, fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            val monday = startMonday?.toString() ?: run { dateErr = true; return@Button }
            val title = if (year.isNotBlank()) "${year} ${if (term == 0) "上学期" else "下学期"}" else if (term == 0) "上学期" else "下学期"
            val existing = vm.semesters.firstOrNull { it.title == title }
            if (existing != null) {
                mondaySave = monday
                replaceInfo = title to existing.id
            } else doSave(title, monday, null)
        }, modifier = Modifier.fillMaxWidth(), enabled = list.isNotEmpty()) { Text(if (list.isEmpty()) "没有课程可保存" else "保存课表") }
    }

    replaceInfo?.let { (title, existingId) ->
        AlertDialog(
            onDismissRequest = { replaceInfo = null },
            title = { Text("学期「$title」已存在") },
            text = { Text("可以替换原来那份课表（开学日期用本次填写的），也可以另建一个新学期，两份都保留。") },
            confirmButton = {
                TextButton(onClick = { replaceInfo = null; doSave(title, mondaySave, existingId) }) { Text("替换") }
            },
            dismissButton = {
                TextButton(onClick = { replaceInfo = null; doSave(title, mondaySave, null) }) { Text("另建新学期") }
            }
        )
    }

    editing?.let { c ->
        CourseDialog(course = c, onDismiss = { editingId = null },
            periodStarts = vm.data.periodStarts,
            onDelete = { id -> list = list.filterNot { it.id == id }; editingId = null },
            onConfirm = { e -> list = list.map { if (it.id == e.id) e else it }; editingId = null })
    }
}

@Composable
private fun FieldPicker(label: String, header: List<String>, selected: Int, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, fontSize = 13.sp, color = Color.Gray)
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(if (selected in header.indices) "#${selected} ${header[selected]}" else "选择列", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                header.indices.forEach { i ->
                    DropdownMenuItem(text = { Text("#$i ${header[i]}") }, onClick = { onPick(i); open = false })
                }
            }
        }
    }
}

@Composable
private fun DropdownField(label: String, options: List<String>, selected: Int, modifier: Modifier = Modifier, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(options.getOrElse(selected) { options.first() })
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.indices.forEach { i ->
                DropdownMenuItem(text = { Text(options[i]) }, onClick = { onPick(i); open = false })
            }
        }
    }
}

// ===========================================================================
// 课程编辑对话框（含删除）
// ===========================================================================

@Composable
internal fun CourseDialog(course: Course?, editable: Boolean = true, periodStarts: List<String> = emptyList(), onDismiss: () -> Unit, onDelete: ((String) -> Unit)? = null, onConfirm: (Course) -> Unit) {
    var name by rememberSaveable { mutableStateOf(course?.name ?: "") }
    var teacher by rememberSaveable { mutableStateOf(course?.teacher ?: "") }
    var room by rememberSaveable { mutableStateOf(course?.room ?: "") }
    var code by rememberSaveable { mutableStateOf(course?.code ?: "") }
    var weeks by rememberSaveable { mutableStateOf(course?.weeks ?: "") }
    var weekday by rememberSaveable { mutableStateOf(course?.weekday?.takeIf { it in 1..7 } ?: 0) }
    var start by rememberSaveable { mutableStateOf((course?.startPeriod ?: 0).toString()) }
    var end by rememberSaveable { mutableStateOf((course?.endPeriod ?: 0).toString()) }
    val startNumber = start.toIntOrNull()
    val endNumber = end.toIntOrNull()
    val formError = when {
        name.isBlank() -> "请填写课程名称"
        startNumber == null || endNumber == null -> "请填写有效节次"
        !(startNumber == 0 && endNumber == 0) && (startNumber !in 1..16 || endNumber !in startNumber..16) -> "节次应为 1～16，止节不能早于起节；未排课可填 0 / 0"
        Regex("\\d+").findAll(weeks).any { (it.value.toIntOrNull() ?: Int.MAX_VALUE) !in 1..520 } -> "周次应为 1～520"
        else -> null
    }
    // 按当前节次时间表给出这节课的几点到几点（起止节变化时实时刷新）
    val timeLine = run {
        val sp = start.toIntOrNull()
        val ep = end.toIntOrNull()
        if (sp != null && sp in 1..16 && ep != null && ep in sp..16) {
            val s = periodStartFor(periodStarts, sp)
            val e = periodStartFor(periodStarts, ep).plusMinutes(PERIOD_MINUTES)
            "上课时间：${fmtTime(s)} – ${fmtTime(e)}"
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (course == null) "添加课程" else if (editable) "编辑课程" else "课程详情") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (timeLine != null) {
                    Text(timeLine, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                OutlinedTextField(name, { name = it }, label = { Text("课程名称") }, singleLine = true, readOnly = !editable)
                Row {
                    OutlinedTextField(teacher, { teacher = it }, label = { Text("教师") }, singleLine = true, readOnly = !editable, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(room, { room = it }, label = { Text("教室") }, singleLine = true, readOnly = !editable, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(code, { code = it }, label = { Text("教学班号/代码") }, singleLine = true, readOnly = !editable)
                Row {
                    var wdOpen by remember { mutableStateOf(false) }
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { wdOpen = true }, enabled = editable, modifier = Modifier.fillMaxWidth()) {
                            Text(if (weekday in 1..7) weekdayShort(weekday) else "未知")
                        }
                        DropdownMenu(expanded = wdOpen, onDismissRequest = { wdOpen = false }) {
                            DropdownMenuItem(text = { Text("未知") }, onClick = { weekday = 0; wdOpen = false })
                            for (d in 1..7) DropdownMenuItem(text = { Text(weekdayShort(d)) }, onClick = { weekday = d; wdOpen = false })
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(start, { start = it.filter { c -> c.isDigit() } }, label = { Text("起节") }, singleLine = true, readOnly = !editable, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(end, { end = it.filter { c -> c.isDigit() } }, label = { Text("止节") }, singleLine = true, readOnly = !editable, modifier = Modifier.weight(1f))
                }
                if (editable && formError != null) Text(formError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                OutlinedTextField(weeks, { weeks = it }, label = { Text("周数，如 1-16 或 1-4,6-9") }, singleLine = true, readOnly = !editable)
            }
        },
        confirmButton = {
            if (editable) {
                TextButton(enabled = formError == null, onClick = {
                    if (name.isBlank()) return@TextButton
                    onConfirm(
                        course?.copy(
                            name = name, teacher = teacher, room = room, code = code,
                            weekday = weekday, startPeriod = start.toIntOrNull() ?: 0,
                            endPeriod = end.toIntOrNull() ?: 0, weeks = weeks
                        ) ?: Course(
                            UUID.randomUUID().toString(), name = name, code = code, teacher = teacher, room = room,
                            weekday = weekday, startPeriod = start.toIntOrNull() ?: 0,
                            endPeriod = end.toIntOrNull() ?: 0, weeks = weeks
                        )
                    )
                }) { Text(course?.let { "保存" } ?: "添加") }
            } else {
                TextButton(onClick = onDismiss) { Text("确定") }
            }
        },
        dismissButton = {
            if (editable) {
                Row {
                    if (onDelete != null) TextButton(onClick = { onDelete(course!!.id) }) { Text("删除", color = Color.Red) }
                    TextButton(onClick = onDismiss) { Text("取消") }
                }
            }
        }
    )
}

// ===========================================================================
// 工具
// ===========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun topBarTransparent() = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)

private fun readSheets(ctx: Context, uri: Uri): List<XlsxReader.Sheet> {
    val name = displayName(ctx, uri)?.lowercase().orEmpty()
    return (ctx.contentResolver.openInputStream(uri) ?: error("无法打开文件")).use { input ->
        if (name.endsWith(".xlsx")) XlsxReader.readSheets(input)
        else listOf(XlsxReader.Sheet(name.ifBlank { "CSV" }, CsvReader.read(input)))
    }
}

private fun displayName(ctx: Context, uri: Uri): String? {
    ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) return c.getString(0)
    }
    return null
}

// ===========================================================================
// 全屏背景（主题渐变 + 光斑 + 图案 / 自定义壁纸）
// ===========================================================================

@Composable
fun AppBackdrop(vm: com.schedule.app.AppViewModel, dark: Boolean) {
    val ctx = LocalContext.current
    val uri = vm.bgImageUri
    // 异步解码壁纸，避免大图在主线程解码造成卡顿；加载完成前先显示主题渐变
    var bmp by remember(uri, vm.wallpaperRevision) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(uri, vm.wallpaperRevision) {
        bmp = null
        bmp = uri?.let { u ->
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { loadBitmapSampled(ctx, u) }
        }
    }
    val p = resolvePreset(vm.theme, vm.customAccent)
    // 换肤时颜色平滑过渡
    val c1 by animateColorAsState(if (dark) darken(p.c1, 0.84f) else p.c1, tween(motionDuration(400)), label = "bg1")
    val c2 by animateColorAsState(if (dark) darken(p.c2, 0.80f) else p.c2, tween(motionDuration(400)), label = "bg2")
    val ac by animateColorAsState(p.accent, tween(motionDuration(400)), label = "bg3")
    Box(Modifier.fillMaxSize()) {
        val currentBmp = bmp
        if (currentBmp != null) {
            WallpaperImage(currentBmp, vm.bgScale, vm.bgOffX, vm.bgOffY, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(if (dark) Color.Black.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.76f)))
        } else {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c1, c2))))
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.radialGradient(listOf(ac.copy(alpha = if (dark) .12f else .10f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width, size.height * .1f), radius = size.width * .95f))
                drawRect(Brush.radialGradient(listOf(ac.copy(alpha = .06f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(0f, size.height * .8f), radius = size.width))
            }
            PatternLayer(vm.pattern, if (dark) lighten(p.accent, 0.45f) else darken(p.c1, 0.32f), Modifier.fillMaxSize())
        }
    }
}

/** 按 scale/offset 把壁纸图片铺满指定区域。 */
@Composable
fun WallpaperImage(bmp: androidx.compose.ui.graphics.ImageBitmap, scale: Float, offX: Float, offY: Float, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        if (scale <= 0f) return@Canvas
        val sc = maxOf(size.width / bmp.width, size.height / bmp.height) * scale
        val dw = bmp.width * sc
        val dh = bmp.height * sc
        val cx = size.width / 2 + offX * size.width
        val cy = size.height / 2 + offY * size.height
        drawImage(
            image = bmp,
            dstOffset = androidx.compose.ui.unit.IntOffset((cx - dw / 2).toInt(), (cy - dh / 2).toInt()),
            dstSize = androidx.compose.ui.unit.IntSize(dw.toInt(), dh.toInt())
        )
    }
}

/** 装饰图案层（0=无）。用低透明度图形点缀在渐变上。 */
@Composable
fun PatternLayer(pattern: Int, base: Color, modifier: Modifier) {
    if (pattern <= 0) return
    androidx.compose.foundation.Canvas(modifier) {
        val c = base.copy(alpha = 0.16f)
        val d = base.copy(alpha = 0.12f)
        // 大图案按画布尺寸等比缩放：屏幕和 64dp 预览块里都显得足够大
        val minD = minOf(size.width, size.height)
        when (pattern) {
            1 -> { // 圆点点阵
                val step = 46f
                var x = step / 2
                while (x < size.width) {
                    var y = step / 2
                    while (y < size.height) {
                        drawCircle(d, radius = 4.5f, center = androidx.compose.ui.geometry.Offset(x, y))
                        y += step
                    }
                    x += step
                }
            }
            2 -> { // 大小气泡
                val rnd = kotlin.random.Random(size.width.toInt())
                repeat(26) {
                    val r = 12f + rnd.nextFloat() * 40f
                    val x = rnd.nextFloat() * size.width
                    val y = rnd.nextFloat() * size.height
                    drawCircle(c.copy(alpha = 0.24f), radius = r, center = androidx.compose.ui.geometry.Offset(x, y),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
                }
            }
            3 -> { // 波浪线
                val step = 42f
                var y0 = 0f
                while (y0 < size.height) {
                    val p = androidx.compose.ui.graphics.Path()
                    var x = 0f
                    p.moveTo(x, y0)
                    while (x <= size.width) {
                        p.lineTo(x, y0 + 18f * kotlin.math.sin(x / 30f))
                        x += 8f
                    }
                    drawPath(p, d.copy(alpha = 0.30f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f))
                    y0 += step
                }
            }
            4 -> { // 星星点缀（大颗，按画布比例）
                val rnd = kotlin.random.Random(size.width.toInt() + 7)
                repeat(10) {
                    val x = rnd.nextFloat() * size.width
                    val y = rnd.nextFloat() * size.height
                    val r = minD * (0.045f + rnd.nextFloat() * 0.035f)
                    val star = androidx.compose.ui.graphics.Path()
                    for (k in 0..9) {
                        val ang = Math.PI / 5 * k - Math.PI / 2
                        val rr = if (k % 2 == 0) r.toDouble() else r * 0.45
                        val px = x + (rr * kotlin.math.cos(ang)).toFloat()
                        val py = y + (rr * kotlin.math.sin(ang)).toFloat()
                        if (k == 0) star.moveTo(px, py) else star.lineTo(px, py)
                    }
                    star.close()
                    drawPath(star, d)
                }
            }
            5 -> { // 简约网格
                val step = 64f
                var x = 0f
                while (x <= size.width) {
                    drawLine(d, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 1.5f)
                    x += step
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(d, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1.5f)
                    y += step
                }
            }
            6 -> { // 斜纹
                val step = 58f
                var x = -size.height
                while (x < size.width) {
                    drawLine(c.copy(alpha = 0.20f), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x + size.height, size.height), strokeWidth = 7f)
                    x += step
                }
            }
            7 -> { // 菱形点阵（交错排布，按画布比例放大）
                val step = minD * 0.17f
                var x = step / 2
                var row = 0
                while (x < size.width) {
                    var y = step / 2
                    var col = 0
                    while (y < size.height) {
                        val px = x + if (row % 2 == 0) 0f else step / 2
                        val py = y + if (col % 2 == 0) 0f else step / 2
                        val rr = minD * 0.028f
                        val p = androidx.compose.ui.graphics.Path()
                        p.moveTo(px, py - rr); p.lineTo(px + rr, py)
                        p.lineTo(px, py + rr); p.lineTo(px - rr, py)
                        p.close()
                        drawPath(p, d)
                        y += step
                        col++
                    }
                    x += step
                    row++
                }
            }
            8 -> { // 小爱心（按画布比例）
                val rnd = kotlin.random.Random(size.width.toInt() + 19)
                repeat(8) {
                    val x = rnd.nextFloat() * size.width
                    val y = rnd.nextFloat() * size.height
                    val r = minD * (0.05f + rnd.nextFloat() * 0.035f)
                    val p = androidx.compose.ui.graphics.Path()
                    p.moveTo(x, y + r * 0.35f)
                    p.cubicTo(x, y - r * 0.35f, x - r, y - r * 0.35f, x - r, y + r * 0.15f)
                    p.cubicTo(x - r, y + r * 0.6f, x - r * 0.4f, y + r * 0.85f, x, y + r * 1.15f)
                    p.cubicTo(x + r * 0.4f, y + r * 0.85f, x + r, y + r * 0.6f, x + r, y + r * 0.15f)
                    p.cubicTo(x + r, y - r * 0.35f, x, y - r * 0.35f, x, y + r * 0.35f)
                    p.close()
                    drawPath(p, d)
                }
            }
            9 -> { // 小十字（按画布比例）
                val step = minD * 0.15f
                var x = step / 2
                while (x < size.width) {
                    var y = step / 2
                    while (y < size.height) {
                        val s = minD * 0.03f
                        drawLine(d, androidx.compose.ui.geometry.Offset(x, y - s), androidx.compose.ui.geometry.Offset(x, y + s), strokeWidth = minD * 0.012f)
                        drawLine(d, androidx.compose.ui.geometry.Offset(x - s, y), androidx.compose.ui.geometry.Offset(x + s, y), strokeWidth = minD * 0.012f)
                        y += step
                    }
                    x += step
                }
            }
            10 -> { // 六边形蜂窝（描边，按画布比例）
                val rnd = kotlin.random.Random(size.width.toInt() + 23)
                repeat(6) {
                    val x = rnd.nextFloat() * size.width
                    val y = rnd.nextFloat() * size.height
                    val r = minD * (0.06f + rnd.nextFloat() * 0.04f)
                    val p = androidx.compose.ui.graphics.Path()
                    for (k in 0..5) {
                        val ang = Math.PI / 3 * k - Math.PI / 6
                        val px = x + (r * kotlin.math.cos(ang)).toFloat()
                        val py = y + (r * kotlin.math.sin(ang)).toFloat()
                        if (k == 0) p.moveTo(px, py) else p.lineTo(px, py)
                    }
                    p.close()
                    drawPath(p, c.copy(alpha = 0.22f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = minD * 0.0045f))
                }
            }
            11 -> { // 小花（规整对称：6 片花瓣 + 花芯，全部圆润）
                val rnd = kotlin.random.Random(size.width.toInt() + 31)
                repeat(5) {
                    val x = rnd.nextFloat() * size.width
                    val y = rnd.nextFloat() * size.height
                    val r = minD * (0.055f + rnd.nextFloat() * 0.04f)
                    val petal = r * 0.42f
                    val p = androidx.compose.ui.graphics.Path()
                    for (k in 0 until 6) {
                        val ang = Math.PI / 3 * k
                        val cx = x + (r * 0.6f * kotlin.math.cos(ang)).toFloat()
                        val cy = y + (r * 0.6f * kotlin.math.sin(ang)).toFloat()
                        p.addOval(
                            androidx.compose.ui.geometry.Rect(cx - petal, cy - petal, cx + petal, cy + petal),
                            androidx.compose.ui.graphics.Path.Direction.Clockwise
                        )
                    }
                    p.addOval(
                        androidx.compose.ui.geometry.Rect(x - petal * 0.85f, y - petal * 0.85f, x + petal * 0.85f, y + petal * 0.85f),
                        androidx.compose.ui.graphics.Path.Direction.Clockwise
                    )
                    drawPath(p, d)
                }
            }
        }
    }
}

/** 解码壁纸图片并按最大边降采样，避免一张大照片直接把内存打爆。支持应用私有目录标记。 */
fun loadBitmapSampled(ctx: Context, uriString: String, maxDim: Int = 2048): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        // 壁纸存放在应用私有目录时直接读文件（备份/恢复后也走这里）；小组件壁纸同理
        val src: () -> java.io.InputStream? = if (uriString == Store.WALLPAPER_MARKER) {
            { java.io.File(ctx.filesDir, "wallpaper.img").takeIf { it.exists() }?.inputStream() }
        } else if (uriString == Store.WIDGET_WALLPAPER_MARKER) {
            { Store.widgetWallpaperFile(ctx).takeIf { it.exists() }?.inputStream() }
        } else {
            { ctx.contentResolver.openInputStream(Uri.parse(uriString)) }
        }
        // 第一步只读尺寸：此模式 decodeStream 返回 null 是正常的，不能当失败处理
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        src()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxDim || bounds.outHeight / (sample * 2) >= maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        src()?.use {
            BitmapFactory.decodeStream(it, null, opts)?.asImageBitmap()
        }
    } catch (e: Exception) { null }
}
