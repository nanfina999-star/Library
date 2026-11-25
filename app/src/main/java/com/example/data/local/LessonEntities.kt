package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: Long,
    val iconKey: String = "book",
    val createdAt: Long = System.currentTimeMillis(),
)

enum class LessonType {
    PDF,
    IMAGE,
    NOTE,
    DOCUMENT,
}

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val description: String = "",
    val type: LessonType,
    val internalFilePath: String? = null,
    val originalFileName: String? = null,
    val mimeType: String? = null,
    val fileSizeBytes: Long = 0L,
    val noteContent: String = "",
    val labels: String = "",
    val pageCount: Int = 0,
    val lastPageRead: Int = 0,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

fun LessonEntity.labelList(): List<String> =
    labels.split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }

fun formatLabelsStorage(labels: List<String>): String =
    labels.map { it.trim().replace(",", "") }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .joinToString(", ")
