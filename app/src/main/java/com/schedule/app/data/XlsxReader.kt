package com.schedule.app.data

import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Bounded XLSX reader; sheet order follows workbook relationships rather than ZIP order. */
object XlsxReader {
    data class Sheet(val name: String, val rows: List<List<String>>)
    fun read(stream: InputStream): List<List<String>> = readSheets(stream).firstOrNull()?.rows.orEmpty()

    fun readSheets(stream: InputStream): List<Sheet> {
        val entries = linkedMapOf<String, ByteArray>()
        var total = 0
        var count = 0
        ZipInputStream(stream.buffered()).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                require(++count <= 2048) { "Excel 文件条目过多" }
                if (e.name == "xl/workbook.xml" || e.name == "xl/_rels/workbook.xml.rels" ||
                    e.name == "xl/sharedStrings.xml" || Regex("xl/worksheets/[^/]+\\.xml").matches(e.name)) {
                    val bytes = zip.readBounded(8 * 1024 * 1024)
                    total += bytes.size
                    require(total <= 24 * 1024 * 1024) { "Excel 表格数据过大" }
                    entries[e.name] = bytes
                }
                zip.closeEntry()
            }
        }
        val shared = entries["xl/sharedStrings.xml"]?.let { bytes ->
            val items = document(bytes).getElementsByTagName("si")
            (0 until items.length).map { textRuns(items.item(it) as Element) }
        }.orEmpty()
        val rels = entries["xl/_rels/workbook.xml.rels"]?.let { bytes ->
            val items = document(bytes).getElementsByTagName("Relationship")
            (0 until items.length).mapNotNull { i ->
                val e = items.item(i) as Element
                if (e.getAttribute("TargetMode") == "External") null else {
                    val target = e.getAttribute("Target")
                    val path = if (target.startsWith("/")) target.removePrefix("/") else "xl/$target"
                    e.getAttribute("Id") to java.net.URI(path).normalize().path
                }
            }.toMap()
        }.orEmpty()
        val ordered = entries["xl/workbook.xml"]?.let { bytes ->
            val items = document(bytes).getElementsByTagName("sheet")
            (0 until items.length).mapNotNull { i ->
                val e = items.item(i) as Element
                val path = rels[e.getAttribute("r:id")]
                path?.let { e.getAttribute("name") to it }
            }
        }.orEmpty()
        val sheets = ordered.ifEmpty { entries.keys.filter { it.startsWith("xl/worksheets/") }.sorted().map { it.substringAfterLast('/') to it } }
        return sheets.mapNotNull { (name, path) -> entries[path]?.let { Sheet(name, parseSheet(it, shared)) } }
    }

    private fun document(bytes: ByteArray): org.w3c.dom.Document {
        // Reject declarations before parsing, including UTF-16 documents, on both Android and JVM parsers.
        val probe = bytes.toString(Charsets.ISO_8859_1).replace("\u0000", "")
        require(!probe.contains("<!DOCTYPE", true) && !probe.contains("<!ENTITY", true)) { "不支持含外部实体的 Excel XML" }
        return DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isExpandEntityReferences = false
        }.newDocumentBuilder().apply {
            setEntityResolver { _, _ -> throw IllegalArgumentException("不允许读取外部实体") }
        }.parse(bytes.inputStream())
    }

    private fun textRuns(element: Element): String {
        val nodes = element.getElementsByTagName("t")
        return (0 until nodes.length).joinToString("") { nodes.item(it).textContent }
    }

    private fun parseSheet(bytes: ByteArray, shared: List<String>): List<List<String>> {
        val doc = document(bytes)
        val rows = doc.getElementsByTagName("row")
        require(rows.length <= 10000) { "Excel 超过 10000 行" }
        val grid = mutableListOf<List<String>>()
        var cellsSeen = 0
        for (i in 0 until rows.length) {
            val cells = (rows.item(i) as Element).getElementsByTagName("c")
            val row = mutableMapOf<Int, String>()
            var nextColumn = 0
            for (j in 0 until cells.length) {
                require(++cellsSeen <= 200000) { "Excel 单元格过多" }
                val cell = cells.item(j) as Element
                val column = cell.getAttribute("r").takeIf { it.isNotEmpty() }?.let(::colIndex) ?: nextColumn
                require(column in 0..511) { "Excel 超过 512 列" }
                nextColumn = column + 1
                val value = cell.getElementsByTagName("v").item(0)?.textContent.orEmpty()
                row[column] = when (cell.getAttribute("t")) {
                    "inlineStr" -> textRuns(cell)
                    "s" -> shared.getOrNull(value.toIntOrNull() ?: -1).orEmpty()
                    else -> value
                }
            }
            if (row.isNotEmpty()) grid.add(List(row.keys.max() + 1) { row[it].orEmpty() })
        }
        return grid
    }

    private fun colIndex(ref: String): Int {
        val letters = ref.takeWhile { it.isLetter() }
        require(letters.length in 1..3) { "Excel 单元格地址无效" }
        return letters.fold(0) { n, c -> n * 26 + (c.uppercaseChar() - 'A' + 1) } - 1
    }
}
