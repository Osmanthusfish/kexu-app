package com.schedule.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.schedule.app.data.TodoAiKeyStore

/** The key is never exposed by the UI, even when a saved key is being replaced. */
@Composable
fun ScreenshotTodoAiKeyDialog(onDismiss: () -> Unit, onChanged: () -> Unit, onError: (String) -> Unit, onPrivacy: (() -> Unit)? = null) {
    val context = LocalContext.current
    val store = remember(context.applicationContext) { TodoAiKeyStore(context.applicationContext) }
    val configured = remember { store.configured() }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { input = ""; onDismiss() },
        title = { Text("DeepSeek API 密钥") },
        text = { Column {
            Text("填写后可使用 DeepSeek 整理识别文字，发送前会再次征求同意，调用可能产生费用。密钥加密保存在本机，不写入备份。不填写也能使用本地截图识别。", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(input, { input = it }, modifier = Modifier.fillMaxWidth(), label = { Text(if (configured) "输入新 API Key（替换原密钥）" else "API Key") },
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false), singleLine = true)
            if (onPrivacy != null) TextButton(onClick = onPrivacy) { Text("截图识别隐私说明") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (configured) TextButton(onClick = {
                runCatching { store.clear() }.onSuccess { input = ""; onChanged() }
                    .onFailure { error = "无法删除密钥，请重试。"; onError(error!!) }
            }) { Text("删除已保存的密钥") }
        } },
        confirmButton = { TextButton(enabled = input.isNotBlank(), onClick = {
            runCatching { store.save(input) }.onSuccess { input = ""; onChanged() }
                .onFailure { error = "无法保存密钥，请重试。"; onError(error!!) }
        }) { Text("保存") } },
        dismissButton = { TextButton(onClick = { input = ""; onDismiss() }) { Text("取消") } }
    )
}
