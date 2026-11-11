package com.schedule.app.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.util.concurrent.TimeUnit

class ScreenshotTooLargeException : IllegalArgumentException()

/** Runs the bundled Chinese recognizer locally. Call from a worker dispatcher. */
object ScreenshotTodoOcr {
    fun recognize(context: Context, uri: Uri): String {
        // Compressed images may decode to very large bitmaps; reject those before ML Kit allocates one.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds); Unit }
            ?: throw IllegalArgumentException("无法读取图片")
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法读取图片" }
        if (bounds.outWidth.toLong() * bounds.outHeight > 24_000_000L) throw ScreenshotTooLargeException()
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        return try {
            Tasks.await(recognizer.process(image), 60, TimeUnit.SECONDS).text
        } finally {
            recognizer.close()
        }
    }
}
