package com.schedule.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.app.data.ScreenshotTodoDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Retains a single, explicitly approved request across Activity recreation. */
class ScreenshotTodoAiSession : ViewModel() {
    var working by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var result by mutableStateOf<Pair<ScreenshotTodoDraft, ScreenshotTodoDraft>?>(null)
        private set
    private var job: Job? = null
    private var generation = 0

    fun start(draft: ScreenshotTodoDraft, loadKey: () -> String, organize: (String, String, ScreenshotTodoDraft) -> ScreenshotTodoDraft) {
        if (working) return
        val current = ++generation
        working = true
        error = null
        result = null
        job = viewModelScope.launch {
            try {
                val organized = withContext(Dispatchers.IO) { organize(draft.recognizedText, loadKey(), draft) }
                if (generation == current) result = draft to organized
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == current) error = "AI 整理失败，请检查网络、密钥和账户状态。原草稿仍在。"
            } finally {
                if (generation == current) { working = false; job = null }
            }
        }
    }

    fun showError(message: String) { error = message }
    fun consumeResult() { result = null }
    fun cancel() {
        generation++
        job?.cancel()
        job = null
        working = false
        error = null
        result = null
    }
}
