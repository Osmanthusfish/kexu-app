package com.schedule.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.schedule.app.data.ScreenshotTodoDraft
import com.schedule.app.data.TodoAi
import com.schedule.app.data.TodoAiKeyStore
import com.schedule.app.model.Todo
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Composable
fun ScreenshotTodoReviewDialog(
    draft: ScreenshotTodoDraft,
    onDismiss: () -> Unit,
    onAiDraft: (ScreenshotTodoDraft) -> Unit,
    organize: (String, String, ScreenshotTodoDraft) -> ScreenshotTodoDraft = TodoAi::organize,
    saving: Boolean = false,
    onKeyChanged: () -> Unit = {},
    semesters: List<com.schedule.app.model.Semester> = emptyList(),
    onSaveMany: ((List<Todo>) -> Unit)? = null,
    onSave: (Todo) -> Unit
) {
    val ctx = LocalContext.current
    val keyStore = remember(ctx.applicationContext) { TodoAiKeyStore(ctx.applicationContext) }
    val aiSession: ScreenshotTodoAiSession = viewModel()
    var optionsEncoded by rememberSaveable { mutableStateOf(ScreenshotTodoOptions().encode()) }
    val options = ScreenshotTodoOptions.decode(optionsEncoded)
    var showOptions by rememberSaveable { mutableStateOf(false) }
    var configured by remember { mutableStateOf(keyStore.configured()) }
    var showKey by remember { mutableStateOf(false) }
    var showConsent by rememberSaveable { mutableStateOf(false) }
    // Offer AI once per review, including after a key is added; never resend on rotation or result updates.
    var autoAiOffered by rememberSaveable { mutableStateOf(false) }
    var showSource by rememberSaveable(draft) { mutableStateOf(false) }
    var title by rememberSaveable(draft) { mutableStateOf(draft.title) }
    var dueDate by rememberSaveable(draft) { mutableStateOf(draft.dueDate?.toString()) }
    var dueTime by rememberSaveable(draft) { mutableStateOf(draft.dueTime.orEmpty()) }
    // An OCR screenshot can contain message timestamps as well as deadlines.
    var dateReviewed by rememberSaveable(draft) { mutableStateOf(false) }
    var timeReviewed by rememberSaveable(draft) { mutableStateOf(false) }
    var noDueDate by rememberSaveable(draft) { mutableStateOf(false) }
    var skipAmbiguousTime by rememberSaveable(draft) { mutableStateOf(false) }
    var chooseDate by rememberSaveable(draft) { mutableStateOf(false) }
    val timeValid = dueTime.isBlank() || runCatching { LocalTime.parse(dueTime) }.isSuccess
    val timeResolved = noDueDate || if (dueTime.isNotBlank()) timeReviewed else draft.ambiguousTimeText == null || skipAmbiguousTime
    val canSave = title.isNotBlank() && (options.remindMinutes == null || (!noDueDate && dueDate != null)) && (noDueDate || timeValid) && timeResolved && (noDueDate || (dueDate != null && dateReviewed))
    val editingEnabled = !saving && !aiSession.working
    LaunchedEffect(configured) {
        if (configured && !autoAiOffered && !aiSession.working && aiSession.result == null) {
            autoAiOffered = true
            showConsent = true
        }
    }
    LaunchedEffect(aiSession.result) {
        aiSession.result?.let { (source, organized) ->
            aiSession.consumeResult()
            if (source == draft) onAiDraft(organized)
        }
    }

    Dialog(
        onDismissRequest = { if (!saving) { aiSession.cancel(); onDismiss() } },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize().imePadding().navigationBarsPadding().padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val sheetHeight = maxHeight * 0.94f
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp).heightIn(max = sheetHeight),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                if (draft.additionalItems.isNotEmpty() && onSaveMany != null) {
                    ScreenshotTodoBatchContent(
                        draft = draft, semesters = semesters, initialFirstOptions = options, enabled = editingEnabled, saving = saving,
                        aiWorking = aiSession.working, aiError = aiSession.error,
                        onAi = { if (!configured) showKey = true else showConsent = true },
                        onStopAi = aiSession::cancel,
                        onCancel = { aiSession.cancel(); onDismiss() },
                        onSave = { todos -> aiSession.cancel(); onSaveMany(todos) }
                    )
                } else Column {
                    Row(
                        Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ))).padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                            Icon(Icons.Default.EventNote, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(11.dp).size(24.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("检查识别结果", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text(if (aiSession.working) "AI 正在整理，请稍候" else "核对内容后添加到待办", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Column(
                        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(title, { title = it }, label = { Text("待办内容") },
                            modifier = Modifier.fillMaxWidth(), enabled = editingEnabled, shape = RoundedCornerShape(16.dp))
                        if (draft.multipleDeliveryFormats) Text("识别到纸质稿和电子稿，提交时间可能不同。请分别核对，必要时创建多条待办。",
                            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("日期与时间", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                if (draft.multipleDateMentions) Text("截图中有多个日期，请确认哪个才是截止日期。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                if (draft.sourceDateText != null) {
                                    Text(if (draft.needsDateConfirmation) "日期“${draft.sourceDateText}”不够明确，请确认或重新选择。"
                                        else "识别到日期：${draft.sourceDateText}",
                                        color = if (draft.needsDateConfirmation) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall)
                                } else if (!noDueDate) {
                                    Text("请选择截止日期，或标记为无截止日期。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                                OutlinedButton(onClick = { chooseDate = true }, enabled = !noDueDate && editingEnabled, modifier = Modifier.fillMaxWidth()) {
                                    Text(dueDate ?: "选择截止日期")
                                }
                                if (dueDate != null && !noDueDate) {
                                    Row(Modifier.fillMaxWidth().toggleable(value = dateReviewed, enabled = editingEnabled, role = Role.Checkbox, onValueChange = { dateReviewed = it }), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = dateReviewed, onCheckedChange = null)
                                        Text("确认 $dueDate 是截止日期", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Row(Modifier.fillMaxWidth().toggleable(value = noDueDate, enabled = editingEnabled, role = Role.Checkbox, onValueChange = {
                                    noDueDate = it
                                    if (it) { dueDate = null; dateReviewed = true } else dateReviewed = false
                                }), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = noDueDate, onCheckedChange = null)
                                    Text("这条待办没有截止日期", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                }
                                OutlinedTextField(dueTime, { dueTime = it; timeReviewed = false }, label = { Text("截止时间（可选，HH:mm）") },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !noDueDate && editingEnabled, shape = RoundedCornerShape(14.dp))
                                if (dueTime.isNotBlank() && !noDueDate && timeValid) {
                                    Row(Modifier.fillMaxWidth().toggleable(value = timeReviewed, enabled = editingEnabled, role = Role.Checkbox, onValueChange = { timeReviewed = it }), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = timeReviewed, onCheckedChange = null)
                                        Text("确认 $dueTime 是截止时间", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                if (draft.ambiguousTimeText != null && !noDueDate) Text(
                                    if (draft.ambiguousTimeText == "识别到多个时间") "识别到多个时间，请核对后选择或填写。"
                                    else "时间“${draft.ambiguousTimeText}”不明确，请核对并填写 24 小时制时间。",
                                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                if (draft.ambiguousTimeText != null && draft.timeCandidates.isNotEmpty() && !noDueDate) {
                                    Text("识别到的时间可能来自状态栏或时间范围，请结合原文选择。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        draft.timeCandidates.forEach { candidate ->
                                            OutlinedButton(enabled = editingEnabled, onClick = { dueTime = candidate; timeReviewed = false; skipAmbiguousTime = false }) { Text(candidate) }
                                        }
                                    }
                                }
                                if (draft.ambiguousTimeText != null && dueTime.isBlank() && !noDueDate) {
                                    Row(Modifier.fillMaxWidth().toggleable(value = skipAmbiguousTime, enabled = editingEnabled, role = Role.Checkbox, onValueChange = { skipAmbiguousTime = it }), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = skipAmbiguousTime, onCheckedChange = null)
                                        Text("确认不设置截止时间", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                if (!timeValid && !noDueDate) Text("时间格式应为 HH:mm。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        TextButton(enabled = editingEnabled, onClick = { showOptions = !showOptions }) {
                            Text(if (showOptions) "收起课程与提醒" else "课程与提醒 · ${if (options.remindMinutes == null) "未设置提醒" else "已设置提醒"}")
                        }
                        if (showOptions) ScreenshotTodoOptionFields(options, semesters, dueDate != null && !noDueDate, editingEnabled) { optionsEncoded = it.encode() }
                        if (options.remindMinutes != null && (dueDate == null || noDueDate)) Text("提醒需要截止日期，请补充日期或关闭提醒。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        if (onSaveMany != null) TextButton(enabled = editingEnabled && timeValid, onClick = {
                            onAiDraft(draft.copy(title = title,
                                dueDate = dueDate?.takeIf { !noDueDate }?.let { LocalDate.parse(it) },
                                dueTime = dueTime.takeIf { !noDueDate && it.isNotBlank() },
                                additionalItems = listOf(com.schedule.app.data.ScreenshotTodoItem("", null, null))))
                        }) { Text("添加另一项") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showSource = !showSource }) { Text(if (showSource) "收起识别原文" else "查看识别原文") }
                            TextButton(enabled = editingEnabled, onClick = { if (!configured) showKey = true else showConsent = true }) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(if (aiSession.working) "AI 整理中…" else "AI 整理文字")
                            }
                        }
                        if (showSource) Text(draft.recognizedText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (aiSession.working) {
                            Text("整理期间暂不可编辑。停止后可继续修改；已发送文字可能已被服务接收。", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = aiSession::cancel) { Text("停止 AI 整理") }
                        }
                        aiSession.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(enabled = !saving, onClick = { aiSession.cancel(); onDismiss() }) { Text("取消") }
                        Button(modifier = Modifier.weight(1f), enabled = canSave && editingEnabled, onClick = {
                            val date = dueDate?.takeIf { !noDueDate }?.let { LocalDate.parse(it) }
                            aiSession.cancel()
                            onSave(Todo(UUID.randomUUID().toString(), title.trim(), null, date?.toString(), false,
                                linkedSemesterId = options.semesterId, linkedCourseId = options.courseId,
                                dueTime = if (date != null && dueTime.isNotBlank()) LocalTime.parse(dueTime).toString() else null,
                                remindMinutesBefore = options.remindMinutes))
                        }) { Text(if (saving) "保存中…" else "确认添加") }
                    }
                }
            }
        }
    }
    if (chooseDate) DatePick(onDismiss = { chooseDate = false }) { selected ->
        dueDate = selected
        dateReviewed = true
        noDueDate = false
        chooseDate = false
    }
    if (showKey) ScreenshotTodoAiKeyDialog(
        onDismiss = { showKey = false },
        onChanged = { configured = keyStore.configured(); showKey = false; onKeyChanged() },
        onError = aiSession::showError
    )
    if (showConsent) AlertDialog(
        onDismissRequest = { showConsent = false },
        title = { Text("发送识别文字给 DeepSeek？") },
        text = { Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
            Text("下方全部识别文字将发送给 DeepSeek 处理，可能包含个人信息并产生 API 费用。图片不会发送。模型结果会替换当前草稿，可能包含多条待办，请逐条核对后保存。")
            if (draft.recognizedText.length > 20_000) Text("识别文字超过 2 万字，暂不能发送。请裁剪图片后重试。", color = MaterialTheme.colorScheme.error)
            Text(draft.recognizedText, style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton(enabled = draft.recognizedText.length <= 20_000, onClick = {
            showConsent = false
            aiSession.start(draft, keyStore::load, organize)
        }) { Text("同意并发送文字") } },
        dismissButton = { Row { TextButton(onClick = { showConsent = false; showKey = true }) { Text("管理密钥") }; TextButton(onClick = { showConsent = false }) { Text("取消") } } }
    )
}
