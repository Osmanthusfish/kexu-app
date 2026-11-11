package com.schedule.app.data

import java.io.ByteArrayOutputStream
import java.io.InputStream

fun InputStream.readBounded(maxBytes: Int = 32 * 1024 * 1024): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        require(out.size().toLong() + count <= maxBytes) { "文件过大，请选择小于 ${maxBytes / 1024 / 1024} MB 的文件" }
        out.write(buffer, 0, count)
    }
    return out.toByteArray()
}
