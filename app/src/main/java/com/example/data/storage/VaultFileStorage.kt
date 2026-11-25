package com.example.data.storage

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.data.local.LessonType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ImportedVaultFile(
    val relativePath: String,
    val originalFileName: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val detectedType: LessonType,
    val pageCount: Int,
    val extractedTextPreview: String = "",
)

class VaultFileStorage(private val context: Context) {

    private val vaultDir: File
        get() = File(context.filesDir, "lesson_vault").apply {
            if (!exists()) mkdirs()
        }

    suspend fun importUri(uri: Uri, forceImage: Boolean = false): ImportedVaultFile = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val originalName = queryDisplayName(uri) ?: if (forceImage) "lesson_image.jpg" else "lesson_document"
        val rawMime = resolver.getType(uri)
        val extFromName = originalName.substringAfterLast('.', "").lowercase()
        val extFromMime = rawMime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }?.lowercase()
        val extension = when {
            extFromName.isNotBlank() && extFromName.length <= 8 -> extFromName
            !extFromMime.isNullOrBlank() -> extFromMime
            forceImage -> "jpg"
            else -> "bin"
        }
        val resolvedMime = rawMime
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: if (forceImage) "image/jpeg" else "application/octet-stream"

        val storedFileName = "${UUID.randomUUID()}.$extension"
        val targetFile = File(vaultDir, storedFileName)

        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        } ?: error("Cannot open selected file")

        val sizeBytes = targetFile.length()
        val lessonType = classifyLessonType(resolvedMime, extension, forceImage)
        val pageCount = if (lessonType == LessonType.PDF) countPdfPages(targetFile) else 0
        val textPreview = if (lessonType == LessonType.NOTE && sizeBytes <= 512_000L) {
            runCatching { targetFile.readText(Charsets.UTF_8) }.getOrDefault("")
        } else {
            ""
        }

        ImportedVaultFile(
            relativePath = storedFileName,
            originalFileName = originalName,
            mimeType = resolvedMime,
            fileSizeBytes = sizeBytes,
            detectedType = lessonType,
            pageCount = pageCount,
            extractedTextPreview = textPreview,
        )
    }

    fun resolveFile(relativePath: String?): File? {
        if (relativePath.isNullOrBlank()) return null
        val file = File(vaultDir, relativePath)
        return if (file.exists()) file else null
    }

    suspend fun readTextFile(relativePath: String?): String = withContext(Dispatchers.IO) {
        val file = resolveFile(relativePath) ?: return@withContext ""
        runCatching { file.readText(Charsets.UTF_8) }.getOrDefault("")
    }

    fun deleteFile(relativePath: String?) {
        if (relativePath.isNullOrBlank()) return
        runCatching {
            val file = File(vaultDir, relativePath)
            if (file.exists()) file.delete()
        }
    }

    fun getShareUri(relativePath: String?): Uri? {
        val file = resolveFile(relativePath) ?: return null
        return runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    }

    private fun queryDisplayName(uri: Uri): String? {
        val fromCursor = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull()
        return fromCursor?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
    }

    private fun classifyLessonType(mimeType: String, ext: String, forceImage: Boolean): LessonType {
        if (forceImage || mimeType.startsWith("image/") || ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")) {
            return LessonType.IMAGE
        }
        if (mimeType == "application/pdf" || ext == "pdf") {
            return LessonType.PDF
        }
        if (mimeType.startsWith("text/") || ext in setOf("txt", "md", "markdown", "csv", "json", "log")) {
            return LessonType.NOTE
        }
        return LessonType.DOCUMENT
    }

    private fun countPdfPages(file: File): Int {
        return runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        }.getOrDefault(0)
    }
}
