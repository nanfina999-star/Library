package com.example.data.repository

import android.net.Uri
import com.example.data.local.LessonDao
import com.example.data.local.LessonEntity
import com.example.data.local.LessonType
import com.example.data.local.SubjectDao
import com.example.data.local.SubjectEntity
import com.example.data.local.formatLabelsStorage
import com.example.data.storage.ImportedVaultFile
import com.example.data.storage.VaultFileStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class LessonRepository(
    private val subjectDao: SubjectDao,
    private val lessonDao: LessonDao,
    private val fileStorage: VaultFileStorage,
) {
    private val seedMutex = Mutex()

    val subjects: Flow<List<SubjectEntity>> = subjectDao.observeAllSubjects()
    val allLessons: Flow<List<LessonEntity>> = lessonDao.observeAllLessons()
    val pinnedLessons: Flow<List<LessonEntity>> = lessonDao.observePinnedLessons()

    fun observeLesson(lessonId: Long): Flow<LessonEntity?> = lessonDao.observeLessonById(lessonId)

    fun observeLessonsBySubject(subjectId: Long): Flow<List<LessonEntity>> =
        lessonDao.observeLessonsBySubject(subjectId)

    suspend fun ensureDefaultSubjects() {
        seedMutex.withLock {
            if (subjectDao.countSubjects() == 0) {
                val now = System.currentTimeMillis()
                val defaults = listOf(
                    SubjectEntity(name = "ទូទៅ", colorHex = 0xFF0C4E5E, iconKey = "folder", createdAt = now),
                    SubjectEntity(name = "គណិតវិទ្យា", colorHex = 0xFFD97706, iconKey = "math", createdAt = now + 1),
                    SubjectEntity(name = "ប្រវត្តិសាស្ត្រ", colorHex = 0xFF86198F, iconKey = "history", createdAt = now + 2),
                    SubjectEntity(name = "វិទ្យាសាស្ត្រ", colorHex = 0xFF2E6F5E, iconKey = "science", createdAt = now + 3),
                    SubjectEntity(name = "ភាសា", colorHex = 0xFF9A3412, iconKey = "language", createdAt = now + 4),
                    SubjectEntity(name = "ព័ត៌មានវិទ្យា", colorHex = 0xFF3730A3, iconKey = "code", createdAt = now + 5),
                )
                subjectDao.insertSubjects(defaults)
            }
        }
    }

    suspend fun stageImportFromUri(uri: Uri, forceImage: Boolean): ImportedVaultFile =
        fileStorage.importUri(uri, forceImage)

    fun discardStagedImport(staged: ImportedVaultFile) {
        fileStorage.deleteFile(staged.relativePath)
    }

    suspend fun commitImportedLesson(
        staged: ImportedVaultFile,
        title: String,
        description: String,
        subjectId: Long,
        studyNote: String,
        labels: List<String> = emptyList(),
    ): Long {
        val now = System.currentTimeMillis()
        val combinedNote = when {
            staged.detectedType == LessonType.NOTE && staged.extractedTextPreview.isNotBlank() -> {
                if (studyNote.isBlank()) staged.extractedTextPreview
                else "$studyNote\n\n---\n\n${staged.extractedTextPreview}"
            }
            else -> studyNote
        }
        val entity = LessonEntity(
            subjectId = subjectId,
            title = title.trim().ifBlank { staged.originalFileName.substringBeforeLast('.') },
            description = description.trim(),
            type = staged.detectedType,
            internalFilePath = staged.relativePath,
            originalFileName = staged.originalFileName,
            mimeType = staged.mimeType,
            fileSizeBytes = staged.fileSizeBytes,
            noteContent = combinedNote,
            labels = formatLabelsStorage(labels),
            pageCount = staged.pageCount,
            lastPageRead = 0,
            isPinned = false,
            createdAt = now,
            updatedAt = now,
        )
        return lessonDao.insertLesson(entity)
    }

    suspend fun saveNoteLesson(
        lessonId: Long?,
        title: String,
        description: String,
        subjectId: Long,
        noteContent: String,
        labels: List<String> = emptyList(),
    ): Long {
        val now = System.currentTimeMillis()
        val byteSize = noteContent.toByteArray(Charsets.UTF_8).size.toLong()
        val formattedLabels = formatLabelsStorage(labels)
        return if (lessonId == null || lessonId <= 0L) {
            lessonDao.insertLesson(
                LessonEntity(
                    subjectId = subjectId,
                    title = title.trim(),
                    description = description.trim(),
                    type = LessonType.NOTE,
                    internalFilePath = null,
                    originalFileName = null,
                    mimeType = "text/plain",
                    fileSizeBytes = byteSize,
                    noteContent = noteContent,
                    labels = formattedLabels,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        } else {
            val existing = lessonDao.getLessonById(lessonId)
            if (existing != null) {
                lessonDao.updateLesson(
                    existing.copy(
                        title = title.trim(),
                        description = description.trim(),
                        subjectId = subjectId,
                        noteContent = noteContent,
                        labels = formattedLabels,
                        fileSizeBytes = if (existing.internalFilePath == null) byteSize else existing.fileSizeBytes,
                        updatedAt = now,
                    )
                )
            }
            lessonId
        }
    }

    suspend fun updateLessonMetadata(
        lessonId: Long,
        title: String,
        description: String,
        subjectId: Long,
        labels: List<String>,
    ) {
        val existing = lessonDao.getLessonById(lessonId) ?: return
        lessonDao.updateLesson(
            existing.copy(
                title = title.trim().ifBlank { existing.title },
                description = description.trim(),
                subjectId = subjectId,
                labels = formatLabelsStorage(labels),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun updateLessonCategoryAndLabels(
        lessonId: Long,
        subjectId: Long,
        labels: List<String>,
    ) {
        val existing = lessonDao.getLessonById(lessonId) ?: return
        lessonDao.updateLesson(
            existing.copy(
                subjectId = subjectId,
                labels = formatLabelsStorage(labels),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun renameLabelAcrossLessons(oldLabel: String, newLabel: String): Int {
        val cleanNew = newLabel.trim().replace(",", "")
        if (oldLabel.isBlank() || cleanNew.isBlank()) return 0
        val now = System.currentTimeMillis()
        var updatedCount = 0
        lessonDao.getAllLessonsOnce().forEach { lesson ->
            val currentLabels = lesson.labels.split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (currentLabels.any { it.equals(oldLabel, ignoreCase = true) }) {
                val replaced = currentLabels.map { tag ->
                    if (tag.equals(oldLabel, ignoreCase = true)) cleanNew else tag
                }
                lessonDao.updateLesson(
                    lesson.copy(
                        labels = formatLabelsStorage(replaced),
                        updatedAt = now,
                    )
                )
                updatedCount++
            }
        }
        return updatedCount
    }

    suspend fun removeLabelFromAllLessons(label: String): Int {
        if (label.isBlank()) return 0
        val now = System.currentTimeMillis()
        var updatedCount = 0
        lessonDao.getAllLessonsOnce().forEach { lesson ->
            val currentLabels = lesson.labels.split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (currentLabels.any { it.equals(label, ignoreCase = true) }) {
                val filtered = currentLabels.filterNot { it.equals(label, ignoreCase = true) }
                lessonDao.updateLesson(
                    lesson.copy(
                        labels = formatLabelsStorage(filtered),
                        updatedAt = now,
                    )
                )
                updatedCount++
            }
        }
        return updatedCount
    }

    suspend fun togglePinned(lesson: LessonEntity) {
        lessonDao.setPinned(lesson.id, !lesson.isPinned)
    }

    suspend fun updateLastPage(lessonId: Long, page: Int) {
        lessonDao.updateLastPage(lessonId, page)
    }

    suspend fun updateStudyNotes(lessonId: Long, noteContent: String) {
        lessonDao.updateStudyNotes(lessonId, noteContent)
    }

    suspend fun deleteLesson(lesson: LessonEntity) {
        fileStorage.deleteFile(lesson.internalFilePath)
        lessonDao.deleteLessonById(lesson.id)
    }

    suspend fun addSubject(name: String, colorHex: Long, iconKey: String): Long {
        return subjectDao.insertSubject(
            SubjectEntity(
                name = name.trim(),
                colorHex = colorHex,
                iconKey = iconKey,
            )
        )
    }

    suspend fun updateSubject(subject: SubjectEntity) {
        subjectDao.updateSubject(subject)
    }

    suspend fun deleteSubject(subjectId: Long, fallbackSubjectId: Long?) {
        if (fallbackSubjectId != null && fallbackSubjectId != subjectId) {
            lessonDao.reassignLessonsSubject(subjectId, fallbackSubjectId)
        } else {
            val lessons = lessonDao.getLessonsBySubjectOnce(subjectId)
            lessons.forEach { fileStorage.deleteFile(it.internalFilePath) }
            lessons.forEach { lessonDao.deleteLessonById(it.id) }
        }
        subjectDao.deleteSubjectById(subjectId)
    }

    fun resolveLessonFile(relativePath: String?): File? = fileStorage.resolveFile(relativePath)

    fun getShareUri(relativePath: String?) = fileStorage.getShareUri(relativePath)

    suspend fun readStoredText(relativePath: String?): String = fileStorage.readTextFile(relativePath)
}
