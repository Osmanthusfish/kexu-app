package com.schedule.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.schedule.app.data.ScreenshotTodoDraft
import com.schedule.app.data.TodoAi
import com.schedule.app.data.TodoAiKeyStore
import com.schedule.app.model.Todo
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ScreenshotTodoReviewDialog(
    draft: ScreenshotTodoDraft,
    onDismiss: () -> Unit,
    onAiDraft: (ScreenshotTodoDraft) -> Unit,
    organize: (String, String, ScreenshotTodoDraft) -> ScreenshotTodoDraft = TodoAi::organize,
    onSave: (Todo) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyStore = remember(ctx) { TodoAiKeyStore(ctx) }
    var configured by remember { mutableStateOf(keyStore.configured()) }
    var showKey by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf("") }
    var showConsent by remember { mutableStateOf(false) }
    var aiWorking by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var title by remember(draft) { mutableStateOf(draft.title) }
    var dueDate by remember(draft) { mutableStateOf(draft.dueDate?.toString()) }
    var dueTime by remember(draft) { mutableStateOf(draft.dueTime.orEmpty()) }
    // An OCR screenshot can contain message timestamps as well as deadlines.
    var dateReviewed by remember(draft) { mutableStateOf(false) }
    var timeReviewed by remember(draft) { mutableStateOf(false) }
    var noDueDate by remember(draft) { mutableStateOf(false) }
    var skipAmbiguousTime by remember(draft) { mutableStateOf(false) }
    var chooseDate by remember(draft) { mutableStateOf(false) }
    val timeValid = dueTime.isBlank() || runCatching { LocalTime.parse(dueTime) }.isSuccess
    val timeResolved = noDueDate || if (dueTime.isNotBlank()) timeReviewed else draft.ambiguousTimeText == null || skipAmbiguousTime
    val canSave = title.isNotBlank() && timeValid && timeResolved && (noDueDate || (dueDate != null && dateReviewed))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("检查识别结果") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("识别在本机完成；只有你确认使用 AI 时才会发送识别文字，图片不会发送。保存前请核对内容。", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(title, { title = it }, label = { Text("待办内容") }, modifier = Modifier.fillMaxWidth())
                if (draft.sourceDateText != null) {
                    Text(
                        if (draft.needsDateConfirmation) "日期“${draft.sourceDateText}”不够明确，请确认或重新选择。"
                        else "识别到日期：${draft.sourceDateText}",
                        color = if (draft.needsDateConfirmation) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (!noDueDate) {
                    Text("没有识别到截止日期，请选择日期，或明确标记为无截止日期。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { chooseDate = true }, enabled = !noDueDate, modifier = Modifier.weight(1f)) {
                        Text(dueDate ?: "选择截止日期")
                    }
                    if (dueDate != null) TextButton(onClick = { dueDate = null; dateReviewed = false }) { Text("清除") }
                }
                if (dueDate != null && !noDueDate) {
                    Row(Modifier.fillMaxWidth().toggleable(value = dateReviewed, role = Role.Checkbox, onValueChange = { dateReviewed = it })) {
                        Checkbox(checked = dateReviewed, onCheckedChange = null)
                        Text("确认 $dueDate 是截止日期", modifier = Modifier.weight(1f))
                    }
                }
                Row {
                    Checkbox(checked = noDueDate, onCheckedChange = {
                        noDueDate = it
                        if (it) { dueDate = null; dateReviewed = true } else dateReviewed = false
                    })
                    Text("这条待办没有截止日期", modifier = Modifier.weight(1f).then(Modifier))
                }
                OutlinedTextField(dueTime, { dueTime = it; timeReviewed = false }, label = { Text("截止时间（可选，HH:mm）") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !noDueDate)
                if (dueTime.isNotBlank() && !noDueDate && timeValid) {
                    Row(Modifier.fillMaxWidth().toggleable(value = timeReviewed, role = Role.Checkbox, onValueChange = { timeReviewed = it })) {
                        Checkbox(checked = timeReviewed, onCheckedChange = null)
                        Text("确认 $dueTime 是截止时间", modifier = Modifier.weight(1f))
                    }
                }
                if (draft.ambiguousTimeText != null) Text("时间“${draft.ambiguousTimeText}”不明确，请核对并填写 24 小时制时间。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (draft.ambiguousTimeText != null && dueTime.isBlank() && !noDueDate) {
                    Row {
                        Checkbox(checked = skipAmbiguousTime, onCheckedChange = { skipAmbiguousTime = it })
                        Text("确认不设置截止时间", modifier = Modifier.weight(1f))
                    }
                }
                if (!timeValid) Text("时间格式应为 HH:mm。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Text("识别文字：${draft.recognizedText.take(500)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row {
                    TextButton(enabled = !aiWorking, onClick = { showKey = true }) { Text(if (configured) "管理 AI 密钥" else "设置 AI 密钥") }
                    TextButton(enabled = !aiWorking, onClick = {
                        if (!configured) showKey = true else showConsent = true
                    }) { Text(if (aiWorking) "AI 整理中…" else "AI 整理文字") }
                }
                aiError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(enabled = canSave, onClick = {
                val date = dueDate?.takeIf { !noDueDate }?.let { LocalDate.parse(it) }
                onSave(Todo(UUID.randomUUID().toString(), title.trim(), null, date?.toString(), false,
                    dueTime = if (date != null && dueTime.isNotBlank()) LocalTime.parse(dueTime).toString() else null))
            }) { Text("确认添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
    if (chooseDate) DatePick(onDismiss = { chooseDate = false }) { selected ->
        dueDate = selected
        dateReviewed = true
        noDueDate = false
        chooseDate = false
    }
    if (showKey) AlertDialog(
        onDismissRequest = { keyInput = ""; showKey = false },
        title = { Text("DeepSeek API 密钥") },
        text = { Column {
            Text("密钥加密保存在本机、不写入备份。调用 DeepSeek 可能产生费用。", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(keyInput, { keyInput = it }, label = { Text("API Key") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            if (configured) TextButton(onClick = {
                runCatching { keyStore.clear() }.onSuccess { configured = false; keyInput = ""; showKey = false }
                    .onFailure { aiError = "无法删除密钥，请重试。" }
            }) { Text("删除已保存的密钥") }
        } },
        confirmButton = { TextButton(enabled = keyInput.isNotBlank(), onClick = {
            runCatching { keyStore.save(keyInput) }.onSuccess { configured = true; keyInput = ""; showKey = false }
                .onFailure { aiError = "无法保存密钥，请重试。" }
        }) { Text("保存") } },
        dismissButton = { TextButton(onClick = { keyInput = ""; showKey = false }) { Text("取消") } }
    )
    if (showConsent) AlertDialog(
        onDismissRequest = { showConsent = false },
        title = { Text("发送识别文字给 DeepSeek？") },
        text = { Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
            Text("下方全部识别文字将发送给 DeepSeek 处理，可能包含个人信息并产生 API 费用。图片不会发送。模型结果会替换当前草稿，请再次核对后保存。")
            if (draft.recognizedText.length > 20_000) Text("识别文字超过 2 万字，暂不能发送。请裁剪图片后重试。", color = MaterialTheme.colorScheme.error)
            Text(draft.recognizedText, style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton(enabled = draft.recognizedText.length <= 20_000, onClick = {
            showConsent = false
            aiWorking = true
            aiError = null
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) { organize(draft.recognizedText, keyStore.load(), draft) }
                    onAiDraft(result)
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { aiError = "AI 整理失败，请检查网络、密钥和账户状态。原草稿仍在。" }
                finally { aiWorking = false }
            }
        }) { Text("同意并发送文字") } },
        dismissButton = { TextButton(onClick = { showConsent = false }) { Text("取消") } }
    )
}
