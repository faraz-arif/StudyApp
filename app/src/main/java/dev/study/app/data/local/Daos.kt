package dev.study.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE archived = 0 ORDER BY createdAt DESC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY createdAt DESC")
    suspend fun getAllSubjectsRaw(): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: String): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    fun getSubjectFlow(id: String): Flow<SubjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Query("UPDATE subjects SET archived = 1 WHERE id = :id")
    suspend fun archiveSubject(id: String)
}

@Dao
interface AssessmentDao {
    @Query("SELECT * FROM assessments ORDER BY dueAt ASC")
    fun getAllAssessmentsFlow(): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments ORDER BY dueAt ASC")
    suspend fun getAllAssessmentsRaw(): List<AssessmentEntity>

    @Query("SELECT * FROM assessments ORDER BY dueAt ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllAssessmentsPaged(limit: Int, offset: Int): List<AssessmentEntity>

    @Query("SELECT * FROM assessments WHERE subjectId = :subjectId ORDER BY dueAt ASC")
    fun getAssessmentsForSubject(subjectId: String): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE id = :id LIMIT 1")
    suspend fun getAssessmentById(id: String): AssessmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: AssessmentEntity)

    @Query("DELETE FROM assessments WHERE id = :id")
    suspend fun deleteAssessment(id: String)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE subjectId = :subjectId ORDER BY updatedAt DESC")
    fun getNotesForSubject(subjectId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE subjectId = :subjectId ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getNotesForSubjectPaged(subjectId: String, limit: Int, offset: Int): List<NoteEntity>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAllNotesRaw(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: String)
}
