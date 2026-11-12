package com.schedule.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.schedule.app.data.ScreenshotTodoDraft
import com.schedule.app.model.Todo
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

private data class ReviewItem(
    val id: String, val title: String, val date: String, val time: String,
    val selected: Boolean = true, val confirmed: Boolean = false,
    val options: ScreenshotTodoOptions = ScreenshotTodoOptions(), val showOptions: Boolean = false
) {
    fun encode(): String = JSONObject().put("id", id).put("title", title).put("date", date)
        .put("time", time).put("selected", selected).put("confirmed", confirmed)
        .put("options", options.encode()).put("showOptions", showOptions).toString()
    fun valid(): Boolean = title.isNotBlank() &&
        (date.isBlank() || runCatching { LocalDate.parse(date) }.isSuccess) &&
        (time.isBlank() || (date.isNotBlank() && runCatching { LocalTime.parse(time) }.isSuccess)) &&
        (options.remindMinutes == null || date.isNotBlank())
    fun todo() = Todo(id, title.trim(), null, date.ifBlank { null }, false,
        linkedSemesterId = options.semesterId, linkedCourseId = options.courseId,
        dueTime = time.takeIf { date.isNotBlank() && it.isNotBlank() }?.let { LocalTime.parse(it).toString() },
        remindMinutesBefore = options.remindMinutes)
    companion object {
        fun decode(value: String): ReviewItem = JSONObject(value).let {
            ReviewItem(it.getString("id"), it.getString("title"), it.getString("date"), it.getString("time"),
                it.getBoolean("selected"), it.getBoolean("confirmed"),
                ScreenshotTodoOptions.decode(it.optString("options").ifBlank { ScreenshotTodoOptions().encode() }), it.optBoolean("showOptions", false))
        }
    }
}

