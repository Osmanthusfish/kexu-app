package com.schedule.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.Screen
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.schedule.app.data.ReminderScheduler
import com.schedule.app.data.Wk
import com.schedule.app.data.readBounded
import com.schedule.app.model.Countdown
import com.schedule.app.model.Course
import com.schedule.app.model.Reminder
import com.schedule.app.model.Semester
import com.schedule.app.model.Todo
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** 统一的卡片圆角。 */
private val CardShape = RoundedCornerShape(22.dp)

// ===========================================================================
// 课程闹钟
// ===========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(vm: com.schedule.app.AppViewModel) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var batch by rememberSaveable { mutableStateOf(false) }
    var draftSemesterId by rememberSaveable { mutableStateOf<String?>(null) }
    var draftCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = vm.reminders.firstOrNull { it.id == editingId }
    val draftSem = vm.semesters.find { it.id == draftSemesterId }
    val draftCourse = draftSem?.courses?.find { it.id == draftCourseId }
    val draft = draftCourse?.let { c ->
        val time = com.schedule.app.data.PeriodTimes.start(draftSem.periodStarts.ifEmpty { vm.data.periodStarts }, c.startPeriod)
        Reminder("", c.name, c.weekday, time.hour, time.minute, 10, c.room, true, c.startPeriod, draftSem.id, c.id)
    }
    androidx.compose.runtime.LaunchedEffect(vm.pendingRoute) {
        vm.pendingRoute?.takeIf { it[0] == "new-reminder" }?.let { route ->
            val existing = vm.reminders.find { it.linkedSemesterId == route[1] && it.linkedCourseId == route[2] }
            if (existing != null) editingId = existing.id else { draftSemesterId = route[1].takeIf { it.isNotBlank() }; draftCourseId = route[2].takeIf { it.isNotBlank() }; adding = true }
            vm.clearRoute()
        }
    }
    var delTarget by remember { mutableStateOf<Reminder?>(null) }
    val ctx = LocalContext.current
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun apply(r: Reminder) {
        vm.upsertReminder(r)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = topBarTransparent(),
                title = { Text("课程闹钟") },
                actions = { TextButton(onClick = { batch = true }) { Text("批量添加") } },
                navigationIcon = { IconButton(onClick = { vm.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { adding = true },modifier=Modifier.pressMotion(.92f)) { Icon(Icons.Filled.Add, "新增提醒") } }
    ) { pad ->
        val notifGranted = android.os.Build.VERSION.SDK_INT < 33 || androidx.core.content.ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val exactOk = android.os.Build.VERSION.SDK_INT < 31 ||
            (ctx.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager).canScheduleExactAlarms()
        val fsiOk = android.os.Build.VERSION.SDK_INT < 34 ||
            (ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).canUseFullScreenIntent()
        val overlayOk = android.provider.Settings.canDrawOverlays(ctx)
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (!notifGranted) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFD25A5A).copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "还没通知权限，课程提醒不会弹通知。",
                        color = Color(0xFFB34141), fontSize = 12.sp, modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text("允许", color = Color(0xFFB34141), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
            if (!exactOk) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "还没「精确到点响铃」，闹钟可能被系统延迟，建议开启。",
                        color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        try {
                            ctx.startActivity(
                                android.content.Intent(
                                    android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    android.net.Uri.fromParts("package", ctx.packageName, null)
                                )
                            )
                        } catch (e: Exception) { }
                    }) {
                        Text("去开启", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
            if (!fsiOk) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "还没开启「全屏响铃」，锁屏时可能只剩横幅。",
                        color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        try {
                            ctx.startActivity(
                                android.content.Intent(
                                    android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                    android.net.Uri.fromParts("package", ctx.packageName, null)
                                )
                            )
                        } catch (e: Exception) { }
                    }) {
                        Text("去开启", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
            if (!overlayOk) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "开启「悬浮提醒」后，闹钟能在任何应用/桌面上弹出响铃窗（停止/稍后提醒）。荣耀、小米等系统还需在电池设置里允许应用后台运行。",
                        color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        try {
                            ctx.startActivity(
                                android.content.Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.fromParts("package", ctx.packageName, null)
                                )
                            )
                        } catch (e: Exception) { }
                    }) {
                        Text("去开启", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
            if (vm.reminders.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Box(
                        Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Alarm, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp)) }
                    Spacer(Modifier.height(14.dp))
                    Text("还没有课程提醒", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("点击 + 从课表选一门课，上课前提醒你", color = Color(0xFF8A90A0), fontSize = 13.sp)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vm.reminders, key = { it.id }) { r ->
                        val next = com.schedule.app.data.ReminderRules.nextMillis(r, vm.data, LocalAppTime.current.atZone(java.time.ZoneId.systemDefault()))
                        val nextLabel = if (!r.enabled) "已关闭" else if (next < 0) "没有后续有效上课安排" else "下次提醒：" + java.time.Instant.ofEpochMilli(next).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd E HH:mm"))
                        ReminderRow(r, nextLabel, onToggle = { apply(r.copy(enabled = !r.enabled)) }, onEdit = { editingId = r.id }, onDelete = { delTarget = r })
                    }
                }
            }
        }
    }

    delTarget?.let { r ->
        ConfirmDelete(
            title = "删除提醒",
            text = "确定删除「${r.label}」吗？",
            onDismiss = { delTarget = null },
            onConfirm = {
                vm.deleteReminder(r.id)
                delTarget = null
            }
        )
    }

    if (batch) BatchReminderDialog(vm) { batch = false }
    if (adding || editing != null) {
        // 提醒关联的节次时间：用所选学期（或主页学期）生效的节次表
        val linkSem = vm.semesters.firstOrNull { it.id == vm.selectedSemesterId }
            ?: pickDisplaySemester(vm.semesters, vm.homeSemesterId, Wk.today())
        ReminderDialog(
            semesters = vm.semesters,
            initialSemesterId = vm.selectedSemesterId,
            periodStarts = vm.data.periodStarts,
            periodsPerDay = vm.data.periodsPerDay,
            initial = editing ?: draft,
            onDismiss = { adding = false; editingId = null; draftCourseId = null },
            onSave = { apply(if (it.id.isBlank()) it.copy(id = UUID.randomUUID().toString()) else it); adding = false; editingId = null; draftCourseId = null }
        )
    }
}

