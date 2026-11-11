package com.schedule.app.data

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** A failed write never replaces the current file or the last readable backup. */
internal class AtomicSnapshotFile(
    val file: File,
    private val validate: (String) -> Unit,
    private val beforeCommit: () -> Unit = {}
) {
    val backup = File(file.parentFile, file.name + ".bak")

    fun readValid(): Pair<String, Boolean>? {
        for ((candidate, recovered) in listOf(file to false, backup to true)) {
            if (!candidate.isFile) continue
            val text = runCatching { read(candidate).also(validate) }.getOrNull() ?: continue
            return text to recovered
        }
        return null
    }

    fun write(text: String, allowRecovery: Boolean = false) {
        validate(text)
        val valid = readValid()
        if (valid == null && (file.exists() || backup.exists()) && !allowRecovery) {
            throw IOException("现有数据文件无法读取，原文件已保留。请先导入有效备份恢复。")
        }
        // Never rotate a corrupted main file over the last valid backup.
        if (valid != null && !valid.second) writeAtomically(backup, valid.first.toByteArray(Charsets.UTF_8))
        beforeCommit()
        writeAtomically(file, text.toByteArray(Charsets.UTF_8))
    }

    companion object {
        const val MAX_BYTES = 32 * 1024 * 1024

        fun read(file: File): String {
            require(file.length() <= MAX_BYTES) { "备份文件超过 32 MB，请选择较小的文件" }
            return file.readText(Charsets.UTF_8)
        }

        fun writeAtomically(file: File, bytes: ByteArray) {
            val parent = file.parentFile ?: throw IOException("文件缺少保存目录")
            if (!parent.isDirectory && !parent.mkdirs()) throw IOException("无法创建保存目录")
            val temp = File.createTempFile(file.name + ".", ".pending", parent)
            try {
                FileOutputStream(temp).use { output -> output.write(bytes); output.fd.sync() }
                try {
                    Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            } finally {
                temp.delete()
            }
        }
    }
}
