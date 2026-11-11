package com.schedule.app.data

import java.io.InputStream

/** 轻量 CSV 解析器：处理 UTF-8 BOM、引号包裹、字段内逗号/换行。 */
object CsvReader {

    fun read(stream: InputStream): List<List<String>> {
        return parse(decode(stream.readBounded()))
    }

    fun decode(bytes: ByteArray): String = when {
        bytes.size >= 2 && bytes[0] == 0xff.toByte() && bytes[1] == 0xfe.toByte() -> bytes.toString(Charsets.UTF_16LE)
        bytes.size >= 2 && bytes[0] == 0xfe.toByte() && bytes[1] == 0xff.toByte() -> bytes.toString(Charsets.UTF_16BE)
        else -> runCatching { Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString() }
            .getOrElse { bytes.toString(java.nio.charset.Charset.forName("GB18030")) }
    }.removePrefix("\uFEFF")

    fun readText(raw: String): List<List<String>> {
        val text = raw.lines().filterNot { it.trim().startsWith("```") }.joinToString("\n").trim()
        require(text.length <= 2 * 1024 * 1024) { "粘贴文本过大" }
        if (text.lines().firstOrNull()?.trim()?.startsWith("|") == true) {
            return text.lines().filter { it.trim().startsWith("|") }.map { line ->
                line.trim().removePrefix("|").removeSuffix("|").split(Regex("(?<!\\\\)\\|"))
                    .map { it.trim().replace("\\|", "|") }
            }.filterNot { row -> row.all { Regex(":?-{3,}:?").matches(it.replace(" ", "")) } }
        }
        return parse(text, if (text.lineSequence().firstOrNull()?.contains('\t') == true) '\t' else ',')
    }

    fun parse(raw: String, delimiter: Char = ','): List<List<String>> {
        val text = raw.removePrefix("\uFEFF").replace("\r\n", "\n").replace("\r", "\n")
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                inQuotes && ch == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    cell.append('"'); i++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == delimiter && !inQuotes -> { row.add(cell.toString()); cell.clear() }
                (ch == '\n') && !inQuotes -> {
                    row.add(cell.toString()); cell.clear()
                    rows.add(row.toList()); row.clear()
                }
                else -> cell.append(ch)
            }
            i++
        }
        require(!inQuotes) { "CSV 引号未闭合，请检查文件" }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString())
            rows.add(row.toList())
        }
        return rows
    }
}
