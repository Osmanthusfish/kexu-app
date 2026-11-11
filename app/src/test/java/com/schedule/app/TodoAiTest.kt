package com.schedule.app

import com.schedule.app.data.ScreenshotTodoParser
import com.schedule.app.data.TodoAi
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TodoAiTest {
    private val original = ScreenshotTodoParser.parse("示例通知", LocalDate.of(2026, 11, 1))

    @Test fun `valid response remains subject to date review`() {
        val result = TodoAi.parseResponse("""{"title":"提交材料","dueDate":"2026-11-20","dueTime":"15:00","timeUncertain":false}""", original)
        assertEquals(LocalDate.of(2026, 11, 20), result.dueDate)
        assertEquals("15:00", result.dueTime)
        assertTrue(result.needsDateConfirmation)
        assertEquals(original.recognizedText, result.recognizedText)
    }

    @Test fun `uncertain time is never accepted as certain`() {
        val result = TodoAi.parseResponse("""{"title":"提交材料","dueDate":null,"dueTime":"03:00","timeUncertain":true}""", original)
        assertNull(result.dueDate)
        assertNull(result.dueTime)
        assertNotNull(result.ambiguousTimeText)
    }

    @Test fun `invalid model responses fail instead of silently saving`() {
        listOf("not json", """{"title":""}""", """{"title":"示例","dueDate":"2026-02-30"}""", """{"title":"示例","dueTime":"25:00"}""").forEach {
            assertTrue(runCatching { TodoAi.parseResponse(it, original) }.isFailure)
        }
    }
}
