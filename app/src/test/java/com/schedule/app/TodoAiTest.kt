package com.schedule.app

import com.schedule.app.data.ScreenshotTodoParser
import com.schedule.app.data.TodoAi
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.net.URL
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.Principal
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection
import org.json.JSONObject

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
        listOf("not json", """{"title":""}""", """{"title":null}""", """{"title":"示例","dueDate":"2026-02-30"}""", """{"title":"示例","dueTime":"25:00"}""").forEach {
            assertTrue(runCatching { TodoAi.parseResponse(it, original) }.isFailure)
        }
    }

    @Test fun `request sends only text to fixed HTTPS endpoint and parses response`() {
        val modelOutput = JSONObject().put("title", "提交示例").put("dueDate", "2026-11-20").put("dueTime", "15:00")
        val response = JSONObject().put("choices", org.json.JSONArray().put(JSONObject().put("finish_reason", "stop").put("message", JSONObject().put("content", modelOutput.toString())))).toString()
        lateinit var fake: FakeConnection
        val result = TodoAi.organizeWithConnection("示例文字", "synthetic-key", original) { url ->
            assertEquals("https://api.deepseek.com/chat/completions", url.toString())
            FakeConnection(url, 200, response).also { fake = it }
        }
        val payload = JSONObject(fake.written.toString("UTF-8"))
        assertEquals("POST", fake.requestMethod)
        assertFalse(fake.instanceFollowRedirects)
        assertEquals("Bearer synthetic-key", fake.getRequestProperty("Authorization"))
        assertEquals("示例文字", payload.getJSONArray("messages").getJSONObject(1).getString("content"))
        assertEquals("deepseek-flash", payload.getString("model"))
        assertEquals("disabled", payload.getJSONObject("thinking").getString("type"))
        assertFalse(payload.toString().contains("image_url"))
        assertEquals("提交示例", result.title)
        assertTrue(fake.disconnected)
    }

    @Test fun `server errors and oversized responses fail and close the connection`() {
        listOf(503 to "error", 200 to "x".repeat(100_001)).forEach { (status, body) ->
            lateinit var fake: FakeConnection
            assertTrue(runCatching {
                TodoAi.organizeWithConnection("示例文字", "synthetic-key", original) { FakeConnection(it, status, body).also { connection -> fake = connection } }
            }.isFailure)
            assertTrue(fake.disconnected)
        }
    }

    @Test fun `truncated model output is rejected even if it contains valid JSON`() {
        val partial = JSONObject().put("choices", org.json.JSONArray().put(JSONObject()
            .put("finish_reason", "length")
            .put("message", JSONObject().put("content", """{"title":"示例"}""")))).toString()
        lateinit var fake: FakeConnection
        assertTrue(runCatching {
            TodoAi.organizeWithConnection("示例文字", "synthetic-key", original) { FakeConnection(it, 200, partial).also { c -> fake = c } }
        }.isFailure)
        assertTrue(fake.disconnected)
    }

    private class FakeConnection(url: URL, private val status: Int, private val response: String) : HttpsURLConnection(url) {
        val written = ByteArrayOutputStream()
        var disconnected = false
        override fun getOutputStream() = written
        override fun getInputStream() = ByteArrayInputStream(response.toByteArray(Charsets.UTF_8))
        override fun getResponseCode() = status
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
        override fun connect() = Unit
        override fun getCipherSuite(): String = "test"
        override fun getLocalCertificates(): Array<Certificate>? = null
        override fun getServerCertificates(): Array<Certificate> = emptyArray()
        override fun getPeerPrincipal(): Principal? = null
        override fun getLocalPrincipal(): Principal? = null
    }
}
