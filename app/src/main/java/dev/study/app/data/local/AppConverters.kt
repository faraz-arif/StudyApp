package dev.study.app.data.local

import androidx.room.TypeConverter
import dev.study.app.domain.model.AssessmentType
import dev.study.app.domain.model.ScheduleBlock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AppConverters {
    private val json = Json

    @TypeConverter
    fun fromScheduleBlockList(value: List<ScheduleBlock>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toScheduleBlockList(value: String): List<ScheduleBlock> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromAssessmentType(value: AssessmentType): String {
        return value.name
    }

    @TypeConverter
    fun toAssessmentType(value: String): AssessmentType {
        return try {
            AssessmentType.valueOf(value)
        } catch (e: Exception) {
            AssessmentType.OTHER
        }
    }
}
