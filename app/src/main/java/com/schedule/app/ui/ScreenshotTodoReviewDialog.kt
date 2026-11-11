package com.schedule.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schedule.app.data.ScreenshotTodoDraft
import com.schedule.app.model.Todo
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Composable
fun ScreenshotTodoReviewDialog(
    draft: ScreenshotTodoDraft,
    onDismiss: () -> Unit,
    onSave: (Todo) -> Unit
) {
    var title by remember(draft) { mutableStateOf(draft.title) }
    var dueDate by remember(draft) { mutableStateOf(draft.dueDate?.toString()) }
    var dueTime by remember(draft) { mutableStateOf(draft.dueTime.orEmpty()) }
    var dateReviewed by remember(draft) { mutableStateOf(!draft.needsDateConfirmation && draft.dueDate != null) }
    var noDueDate by remember(draft) { mutableStateOf(false) }
    var chooseDate by remember(draft) { mutableStateOf(false) }
    val timeValid = dueTime.isBlank() || runCatching { LocalTime.parse(dueTime) }.isSuccess
    val canSave = title.isNotBlank() && timeValid && (noDueDate || (dueDate != null && dateReviewed))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("检查识别结果") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("识别在本机完成，图片没有上传。保存前请核对待办和日期。", style = MaterialTheme.typography.bodySmall)
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
                Row {
                    Checkbox(checked = noDueDate, onCheckedChange = {
                        noDueDate = it
                        if (it) { dueDate = null; dateReviewed = true } else dateReviewed = false
                    })
                    Text("这条待办没有截止日期", modifier = Modifier.weight(1f).then(Modifier))
                }
                OutlinedTextField(dueTime, { dueTime = it }, label = { Text("截止时间（可选，HH:mm）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (!timeValid) Text("时间格式应为 HH:mm。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Text("识别文字：${draft.recognizedText.take(500)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}
