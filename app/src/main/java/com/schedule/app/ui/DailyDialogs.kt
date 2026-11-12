package com.schedule.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.AppViewModel
import com.schedule.app.data.*
import com.schedule.app.model.*
import java.time.LocalDate
import java.util.UUID

@Composable
fun DayAgendaDialog(vm: AppViewModel, sem: Semester, date: LocalDate, onDismiss: () -> Unit, onOpen: (CourseOccurrence) -> Unit) {
    val courses = OccurrenceRules.onDate(vm.data, sem, date)
    val starts = sem.periodStarts.ifEmpty { vm.data.periodStarts }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("$date ${weekdayFull(date.dayOfWeek.value)}") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (courses.isEmpty()) Text("这一天没有课程。")
            courses.forEach { o ->
                val c = o.course
                val overlaps = courses.count { it.key != o.key && it.course.startPeriod <= c.endPeriod && it.course.endPeriod >= c.startPeriod }
                Card(onClick = { onOpen(o) }, modifier = Modifier.fillMaxWidth().pressMotion()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(c.name, fontWeight = FontWeight.Bold)
                        Text("${PeriodTimes.start(starts, c.startPeriod)}–${PeriodTimes.start(starts, c.endPeriod).plusMinutes(45)} · ${periodLabel(c.startPeriod, c.endPeriod)}", fontSize = 13.sp)
                        Text("教室：${c.room.ifBlank { "未填写" }}")
                        Text("教师：${c.teacher.ifBlank { "未填写" }}")
                        if (overlaps > 0) Text("与 $overlaps 门课程时间重叠", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetails(vm: AppViewModel, sem: Semester, course: Course, occurrence: CourseOccurrence?, onDismiss: () -> Unit, onEdit: () -> Unit, sourceBounds: androidx.compose.ui.geometry.Rect? = null, sourceStatus: String? = null) {
    var adjusting by rememberSaveable { mutableStateOf(false) }
    var addTask by rememberSaveable { mutableStateOf(false) }
    val shown = occurrence?.course ?: course
    val starts = sem.periodStarts.ifEmpty { vm.data.periodStarts }
    val tasks = vm.todos.filter { it.linkedSemesterId == sem.id && it.linkedCourseId == course.id && !it.done }
    val content: @Composable (onClose: () -> Unit) -> Unit = { onClose ->
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${occurrence?.date?.toString() ?: weekdayFull(shown.weekday)} · ${periodLabel(shown.startPeriod, shown.endPeriod)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, "关闭") }
            }
            Reveal {
                Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer,MaterialTheme.colorScheme.surface)),RoundedCornerShape(24.dp))) {
                    Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("课程详情",fontSize=11.sp,letterSpacing=1.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(shown.name,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Reveal(1) {
                Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (shown.startPeriod in 1..16 && shown.endPeriod in 1..16) DetailLine(Icons.Default.Schedule, "时间", "${PeriodTimes.start(starts, shown.startPeriod)}–${PeriodTimes.start(starts, shown.endPeriod).plusMinutes(45)}")
                        DetailLine(Icons.Default.Place, "教室", shown.room.ifBlank { "未填写" })
                        DetailLine(Icons.Default.Person, "教师", shown.teacher.ifBlank { "未填写" })
                    }
                }
            }
            val canRemind = course.weekday in 1..7 && course.startPeriod in 1..16
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { addTask = true }, modifier = Modifier.weight(1f).pressMotion()) { Icon(Icons.Default.Add, null, Modifier.size(17.dp)); Text("添加作业") }
                OutlinedButton(onClick = { onDismiss(); vm.openRoute("new-reminder", sem.id, course.id) }, enabled = canRemind, modifier = Modifier.weight(1f).pressMotion()) { Text("课程提醒") }
            }
            OutlinedButton(onClick = { adjusting = true }, enabled = occurrence != null, modifier = Modifier.fillMaxWidth().pressMotion()) { Text("调整这一次 / 补课") }
            if (!canRemind) Text("设置上课星期和节次后可添加课程提醒", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (tasks.isNotEmpty()) {
                Text("待完成作业 ${tasks.size} 项", fontWeight = FontWeight.SemiBold)
                tasks.forEach { t -> Surface(onClick = { onDismiss(); vm.openRoute("todo", todoId = t.id) }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) { Text(t.title, fontWeight = FontWeight.Medium); Text("${t.dueDate.orEmpty()} ${t.dueTime.orEmpty()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("常规周次：${course.weeks}" + if (course.code.isNotBlank()) " · ${course.code}" else "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (occurrence?.changeId != null) Text("本次已单独调整", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("编辑常规课程") }
        }
    }
    if (sourceBounds != null && sourceBounds.width > 1f && sourceBounds.height > 1f) {
        SkyCourseDialog(sourceBounds, shown.name, shown.room, shown.teacher, sourceStatus, onDismiss, content)
    } else {
        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) { content(onDismiss) }
    }
    if (adjusting && occurrence != null) CourseChangeDialog(sem, occurrence,
        vm.data.courseChanges.find { it.id == occurrence.changeId }, onDismiss = { adjusting = false },
        onSave = { vm.upsertCourseChange(it); adjusting = false; onDismiss() },
        onRestore = { id -> vm.deleteCourseChange(id); adjusting = false; onDismiss() })
    if (addTask) LinkedTodoDialog(null, vm, sem.id, course.id, { addTask = false }) { vm.upsertTodo(it); addTask = false }
}

@Composable private fun DetailLine(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

@Composable
fun CourseChangeDialog(sem: Semester, occurrence: CourseOccurrence, initial: CourseChange?, onDismiss: () -> Unit, onSave: (CourseChange) -> Unit, onRestore: (String) -> Unit) {
    var mode by rememberSaveable { mutableStateOf(initial?.mode ?: "move") }
    var date by rememberSaveable { mutableStateOf(initial?.date ?: occurrence.date.toString()) }
    var start by rememberSaveable { mutableStateOf((initial?.startPeriod ?: occurrence.course.startPeriod).toString()) }
    var end by rememberSaveable { mutableStateOf((initial?.endPeriod ?: occurrence.course.endPeriod).toString()) }
    var room by rememberSaveable { mutableStateOf(initial?.room ?: occurrence.course.room) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    val valid = Wk.parseDateOrNull(date) != null && start.toIntOrNull() in 1..16 && end.toIntOrNull() in (start.toIntOrNull() ?: 1)..16
    AlertDialog(onDismissRequest = onDismiss, title = { Text("调整 ${occurrence.base.name}") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("原安排 ${occurrence.originalDate}，其他周保持原样。", fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("move" to "调课", "cancel" to "停课", "extra" to "补课").forEach { (value, label) -> FilterChip(selected = mode == value, onClick = { mode = value }, label = { Text(label) }) }
            }
            if (mode == "cancel") Text("仅取消这一次；可以在「单次调整」中恢复。") else {
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) { Text("上课日期 $date") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(start, { start = it }, label = { Text("开始节") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(end, { end = it }, label = { Text("结束节") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(room, { room = it }, label = { Text("本次教室") }, modifier = Modifier.fillMaxWidth())
                Text(if (mode == "extra") "额外增加一次，原课保留。" else "日期不变时可只修改教室或节次。", fontSize = 12.sp)
            }
            if (!valid) Text("节次须在 1～16 内，结束节不能早于开始节。", color = MaterialTheme.colorScheme.error)
            if (initial != null) TextButton(onClick = { onRestore(initial.id) }) { Text(if (initial.mode == "extra") "撤销这次补课" else "恢复原安排") }
        }
    }, confirmButton = { TextButton(enabled = valid, onClick = {
        onSave(CourseChange(initial?.id ?: UUID.randomUUID().toString(), sem.id, occurrence.base.id,
            occurrence.originalDate.toString(), date, mode, start.toInt(), end.toInt(), room.trim()))
    }) { Text("保存本次调整") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
    if (pickDate) DatePick({ pickDate = false }) { date = it; pickDate = false }
}

@Composable
fun CourseChangesDialog(vm: AppViewModel, sem: Semester, onDismiss: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val changes = vm.data.courseChanges.filter { it.semesterId == sem.id }.sortedByDescending { it.date }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("单次调整") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (changes.isEmpty()) Text("暂无调整。点击课表中的课程，可调课、停课或补课。")
            changes.forEach { c ->
                val base = sem.courses.find { it.id == c.courseId }
                Text(base?.name ?: "原课程已删除", fontWeight = FontWeight.SemiBold)
                Text("${c.originalDate} → ${if (c.mode == "cancel") "停课" else "${c.date} 第${c.startPeriod}–${c.endPeriod}节 ${c.room}"}", fontSize = 12.sp)
                Row {
                    if (base != null) TextButton(onClick = { selected = c.id }) { Text("修改") }
                    TextButton(onClick = { vm.deleteCourseChange(c.id) }) { Text(if (c.mode == "extra") "撤销补课" else "恢复原安排") }
                }
                HorizontalDivider()
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } })
    changes.find { it.id == selected }?.let { c -> sem.courses.find { it.id == c.courseId }?.let { base ->
        CourseChangeDialog(sem, CourseOccurrence(base, base, LocalDate.parse(c.date), LocalDate.parse(c.originalDate), c.id), c,
            { selected = null }, { vm.upsertCourseChange(it); selected = null }, { vm.deleteCourseChange(it); selected = null })
    } }
}

@Composable
fun BatchReminderDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    var semId by rememberSaveable { mutableStateOf(vm.selectedSemesterId ?: vm.homeSemesterId ?: vm.semesters.firstOrNull()?.id) }
    val sem = vm.semesters.find { it.id == semId }
    var selected by rememberSaveable(semId) { mutableStateOf(arrayListOf<String>()) }
    var lead by rememberSaveable { mutableStateOf("10") }
    var replace by rememberSaveable { mutableStateOf(false) }
    val courses = sem?.courses.orEmpty().filter { it.weekday in 1..7 && it.startPeriod in 1..16 }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("批量添加课程提醒") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            vm.semesters.forEach { s -> Row(Modifier.fillMaxWidth().clickable { semId = s.id }) { RadioButton(selected = s.id == semId, onClick = { semId = s.id }); Text(s.title, modifier = Modifier.padding(top = 12.dp)) } }
            OutlinedTextField(lead, { lead = it }, label = { Text("提前分钟数（0～10080）") }, singleLine = true)
            Row { Checkbox(selected.size == courses.size && courses.isNotEmpty(), { selected = if (it) ArrayList(courses.map { c -> c.id }) else arrayListOf() }); Text("全选本学期课程", modifier = Modifier.padding(top = 12.dp)) }
            courses.forEach { c -> Row(Modifier.fillMaxWidth().clickable { selected = ArrayList(if (c.id in selected) selected - c.id else selected + c.id) }) {
                Checkbox(c.id in selected, { selected = ArrayList(if (it) selected + c.id else selected - c.id) })
                Column(Modifier.padding(top = 8.dp)) { Text(c.name); Text("${weekdayFull(c.weekday)} ${periodLabel(c.startPeriod, c.endPeriod)}", fontSize = 11.sp) }
            } }
            Row { Checkbox(replace, { replace = it }); Text("同时更新已有提醒（开启并统一提前量）", modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp) }
            Text("默认保留已有提醒的个别设置，不重复添加。", fontSize = 12.sp)
        }
    }, confirmButton = { TextButton(enabled = sem != null && selected.isNotEmpty() && lead.toIntOrNull() in 0..10080, onClick = {
        vm.batchReminders(sem!!.id, selected.toSet(), lead.toInt(), replace); onDismiss()
    }) { Text("应用到 ${selected.size} 门课") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
fun LinkedTodoDialog(todo: Todo?, vm: AppViewModel, initialSemesterId: String? = null, initialCourseId: String? = null, onDismiss: () -> Unit, onSave: (Todo) -> Unit) {
    var title by rememberSaveable { mutableStateOf(todo?.title ?: "") }
    var start by rememberSaveable { mutableStateOf(todo?.startDate) }
    var due by rememberSaveable { mutableStateOf(todo?.dueDate) }
    var time by rememberSaveable { mutableStateOf(todo?.dueTime ?: if (todo == null) "23:59" else "") }
    var remind by rememberSaveable { mutableStateOf(todo?.remindMinutesBefore != null) }
    var lead by rememberSaveable { mutableStateOf((todo?.remindMinutesBefore ?: 30).toString()) }
    var semId by rememberSaveable { mutableStateOf(todo?.linkedSemesterId ?: initialSemesterId) }
    var courseId by rememberSaveable { mutableStateOf((todo?.linkedCourseId ?: initialCourseId)?.takeIf { it.isNotBlank() }) }
    var picker by rememberSaveable { mutableStateOf("") }
    val course = vm.semesters.find { it.id == semId }?.courses?.find { it.id == courseId }
    val valid = title.isNotBlank() && (start == null || due == null || start!! <= due!!) &&
        (due == null || time.isBlank() || (Regex("\\d{2}:\\d{2}").matches(time) && runCatching { java.time.LocalTime.parse(time) }.isSuccess)) && (!remind || (due != null && lead.toIntOrNull() in 0..10080))
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val notifications = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (todo == null) "新增作业 / 待办" else "编辑作业 / 待办") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("要做什么") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = { picker = "course" }, modifier = Modifier.fillMaxWidth()) { Text(course?.name ?: if (courseId == null) "关联课程（可选）" else "原课程已删除，可重新关联") }
            if (courseId != null) TextButton(onClick = { courseId = null; semId = null }) { Text("取消课程关联") }
            Row { OutlinedButton(onClick = { picker = "start" }, modifier = Modifier.weight(1f)) { Text(start ?: "开始日期（可选）") }; if (start != null) TextButton(onClick = { start = null }) { Text("清除") } }
            Row { OutlinedButton(onClick = { picker = "due" }, modifier = Modifier.weight(1f)) { Text(due ?: "截止日期（可选）") }; if (due != null) TextButton(onClick = { due = null; remind = false }) { Text("清除") } }
            if (due != null) {
                OutlinedTextField(time, { time = it }, label = { Text("截止时间 HH:mm") }, singleLine = true)
                Row { Checkbox(remind, { remind = it; if (it && android.os.Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) notifications.launch(android.Manifest.permission.POST_NOTIFICATIONS) }); Text("截止前提醒", modifier = Modifier.padding(top = 12.dp)) }
                if (remind) OutlinedTextField(lead, { lead = it }, label = { Text("提前分钟数（0～10080）") }, singleLine = true)
            }
            if (!valid && title.isNotBlank()) Text("请检查日期顺序、时间格式和提前分钟数。", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            if (remind) Text("完成后自动取消提醒；未填写截止时间时按 23:59。", fontSize = 12.sp)
            if (remind && android.os.Build.VERSION.SDK_INT >= 31 && !ctx.getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms()) {
                Text("未开启精确闹钟权限，系统可能延迟提醒。", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                TextButton(onClick = { runCatching { ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:${ctx.packageName}"))) } }) { Text("设置准时提醒") }
            }
        }
    }, confirmButton = { TextButton(enabled = valid, onClick = { onSave(Todo(todo?.id ?: UUID.randomUUID().toString(), title.trim(), start, due, todo?.done ?: false,
        semId.takeIf { courseId != null }, courseId, if (due != null && time.isNotBlank()) java.time.LocalTime.parse(time).toString() else null, if (remind) lead.toInt() else null)) }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
    if (picker == "start" || picker == "due") DatePick({ picker = "" }) { if (picker == "start") start = it else due = it; picker = "" }
    if (picker == "course") AlertDialog(onDismissRequest = { picker = "" }, title = { Text("关联课程") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) { vm.semesters.forEach { s ->
            Text(s.title, fontWeight = FontWeight.Bold)
            s.courses.forEach { c -> TextButton(onClick = { semId = s.id; courseId = c.id; picker = "" }, modifier = Modifier.fillMaxWidth()) { Text("${c.name} · ${weekdayFull(c.weekday)} ${periodLabel(c.startPeriod, c.endPeriod)}") } }
        } }
    }, confirmButton = { TextButton(onClick = { picker = "" }) { Text("关闭") } })
}
