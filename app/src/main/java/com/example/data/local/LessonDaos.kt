package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY id ASC")
    fun observeAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun countSubjects(): Int

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: Long)
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAllLessons(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE subjectId = :subjectId ORDER BY isPinned DESC, updatedAt DESC")
    fun observeLessonsBySubject(subjectId: Long): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE isPinned = 1 ORDER BY updatedAt DESC")
    fun observePinnedLessons(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    fun observeLessonById(lessonId: Long): Flow<LessonEntity?>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    suspend fun getLessonById(lessonId: Long): LessonEntity?

    @Query("SELECT * FROM lessons")
    suspend fun getAllLessonsOnce(): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE subjectId = :subjectId")
    suspend fun getLessonsBySubjectOnce(subjectId: Long): List<LessonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity): Long

    @Update
    suspend fun updateLesson(lesson: LessonEntity)

    @Query("UPDATE lessons SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :lessonId")
    suspend fun setPinned(lessonId: Long, pinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE lessons SET lastPageRead = :page, updatedAt = :updatedAt WHERE id = :lessonId")
    suspend fun updateLastPage(lessonId: Long, page: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE lessons SET noteContent = :noteContent, updatedAt = :updatedAt WHERE id = :lessonId")
    suspend fun updateStudyNotes(lessonId: Long, noteContent: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE lessons SET subjectId = :targetSubjectId WHERE subjectId = :oldSubjectId")
    suspend fun reassignLessonsSubject(oldSubjectId: Long, targetSubjectId: Long)

    @Query("DELETE FROM lessons WHERE id = :lessonId")
    suspend fun deleteLessonById(lessonId: Long)
}
