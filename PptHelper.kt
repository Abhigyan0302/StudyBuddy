package com.example.myapplication

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFTextShape

class PptHelper {

    suspend fun extractFromPptx(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val ppt = XMLSlideShow(stream)
                val sb = StringBuilder()

                ppt.slides.forEachIndexed { index, slide ->
                    sb.appendLine("=== SLIDE ${index + 1}: ${slide.slideName ?: "Untitled"} ===")
                    slide.shapes
                        .filterIsInstance<XSLFTextShape>()
                        .forEach { shape ->
                            val text = shape.text.trim()
                            if (text.isNotBlank()) sb.appendLine(text)
                        }
                    sb.appendLine()
                }

                ppt.close()
                sb.toString().ifBlank { "No text found in this presentation." }
            } ?: "Could not open file."
        }.getOrElse { e -> "Error reading PPTX: ${e.localizedMessage}" }
    }
}
