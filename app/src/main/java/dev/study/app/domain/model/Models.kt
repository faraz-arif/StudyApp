package dev.study.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Subject(
    val id: String,                              // UUID
    val name: String,
    val colorHex: String,
    val classBlocks: List<ScheduleBlock> = emptyList(),  // optional
    val studyBlocks: List<ScheduleBlock> = emptyList(),  // optional
    val focusModeEnabled: Boolean = false,        // auto-DND during this subject's sessions
    val archived: Boolean = false,
    val createdAt: Long
) {
    fun isValid(): Boolean {
        return classBlocks.isNotEmpty() || studyBlocks.isNotEmpty()
    }
}

@Serializable
data class ScheduleBlock(
    val id: String,
    val dayOfWeek: Int,        // 1 (Mon) .. 7 (Sun)
    val startTime: String,     // Store local time as HH:mm to simplify DB/JSON mapping
    val endTime: String,       // Store local time as HH:mm
    val label: String? = null // e.g. room/building, optional
)

enum class AssessmentType { EXAM, QUIZ, PROJECT, ASSIGNMENT, OTHER }

@Serializable
data class Assessment(
    val id: String,
    val subjectId: String,
    val type: AssessmentType,
    val title: String,
    val dueAt: Long,                                   // epoch millis
    val notes: String? = null,
    val reminderOffsetsMinutes: List<Int> = listOf(1440, 60), // default: 1 day + 1 hour before
    val completed: Boolean = false
)

@Serializable
data class Note(
    val id: String,
    val subjectId: String,
    val title: String,
    val contentJson: String,   // serialized rich-text block model
    val updatedAt: Long
)

// Document Models for Notes Editor
@Serializable
data class RichTextDocument(
    val blocks: List<RichTextBlock>
)

@Serializable
data class RichTextBlock(
    val type: BlockType,
    val text: String,
    val isChecked: Boolean? = null, // for checkboxes
    val level: Int? = null,          // for headers (e.g. 1 or 2)
    val runs: List<TextRun> = emptyList() // inline styles for text span runs
)

enum class BlockType {
    PARAGRAPH,
    HEADING,
    BULLET_ITEM,
    NUMBERED_ITEM,
    CHECKBOX_ITEM
}

@Serializable
data class TextRun(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false
)
