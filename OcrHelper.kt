package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException

class OcrHelper {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val maxDimension = 2048

    private fun downscaleBitmap(bitmap: Bitmap): Bitmap {
        val width  = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val scale     = maxDimension.toFloat() / maxOf(width, height)
        val newWidth  = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun extractTextFromBitmap(
        bitmap: Bitmap,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val safe = try {
            downscaleBitmap(bitmap)
        } catch (e: Exception) {
            onError(e)
            return
        }
        val image = InputImage.fromBitmap(safe, 0)
        processImage(image, onSuccess, onError)
    }

    fun extractTextFromUri(
        context: Context,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            // Step 1 — decode bounds only to check dimensions without loading full bitmap
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOpts)
            }

            val width  = boundsOpts.outWidth
            val height = boundsOpts.outHeight

            // Step 2 — calculate safe inSampleSize
            var inSampleSize = 1
            if (width > maxDimension || height > maxDimension) {
                val halfWidth  = width / 2
                val halfHeight = height / 2
                while (halfWidth  / inSampleSize >= maxDimension ||
                    halfHeight / inSampleSize >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            // Step 3 — load downsampled bitmap using RGB_565 (half memory vs ARGB_8888)
            val decodeOpts = BitmapFactory.Options().apply {
                this.inSampleSize  = inSampleSize
                inPreferredConfig  = Bitmap.Config.RGB_565
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: throw IOException("Could not open image stream")

            val image = InputImage.fromBitmap(bitmap, 0)
            processImage(image, onSuccess, onError)

        } catch (e: Exception) {
            onError(e)
        }
    }

    private fun processImage(
        image: InputImage,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (visionText.text.isBlank()) {
                    onSuccess("No text could be recognized in this image.")
                } else {
                    onSuccess(visionText.text)
                }
            }
            .addOnFailureListener { e -> onError(e) }
    }
}