@Composable
private fun ReminderRow(r: Reminder, nextLabel: String, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onEdit, modifier = Modifier.fillMaxWidth().pressMotion(), shape = CardShape) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "${weekdayShort(r.weekday)} ${"%02d:%02d".format(r.hour, r.minute)}${if (r.leadMinutes > 0) "（提前${r.leadMinutes}分钟）" else ""}",
                    color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium
                )
                if (r.note.isNotBlank()) Text("备注：${r.note}", color = Color(0xFF8A90A0), fontSize = 12.sp)
                Text(nextLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Switch(checked = r.enabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "删除", tint = Color(0xFFD25A5A)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderDialog(
    semesters: List<Semester>,
    initialSemesterId: String?,
    periodStarts: List<String>,
    periodsPerDay: Int,
    initial: Reminder?,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit
) {
    var label by rememberSaveable { mutableStateOf(initial?.label ?: "") }
    var weekday by rememberSaveable { mutableStateOf(initial?.weekday ?: 1) }
    var hour by rememberSaveable { mutableStateOf(initial?.hour ?: 8) }
    var minute by rememberSaveable { mutableStateOf(initial?.minute ?: 0) }
    var lead by rememberSaveable { mutableStateOf(initial?.leadMinutes ?: 10) }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var enabled by rememberSaveable { mutableStateOf(initial?.enabled ?: true) }
    var linkedPeriod by rememberSaveable { mutableStateOf(initial?.linkedPeriod ?: 0) }
    var linkedSemesterId by rememberSaveable { mutableStateOf(initial?.linkedSemesterId) }
    var linkedCourseId by rememberSaveable { mutableStateOf(initial?.linkedCourseId) }
    fun unlink() { linkedPeriod = 0; linkedSemesterId = null; linkedCourseId = null }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var wdOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null || initial.id.isBlank()) "新增课程提醒" else "编辑提醒") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Schedule, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                    Text(if (label.isNotBlank()) label else "从课表选择课程", maxLines = 1)
                }
                OutlinedTextField(label, { label = it }, label = { Text("提醒内容") }, singleLine = true)
                Row {
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { wdOpen = true }, modifier = Modifier.fillMaxWidth()) { Text(weekdayShort(weekday)) }
                        DropdownMenu(expanded = wdOpen, onDismissRequest = { wdOpen = false }) {
                            for (d in 1..7) DropdownMenuItem(text = { Text(weekdayShort(d)) }, onClick = { weekday = d; unlink(); wdOpen = false })
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("%02d:%02d".format(hour, minute))
                        }
                    }
                }
                Row {
                    Box(Modifier.weight(1f)) {
                        var leadOpen by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { leadOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (lead == 0) "准时提醒" else "提前${lead}分钟")
                        }
                        DropdownMenu(expanded = leadOpen, onDismissRequest = { leadOpen = false }) {
                            listOf(0, 5, 10, 15, 30, 60).forEach { m ->
                                DropdownMenuItem(text = { Text(if (m == 0) "准时提醒" else "提前${m}分钟") }, onClick = { lead = m; leadOpen = false })
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("启停", fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                }
                OutlinedTextField(note, { note = it }, label = { Text("备注") }, singleLine = true)
                if (linkedPeriod > 0) {
                    Text(
                        if (linkedSemesterId != null) "跟随所选学期的课程时间与周次；周次为空时开学后每周提醒。课程被删除或取消排课时停用提醒。" else "旧版提醒保持每周固定时间。重新选择课程可启用学期与周次关联。",
                        color = MaterialTheme.colorScheme.primary, fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (label.isBlank()) return@TextButton
                onSave(Reminder(
                    initial?.id ?: UUID.randomUUID().toString(), label, weekday, hour, minute, lead, note, enabled,
                    linkedPeriod = linkedPeriod.takeIf { it > 0 }, linkedSemesterId = linkedSemesterId, linkedCourseId = linkedCourseId
                ))
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择提醒时间") },
            text = { Box { TimePicker(state = state) } },
            confirmButton = {
                TextButton(onClick = { hour = state.hour; minute = state.minute; unlink(); showTimePicker = false }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("取消") } }
        )
    }

    if (showPicker) {
        CoursePickerDialog(
            semesters, initialSemesterId,
            periodStarts = periodStarts, periodsPerDay = periodsPerDay,
            onDismiss = { showPicker = false }
        ) { selectedSem, c ->
            label = "准备上课：${c.name}"
            weekday = c.weekday
            val t = periodTime(effectivePeriodStarts(selectedSem, periodStarts), c.startPeriod)
            hour = t.first; minute = t.second
            linkedPeriod = c.startPeriod
            linkedSemesterId = selectedSem.id
            linkedCourseId = c.id
            showPicker = false
        }
    }
}

/** 节次 -> 默认开始时间（小时, 分钟），优先用用户配置的节次时间表。 */
private fun periodTime(config: List<String>, p: Int): Pair<Int, Int> {
    val t = periodStartFor(config, p)
    return t.hour to t.minute
}

/**
 * 从课表选课：等比例复刻主课表界面 —— 同样的表头、节次列、课程块渲染，
 * 可左右翻周、（多学期时）切换学期，点课程块即选中。课程块不再互相覆盖。
 */
@Composable
private fun CoursePickerDialog(
    semesters: List<Semester>,
    initialSemesterId: String?,
    periodStarts: List<String>,
    periodsPerDay: Int,
    onDismiss: () -> Unit,
    onPick: (Semester, Course) -> Unit
) {
    var semId by rememberSaveable { mutableStateOf(initialSemesterId ?: semesters.firstOrNull()?.id ?: "") }
    val sem = semesters.firstOrNull { it.id == semId } ?: semesters.firstOrNull()

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.86f),
            shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface
        ) {
            if (sem == null) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("课表里还没有课程，先去导入。", color = Color(0xFF8A90A0))
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
            } else {
    val start = Wk.parseISO(sem.startDate)
    // 默认落在真实"本周"：开学前是 0/负数周，与其他入口保持一致
    var week by rememberSaveable(sem.id) { mutableStateOf(Wk.currentWeek(start)) }
                val cur = Wk.currentWeek(start)
                val primary = MaterialTheme.colorScheme.primary

                Column(Modifier.fillMaxSize().padding(12.dp)) {
                    // 顶栏：标题 + 周导航 + 关闭
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("点选课程", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        if (week != cur) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "回本周", color = primary, fontSize = 11.sp,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .clickable { week = cur }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { week -= 1 }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上一周", modifier = Modifier.size(20.dp))
                        }
                        Text("第${week}周", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        IconButton(onClick = { week += 1 }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下一周", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Filled.Close, "关闭", modifier = Modifier.size(18.dp))
                        }
                    }

                    // 多学期时可切换学期
                    if (semesters.size > 1) {
                        var open by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { open = true },
                                modifier = Modifier.fillMaxWidth().height(34.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                            ) {
                                Text(sem.title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                                semesters.forEach { s ->
                                    DropdownMenuItem(text = { Text(s.title, fontSize = 13.sp) }, onClick = { semId = s.id; open = false })
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    // ===== 与主课表等比例的网格 =====
                    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(top = 4.dp)) {
                        val headerH = 38.dp
                        val periodW = 22.dp
                        val periodCount = effectivePeriodsPerDay(sem, periodsPerDay)
                        val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                        val bodyH = if (landscape) maxOf(maxHeight - headerH, 28.dp * periodCount) else maxHeight - headerH
                        val dayW = (maxWidth - periodW) / 7f
                        val rowH = bodyH / periodCount.toFloat()
                        val rowHpx = rowH.value
                        val today = LocalAppTime.current.toLocalDate()
                        val isCurrent = Wk.isWeekOf(start, week)
                        val weekCourses = sem.courses
                            .filter { it.weekday in 1..7 && Wk.weeksContains(it.weeks, week) }
                            .sortedBy { it.startPeriod }

                        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState(), enabled = landscape)) {
                        Box(
                            Modifier.fillMaxWidth().height(bodyH + headerH).pointerInput(Unit) {
                                // 左右轻扫翻周，与主课表手势一致
                                var acc = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { acc = 0f },
                                    onHorizontalDrag = { change, amount -> change.consume(); acc += amount },
                                    onDragEnd = {
                                        if (acc < -100f) week += 1 else if (acc > 100f) week -= 1
                                    }
                                )
                            }
                        ) {
                            // 网格主体
                            Row(Modifier.align(Alignment.BottomStart).height(bodyH).width(periodW + dayW * 7f)) {
                                Box(Modifier.width(periodW).height(bodyH)) {
                                    for (p in 1..periodCount) {
                                        Box(
                                            Modifier.offset(y = ((p - 1) * rowHpx).dp).height(rowH).width(periodW),
                                            contentAlignment = Alignment.Center
                                        ) { Text("$p", fontSize = 8.5.sp, color = Color(0xFFA6ACBA)) }
                                    }
                                }
                                for (d in 1..7) {
                                    val isTodayCol = isCurrent && Wk.dayOfWeek(today) == d
                                    Box(Modifier.width(dayW).height(bodyH).clipToBounds()) {
                                        Box(
                                            Modifier.fillMaxSize().padding(horizontal = 1.dp).background(
                                                if (isTodayCol) primary.copy(alpha = 0.07f) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                        )
                                        com.schedule.app.data.layoutCourses(weekCourses.filter { it.weekday == d }, periodCount).forEach { slot ->
                                            val c = slot.course
                                            val slotW = dayW / slot.columns
                                            CourseBlock(c = c, modifier = Modifier
                                                .offset(x = slotW * slot.column, y = ((c.startPeriod - 1) * rowHpx).dp)
                                                .width(slotW).height(((c.endPeriod - c.startPeriod + 1) * rowHpx).dp)
                                            ) { onPick(sem, c) }
                                        }
                                    }
                                }
                            }
                            // 表头（后画，在最上层）
                            Row(Modifier.align(Alignment.TopStart).height(headerH).fillMaxWidth()) {
                                Spacer(Modifier.width(periodW))
                                for (d in 1..7) {
                                    val date = try { Wk.weekDate(start, week, d) } catch (e: Exception) { start }
                                    val isToday = isCurrent && Wk.dayOfWeek(today) == d
                                    Box(Modifier.width(dayW).height(headerH), contentAlignment = Alignment.Center) {
                                        Column(
                                            Modifier.clip(RoundedCornerShape(10.dp))
                                                .background(if (isToday) primary.copy(alpha = 0.15f) else Color.Transparent)
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                weekdayShort(d), fontSize = 9.5.sp, lineHeight = 11.sp, maxLines = 1,
                                                color = if (isToday) primary else Color(0xFF8A90A0),
                                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Text(
                                                "${date.dayOfMonth}", fontSize = 11.5.sp, lineHeight = 13.sp, maxLines = 1,
                                                color = if (isToday) primary else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }
                    Text(
                        "点击课程块选为提醒 · 左右翻周找课",
                        color = Color(0xFF8A90A0), fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// ===========================================================================
// 主题换肤 / 壁纸
// ===========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(vm: com.schedule.app.AppViewModel) {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            vm.openWallpaperEdit(uri.toString())
        }
    }
    // 小组件壁纸：独立文件，与主界面壁纸互不影响
    val widgetLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            vm.openWidgetWallpaperEdit(uri.toString())
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = topBarTransparent(),
                title = { Text("主题换肤") },
                navigationIcon = { IconButton(onClick = { vm.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("内置主题", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("选一套主题色，全屏背景、顶栏、按钮、卡片一起跟着变", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            val customPreview = if (vm.customAccent != -1) Color(vm.customAccent).copy(alpha = 1f) else Color(0xFFB9C2D2)
            val swatches = PRESETS.mapIndexed { i, p -> ThemeSwatchItem(if (i == 0) "默认" else p.name, p.c1, p.c2, i) }
            swatches.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { s -> ThemeSwatch(s.name, s.c1, s.c2, s.id, vm.theme) { vm.setTheme(s.id) } }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(12.dp))
            }
            // 「自定义」单独一行居中，不孤零零贴在左边
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CustomThemeSwatch(
                    ThemeSwatchItem("自定义", lighten(customPreview, 0.72f), lighten(customPreview, 0.90f), 6),
                    vm.theme, vm.customAccent
                ) { argb -> vm.setCustomAccent(argb) }
            }
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = { vm.go(Screen.Widgets) }, modifier = Modifier.fillMaxWidth().pressMotion()) { Text("小组件工作室 · 预览与定制") }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text("深色模式", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "跟随系统", 1 to "浅色", 2 to "深色").forEach { (i, label) ->
                    val selected = vm.darkMode == i
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { vm.setDarkMode(i) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label, fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("字体大小", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("手机系统字体太大的话选「小」即可，全应用即时生效", color = Color(0xFF8A90A0), fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.85f to "小", 1.0f to "标准", 1.15f to "大", 1.3f to "特大").forEach { (v, label) ->
                    val selected = (vm.fontScale - v).let { k -> kotlin.math.abs(k) < 0.01f }
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { vm.setFontScale(v) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label, fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(16.dp))
            // 课程块不透明度：数值越大越实、越小越透，松手保存
            var opacity by remember { mutableStateOf(vm.blockOpacity) }
            Text("课程块不透明度", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "当前 ${Math.round(opacity * 100)}%——越大格子越实、越小越透",
                color = Color(0xFF8A90A0), fontSize = 12.sp
            )
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                valueRange = 0.25f..1f,
                onValueChangeFinished = { vm.setBlockOpacity(opacity) }
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text("背景图案", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("主题色上可加点图案点缀，或保持素色", color = Color(0xFF8A90A0), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            listOf(
                0 to "素", 1 to "圆点", 2 to "气泡", 3 to "波浪",
                4 to "星星", 5 to "网格", 6 to "斜纹", 7 to "菱形",
                8 to "爱心", 9 to "十字", 10 to "六边形", 11 to "花朵"
            ).chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { (pid, name) ->
                        PatternSwatch(pid, name, resolvePreset(vm.theme, vm.customAccent), vm.pattern) { vm.setPattern(pid) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text("自定义壁纸", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("选一张图片，可缩放 / 拖动选取想作为壁纸的部分", color = Color(0xFF8A90A0), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { launcher.launch(arrayOf("image/*")) }) {
                    Icon(Icons.Filled.Image, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("选择壁纸")
                }
                OutlinedButton(onClick = { vm.setBg(null) }) { Text("恢复默认") }
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text("小组件壁纸", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("桌面小组件的独立壁纸：同样可缩放 / 拖动选取，不影响应用内壁纸", color = Color(0xFF8A90A0), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { widgetLauncher.launch(arrayOf("image/*")) }) {
                    Icon(Icons.Filled.Image, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("选择小组件壁纸")
                }
                OutlinedButton(onClick = { vm.setWidgetBg(null) }) { Text("恢复默认") }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

private data class ThemeSwatchItem(val name: String, val c1: Color, val c2: Color, val id: Int)

/** 自定义主题色候选（精选主色盘）。 */
private val CUSTOM_ACCENTS = listOf(
    Color(0xFF3F7FE0), Color(0xFF2E9BD6), Color(0xFF12A5A5), Color(0xFF1F9E68), Color(0xFF7CB342), Color(0xFFB8A11F), Color(0xFFEE8245),
    Color(0xFFE15D3C), Color(0xFFE0447A), Color(0xFFD14A9E), Color(0xFF9C4DE0), Color(0xFF8460DD), Color(0xFF5C6BE8), Color(0xFF3A67D6)
)

/** 「自定义」主题色块：点开色盘选主色，整套界面按主色派生。 */
@Composable
private fun CustomThemeSwatch(item: ThemeSwatchItem, currentTheme: Int, customAccent: Int, onPick: (Int) -> Unit) {
    val selected = currentTheme == 6
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier.clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = { open = true })
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(item.c1, item.c2)))
                .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (selected && customAccent != -1) {
                Box(Modifier.size(30.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
            } else {
                Icon(Icons.Filled.Add, null, tint = Color(0xFF526274), modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(item.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("选择主题色") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("选一个主色，背景渐变和整套界面都会按它生成。", color = Color(0xFF8A90A0), fontSize = 12.sp)
                    CUSTOM_ACCENTS.chunked(7).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            row.forEach { c ->
                                val argb = c.toArgb().toInt()
                                Box(
                                    Modifier.size(34.dp).clip(CircleShape).background(c)
                                        .border(2.dp, if (customAccent == argb) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                                        .clickable { onPick(argb); open = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun ThemeSwatch(name: String, c1: Color, c2: Color, id: Int, current: Int, onPick: () -> Unit) {
    val selected = current == id
    Column(
        Modifier
            .pressMotion(.94f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onPick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(c1, c2)))
                .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Box(Modifier.size(30.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

@Composable
private fun PatternSwatch(pid: Int, name: String, preset: ThemePreset, current: Int, onPick: () -> Unit) {
    val selected = current == pid
    val c1 = preset.c1
    val c2 = preset.c2
    Column(
        Modifier
            .pressMotion(.94f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onPick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(c1, c2)))
                .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            PatternLayer(pid, c1, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.15f)))
            if (selected) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

// ===========================================================================
// 壁纸选取（缩放 / 拖动裁取）—— 覆盖在 Activity 根层的全屏编辑层
// ===========================================================================

/**
 * 壁纸裁剪层。
 * - widgetAspect = null：主壁纸模式（对象是全屏，铺满即可，无需框）；
 * - widgetAspect != null：小组件壁纸模式——中央显示按小组件宽高比的预览框，
 *   框内所见即小组件卡片将显示的背景，拖动/缩放都作用于框内画面。
 */
@Composable
fun WallpaperEditOverlay(
    uri: String,
    widgetAspect: Float? = null,
    onDismiss: () -> Unit,
    onConfirm: (scale: Float, offX: Float, offY: Float) -> Unit
) {
    val ctx = LocalContext.current
    var bmp by remember(uri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var loading by remember(uri) { mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(uri) {
        bmp = withContext(Dispatchers.IO) { loadBitmapSampled(ctx, uri) }
        loading = false
    }
    var scale by rememberSaveable { mutableStateOf(1f) }
    var offX by rememberSaveable { mutableStateOf(0f) }
    var offY by rememberSaveable { mutableStateOf(0f) }

    // 系统返回键 = 取消裁剪
    BackHandler { onDismiss() }

    Box(Modifier.fillMaxSize().background(Color(0xFF101114))) {
        if (widgetAspect == null) {
            // —— 主壁纸：整屏就是目标区域 ——
            BoxWithConstraints(
                Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            offX = (offX + pan.x / size.width).coerceIn(-1f, 1f)
                            offY = (offY + pan.y / size.height).coerceIn(-1f, 1f)
                        }
                    }
            ) {
                if (bmp != null) {
                    WallpaperImage(bmp!!, scale, offX, offY, Modifier.fillMaxSize())
                } else {
                    Text(
                        if (loading) "正在读取图片…" else "无法加载这张图片，换一张试试",
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        } else {
            // —— 小组件壁纸：预览框里所见即卡片所得 ——
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "预览框里显示的内容，就是小组件卡片的背景",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp
                )
                Spacer(Modifier.height(10.dp))
                if (bmp != null) {
                    BoxWithConstraints(
                        Modifier.fillMaxWidth(0.88f).aspectRatio(widgetAspect)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 4f)
                                    offX = (offX + pan.x / size.width).coerceIn(-1f, 1f)
                                    offY = (offY + pan.y / size.height).coerceIn(-1f, 1f)
                                }
                            }
                    ) {
                        WallpaperImage(bmp!!, scale, offX, offY, Modifier.fillMaxSize())
                    }
                } else {
                    Text(if (loading) "正在读取图片…" else "无法加载这张图片，换一张试试", color = Color.White.copy(alpha = 0.7f))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "在框内拖动图片移动 · 捏合或滑条缩放",
                    color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp
                )
            }
        }
        Text(
            "拖动图片移动 · 捏合或上下滑缩放",
            color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 18.dp)
        )
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Slider(value = scale, onValueChange = { scale = it.coerceIn(1f, 4f) }, valueRange = 1f..4f)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("取消", color = Color.White)
                }
                Button(enabled = bmp != null, onClick = { onConfirm(scale, offX, offY) }, modifier = Modifier.weight(1f)) {
                    Text(if (widgetAspect == null) "以此部分为壁纸" else "设为小组件壁纸")
                }
            }
        }
    }
}

// ===========================================================================
// 倒计时
// ===========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownScreen(vm: com.schedule.app.AppViewModel) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var delTarget by remember { mutableStateOf<Countdown?>(null) }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = topBarTransparent(),
                title = { Text("倒计时") },
                navigationIcon = { IconButton(onClick = { vm.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { adding = true },modifier=Modifier.pressMotion(.92f)) { Icon(Icons.Filled.Add, "新增") } }
    ) { pad ->
        if (vm.countdowns.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(pad).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp)) }
                Spacer(Modifier.height(14.dp))
                Text("还没有倒计时", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("添加考试、作业等截止日", color = Color(0xFF8A90A0), fontSize = 13.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.countdowns, key = { it.id }) { c ->
                    CountdownRow(c, onDelete = { delTarget = c })
                }
            }
        }
    }
    delTarget?.let { c ->
        ConfirmDelete(
            title = "删除倒计时",
            text = "确定删除「${c.title}」吗？",
            onDismiss = { delTarget = null },
            onConfirm = { vm.deleteCountdown(c.id); delTarget = null }
        )
    }
    if (adding) CountdownDialog(onDismiss = { adding = false }) { c -> vm.upsertCountdown(c); adding = false }
}

@Composable
private fun CountdownRow(c: Countdown, onDelete: () -> Unit) {
    val today = LocalAppTime.current.toLocalDate()
    val days = try { java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.parse(c.date)) } catch (e: Exception) { 0L }
    val urgent = days in 0..7
    Card(modifier = Modifier.fillMaxWidth(), shape = CardShape) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                Text(c.date, color = Color(0xFF8A90A0), fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    when { days > 0 -> "$days"; days == 0L -> "今天"; else -> "${-days}" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = if (days <= 0) Color(0xFFD25A5A) else MaterialTheme.colorScheme.primary
                )
                Text(
                    when { days > 0 -> "天后"; days == 0L -> "就是今天"; else -> "天前" },
                    fontSize = 11.sp,
                    color = if (urgent) Color(0xFFD25A5A) else Color(0xFF8A90A0)
                )
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "删除", tint = Color(0xFFD25A5A)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountdownDialog(onDismiss: () -> Unit, onSave: (Countdown) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf<String?>(null) }
    val date = dateText?.let(Wk::parseDateOrNull)
    var showDate by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增倒计时") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("事件名称") }, singleLine = true)
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(date?.toString() ?: "选择日期")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val d = date ?: return@TextButton
                onSave(Countdown(UUID.randomUUID().toString(), title.ifBlank { "事件" }, d.toString()))
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
    if (showDate) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    val ms = state.selectedDateMillis
                    dateText = ms?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString() }
                    showDate = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("取消") } }
        ) {
            DatePicker(state = state)
        }
    }
}

// ===========================================================================
// 数据备份
// ===========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(vm: com.schedule.app.AppViewModel) {
    val ctx = LocalContext.current
    var msg by remember { mutableStateOf<String?>(null) }
    var pendingImport by vm::pendingBackupImport
    val backupScope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var confirmRestore by rememberSaveable { mutableStateOf(false) }
    val upgradeExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) backupScope.launch {
            busy = true
            try {
                val text = vm.exportUpgradeSnapshot()
                withContext(Dispatchers.IO) { (ctx.contentResolver.openOutputStream(uri) ?: error("无法打开保存位置")).use { it.write(text.toByteArray(Charsets.UTF_8)) } }
                msg = "已导出升级前原始快照，可配合旧版安装包回滚。"
            } catch (e: Exception) { msg = "导出失败：${e.message}" }
            finally { busy = false }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) backupScope.launch {
            busy = true
            try {
                val text = vm.exportJson()
                withContext(Dispatchers.IO) {
                    (ctx.contentResolver.openOutputStream(uri) ?: error("无法打开保存位置")).use { it.write(text.toByteArray(Charsets.UTF_8)) }
                }
                msg = "导出成功"
            } catch (e: Exception) { msg = "导出失败：${e.message}" }
            finally { busy = false }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) backupScope.launch {
            busy = true
            try {
                pendingImport = withContext(Dispatchers.IO) {
                    val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBounded().toString(Charsets.UTF_8) } ?: error("无法读取选择的文件")
                    text to vm.previewImport(text)
                }
            } catch (e: Exception) { msg = "导入失败：${e.message}" }
            finally { busy = false }
        }
    }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = topBarTransparent(),
                title = { Text("数据备份") },
                navigationIcon = { IconButton(onClick = { vm.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("备份包含课表、单次调整、提醒、作业待办、倒计时和外观设置，可保存到云盘或电脑。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Text(
                        "当前数据：${vm.semesters.size} 个学期 · ${vm.semesters.sumOf { it.courses.size }} 条课程 · ${vm.todos.size} 条待办 · ${vm.reminders.size} 个提醒 · ${vm.countdowns.size} 个倒计时",
                        color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium
                    )
                }
            }
            Button(enabled = !busy && !vm.isSaving, onClick = { exportLauncher.launch("schedule-backup.json") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Check, null); Spacer(Modifier.width(8.dp)); Text("导出备份")
            }
            OutlinedButton(enabled = !busy && !vm.isSaving, onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) {
                Text("导入备份")
            }
            if (vm.canRestoreImport) {
                OutlinedButton(onClick = { confirmRestore = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("恢复上次导入前的数据")
                }
            }
            if (vm.canExportUpgradeSnapshot) {
                OutlinedButton(enabled = !busy && !vm.isSaving, onClick = { upgradeExportLauncher.launch("schedule-before-upgrade.json") }, modifier = Modifier.fillMaxWidth()) { Text("导出升级前快照（旧版回滚）") }
                Text("此快照保留本轮数据格式升级前的内容，早期快照也继续保留。回滚前请同时导出当前备份，保存新版本期间的改动。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            msg?.let { Text(it, color = if (it.startsWith("导入成功") || it == "导出成功") Color(0xFF3C8C5A) else Color.Red) }
        }
    }
    pendingImport?.let { (text, preview) ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("确认恢复备份") },
            text = { Text("备份包含 ${preview.semesters.size} 个学期、${preview.semesters.sumOf { it.courses.size }} 条课程、${preview.reminders.size} 个提醒、${preview.todos.size} 条待办。\n\n确认后会替换当前数据。导入前的数据会自动保留，可在本页恢复。") },
            confirmButton = { TextButton(onClick = {
                pendingImport = null
                vm.importJson(text) { ok -> msg = if (ok) "导入成功，已保留导入前的数据" else "导入失败，请查看保存提示" }
            }) { Text("确认恢复") } },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("取消") } }
        )
    }
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("恢复上次导入前的数据？") },
            text = { Text("当前课表、提醒、待办、设置和壁纸将恢复到上次导入前的状态。") },
            confirmButton = { TextButton(onClick = {
                confirmRestore = false
                vm.restoreLastImport { ok -> msg = if (ok) "导入成功，已恢复上次导入前的数据" else "恢复失败，请查看保存提示" }
            }) { Text("恢复") } },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("取消") } }
        )
    }
}

// ===========================================================================
// 待办 / 备忘
// ===========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(vm: com.schedule.app.AppViewModel) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = vm.todos.firstOrNull { it.id == editingId }
    var draftSemesterId by rememberSaveable { mutableStateOf<String?>(null) }
    var draftCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(vm.pendingRoute) {
        vm.pendingRoute?.let { route ->
            if (route[0] == "todo") { editingId = route[4]; vm.clearRoute() }
            else if (route[0] == "new-todo") { draftSemesterId = route[1].takeIf { it.isNotBlank() }; draftCourseId = route[2].takeIf { it.isNotBlank() }; adding = true; vm.clearRoute() }
        }
    }
    val snackbar = remember { SnackbarHostState() }
    val actionScope = rememberCoroutineScope()
    var showOcrDisclosure by remember { mutableStateOf(false) }
    val screenshotPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.recognizeScreenshotTodo(uri)
    }
    fun toggle(todo: Todo) {
        vm.setTodoDone(todo.id, !todo.done)
        if (!todo.done) actionScope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar("已完成「${todo.title}」", actionLabel = "撤销", withDismissAction = true) == SnackbarResult.ActionPerformed) vm.setTodoDone(todo.id, false)
        }
    }
    var delTarget by remember { mutableStateOf<Todo?>(null) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = topBarTransparent(),
                title = { Text("待办 / 备忘") },
                actions = {
                    TextButton(enabled = !vm.readingScreenshot, onClick = { showOcrDisclosure = true }) {
                        Icon(Icons.Filled.Image, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (vm.readingScreenshot) "识别中…" else "截图转待办")
                    }
                },
                navigationIcon = { IconButton(onClick = { vm.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { adding = true },modifier=Modifier.pressMotion(.92f)) { Icon(Icons.Filled.Add, "新增") } }
    ) { pad ->
        val pending = vm.todos.filter { !it.done }.sortedWith(compareBy({ it.dueDate ?: "9999" }, { it.dueTime ?: "23:59" }, { it.startDate ?: "0000" }))
        val done = vm.todos.filter { it.done }
        if (vm.todos.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(pad).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp)) }
                Spacer(Modifier.height(14.dp))
                Text("还没有待办", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text("记下要做什么、什么时候做，做完打个勾", color = Color(0xFF8A90A0), fontSize = 13.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key="progress") { CompletionMeter(done.size,vm.todos.size) }
                item(key="gesture-hint") { Text("右滑完成 · 左滑删除 · 点击编辑",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(horizontal=4.dp,vertical=4.dp)) }
                if (pending.isNotEmpty()) {
                    item { Text("待完成 (${pending.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary) }
                    items(pending, key = { it.id }) { t -> TodoRow(t, modifier = Modifier.animateItem(fadeInSpec = androidx.compose.animation.core.tween(motionDuration(180)), fadeOutSpec = androidx.compose.animation.core.tween(motionDuration(140)), placementSpec = androidx.compose.animation.core.tween(motionDuration(300))), courseLabel = t.linkedCourseId?.let { id -> vm.semesters.find { it.id == t.linkedSemesterId }?.courses?.find { it.id == id }?.name ?: "原课程已删除" }, onToggle = { toggle(t) }, onEdit = { editingId = t.id }, onDelete = { delTarget = t }) }
                    if (done.isNotEmpty()) { item { Spacer(Modifier.height(10.dp)) } }
                }
                if (done.isNotEmpty()) {
                    item { Text("已完成 (${done.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF8A90A0)) }
                    items(done, key = { it.id }) { t -> TodoRow(t, modifier = Modifier.animateItem(fadeInSpec = androidx.compose.animation.core.tween(motionDuration(180)), fadeOutSpec = androidx.compose.animation.core.tween(motionDuration(140)), placementSpec = androidx.compose.animation.core.tween(motionDuration(300))), courseLabel = t.linkedCourseId?.let { id -> vm.semesters.find { it.id == t.linkedSemesterId }?.courses?.find { it.id == id }?.name ?: "原课程已删除" }, onToggle = { toggle(t) }, onEdit = { editingId = t.id }, onDelete = { delTarget = t }) }
                }
            }
        }
    }

    delTarget?.let { t ->
        ConfirmDelete(
            title = "删除待办",
            text = "确定删除「${t.title}」吗？",
            onDismiss = { delTarget = null },
            onConfirm = { vm.deleteTodo(t.id); delTarget = null }
        )
    }

    if (adding || editing != null) {
        LinkedTodoDialog(todo = editing, vm = vm, initialSemesterId = draftSemesterId, initialCourseId = draftCourseId, onDismiss = { adding = false; editingId = null; draftCourseId = null }) { t ->
            vm.upsertTodo(t)
            adding = false; editingId = null; draftCourseId = null
        }
    }

    if (showOcrDisclosure) {
        AlertDialog(
            onDismissRequest = { showOcrDisclosure = false },
            title = { Text("截图识别与隐私") },
            text = { Text("截图和识别文字在设备上处理，不会由识别组件发送给 Google。Google ML Kit 组件可能联网收集设备、应用和性能等诊断信息。使用在线 AI 整理时会另行征求你的同意。") },
            confirmButton = { TextButton(onClick = { showOcrDisclosure = false; screenshotPicker.launch("image/*") }) { Text("选择截图") } },
            dismissButton = { TextButton(onClick = { showOcrDisclosure = false }) { Text("取消") } }
        )
    }
    vm.screenshotTodoError?.let { message ->
        AlertDialog(onDismissRequest = vm::dismissScreenshotTodoError, title = { Text("截图识别") },
            text = { Text(message) }, confirmButton = { TextButton(onClick = vm::dismissScreenshotTodoError) { Text("知道了") } })
    }
    vm.screenshotTodoDraft?.let { draft ->
        ScreenshotTodoReviewDialog(draft, onDismiss = { vm.screenshotTodoDraft = null }, onAiDraft = { vm.screenshotTodoDraft = it }) { todo ->
            vm.upsertTodo(todo)
            vm.screenshotTodoDraft = null
            actionScope.launch { snackbar.showSnackbar("已添加待办：${todo.title}") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoRow(t: Todo, modifier: Modifier = Modifier, courseLabel: String? = null, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val latestToggle by rememberUpdatedState(onToggle)
    val latestDelete by rememberUpdatedState(onDelete)
    val swipe=rememberSwipeToDismissBoxState(confirmValueChange={value->
        when(value) { SwipeToDismissBoxValue.StartToEnd->latestToggle();SwipeToDismissBoxValue.EndToStart->latestDelete();else->Unit }
        false // Keep the item until the existing state update / delete confirmation completes.
    })
    val right=swipe.dismissDirection==SwipeToDismissBoxValue.StartToEnd
    val bg by animateColorAsState(if(swipe.dismissDirection==SwipeToDismissBoxValue.Settled)Color.Transparent else if(right)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,androidx.compose.animation.core.tween(motionDuration(180)),label="swipe action")
    SwipeToDismissBox(state=swipe,modifier=modifier,backgroundContent={
        Box(Modifier.fillMaxSize().clip(CardShape).background(bg).padding(horizontal=22.dp),contentAlignment=if(right)Alignment.CenterStart else Alignment.CenterEnd) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Icon(if(right)Icons.Filled.Check else Icons.Filled.Delete,null,Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp));Text(if(right)if(t.done)"恢复待办" else "完成" else "删除",fontSize=13.sp)
            }
        }
    }) {
    Card(onClick = onEdit, modifier = Modifier.fillMaxWidth().pressMotion(), shape = CardShape) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = t.done, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(t.title, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                    textDecoration = if (t.done) TextDecoration.LineThrough else null,
                    color = if (t.done) Color(0xFF9AA0AE) else MaterialTheme.colorScheme.onSurface)
                if (courseLabel != null) Text("课程：$courseLabel", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                val overdue = !t.done && com.schedule.app.data.TodoRules.deadline(t)?.isBefore(LocalAppTime.current.atZone(ZoneId.systemDefault())) == true
                val dates = buildString {
                    if (overdue) append("已过期 · ")
                    if (t.startDate != null) append("开始 ${t.startDate}")
                    if (t.dueDate != null) { if (isNotEmpty()) append(" · "); append("截止 ${t.dueDate} ${t.dueTime.orEmpty()}") }
                    if (t.remindMinutesBefore != null && !t.done) append(" · 提前${t.remindMinutesBefore}分钟提醒")
                    if (t.startDate == null && t.dueDate == null) append("无日期")
                }
                Text(dates, color = if (overdue) Color(0xFFD25A5A) else Color(0xFF8A90A0), fontSize = 12.sp,
                    fontWeight = if (overdue) FontWeight.Medium else FontWeight.Normal)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "删除", tint = Color(0xFFD25A5A)) }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePick(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val state = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val ms = state.selectedDateMillis
                if (ms != null) onPick(java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString())
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    ) { DatePicker(state = state) }
}