@Composable
fun ScreenshotTodoBatchContent(
    draft: ScreenshotTodoDraft, semesters: List<com.schedule.app.model.Semester>, initialFirstOptions: ScreenshotTodoOptions,
    enabled: Boolean, saving: Boolean, aiWorking: Boolean, aiError: String?,
    onAi: () -> Unit, onStopAi: () -> Unit, onCancel: () -> Unit, onSave: (List<Todo>) -> Unit
) {
    val initial = listOf(ReviewItem(UUID.randomUUID().toString(), draft.title, draft.dueDate?.toString().orEmpty(), draft.dueTime.orEmpty(), options = initialFirstOptions)) +
        draft.additionalItems.map { ReviewItem(UUID.randomUUID().toString(), it.title, it.dueDate?.toString().orEmpty(), it.dueTime.orEmpty()) }
    var encoded by rememberSaveable(draft) { mutableStateOf(ArrayList(initial.map { it.encode() })) }
    var batchOptionsEncoded by rememberSaveable(draft) { mutableStateOf(ScreenshotTodoOptions().encode()) }
    var showBatchOptions by rememberSaveable(draft) { mutableStateOf(false) }
    val batchOptions = ScreenshotTodoOptions.decode(batchOptionsEncoded)
    var showSource by rememberSaveable(draft) { mutableStateOf(false) }
    var pickingIndex by rememberSaveable(draft) { mutableIntStateOf(-1) }
    val items = encoded.map(ReviewItem::decode)
    fun update(index: Int, item: ReviewItem) { encoded = ArrayList(encoded).also { it[index] = item.encode() } }
    val selected = items.filter { it.selected }
    val ready = selected.isNotEmpty() && selected.all { it.valid() && it.confirmed }
    Column {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f)).padding(18.dp)) {
            Text("核对待办", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("识别到 ${items.size} 项 · 每项确认后一起添加", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("日期和时间可能识别有误，请结合原文核对；不需要的项目可取消勾选。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (draft.mayHaveMoreItems) Text("本次最多处理 15 项，请查看原文，剩余事项可分批添加。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            TextButton(enabled = enabled, onClick = { showBatchOptions = !showBatchOptions }) { Text(if (showBatchOptions) "收起批量设置" else "批量设置课程与提醒") }
            if (showBatchOptions) {
                ScreenshotTodoOptionFields(batchOptions, semesters, selected.isNotEmpty() && selected.all { it.date.isNotBlank() }, enabled) { batchOptionsEncoded = it.encode() }
                TextButton(enabled = enabled && selected.isNotEmpty() && (batchOptions.remindMinutes == null || selected.all { it.date.isNotBlank() }), onClick = {
                    encoded = ArrayList(items.map { if (it.selected) it.copy(options = batchOptions, confirmed = false).encode() else it.encode() })
                }) { Text("将课程与提醒应用到已选项") }
            }
            items.forEachIndexed { index, item ->
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth().toggleable(value = item.selected, enabled = enabled, role = Role.Checkbox,
                            onValueChange = { update(index, item.copy(selected = it, confirmed = false)) }), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = item.selected, enabled = enabled, onCheckedChange = null)
                            Text("待办 ${index + 1}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        }
                        OutlinedTextField(item.title, { update(index, item.copy(title = it, confirmed = false)) },
                            label = { Text("待办内容") }, modifier = Modifier.fillMaxWidth(), enabled = enabled && item.selected)
                        OutlinedTextField(item.date, { update(index, item.copy(date = it, confirmed = false)) },
                            label = { Text("截止日期 YYYY-MM-DD（可留空）") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = enabled && item.selected)
                        TextButton(enabled = enabled && item.selected, onClick = { pickingIndex = index }) { Text("选择日期") }
                        OutlinedTextField(item.time, { update(index, item.copy(time = it, confirmed = false)) },
                            label = { Text("截止时间 HH:mm（可留空）") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = enabled && item.selected)
                        TextButton(enabled = enabled && item.selected, onClick = { update(index, item.copy(showOptions = !item.showOptions)) }) {
                            Text(if (item.showOptions) "收起课程与提醒" else "课程与提醒 · ${if (item.options.remindMinutes == null) "未设置提醒" else "已设置提醒"}")
                        }
                        if (item.showOptions) ScreenshotTodoOptionFields(item.options, semesters, item.date.isNotBlank(), enabled && item.selected) { update(index, item.copy(options = it, confirmed = false)) }
                        if (item.selected && !item.valid()) Text("请检查内容、日期和时间；设置时间或提醒时须有截止日期。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth().toggleable(value = item.confirmed, enabled = enabled && item.selected && item.valid(), role = Role.Checkbox,
                            onValueChange = { update(index, item.copy(confirmed = it)) }), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = item.confirmed, enabled = enabled && item.selected && item.valid(), onCheckedChange = null)
                            Text("已核对第 ${index + 1} 项内容与日期时间", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            TextButton(enabled = enabled && items.size < 15, onClick = {
                encoded = ArrayList(encoded).also { it.add(ReviewItem(UUID.randomUUID().toString(), "", "", "").encode()) }
            }) { Text("添加一项") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { showSource = !showSource }) { Text(if (showSource) "收起识别原文" else "查看识别原文") }
                TextButton(enabled = enabled, onClick = onAi) { Text("AI 重新整理") }
            }
            if (showSource) Text(draft.recognizedText, style = MaterialTheme.typography.bodySmall)
            if (aiWorking) {
                Text("AI 整理中，当前内容暂不可编辑。", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onStopAi) { Text("停止 AI 整理") }
            }
            aiError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(enabled = !saving, onClick = onCancel) { Text("取消") }
            Button(modifier = Modifier.weight(1f), enabled = enabled && ready, onClick = { onSave(selected.map { it.todo() }) }) {
                Text(if (saving) "保存中…" else "添加 ${selected.size} 项")
            }
        }
    }
    if (pickingIndex in items.indices) DatePick(onDismiss = { pickingIndex = -1 }) { date ->
        val index = pickingIndex
        if (index in items.indices) update(index, items[index].copy(date = date, confirmed = false))
        pickingIndex = -1
    }
}
