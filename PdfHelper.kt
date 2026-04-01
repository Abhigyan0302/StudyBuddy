package com.example.myapplication

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PdfHelper(private val context: Context) {

    init { PDFBoxResourceLoader.init(context) }

    suspend fun extractFromPdf(
        uri: Uri,
        ocrHelper: OcrHelper,
        onProgress: (String) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        val result = StringBuilder()

        context.contentResolver.openInputStream(uri)?.use { stream ->
            PDDocument.load(stream).use { doc ->
                val pageCount = doc.numberOfPages
                onProgress("Found $pageCount pages, extracting text...")

                // 1. Native text layer — fast, handles most digital PDFs
                val nativeText = PDFTextStripper().getText(doc).trim()
                if (nativeText.isNotBlank()) {
                    result.append("=== TEXT LAYER ===\n$nativeText\n\n")
                }

                // 2. Render each page as bitmap → OCR for scanned pages and tables
                val renderer = PDFRenderer(doc)
                val limit = minOf(pageCount, 10)
                for (i in 0 until limit) {
                    onProgress("OCR scanning page ${i + 1}/$limit...")
                    try {
                        val bitmap = renderer.renderImageWithDPI(i, 200f)
                        val latch = CountDownLatch(1)
                        var ocrText = ""
                        ocrHelper.extractTextFromBitmap(
                            bitmap    = bitmap,
                            onSuccess = { ocrText = it; latch.countDown() },
                            onError   = { latch.countDown() }
                        )
                        latch.await(15, TimeUnit.SECONDS)
                        // Only append OCR text for scanned PDFs where native text was blank
                        if (nativeText.isBlank() && ocrText.isNotBlank() &&
                            ocrText != "No text could be recognized in this image.") {
                            result.append("=== PAGE ${i + 1} (OCR) ===\n$ocrText\n\n")
                        }
                        bitmap.recycle()
                    } catch (_: Exception) { /* skip unrenderable pages */ }
                }
            }
        }
        result.toString().ifBlank { "Could not extract text from this PDF." }
    }
}
