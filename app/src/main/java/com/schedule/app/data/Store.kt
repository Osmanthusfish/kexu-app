package com.schedule.app.data

import android.content.Context
import com.schedule.app.model.AppData
import java.io.File
import java.util.UUID

/** Committed JSON includes exact image bytes and is the recovery source of truth. */
class Store(private val directory: File) {
    constructor(context: Context) : this(context.filesDir)
    private val snapshots = AtomicSnapshotFile(File(directory, "data.json"), { BackupCodec.decode(it) })
    private val beforeImport = File(directory, "before-import.json")
    var loadWarning: String? = null
        private set

    companion object {
        const val WALLPAPER_MARKER = "app://wallpaper"
        const val WIDGET_WALLPAPER_MARKER = "app://widget_wallpaper"
        private val lock = Any()
        fun wallpaperFile(context: Context) = File(context.filesDir, "wallpaper.img")
        fun widgetWallpaperFile(context: Context) = File(context.filesDir, "widget_wallpaper.img")
    }

    fun load(): AppData = synchronized(lock) {
        val saved = snapshots.readValid()
        if (saved == null) {
            if (snapshots.file.exists() || snapshots.backup.exists()) loadWarning = "数据文件无法读取。原文件已保留，请导入有效备份恢复，暂时不要重新录入。"
            return@synchronized BackupCodec.emptyData()
        }
        val payload = BackupCodec.decode(saved.first)
        if (saved.second) loadWarning = "主数据文件异常，已读取上一份有效备份。"
        restoreImages(payload)
        payload.data
    }

    fun parse(text: String): AppData = BackupCodec.decode(text).data
    fun preview(text: String): AppData = parse(text)
    fun save(data: AppData) = synchronized(lock) { preserveLegacy(); snapshots.write(dump(data)) }

    /** Keep the exact pre-upgrade bytes permanently; rotating .bak is not a version rollback. */
    private fun preserveLegacy() {
        val previous = snapshots.readValid()?.first ?: return
        val schema = org.json.JSONObject(previous).optInt("schemaVersion", 1)
        for ((version, threshold) in listOf("v143" to 2, "v144" to 3, "v145" to 4)) {
            val target = File(directory, "recovery/before-$version-upgrade.json")
            if (schema < threshold && !target.exists()) AtomicSnapshotFile.writeAtomically(target, previous.toByteArray(Charsets.UTF_8))
        }
    }

    /** JSON is committed before materializing images; cancelling a crop never calls this method. */
    fun saveWallpaper(data: AppData, bytes: ByteArray, widget: Boolean) = synchronized(lock) {
        preserveLegacy()
        require(bytes.isNotEmpty() && bytes.size <= BackupCodec.MAX_IMAGE_BYTES) { "壁纸文件过大或为空" }
        val old = BackupCodec.decode(dump(data))
        val payload = if (widget) old.copy(widgetWallpaper = bytes) else old.copy(wallpaper = bytes)
        snapshots.write(BackupCodec.encode(payload))
        restoreImages(payload)
    }

    /** No repeated bitmap decoding or lossy JPEG recompression. */
    fun dump(data: AppData): String = synchronized(lock) {
        val saved = snapshots.readValid()?.let { BackupCodec.decode(it.first) }
        BackupCodec.encode(BackupPayload(data,
            if (data.bgImageUri == WALLPAPER_MARKER) saved?.wallpaper ?: readImage(data.bgImageUri, WALLPAPER_MARKER, "wallpaper.img") else null,
            if (data.widgetBgImageUri == WIDGET_WALLPAPER_MARKER) saved?.widgetWallpaper ?: readImage(data.widgetBgImageUri, WIDGET_WALLPAPER_MARKER, "widget_wallpaper.img") else null))
    }

    fun importBackup(text: String, current: AppData): AppData = synchronized(lock) {
        val payload = BackupCodec.decode(text)
        preserveLegacy()
        val previous = dump(current).toByteArray(Charsets.UTF_8)
        val history = File(directory, "recovery/before-import-${System.currentTimeMillis()}-${UUID.randomUUID()}.json")
        AtomicSnapshotFile.writeAtomically(history, previous)
        AtomicSnapshotFile.writeAtomically(beforeImport, previous)
        if (snapshots.readValid() == null) {
            listOf(snapshots.file, snapshots.backup).filter { it.isFile }.forEach { source ->
                AtomicSnapshotFile.writeAtomically(File(directory, "recovery/${UUID.randomUUID()}-${source.name}"), source.readBytes())
            }
        }
        snapshots.write(BackupCodec.encode(payload), allowRecovery = true)
        restoreImages(payload)
        payload.data
    }

    fun hasImportSnapshot(): Boolean = beforeImport.isFile
    private fun upgradeSnapshot() = File(directory, "recovery/before-v145-upgrade.json").takeIf { it.isFile }
        ?: File(directory, "recovery/before-v144-upgrade.json").takeIf { it.isFile }
        ?: File(directory, "recovery/before-v143-upgrade.json")
    fun hasUpgradeSnapshot(): Boolean = upgradeSnapshot().isFile
    fun exportUpgradeSnapshot(): String = synchronized(lock) {
        val text = AtomicSnapshotFile.read(upgradeSnapshot())
        BackupCodec.decode(text)
        text
    }
    fun restoreBeforeImport(): AppData = synchronized(lock) {
        preserveLegacy()
        val payload = BackupCodec.decode(AtomicSnapshotFile.read(beforeImport))
        snapshots.write(BackupCodec.encode(payload), allowRecovery = true)
        restoreImages(payload)
        payload.data
    }

    private fun readImage(uri: String?, marker: String, name: String): ByteArray? {
        if (uri != marker) return null
        val image = File(directory, name)
        if (!image.isFile) return null
        require(image.length() <= BackupCodec.MAX_IMAGE_BYTES) { "壁纸文件过大，请重新选择较小的图片" }
        return image.readBytes()
    }

    private fun restoreImages(payload: BackupPayload) {
        for ((name, bytes) in listOf("wallpaper.img" to payload.wallpaper, "widget_wallpaper.img" to payload.widgetWallpaper)) {
            if (bytes == null) continue
            val target = File(directory, name)
            try {
                if (!target.isFile || target.length() != bytes.size.toLong() || !target.readBytes().contentEquals(bytes)) {
                    AtomicSnapshotFile.writeAtomically(target, bytes)
                }
            } catch (_: Exception) {
                loadWarning = "数据已保存，壁纸文件暂时无法写入，图片仍保存在备份中。"
            }
        }
    }
}
