package com.wanderlog.android.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

object FileUtils {

    suspend fun readBytes(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
    }

    suspend fun readText(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)!!.use { it.bufferedReader().readText() }
    }

    fun getMimeType(context: Context, uri: Uri): String? =
        context.contentResolver.getType(uri)

    suspend fun compressImageToJpeg(bytes: ByteArray, maxSizeBytes: Int = 1_048_576): ByteArray =
        withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@withContext bytes
            var quality = 90
            var result: ByteArray
            do {
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                result = out.toByteArray()
                quality -= 10
            } while (result.size > maxSizeBytes && quality > 10)
            bitmap.recycle()
            result
        }

    // Rasterises each PDF page at ~150dpi and returns a list of JPEG byte arrays (max maxPages pages).
    suspend fun rasterizePdf(context: Context, uri: Uri, maxPages: Int = 10): List<ByteArray> =
        withContext(Dispatchers.IO) {
            val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")!!
            pfd.use {
                PdfRenderer(it).use { renderer ->
                    val count = minOf(renderer.pageCount, maxPages)
                    (0 until count).map { idx ->
                        renderer.openPage(idx).use { page ->
                            val scale = 150f / 72f // 150dpi target
                            val width = (page.width * scale).toInt()
                            val height = (page.height * scale).toInt()
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val out = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                            bitmap.recycle()
                            out.toByteArray()
                        }
                    }
                }
            }
        }

    suspend fun renderPdfPages(file: File, maxPages: Int = 10): List<Bitmap> =
        withContext(Dispatchers.IO) {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pfd.use {
                PdfRenderer(it).use { renderer ->
                    val count = minOf(renderer.pageCount, maxPages)
                    (0 until count).map { idx ->
                        renderer.openPage(idx).use { page ->
                            val scale = 150f / 72f
                            val width = (page.width * scale).toInt()
                            val height = (page.height * scale).toInt()
                            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            }
                        }
                    }
                }
            }
        }
}
