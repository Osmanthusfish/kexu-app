package com.schedule.app.data

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions

/** Runs the bundled Chinese recognizer locally. Call from a worker dispatcher. */
object ScreenshotTodoOcr {
    fun recognize(context: Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        return try {
            Tasks.await(recognizer.process(image)).text
        } finally {
            recognizer.close()
        }
    }
}
