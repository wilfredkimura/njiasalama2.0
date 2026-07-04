package com.njiasalama.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {
    /**
     * Reads an image from the given content Uri, downscales it to fit within 800x800 bounding box,
     * compresses it as JPEG with 75% quality, and encodes it into a Base64 data URI string.
     */
    fun compressUriToBase64(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            inputStream.close()

            // Scale bitmap down to a reasonable max size (e.g. 800px width/height) to save database space
            val maxDimension = 800
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val newWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
                val newHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
                Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            // Compress as JPEG with 75% quality
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val bytes = outputStream.toByteArray()
            
            // Clean up bitmap references
            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            originalBitmap.recycle()

            // Convert to Base64 data URL
            val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
            "data:image/jpeg;base64,${base64String.trim().replace("\n", "").replace("\r", "")}"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
