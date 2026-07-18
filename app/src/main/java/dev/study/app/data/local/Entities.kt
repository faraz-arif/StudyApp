package dev.study.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.study.app.domain.model.Assessment
import dev.study.app.domain.model.AssessmentType
import dev.study.app.domain.model.Note
import dev.study.app.domain.model.ScheduleBlock
import dev.study.app.domain.model.Subject
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String,
    val classBlocks: List<ScheduleBlock>,
    val studyBlocks: List<ScheduleBlock>,
    val focusModeEnabled: Boolean,
    val archived: Boolean,
    val createdAt: Long
) {
    fun toDomain(): Subject = Subject(
        id = id,
        name = name,
        colorHex = colorHex,
        classBlocks = classBlocks,
        studyBlocks = studyBlocks,
        focusModeEnabled = focusModeEnabled,
        archived = archived,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(subject: Subject): SubjectEntity = SubjectEntity(
            id = subject.id,
            name = subject.name,
            colorHex = subject.colorHex,
            classBlocks = subject.classBlocks,
            studyBlocks = subject.studyBlocks,
            focusModeEnabled = subject.focusModeEnabled,
            archived = subject.archived,
            createdAt = subject.createdAt
        )
    }
}

@Serializable
@Entity(tableName = "assessments")
data class AssessmentEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val type: AssessmentType,
    val title: String,
    val dueAt: Long,
    val notes: String?,
    val reminderOffsetsMinutes: List<Int>,
    val completed: Boolean
) {
    fun toDomain(): Assessment = Assessment(
        id = id,
        subjectId = subjectId,
        type = type,
        title = title,
        dueAt = dueAt,
        notes = notes,
        reminderOffsetsMinutes = reminderOffsetsMinutes,
        completed = completed
    )

    companion object {
        fun fromDomain(assessment: Assessment): AssessmentEntity = AssessmentEntity(
            id = assessment.id,
            subjectId = assessment.subjectId,
            type = assessment.type,
            title = assessment.title,
            dueAt = assessment.dueAt,
            notes = assessment.notes,
            reminderOffsetsMinutes = assessment.reminderOffsetsMinutes,
            completed = assessment.completed
        )
    }
}

@Serializable
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val title: String,
    val contentJson: String,
    val updatedAt: Long
) {
    fun toDomain(): Note = Note(
        id = id,
        subjectId = subjectId,
        title = title,
        contentJson = contentJson,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(note: Note): NoteEntity = NoteEntity(
            id = note.id,
            subjectId = note.subjectId,
            title = note.title,
            contentJson = note.contentJson,
            updatedAt = note.updatedAt
        )
    }
}

